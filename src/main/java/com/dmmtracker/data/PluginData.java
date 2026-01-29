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

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Root data model for DMMScape plugin data.
 * Loaded from bundled JSON and optionally updated via remote fetch.
 */
public class PluginData
{
	@SerializedName("meta")
	private MetaData meta;

	@SerializedName("bosses")
	private List<BossData> bosses;

	@SerializedName("bossRequirements")
	private Map<String, BossRequirementData> bossRequirements;

	@SerializedName("categoryNames")
	private Map<String, String> categoryNames;

	@SerializedName("sigils")
	private List<SigilData> sigils;

	@SerializedName("lamps")
	private List<LampData> lamps;

	@SerializedName("prayers")
	private List<PrayerData> prayers;

	@SerializedName("skills")
	private List<SkillData> skills;

	@SerializedName("skillPointRules")
	private SkillPointRules skillPointRules;

	@SerializedName("combatAchievementTiers")
	private Map<String, CATierData> combatAchievementTiers;

	@SerializedName("diaryTierPoints")
	private Map<String, Integer> diaryTierPoints;

	@SerializedName("diaries")
	private List<DiaryData> diaries;

	@SerializedName("dmmAutoCompletedQuests")
	private List<String> dmmAutoCompletedQuests;

	@SerializedName("teleports")
	private List<TeleportData> teleports;

	// Getters
	public MetaData getMeta()
	{
		return meta;
	}
	public List<BossData> getBosses()
	{
		return bosses != null ? bosses : Collections.emptyList();
	}
	public Map<String, BossRequirementData> getBossRequirements()
	{
		return bossRequirements != null ? bossRequirements : Collections.emptyMap();
	}
	public Map<String, String> getCategoryNames()
	{
		return categoryNames != null ? categoryNames : Collections.emptyMap();
	}
	public List<SigilData> getSigils()
	{
		return sigils != null ? sigils : Collections.emptyList();
	}
	public List<LampData> getLamps()
	{
		return lamps != null ? lamps : Collections.emptyList();
	}
	public List<PrayerData> getPrayers()
	{
		return prayers != null ? prayers : Collections.emptyList();
	}
	public List<SkillData> getSkills()
	{
		return skills != null ? skills : Collections.emptyList();
	}
	public SkillPointRules getSkillPointRules()
	{
		return skillPointRules;
	}
	public Map<String, CATierData> getCombatAchievementTiers()
	{
		return combatAchievementTiers != null ? combatAchievementTiers : Collections.emptyMap();
	}
	public Map<String, Integer> getDiaryTierPoints()
	{
		return diaryTierPoints != null ? diaryTierPoints : Collections.emptyMap();
	}
	public List<DiaryData> getDiaries()
	{
		return diaries != null ? diaries : Collections.emptyList();
	}
	public List<String> getDmmAutoCompletedQuests()
	{
		return dmmAutoCompletedQuests != null ? dmmAutoCompletedQuests : Collections.emptyList();
	}
	public List<TeleportData> getTeleports()
	{
		return teleports != null ? teleports : Collections.emptyList();
	}

	public Set<String> getDmmAutoQuestSet()
	{
		return dmmAutoCompletedQuests != null
			? new HashSet<>(dmmAutoCompletedQuests)
			: Collections.emptySet();
	}

	// Nested classes
	public static class MetaData
	{
		@SerializedName("version")
		private String version;

		@SerializedName("timestamp")
		private long timestamp;

		@SerializedName("source")
		private String source;

		public String getVersion()
		{
			return version;
		}
		public long getTimestamp()
		{
			return timestamp;
		}
		public String getSource()
		{
			return source;
		}
	}

	public static class SkillPointRules
	{
		@SerializedName("pointsPerLevel1To49")
		private int pointsPerLevel1To49;

		@SerializedName("pointsPerLevel50To99")
		private int pointsPerLevel50To99;

		@SerializedName("bonusAt99")
		private int bonusAt99;

		@SerializedName("maxLevel")
		private int maxLevel;

		public int getPointsPerLevel1To49()
		{
			return pointsPerLevel1To49;
		}
		public int getPointsPerLevel50To99()
		{
			return pointsPerLevel50To99;
		}
		public int getBonusAt99()
		{
			return bonusAt99;
		}
		public int getMaxLevel()
		{
			return maxLevel;
		}

		public int calculatePoints(int level)
		{
			if (level <= 1) return 0;
			int capped = Math.min(level, maxLevel);
			int lower = Math.max(0, Math.min(capped, 49) - 1);
			int upper = Math.max(0, capped - 49);
			int points = (lower * pointsPerLevel1To49) + (upper * pointsPerLevel50To99);
			if (capped >= maxLevel)
			{
				points += bonusAt99;
			}
			return points;
		}
	}

	public static class CATierData
	{
		@SerializedName("tier")
		private String tier;

		@SerializedName("points")
		private int points;

		@SerializedName("taskCount")
		private int taskCount;

		@SerializedName("tasks")
		private List<CATaskData> tasks;

		@SerializedName("rewards")
		private List<String> rewards;

		public String getTier()
		{
			return tier;
		}
		public int getPoints()
		{
			return points;
		}
		public int getTaskCount()
		{
			return taskCount;
		}
		public List<CATaskData> getTasks()
		{
			return tasks != null ? tasks : Collections.emptyList();
		}
		public List<String> getRewards()
		{
			return rewards != null ? rewards : Collections.emptyList();
		}
	}
}
