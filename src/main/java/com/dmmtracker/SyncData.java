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
 * Data structure for sync payload
 */
package com.dmmtracker;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SyncData
{
	private String username;
	private String profileType; // DMM, LEAGUES, MAIN
	private long timestamp;
	private boolean fullSync; // true for initial sync, false for delta

	// Combat Achievements (as game IDs)
	private Set<Integer> completedCAs;

	// Quest points
	private int questPoints;

	// Completed quests (quest names)
	private Set<String> completedQuests;

	// Skill levels (skill name -> level)
	private Map<String, Integer> skillLevels;

	// Clue scroll completions (tier -> count)
	private Map<String, Integer> clueCompletions;

	// Collection log entry count
	private int collectionLogCount;

	// Boss kill counts (normalized name -> count)
	private Map<String, Integer> bossKills;

	// Diary progress: diary name -> tier name -> completed
	private Map<String, Map<String, Boolean>> diaryProgress;

	// Diary task counts: diary name -> tier name -> tasks completed count
	private Map<String, Map<String, Integer>> diaryTaskCounts;

	// Diary task updates: completed task text from the diary interface
	private List<DiaryTaskUpdate> diaryTaskUpdates;

	// Completed diary task IDs (exact)
	private Set<String> completedDiaryTasks;

	// CA name mapping: game ID -> CA name (for dynamic mapping)
	private Map<Integer, String> caNameMapping;

	// Owned unlocks (sigils, quest lamps, ruinous prayers)
	private Set<String> ownedSigils;
	private Set<String> ownedLamps;
	private Set<String> ownedPrayers;

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getProfileType()
	{
		return profileType;
	}

	public void setProfileType(String profileType)
	{
		this.profileType = profileType;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(long timestamp)
	{
		this.timestamp = timestamp;
	}

	public boolean isFullSync()
	{
		return fullSync;
	}

	public void setFullSync(boolean fullSync)
	{
		this.fullSync = fullSync;
	}

	public Set<Integer> getCompletedCAs()
	{
		return completedCAs;
	}

	public void setCompletedCAs(Set<Integer> completedCAs)
	{
		this.completedCAs = completedCAs;
	}

	public int getQuestPoints()
	{
		return questPoints;
	}

	public void setQuestPoints(int questPoints)
	{
		this.questPoints = questPoints;
	}

	public Set<String> getCompletedQuests()
	{
		return completedQuests;
	}

	public void setCompletedQuests(Set<String> completedQuests)
	{
		this.completedQuests = completedQuests;
	}

	public Map<String, Integer> getSkillLevels()
	{
		return skillLevels;
	}

	public void setSkillLevels(Map<String, Integer> skillLevels)
	{
		this.skillLevels = skillLevels;
	}

	public Map<String, Integer> getClueCompletions()
	{
		return clueCompletions;
	}

	public void setClueCompletions(Map<String, Integer> clueCompletions)
	{
		this.clueCompletions = clueCompletions;
	}

	public int getCollectionLogCount()
	{
		return collectionLogCount;
	}

	public void setCollectionLogCount(int collectionLogCount)
	{
		this.collectionLogCount = collectionLogCount;
	}

	public Map<String, Integer> getBossKills()
	{
		return bossKills;
	}

	public void setBossKills(Map<String, Integer> bossKills)
	{
		this.bossKills = bossKills;
	}

	public Map<String, Map<String, Boolean>> getDiaryProgress()
	{
		return diaryProgress;
	}

	public void setDiaryProgress(Map<String, Map<String, Boolean>> diaryProgress)
	{
		this.diaryProgress = diaryProgress;
	}

	public Map<String, Map<String, Integer>> getDiaryTaskCounts()
	{
		return diaryTaskCounts;
	}

	public void setDiaryTaskCounts(Map<String, Map<String, Integer>> diaryTaskCounts)
	{
		this.diaryTaskCounts = diaryTaskCounts;
	}

	public List<DiaryTaskUpdate> getDiaryTaskUpdates()
	{
		return diaryTaskUpdates;
	}

	public void setDiaryTaskUpdates(List<DiaryTaskUpdate> diaryTaskUpdates)
	{
		this.diaryTaskUpdates = diaryTaskUpdates;
	}

	public Set<String> getCompletedDiaryTasks()
	{
		return completedDiaryTasks;
	}

	public void setCompletedDiaryTasks(Set<String> completedDiaryTasks)
	{
		this.completedDiaryTasks = completedDiaryTasks;
	}

	public Map<Integer, String> getCaNameMapping()
	{
		return caNameMapping;
	}

	public void setCaNameMapping(Map<Integer, String> caNameMapping)
	{
		this.caNameMapping = caNameMapping;
	}

	public Set<String> getOwnedSigils()
	{
		return ownedSigils;
	}

	public void setOwnedSigils(Set<String> ownedSigils)
	{
		this.ownedSigils = ownedSigils;
	}

	public Set<String> getOwnedLamps()
	{
		return ownedLamps;
	}

	public void setOwnedLamps(Set<String> ownedLamps)
	{
		this.ownedLamps = ownedLamps;
	}

	public Set<String> getOwnedPrayers()
	{
		return ownedPrayers;
	}

	public void setOwnedPrayers(Set<String> ownedPrayers)
	{
		this.ownedPrayers = ownedPrayers;
	}
}
