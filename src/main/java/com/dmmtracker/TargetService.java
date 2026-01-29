/*
 * Copyright (c) 2026, DMMScape
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * DMMScape Companion - Target Service
 * Fetches and parses target list from webapp for overlay rendering
 */
package com.dmmtracker;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class TargetService
{
	private static final Logger log = LoggerFactory.getLogger(TargetService.class);
	private static final int DEFAULT_POLL_INTERVAL_SECONDS = 5;
	private static final int MIN_POLL_INTERVAL_SECONDS = 5;
	private static final int MAX_POLL_INTERVAL_SECONDS = 300;
	private static final int MAX_BACKOFF_MULTIPLIER = 16;
	private static final double POLL_JITTER_RATIO = 0.15;
	private static final String TARGETS_FILENAME = "dmm-targets.json";

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Gson gson;

	@Inject
	private DMMTrackerConfig config;

	@Inject
	private ScheduledExecutorService executor;

	private List<TargetPoint> targets = new ArrayList<>();

	private TargetPoint nextTarget = null;

	private long lastFetchTimestamp = 0;

	private long lastLocalUpdateTimestamp = 0;

	private String activeListId = null;

	private String planName = null;

	private long lastSuccessTimestamp = 0;

	private String lastErrorMessage = null;

	private long lastErrorTimestamp = 0;
	private ScheduledFuture<?> pollingTask;
	private volatile String authHeader = null;
	private volatile boolean fetchInProgress = false;
	private volatile long lastFetchAttemptTimestamp = 0;
	private volatile TargetPoint focusTarget = null;
	private volatile String focusTargetKey = null;
	private volatile long lastFocusUpdateTimestamp = 0;
	private volatile int desiredPollIntervalSeconds = DEFAULT_POLL_INTERVAL_SECONDS;
	private volatile int currentEffectivePollIntervalSeconds = DEFAULT_POLL_INTERVAL_SECONDS;
	private volatile int currentScheduledPollIntervalSeconds = DEFAULT_POLL_INTERVAL_SECONDS;
	private volatile int backoffMultiplier = 1;
	private volatile String targetsEtag = null;

	/**
	 * Starts periodic polling for target updates.
	 */
	public void startPolling()
	{
		if (pollingTask != null && !pollingTask.isCancelled())
		{
			return;
		}

		desiredPollIntervalSeconds = normalizePollInterval(config.targetPollInterval());
		backoffMultiplier = 1;
		schedulePolling(computeEffectivePollIntervalSeconds());
		log.info("Target service started polling with interval: {}s", currentScheduledPollIntervalSeconds);
	}

	/**
	 * Restarts polling with the latest configured interval.
	 * Keeps the current target state intact.
	 */
	public void restartPolling()
	{
		if (pollingTask != null)
		{
			pollingTask.cancel(false);
			pollingTask = null;
		}

		startPolling();
	}

	public List<TargetPoint> getTargets()
	{
		return targets;
	}

	public TargetPoint getNextTarget()
	{
		TargetPoint focus = focusTarget;
		if (focus != null && focus.getWorldPoint() != null)
		{
			return focus;
		}
		return nextTarget;
	}

	public long getLastFetchTimestamp()
	{
		return lastFetchTimestamp;
	}

	public long getLastLocalUpdateTimestamp()
	{
		return lastLocalUpdateTimestamp;
	}

	public String getActiveListId()
	{
		return activeListId;
	}

	public String getPlanName()
	{
		return planName;
	}

	public long getLastSuccessTimestamp()
	{
		return lastSuccessTimestamp;
	}

	public String getLastErrorMessage()
	{
		return lastErrorMessage;
	}

	public long getLastErrorTimestamp()
	{
		return lastErrorTimestamp;
	}

	public boolean isFetching()
	{
		return fetchInProgress;
	}

	public long getLastFetchAttemptTimestamp()
	{
		return lastFetchAttemptTimestamp;
	}

	/**
	 * Reschedules polling with the current config interval.
	 * Does not clear data - just restarts the polling task.
	 */
	public void reschedulePolling()
	{
		if (pollingTask == null || pollingTask.isCancelled())
		{
			return;
		}

		desiredPollIntervalSeconds = normalizePollInterval(config.targetPollInterval());
		backoffMultiplier = 1;
		reschedulePollingIfNeeded(true);
	}

	public void setDesiredPollIntervalSeconds(int pollIntervalSeconds)
	{
		int normalized = normalizePollInterval(pollIntervalSeconds);
		if (normalized == desiredPollIntervalSeconds)
		{
			return;
		}
		desiredPollIntervalSeconds = normalized;
		reschedulePollingIfNeeded(false);
	}

	/**
	 * Stops the polling task.
	 */
	public void stopPolling()
	{
		if (pollingTask != null)
		{
			pollingTask.cancel(false);
			pollingTask = null;
		}

		fetchInProgress = false;
		lastFetchAttemptTimestamp = 0;
		targets.clear();
		nextTarget = null;
		lastFetchTimestamp = 0;
		lastLocalUpdateTimestamp = 0;
		activeListId = null;
		planName = null;
		backoffMultiplier = 1;
		currentEffectivePollIntervalSeconds = desiredPollIntervalSeconds;
		currentScheduledPollIntervalSeconds = desiredPollIntervalSeconds;
		targetsEtag = null;
		clearFocusTarget();

		log.info("Target service stopped");
	}

	/**
	 * Fetches targets from configured source.
	 * Supports both URL-based and file-based sync.
	 */
	private void fetchTargets()
	{
		lastFetchAttemptTimestamp = System.currentTimeMillis();
		fetchInProgress = true;
		lastErrorMessage = null;
		lastErrorTimestamp = 0;
		try
		{
			String targetUrl = config.targetSyncUrl();

			// Try URL-based sync first
			if (targetUrl != null && !targetUrl.isEmpty())
			{
				fetchFromUrl(targetUrl);
				return;
			}

			// Fall back to file-based sync
			fetchFromFile();
		}
		finally
		{
			fetchInProgress = false;
		}
	}

	/**
	 * Immediately fetch targets in the background.
	 */
	public void refreshNow()
	{
		executor.submit(this::fetchTargets);
	}

	public void setAuthHeader(String authHeader)
	{
		if ((this.authHeader == null && authHeader != null)
			|| (this.authHeader != null && !this.authHeader.equals(authHeader)))
		{
			targetsEtag = null;
		}
		this.authHeader = authHeader;
	}

	private int normalizePollInterval(int pollInterval)
	{
		int interval = pollInterval;
		if (interval <= 0)
		{
			interval = DEFAULT_POLL_INTERVAL_SECONDS;
		}
		return Math.max(MIN_POLL_INTERVAL_SECONDS, Math.min(MAX_POLL_INTERVAL_SECONDS, interval));
	}

	private int computeEffectivePollIntervalSeconds()
	{
		int base = desiredPollIntervalSeconds > 0
			? desiredPollIntervalSeconds
			: DEFAULT_POLL_INTERVAL_SECONDS;
		long effective = (long) base * Math.max(1, backoffMultiplier);
		if (effective > MAX_POLL_INTERVAL_SECONDS)
		{
			effective = MAX_POLL_INTERVAL_SECONDS;
		}
		return normalizePollInterval((int) effective);
	}

	private int applyJitter(int intervalSeconds)
	{
		if (intervalSeconds <= 1)
		{
			return intervalSeconds;
		}
		double jitter = (Math.random() * 2.0 - 1.0) * POLL_JITTER_RATIO;
		int jittered = (int) Math.round(intervalSeconds * (1.0 + jitter));
		return normalizePollInterval(jittered);
	}

	private void schedulePolling(int effectiveIntervalSeconds)
	{
		int jittered = applyJitter(effectiveIntervalSeconds);
		currentEffectivePollIntervalSeconds = effectiveIntervalSeconds;
		currentScheduledPollIntervalSeconds = jittered;
		pollingTask = executor.scheduleAtFixedRate(
			this::fetchTargets,
			0,
			jittered,
			TimeUnit.SECONDS
		);
	}

	private void reschedulePollingIfNeeded(boolean force)
	{
		if (pollingTask == null || pollingTask.isCancelled())
		{
			return;
		}

		int effectiveInterval = computeEffectivePollIntervalSeconds();
		if (!force && effectiveInterval == currentEffectivePollIntervalSeconds)
		{
			return;
		}

		pollingTask.cancel(false);
		pollingTask = null;
		schedulePolling(effectiveInterval);
		log.info("Target service rescheduled with interval: {}s", currentScheduledPollIntervalSeconds);
	}

	private void registerFailure()
	{
		int nextMultiplier = Math.min(backoffMultiplier * 2, MAX_BACKOFF_MULTIPLIER);
		if (nextMultiplier != backoffMultiplier)
		{
			backoffMultiplier = nextMultiplier;
			reschedulePollingIfNeeded(true);
		}
	}

	private void registerSuccess()
	{
		if (backoffMultiplier != 1)
		{
			backoffMultiplier = 1;
			reschedulePollingIfNeeded(true);
		}
	}

	/**
	 * Fetches targets from a URL endpoint.
	 */
	private void fetchFromUrl(String url)
	{
		try
		{
			Request.Builder builder = new Request.Builder()
				.url(url)
				.header("User-Agent", "DMMScape-Companion/1.0");

			if (targetsEtag != null && !targetsEtag.isEmpty())
			{
				builder.header("If-None-Match", targetsEtag);
			}

			if (authHeader != null && !authHeader.isEmpty())
			{
				if (authHeader.startsWith("Bearer "))
				{
					builder.header("Authorization", authHeader);
				}
				else
				{
					builder.header("X-Api-Key", authHeader);
				}
			}
			else
			{
				log.debug("Target sync URL set but auth is missing; request may be rejected");
			}

			Request request = builder.build();

			try (Response response = httpClient.newCall(request).execute())
			{
				String responseEtag = response.header("ETag");
				if (responseEtag != null && !responseEtag.isEmpty())
				{
					targetsEtag = responseEtag;
				}

				if (response.code() == 304)
				{
					lastSuccessTimestamp = System.currentTimeMillis();
					lastErrorMessage = null;
					registerSuccess();
					return;
				}

				if (!response.isSuccessful() || response.body() == null)
				{
					log.debug("Failed to fetch targets from URL: {}", response.code());
					lastErrorMessage = "Target sync failed (" + response.code() + ")";
					lastErrorTimestamp = System.currentTimeMillis();
					registerFailure();
					return;
				}

				String json = response.body().string();
				if (parseTargets(json))
				{
					registerSuccess();
				}
				else
				{
					registerFailure();
				}
			}
		}
		catch (Exception e)
		{
			log.debug("Error fetching targets from URL: {}", e.getMessage());
			lastErrorMessage = "Target sync error";
			lastErrorTimestamp = System.currentTimeMillis();
			registerFailure();
		}
	}

	/**
	 * Fetches targets from a local file in the RuneLite directory.
	 */
	private void fetchFromFile()
	{
		try
		{
			Path targetsFile = RuneLite.RUNELITE_DIR.toPath().resolve(TARGETS_FILENAME);

			if (!Files.exists(targetsFile))
			{
				return;
			}

			long lastModified = Files.getLastModifiedTime(targetsFile).toMillis();
			if (lastModified <= lastFetchTimestamp)
			{
				return;
			}

			String json = Files.readString(targetsFile);
			if (parseTargets(json))
			{
				registerSuccess();
			}

			log.debug("Loaded targets from file: {}", targetsFile);
		}
		catch (Exception e)
		{
			log.debug("Error reading targets from file: {}", e.getMessage());
			lastErrorMessage = "Target file error";
			lastErrorTimestamp = System.currentTimeMillis();
		}
	}

	/**
	 * Parses the JSON target data and updates the target list.
	 */
	private boolean parseTargets(String json)
	{
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);

			if (root == null || !root.has("timestamp") || !root.has("targets"))
			{
				log.debug("Invalid targets JSON format");
				lastErrorMessage = "Invalid target payload";
				lastErrorTimestamp = System.currentTimeMillis();
				return false;
			}

			long timestamp = root.get("timestamp").getAsLong();
			if (timestamp <= lastFetchTimestamp)
			{
				return true;
			}
			lastFetchTimestamp = timestamp;
			activeListId = getStringOrDefault(root, "activeListId", activeListId);
			planName = getStringOrDefault(root, "planName", planName);

			List<TargetPoint> newTargets = new ArrayList<>();
			JsonArray targetsArray = root.getAsJsonArray("targets");

			for (JsonElement element : targetsArray)
			{
				JsonObject t = element.getAsJsonObject();

				TargetPoint point = new TargetPoint();
				point.setId(getStringOrDefault(t, "id", ""));
				point.setType(getStringOrDefault(t, "type", ""));
				point.setName(getStringOrDefault(t, "name", ""));
				point.setCategory(getStringOrDefault(t, "category", ""));
				point.setCompleted(getBooleanOrDefault(t, "completed", false));
				String completionSource = getStringOrDefault(t, "completionSource", "");
				if (completionSource.isEmpty() && point.isCompleted())
				{
					completionSource = "custom";
				}
				point.setCompletionSource(completionSource.isEmpty() ? "none" : completionSource);
				point.setToggleable(getBooleanOrDefault(t, "canToggle", true));
				point.setOrder(getIntOrDefault(t, "order", 0));

				if (t.has("location") && !t.get("location").isJsonNull())
				{
					JsonObject loc = t.getAsJsonObject("location");
					point.setWorldPoint(new WorldPoint(
						getIntOrDefault(loc, "x", 0),
						getIntOrDefault(loc, "y", 0),
						getIntOrDefault(loc, "z", 0)
					));
				}

				newTargets.add(point);
			}

			targets = newTargets;
			updateNextTarget();
			lastSuccessTimestamp = System.currentTimeMillis();
			lastErrorMessage = null;

			log.debug("Parsed {} targets, next target: {}",
				targets.size(),
				nextTarget != null ? nextTarget.getName() : "none");
			return true;
		}
		catch (Exception e)
		{
			log.debug("Failed to parse targets JSON: {}", e.getMessage());
			lastErrorMessage = "Target parse error";
			lastErrorTimestamp = System.currentTimeMillis();
			return false;
		}
	}

	/**
	 * Updates the next target to the first incomplete target with coordinates.
	 */
	private void updateNextTarget()
	{
		nextTarget = targets.stream()
			.filter(t -> !t.isCompleted())
			.filter(t -> t.getWorldPoint() != null)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Marks a target as completed locally.
	 * Note: This doesn't persist to the webapp.
	 */
	public void markCompleted(String targetId)
	{
		targets.stream()
			.filter(t -> t.getId().equals(targetId))
			.findFirst()
			.ifPresent(t ->
			{
				t.setCompleted(true);
				t.setCompletionSource("custom");
				t.setToggleable(true);
				updateNextTarget();
				lastLocalUpdateTimestamp = System.currentTimeMillis();
			});
	}

	/**
	 * Updates completion state for a target locally.
	 */
	public void setTargetCompletion(String targetId, boolean completed)
	{
		targets.stream()
			.filter(t -> t.getId().equals(targetId))
			.findFirst()
			.ifPresent(t ->
			{
				t.setCompleted(completed);
				t.setCompletionSource(completed ? "custom" : "none");
				t.setToggleable(true);
				updateNextTarget();
				lastLocalUpdateTimestamp = System.currentTimeMillis();
			});
	}

	/**
	 * Gets the distance to the next target from a given world point.
	 */
	public int getDistanceToNextTarget(WorldPoint from)
	{
		if (from == null)
		{
			return Integer.MAX_VALUE;
		}

		TargetPoint focus = focusTarget;
		if (focus != null && focus.getWorldPoint() != null)
		{
			return from.distanceTo(focus.getWorldPoint());
		}

		if (nextTarget == null || nextTarget.getWorldPoint() == null)
		{
			return Integer.MAX_VALUE;
		}

		return from.distanceTo(nextTarget.getWorldPoint());
	}

	/**
	 * Gets filtered targets with coordinates for map/overlay rendering.
	 */
	public List<TargetPoint> getFilteredTargetsWithCoords(DMMTrackerConfig config)
	{
		TargetPoint focus = focusTarget;
		if (focus != null && focus.getWorldPoint() != null)
		{
			List<TargetPoint> focusList = new ArrayList<>();
			focusList.add(focus);
			return focusList;
		}

		boolean showCompleted = config != null && config.mapShowCompleted();
		return targets.stream()
			.filter(t -> t.getWorldPoint() != null)
			.filter(t -> showCompleted || !t.isCompleted())
			.filter(t -> isTargetTypeVisible(t, config))
			.collect(Collectors.toList());
	}

	private boolean isTargetTypeVisible(TargetPoint target, DMMTrackerConfig config)
	{
		if (config == null || target == null)
		{
			return true;
		}

		String type = target.getType();
		if (type == null || type.isEmpty())
		{
			return true;
		}

		switch (type.toLowerCase())
		{
			case "diary":
				return config.mapShowDiaryTargets();
			case "quest":
			case "quest-step":
			case "queststep":
				return config.mapShowQuestTargets();
			case "boss":
				return config.mapShowBossTargets();
			case "ca":
			case "combat":
			case "combat-achievement":
			case "combatachievement":
				return config.mapShowCaTargets();
			case "sigil":
				return config.mapShowSigilTargets();
			case "lamp":
				return config.mapShowLampTargets();
			case "prayer":
				return config.mapShowPrayerTargets();
			case "teleport":
				return config.mapShowTeleportTargets();
			case "custom":
				return config.mapShowCustomTargets();
			default:
				return true;
		}
	}

	/**
	 * Gets all incomplete targets with coordinates.
	 */
	public List<TargetPoint> getIncompleteTargetsWithCoords()
	{
		TargetPoint focus = focusTarget;
		if (focus != null && focus.getWorldPoint() != null)
		{
			List<TargetPoint> focusList = new ArrayList<>();
			focusList.add(focus);
			return focusList;
		}
		return targets.stream()
			.filter(t -> !t.isCompleted())
			.filter(t -> t.getWorldPoint() != null)
			.collect(Collectors.toList());
	}

	public void setFocusTarget(String key, TargetPoint target)
	{
		focusTargetKey = key;
		focusTarget = target;
		lastFocusUpdateTimestamp = System.currentTimeMillis();
	}

	public void clearFocusTarget()
	{
		focusTargetKey = null;
		focusTarget = null;
		lastFocusUpdateTimestamp = System.currentTimeMillis();
	}

	public boolean isFocusTarget(String key)
	{
		return key != null && key.equals(focusTargetKey);
	}

	public TargetPoint getFocusTarget()
	{
		return focusTarget;
	}

	public long getLastFocusUpdateTimestamp()
	{
		return lastFocusUpdateTimestamp;
	}

	/**
	 * Returns a snapshot copy of the current targets list.
	 */
	public List<TargetPoint> getTargetsSnapshot()
	{
		return new ArrayList<>(targets);
	}

	// JSON helper methods
	private String getStringOrDefault(JsonObject obj, String key, String defaultValue)
	{
		return obj.has(key) && !obj.get(key).isJsonNull()
			? obj.get(key).getAsString()
			: defaultValue;
	}

	private boolean getBooleanOrDefault(JsonObject obj, String key, boolean defaultValue)
	{
		return obj.has(key) && !obj.get(key).isJsonNull()
			? obj.get(key).getAsBoolean()
			: defaultValue;
	}

	private int getIntOrDefault(JsonObject obj, String key, int defaultValue)
	{
		return obj.has(key) && !obj.get(key).isJsonNull()
			? obj.get(key).getAsInt()
			: defaultValue;
	}
}
