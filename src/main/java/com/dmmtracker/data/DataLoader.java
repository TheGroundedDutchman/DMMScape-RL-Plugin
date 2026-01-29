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

package com.dmmtracker.data;

import com.dmmtracker.ApiConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.runelite.client.RuneLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Hybrid data loader for DMMScape plugin.
 * Loads bundled baseline data from JAR resources, with optional remote updates via ETag caching.
 */
@Singleton
public class DataLoader
{
	private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
	private static final String BUNDLED_DATA_PATH = "/com/dmmtracker/data/dmmscape-data.json";
	private static final String CACHE_FILE_NAME = "dmmscape-data-cache.json";
	private static final String ETAG_FILE_NAME = "dmmscape-data-etag.txt";
	private static final String REMOTE_DATA_URL = ApiConfig.pluginDataUrl();

	private final Gson gson;
	private PluginData data;
	private String currentEtag;
	private final List<Runnable> dataUpdateListeners = new CopyOnWriteArrayList<>();

	@Inject
	public DataLoader()
	{
		this.gson = new GsonBuilder()
			.registerTypeAdapter(DiaryQuestRequirement.class, new QuestRequirementTypeAdapter())
			.create();
		loadData();
	}

	/**
	 * Loads data with fallback chain: cache -> bundled -> empty.
	 */
	private void loadData()
	{
		// Try loading from cache first
		PluginData cachedData = loadFromCache();
		PluginData bundledData = loadFromBundled();

		if (cachedData != null && bundledData != null)
		{
			// Use whichever is newer
			if (cachedData.getMeta() != null && bundledData.getMeta() != null)
			{
				if (cachedData.getMeta().getTimestamp() > bundledData.getMeta().getTimestamp())
				{
					log.info("Using cached data (version {})", cachedData.getMeta().getVersion());
					this.data = cachedData;
					return;
				}
			}
		}

		if (cachedData != null)
		{
			log.info("Using cached data (version {})", cachedData.getMeta() != null ? cachedData.getMeta().getVersion() : "unknown");
			this.data = cachedData;
			return;
		}

		if (bundledData != null)
		{
			log.info("Using bundled data (version {})", bundledData.getMeta() != null ? bundledData.getMeta().getVersion() : "unknown");
			this.data = bundledData;
			return;
		}

		log.warn("No data available, creating empty data");
		this.data = new PluginData();
	}

	private PluginData loadFromBundled()
	{
		try (InputStream is = getClass().getResourceAsStream(BUNDLED_DATA_PATH))
		{
			if (is == null)
			{
				log.warn("Bundled data not found at {}", BUNDLED_DATA_PATH);
				return null;
			}
			try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8))
			{
				return gson.fromJson(reader, PluginData.class);
			}
		}
		catch (Exception e)
		{
			log.error("Error loading bundled data", e);
			return null;
		}
	}

	private PluginData loadFromCache()
	{
		Path cacheDir = getCacheDirectory();
		if (cacheDir == null) return null;

		Path cacheFile = cacheDir.resolve(CACHE_FILE_NAME);
		if (!Files.exists(cacheFile))
		{
			return null;
		}

		try (Reader reader = Files.newBufferedReader(cacheFile, StandardCharsets.UTF_8))
		{
			PluginData cached = gson.fromJson(reader, PluginData.class);

			// Load stored ETag
			Path etagFile = cacheDir.resolve(ETAG_FILE_NAME);
			if (Files.exists(etagFile))
			{
				currentEtag = Files.readString(etagFile, StandardCharsets.UTF_8).trim();
			}

			return cached;
		}
		catch (Exception e)
		{
			log.warn("Error loading cached data", e);
			return null;
		}
	}

	private void saveToCache(PluginData newData, String etag)
	{
		Path cacheDir = getCacheDirectory();
		if (cacheDir == null) return;

		try
		{
			Files.createDirectories(cacheDir);

			Path cacheFile = cacheDir.resolve(CACHE_FILE_NAME);
			Files.writeString(cacheFile, gson.toJson(newData), StandardCharsets.UTF_8);

			if (etag != null)
			{
				Path etagFile = cacheDir.resolve(ETAG_FILE_NAME);
				Files.writeString(etagFile, etag, StandardCharsets.UTF_8);
				currentEtag = etag;
			}

			log.info("Saved data to cache");
		}
		catch (Exception e)
		{
			log.error("Error saving data to cache", e);
		}
	}

	private Path getCacheDirectory()
	{
		try
		{
			return RuneLite.RUNELITE_DIR.toPath().resolve("dmmtracker");
		}
		catch (Exception e)
		{
			log.warn("Could not get cache directory", e);
			return null;
		}
	}

	/**
	 * Checks for remote updates using ETag-based caching.
	 * Call this asynchronously (not on EDT).
	 */
	public void checkForUpdates()
	{
		try
		{
			URL url = new URL(REMOTE_DATA_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(10000);

			if (currentEtag != null)
			{
				conn.setRequestProperty("If-None-Match", currentEtag);
			}

			int responseCode = conn.getResponseCode();

			if (responseCode == 304)
			{
				log.debug("Data is up to date (ETag match)");
				return;
			}

			if (responseCode == 200)
			{
				String newEtag = conn.getHeaderField("ETag");

				try (InputStream is = conn.getInputStream();
					Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8))
				{
					PluginData newData = gson.fromJson(reader, PluginData.class);

					if (newData != null && newData.getMeta() != null)
					{
						log.info("Downloaded new data (version {})", newData.getMeta().getVersion());
						this.data = newData;
						saveToCache(newData, newEtag);
						notifyListeners();
					}
				}
			}
			else
			{
				log.warn("Remote data fetch returned {}", responseCode);
			}

			conn.disconnect();
		}
		catch (Exception e)
		{
			log.debug("Could not check for updates: {}", e.getMessage());
		}
	}

	/**
	 * Adds a listener to be notified when data is updated.
	 */
	public void addUpdateListener(Runnable listener)
	{
		dataUpdateListeners.add(listener);
	}

	public void removeUpdateListener(Runnable listener)
	{
		dataUpdateListeners.remove(listener);
	}

	private void notifyListeners()
	{
		for (Runnable listener : dataUpdateListeners)
		{
			try
			{
				listener.run();
			}
			catch (Exception e)
			{
				log.error("Error notifying data update listener", e);
			}
		}
	}

	// ========== DATA ACCESS METHODS ==========

	public PluginData getData()
	{
		return data;
	}

	public List<BossData> getBosses()
	{
		return data.getBosses();
	}

	public Map<String, BossRequirementData> getBossRequirements()
	{
		return data.getBossRequirements();
	}

	public BossRequirementData getBossRequirement(String bossName)
	{
		if (bossName == null)
		{
			return null;
		}
		return data.getBossRequirements().get(bossName);
	}

	public List<BossData> searchBosses(String query)
	{
		if (query == null || query.isEmpty()) return getBosses();
		String lower = query.toLowerCase();
		return data.getBosses().stream()
			.filter(b -> b.getName().toLowerCase().contains(lower) ||
						b.getCategory().toLowerCase().contains(lower))
			.collect(Collectors.toList());
	}

	public List<BossData> getBossesByCategory(String category)
	{
		if (category == null || category.isEmpty()) return getBosses();
		return data.getBosses().stream()
			.filter(b -> category.equalsIgnoreCase(b.getCategory()))
			.collect(Collectors.toList());
	}

	public List<SigilData> getSigils()
	{
		return data.getSigils();
	}

	public List<SigilData> searchSigils(String query)
	{
		if (query == null || query.isEmpty()) return getSigils();
		String lower = query.toLowerCase();
		return data.getSigils().stream()
			.filter(s -> s.getName().toLowerCase().contains(lower))
			.collect(Collectors.toList());
	}

	public List<LampData> getLamps()
	{
		return data.getLamps();
	}

	public List<PrayerData> getPrayers()
	{
		return data.getPrayers();
	}

	public List<SkillData> getSkills()
	{
		return data.getSkills();
	}

	public Map<String, PluginData.CATierData> getCATiers()
	{
		return data.getCombatAchievementTiers();
	}

	public List<CATaskData> getCATasks()
	{
		List<CATaskData> tasks = new ArrayList<>();
		for (PluginData.CATierData tier : data.getCombatAchievementTiers().values())
		{
			tasks.addAll(tier.getTasks());
		}
		return tasks;
	}

	public Map<String, Integer> getDiaryTierPoints()
	{
		return data.getDiaryTierPoints();
	}

	public List<DiaryData> getDiaries()
	{
		return data.getDiaries();
	}

	public Set<String> getDmmAutoCompletedQuests()
	{
		return data.getDmmAutoQuestSet();
	}

	public List<TeleportData> getTeleports()
	{
		return data.getTeleports();
	}

	public boolean isAutoCompletedQuest(String questName)
	{
		return data.getDmmAutoQuestSet().contains(questName);
	}

	public String getCategoryDisplayName(String categoryId)
	{
		return data.getCategoryNames().getOrDefault(categoryId, categoryId);
	}

	public int calculateSkillPoints(int level)
	{
		PluginData.SkillPointRules rules = data.getSkillPointRules();
		return rules != null ? rules.calculatePoints(level) : 0;
	}

	public String getDataVersion()
	{
		return data.getMeta() != null ? data.getMeta().getVersion() : "unknown";
	}
}
