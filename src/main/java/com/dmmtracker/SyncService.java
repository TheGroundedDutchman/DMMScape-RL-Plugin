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
 * Service for syncing data to the web app
 */
package com.dmmtracker;

import com.dmmtracker.data.LocationData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.runelite.client.util.LinkBrowser;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

@Singleton
public class SyncService
{
	private static final Logger log = LoggerFactory.getLogger(SyncService.class);
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final int TIMEOUT_SECONDS = 10;
	private static final int MAX_RECENT_EVENTS = 25;
	private static final int MAX_LABEL_LENGTH = 80;

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final Deque<SyncEvent> recentEvents = new ArrayDeque<>();

	private volatile long lastSyncAttemptMs = 0;

	private volatile long lastSyncSuccessMs = 0;

	private volatile String lastSyncError = null;

	private volatile int lastSyncStatusCode = 0;

	private volatile long lastTargetUpdateAttemptMs = 0;

	private volatile long lastTargetUpdateSuccessMs = 0;

	private volatile String lastTargetUpdateError = null;

	private volatile int lastTargetUpdateStatusCode = 0;

	private volatile long lastProgressFetchAttemptMs = 0;
	private volatile long lastProgressFetchSuccessMs = 0;
	private volatile String lastProgressFetchError = null;
	private volatile boolean progressFetchInFlight = false;
	private volatile String progressEtag = null;

	public enum SyncStatus
	{
		PENDING,
		SYNCED,
		FAILED
	}

	public static class SyncEvent
	{
		private final String label;
		private final long createdAt;
		private volatile SyncStatus status;

		private SyncEvent(String label, long createdAt)
		{
			this.label = label;
			this.createdAt = createdAt;
			this.status = SyncStatus.PENDING;
		}

		void setStatus(SyncStatus status)
		{
			this.status = status;
		}

		public String getLabel()
		{
			return label;
		}

		public long getCreatedAt()
		{
			return createdAt;
		}

		public SyncStatus getStatus()
		{
			return status;
		}
	}

	@Inject
	public SyncService(OkHttpClient httpClient)
	{
		this.httpClient = httpClient.newBuilder()
			.connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.build();

		this.gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();
	}

	public long getLastSyncAttemptMs()
	{
		return lastSyncAttemptMs;
	}

	public long getLastSyncSuccessMs()
	{
		return lastSyncSuccessMs;
	}

	public String getLastSyncError()
	{
		return lastSyncError;
	}

	public int getLastSyncStatusCode()
	{
		return lastSyncStatusCode;
	}

	public long getLastTargetUpdateAttemptMs()
	{
		return lastTargetUpdateAttemptMs;
	}

	public long getLastTargetUpdateSuccessMs()
	{
		return lastTargetUpdateSuccessMs;
	}

	public long getLastProgressFetchAttemptMs()
	{
		return lastProgressFetchAttemptMs;
	}

	public long getLastProgressFetchSuccessMs()
	{
		return lastProgressFetchSuccessMs;
	}

	public String getLastProgressFetchError()
	{
		return lastProgressFetchError;
	}

	public boolean isProgressFetchInFlight()
	{
		return progressFetchInFlight;
	}

	public void clearProgressEtag()
	{
		progressEtag = null;
	}

	public String getLastTargetUpdateError()
	{
		return lastTargetUpdateError;
	}

	public int getLastTargetUpdateStatusCode()
	{
		return lastTargetUpdateStatusCode;
	}

	/**
	 * Sends sync data to the configured webhook URL.
	 */
	public void sendSync(SyncData data, String webhookUrl, String apiKey)
	{
		sendSync(data, webhookUrl, apiKey, null, null);
	}

	/**
	 * Sends sync data with an optional callback for status updates.
	 */
	public void sendSync(SyncData data, String webhookUrl, String apiKey, SyncCallback callback)
	{
		sendSync(data, webhookUrl, apiKey, callback, null);
	}

	/**
	 * Sends sync data with an optional callback and response handler.
	 */
	public void sendSync(SyncData data, String webhookUrl, String apiKey, SyncCallback callback, SyncResponseHandler responseHandler)
	{
		if (webhookUrl == null || webhookUrl.isEmpty())
		{
			log.debug("No webhook URL configured, skipping sync");
			if (callback != null)
			{
				callback.onError("Sync URL not configured");
			}
			return;
		}

		List<SyncEvent> events = recordEvents(buildEventLabels(data));
		String json = gson.toJson(data);
		log.debug("Sending sync: {}", json);

		lastSyncAttemptMs = System.currentTimeMillis();
		lastSyncStatusCode = 0;

		RequestBody body = RequestBody.create(JSON, json);
		Request.Builder builder = new Request.Builder()
			.url(webhookUrl)
			.post(body)
			.header("Content-Type", "application/json")
			.header("User-Agent", "DMMScape-Companion/1.0");

		// Support both Bearer token (device code flow) and API key
		if (apiKey != null && !apiKey.isEmpty())
		{
			if (apiKey.startsWith("Bearer "))
			{
				builder.header("Authorization", apiKey);
			}
			else
			{
				builder.header("X-Api-Key", apiKey);
			}
		}
		else
		{
			log.warn("No authentication configured; sync may be rejected");
		}

		Request request = builder.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to send sync data", e);
				lastSyncError = e.getMessage();
				updateEventStatuses(events, SyncStatus.FAILED);
				if (callback != null)
				{
					callback.onError("Network error");
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					lastSyncStatusCode = response.code();
					String bodyText = responseBody != null ? responseBody.string() : "";

					if (response.isSuccessful())
					{
						log.debug("Sync successful");
						lastSyncSuccessMs = System.currentTimeMillis();
						lastSyncError = null;
						updateEventStatuses(events, SyncStatus.SYNCED);
						if (responseHandler != null && bodyText != null && !bodyText.isBlank())
						{
							try
							{
								JsonObject jsonBody = new JsonParser().parse(bodyText).getAsJsonObject();
								responseHandler.onResponse(jsonBody);
							}
							catch (Exception e)
							{
								log.debug("Sync response parsing failed: {}", e.getMessage());
							}
						}
						if (callback != null)
						{
							callback.onSuccess();
						}
					}
					else
					{
						log.warn("Sync failed with status {}: {}",
							response.code(),
							bodyText.isBlank() ? "no body" : bodyText);
						lastSyncError = "HTTP " + response.code();
						updateEventStatuses(events, SyncStatus.FAILED);
						if (callback != null)
						{
							callback.onError("HTTP " + response.code());
						}
					}
				}
			}
		});
	}

	public synchronized SyncEvent recordEvent(String label)
	{
		if (label == null || label.isEmpty())
		{
			return null;
		}

		SyncEvent event = new SyncEvent(truncateLabel(label), System.currentTimeMillis());
		recentEvents.addFirst(event);
		trimRecentEvents();
		return event;
	}

	public synchronized List<SyncEvent> getRecentEvents()
	{
		return new ArrayList<>(recentEvents);
	}

	private synchronized void updateEventStatuses(List<SyncEvent> events, SyncStatus status)
	{
		if (events == null || events.isEmpty())
		{
			return;
		}

		for (SyncEvent event : events)
		{
			if (event != null)
			{
				event.setStatus(status);
			}
		}
	}

	private synchronized List<SyncEvent> recordEvents(List<String> labels)
	{
		List<SyncEvent> events = new ArrayList<>();
		if (labels == null || labels.isEmpty())
		{
			return events;
		}

		long now = System.currentTimeMillis();
		for (String label : labels)
		{
			if (label == null || label.isEmpty())
			{
				continue;
			}
			SyncEvent event = new SyncEvent(truncateLabel(label), now);
			events.add(event);
			recentEvents.addFirst(event);
		}

		trimRecentEvents();
		return events;
	}

	private void trimRecentEvents()
	{
		while (recentEvents.size() > MAX_RECENT_EVENTS)
		{
			recentEvents.removeLast();
		}
	}

	private List<String> buildEventLabels(SyncData data)
	{
		List<String> labels = new ArrayList<>();
		if (data == null)
		{
			return labels;
		}

		if (data.isFullSync())
		{
			labels.add("Full sync");
			return labels;
		}

		if (data.getDiaryTaskUpdates() != null)
		{
			for (DiaryTaskUpdate update : data.getDiaryTaskUpdates())
			{
				if (update == null || !update.isCompleted())
				{
					continue;
				}
				String diary = update.getDiary() != null ? update.getDiary() : "Diary";
				String tier = update.getTier() != null ? update.getTier() : "";
				String text = update.getText() != null ? update.getText() : "Task completed";
				labels.add(String.format("Diary: %s %s - %s", diary, tier, text).trim());
			}
		}

		if (data.getCompletedDiaryTasks() != null && !data.getCompletedDiaryTasks().isEmpty())
		{
			int count = 0;
			for (String taskId : data.getCompletedDiaryTasks())
			{
				if (taskId == null || taskId.isEmpty())
				{
					continue;
				}
				labels.add("Diary task: " + taskId);
				count++;
				if (count >= 3)
				{
					break;
				}
			}
			int remaining = data.getCompletedDiaryTasks().size() - count;
			if (remaining > 0)
			{
				labels.add("Diary task: +" + remaining + " more");
			}
		}

		if (data.getCompletedQuests() != null && !data.getCompletedQuests().isEmpty())
		{
			int count = 0;
			for (String quest : data.getCompletedQuests())
			{
				if (quest == null || quest.isEmpty())
				{
					continue;
				}
				labels.add("Quest: " + quest);
				count++;
				if (count >= 3)
				{
					break;
				}
			}
			int remaining = data.getCompletedQuests().size() - count;
			if (remaining > 0)
			{
				labels.add("Quest: +" + remaining + " more");
			}
		}

		if (data.getCompletedCAs() != null && !data.getCompletedCAs().isEmpty())
		{
			labels.add("Combat achievements +" + data.getCompletedCAs().size());
		}

		if (data.getOwnedSigils() != null && !data.getOwnedSigils().isEmpty())
		{
			labels.add("Sigils owned +" + data.getOwnedSigils().size());
		}

		if (data.getOwnedLamps() != null && !data.getOwnedLamps().isEmpty())
		{
			labels.add("Quest lamps owned +" + data.getOwnedLamps().size());
		}

		if (data.getOwnedPrayers() != null && !data.getOwnedPrayers().isEmpty())
		{
			labels.add("Prayers owned +" + data.getOwnedPrayers().size());
		}

		if (data.getBossKills() != null && !data.getBossKills().isEmpty())
		{
			for (Map.Entry<String, Integer> entry : data.getBossKills().entrySet())
			{
				String name = entry.getKey();
				Integer count = entry.getValue();
				if (name == null || name.isEmpty() || count == null)
				{
					continue;
				}
				labels.add("Boss KC: " + name + " " + count);
			}
		}

		if (data.getClueCompletions() != null && !data.getClueCompletions().isEmpty())
		{
			for (Map.Entry<String, Integer> entry : data.getClueCompletions().entrySet())
			{
				String tier = entry.getKey();
				Integer count = entry.getValue();
				if (tier == null || tier.isEmpty() || count == null)
				{
					continue;
				}
				labels.add("Clues: " + tier + " " + count);
			}
		}

		if (data.getCollectionLogCount() > 0)
		{
			labels.add("Collection log: " + data.getCollectionLogCount());
		}

		if ((data.getCompletedQuests() == null || data.getCompletedQuests().isEmpty())
			&& data.getQuestPoints() > 0)
		{
			labels.add("Quest points: " + data.getQuestPoints());
		}

		if (labels.isEmpty() && data.getDiaryProgress() != null && !data.getDiaryProgress().isEmpty())
		{
			labels.add("Diary tiers updated");
		}

		return labels;
	}

	private String truncateLabel(String label)
	{
		if (label.length() <= MAX_LABEL_LENGTH)
		{
			return label;
		}
		return label.substring(0, MAX_LABEL_LENGTH - 3) + "...";
	}

	/**
	 * Sends target completion updates to the web app.
	 */
	public void sendTargetUpdates(List<TargetUpdate> updates, String targetUrl, String apiKey, String activeListId, SyncCallback callback)
	{
		if (targetUrl == null || targetUrl.isEmpty())
		{
			if (callback != null)
			{
				callback.onError("Target URL not configured");
			}
			return;
		}

		TargetUpdatePayload payload = new TargetUpdatePayload();
		payload.activeListId = activeListId;
		payload.updates = updates;

		String json = gson.toJson(payload);
		lastTargetUpdateAttemptMs = System.currentTimeMillis();
		lastTargetUpdateStatusCode = 0;

		RequestBody body = RequestBody.create(JSON, json);
		Request.Builder builder = new Request.Builder()
			.url(targetUrl)
			.patch(body)
			.header("Content-Type", "application/json")
			.header("User-Agent", "DMMScape-Companion/1.0");

		// Support both Bearer token (device code flow) and API key
		if (apiKey != null && !apiKey.isEmpty())
		{
			if (apiKey.startsWith("Bearer "))
			{
				builder.header("Authorization", apiKey);
			}
			else
			{
				builder.header("X-Api-Key", apiKey);
			}
		}
		else
		{
			log.warn("No authentication configured; target updates may be rejected");
		}

		Request request = builder.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to send target updates", e);
				lastTargetUpdateError = e.getMessage();
				if (callback != null)
				{
					callback.onError("Network error");
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					lastTargetUpdateStatusCode = response.code();
					if (response.isSuccessful())
					{
						log.debug("Target updates successful");
						lastTargetUpdateSuccessMs = System.currentTimeMillis();
						lastTargetUpdateError = null;
						if (callback != null)
						{
							callback.onSuccess();
						}
					}
					else
					{
						String bodyText = responseBody != null ? responseBody.string() : "no body";
						log.warn("Target update failed with status {}: {}",
							response.code(),
							bodyText);
						lastTargetUpdateError = "HTTP " + response.code();
						if (callback != null)
						{
							callback.onError("HTTP " + response.code());
						}
					}
				}
			}
		});
	}

	/**
	 * Generates JSON export for manual copy/paste.
	 */
	public String generateExportJson(SyncData data)
	{
		return gson.toJson(data);
	}

	/**
	 * Generates a compact export format compatible with the web app import.
	 */
	public String generateWebAppExport(SyncData data)
	{
		// Convert to web app format
		WebAppExport export = new WebAppExport();
		export.username = data.getUsername();
		export.profileType = data.getProfileType();
		export.timestamp = data.getTimestamp();

		WebAppExport.DataPayload payload = new WebAppExport.DataPayload();

		// Convert CA IDs
		if (data.getCompletedCAs() != null)
		{
			payload.combatAchievements = new WebAppExport.CAData();
			payload.combatAchievements.completed = data.getCompletedCAs().stream()
				.mapToInt(Integer::intValue)
				.toArray();
		}

		// Boss kills
		payload.bossKills = data.getBossKills();

		// Diary progress
		payload.diaryTiers = data.getDiaryProgress();

		// Quest points
		if (data.getQuestPoints() > 0)
		{
			payload.quests = new WebAppExport.QuestData();
			payload.quests.points = data.getQuestPoints();
		}

		if (data.getCollectionLogCount() > 0)
		{
			payload.collectionLogCount = data.getCollectionLogCount();
		}

		export.data = payload;

		return gson.toJson(export);
	}

	/**
	 * Export format compatible with the web app import feature.
	 */
	private static class WebAppExport
	{
		String username;
		String profileType;
		long timestamp;
		DataPayload data;

		static class DataPayload
		{
			CAData combatAchievements;
			java.util.Map<String, Integer> bossKills;
			java.util.Map<String, java.util.Map<String, Boolean>> diaryTiers;
			QuestData quests;
			Integer collectionLogCount;
		}

		static class CAData
		{
			int[] completed;
		}

		static class QuestData
		{
			int points;
		}
	}

	public interface SyncCallback
	{
		void onSuccess();
		void onError(String message);
	}

	public interface SyncResponseHandler
	{
		void onResponse(JsonObject response);
	}

	public static class TargetUpdate
	{
		public final String id;
		public final String type;
		public final boolean completed;

		public TargetUpdate(String id, String type, boolean completed)
		{
			this.id = id;
			this.type = type;
			this.completed = completed;
		}
	}

	private static class TargetUpdatePayload
	{
		String activeListId;
		List<TargetUpdate> updates;
	}

	// ==================== ADD-TO-PLAN API ====================

	private static final String API_BASE_URL = ApiConfig.apiBase();

	/**
	 * Add an item to the user's plan via API.
	 * @param type The item type (boss, diary, ca, sigil, lamp, prayer, quest)
	 * @param id The item ID
	 * @param name The item name
	 * @param location Optional location data
	 * @param points Optional points value
	 * @param category Optional category
	 * @param tier Optional tier
	 * @param apiKey API key for authentication
	 * @param callback Callback for success/error notification
	 */
	public void addToPlan(
		String type,
		String id,
		String name,
		LocationData location,
		Integer points,
		String category,
		String tier,
		String apiKey,
		SyncCallback callback
	)
	{
		addToPlanInternal(type, id, name, location, points, category, tier, apiKey, API_BASE_URL, callback);
	}

	public void addToPlan(
		String type,
		String id,
		String name,
		LocationData location,
		Integer points,
		String category,
		String tier,
		String authHeader,
		String sourceUrl,
		SyncCallback callback
	)
	{
		String baseUrl = deriveApiBaseUrl(sourceUrl);
		addToPlanInternal(type, id, name, location, points, category, tier, authHeader, baseUrl, callback);
	}

	private void addToPlanInternal(
		String type,
		String id,
		String name,
		LocationData location,
		Integer points,
		String category,
		String tier,
		String authHeader,
		String baseUrl,
		SyncCallback callback
	)
	{
		if (authHeader == null || authHeader.isEmpty())
		{
			if (callback != null)
			{
				callback.onError("API key not configured");
			}
			return;
		}

		JsonObject payload = new JsonObject();
		payload.addProperty("type", type);
		payload.addProperty("id", id);
		payload.addProperty("name", name);

		if (location != null)
		{
			JsonObject loc = new JsonObject();
			loc.addProperty("x", location.getX());
			loc.addProperty("y", location.getY());
			loc.addProperty("z", location.getZ());
			payload.add("location", loc);
		}

		if (points != null)
		{
			payload.addProperty("points", points);
		}

		if (category != null)
		{
			payload.addProperty("category", category);
		}

		if (tier != null)
		{
			payload.addProperty("tier", tier);
		}

		RequestBody body = RequestBody.create(JSON, payload.toString());
		Request.Builder builder = new Request.Builder()
			.url(baseUrl + "/me/plan/targets")
			.header("Content-Type", "application/json")
			.header("User-Agent", "DMMScape-Companion/1.0")
			.post(body);

		// Support both Bearer token (device code flow) and API key
		if (authHeader.startsWith("Bearer "))
		{
			builder.header("Authorization", authHeader);
		}
		else
		{
			builder.header("X-Api-Key", authHeader);
		}

		Request request = builder.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to add to plan", e);
				if (callback != null)
				{
					callback.onError("Network error: " + e.getMessage());
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					if (response.isSuccessful() && responseBody != null)
					{
						String bodyText = responseBody.string();
						try
						{
							JsonObject result = new JsonParser().parse(bodyText).getAsJsonObject();
							String message = result.has("message")
								? result.get("message").getAsString()
								: "Added to plan";
							boolean alreadyExists = result.has("alreadyExists")
								&& result.get("alreadyExists").getAsBoolean();

							if (alreadyExists)
							{
								log.debug("Item already in plan: {}", name);
							}
							else
							{
								log.info("Added to plan: {}", name);
							}

							if (callback != null)
							{
								callback.onSuccess();
							}
						}
						catch (Exception e)
						{
							log.warn("Could not parse add-to-plan response: {}", bodyText);
							if (callback != null)
							{
								callback.onSuccess();
							}
						}
					}
					else
					{
						String error = responseBody != null ? responseBody.string() : "Unknown error";
						log.warn("Add to plan failed ({}): {}", response.code(), error);
						if (callback != null)
						{
							callback.onError("HTTP " + response.code());
						}
					}
				}
			}
		});
	}

	/**
	 * Simplified addToPlan for common use case.
	 */
	public void addToPlan(String type, String id, String name, String apiKey, SyncCallback callback)
	{
		addToPlan(type, id, name, null, null, null, null, apiKey, callback);
	}

	private String deriveApiBaseUrl(String sourceUrl)
	{
		if (sourceUrl == null || sourceUrl.isEmpty())
		{
			return API_BASE_URL;
		}

		try
		{
			URI uri = new URI(sourceUrl);
			String path = uri.getPath();
			String basePath = "";

			int functionsIdx = path.indexOf("/functions/v1/api");
			if (functionsIdx >= 0)
			{
				basePath = path.substring(0, functionsIdx + "/functions/v1/api".length());
			}
			else
			{
				int apiIdx = path.indexOf("/api");
				if (apiIdx >= 0)
				{
					basePath = path.substring(0, apiIdx + 4);
				}
			}

			if (basePath.isEmpty())
			{
				return API_BASE_URL;
			}

			return new URI(uri.getScheme(), uri.getAuthority(), basePath, null, null).toString();
		}
		catch (Exception e)
		{
			log.debug("Failed to derive API base URL: {}", e.getMessage());
			return API_BASE_URL;
		}
	}

	/**
	 * Open user's profile page in browser.
	 * Fetches the profile URL from API and opens it.
	 */
	public void openProfileInBrowser(String apiKey, SyncCallback callback)
	{
		if (apiKey == null || apiKey.isEmpty())
		{
			if (callback != null)
			{
				callback.onError("API key not configured");
			}
			return;
		}

		Request.Builder builder = new Request.Builder()
			.url(API_BASE_URL + "/me/profile/link")
			.header("User-Agent", "DMMScape-Companion/1.0");

		// Support both Bearer token (device code flow) and API key
		if (apiKey.startsWith("Bearer "))
		{
			builder.header("Authorization", apiKey);
		}
		else
		{
			builder.header("X-Api-Key", apiKey);
		}

		Request request = builder.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to get profile link", e);
				if (callback != null)
				{
					callback.onError("Network error: " + e.getMessage());
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					if (response.isSuccessful() && responseBody != null)
					{
						String bodyText = responseBody.string();
						try
						{
							JsonObject result = new JsonParser().parse(bodyText).getAsJsonObject();

							if (result.has("success") && result.get("success").getAsBoolean())
							{
								String url = result.get("profileUrl").getAsString();
								log.info("Opening profile: {}", url);
								javax.swing.SwingUtilities.invokeLater(() -> LinkBrowser.browse(url));
								if (callback != null)
								{
									callback.onSuccess();
								}
							}
							else
							{
								String error = result.has("message")
									? result.get("message").getAsString()
									: "No profile URL available";
								log.warn("Profile link error: {}", error);
								if (callback != null)
								{
									callback.onError(error);
								}
							}
						}
						catch (Exception e)
						{
							log.warn("Could not parse profile link response: {}", bodyText);
							if (callback != null)
							{
								callback.onError("Invalid response");
							}
						}
					}
					else
					{
						String error = responseBody != null ? responseBody.string() : "Unknown error";
						log.warn("Profile link failed ({}): {}", response.code(), error);
						if (callback != null)
						{
							callback.onError("HTTP " + response.code());
						}
					}
				}
			}
		});
	}

	/**
	 * Fetches progress data from the webapp.
	 * Calls GET /api/me/progress to retrieve any data the webapp has (e.g., from hiscores).
	 */
	public void fetchProgress(String apiKey, String syncUrl, ProgressFetchCallback callback)
	{
		lastProgressFetchAttemptMs = System.currentTimeMillis();
		lastProgressFetchError = null;
		progressFetchInFlight = true;

		if (apiKey == null || apiKey.isEmpty())
		{
			lastProgressFetchError = "API key not configured";
			progressFetchInFlight = false;
			if (callback != null)
			{
				callback.onError("API key not configured");
			}
			return;
		}

		// Derive progress URL from sync URL
		String progressUrl = deriveProgressUrl(syncUrl);

		Request.Builder builder = new Request.Builder()
			.url(progressUrl)
			.get()
			.header("User-Agent", "DMMScape-Companion/1.0");

		if (progressEtag != null && !progressEtag.isEmpty())
		{
			builder.header("If-None-Match", progressEtag);
		}

		if (apiKey.startsWith("Bearer "))
		{
			builder.header("Authorization", apiKey);
		}
		else
		{
			builder.header("X-Api-Key", apiKey);
		}

		Request request = builder.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to fetch progress", e);
				lastProgressFetchError = "Network error";
				progressFetchInFlight = false;
				if (callback != null)
				{
					callback.onError("Network error: " + e.getMessage());
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					String responseEtag = response.header("ETag");
					if (responseEtag != null && !responseEtag.isEmpty())
					{
						progressEtag = responseEtag;
					}

					if (response.code() == 304)
					{
						lastProgressFetchSuccessMs = System.currentTimeMillis();
						lastProgressFetchError = null;
						progressFetchInFlight = false;
						if (callback != null)
						{
							callback.onNotModified();
						}
						return;
					}

					if (response.isSuccessful() && responseBody != null)
					{
						String bodyText = responseBody.string();
						try
						{
							JsonObject result = new JsonParser().parse(bodyText).getAsJsonObject();
							lastProgressFetchSuccessMs = System.currentTimeMillis();
							lastProgressFetchError = null;
							progressFetchInFlight = false;
							if (callback != null)
							{
								callback.onSuccess(result);
							}
						}
						catch (Exception e)
						{
							log.warn("Could not parse progress response: {}", bodyText);
							lastProgressFetchError = "Invalid response format";
							progressFetchInFlight = false;
							if (callback != null)
							{
								callback.onError("Invalid response format");
							}
						}
					}
					else
					{
						String error = responseBody != null ? responseBody.string() : "Unknown error";
						log.warn("Fetch progress failed ({}): {}", response.code(), error);
						lastProgressFetchError = "HTTP " + response.code();
						progressFetchInFlight = false;
						if (callback != null)
						{
							callback.onError("HTTP " + response.code());
						}
					}
				}
			}
		});
	}

	private String deriveProgressUrl(String syncUrl)
	{
		String baseUrl = deriveApiBaseUrl(syncUrl);
		return baseUrl + "/me/progress";
	}

	/**
	 * Callback for progress fetch operations.
	 */
	public interface ProgressFetchCallback
	{
		void onSuccess(JsonObject progress);
		void onError(String message);
		default void onNotModified()
		{
		}
	}

	/**
	 * Data class for add-to-plan requests.
	 */
	public static class AddToPlanRequest
	{
		public final String type;
		public final String id;
		public final String name;
		public final LocationData location;
		public final Integer points;
		public final String category;
		public final String tier;

		public AddToPlanRequest(String type, String id, String name)
		{
			this(type, id, name, null, null, null, null);
		}

		public AddToPlanRequest(String type, String id, String name, LocationData location,
			Integer points, String category, String tier)
		{
			this.type = type;
			this.id = id;
			this.name = name;
			this.location = location;
			this.points = points;
			this.category = category;
			this.tier = tier;
		}
	}
}
