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

package com.dmmtracker;

import com.dmmtracker.data.DiaryQuestRequirement;
import com.dmmtracker.data.DiarySkillRequirement;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for checking if the player meets skill and quest requirements.
 * Uses the RuneLite Client API for direct game state access.
 */
@Singleton
public class RequirementChecker
{
	private final Client client;
	private volatile boolean gameStateAvailable;
	private volatile int[] cachedRealLevels = buildEmptySkillCache();
	private volatile int[] cachedBoostedLevels = buildEmptySkillCache();
	private volatile Map<Quest, QuestState> cachedQuestStates = Collections.emptyMap();

	// DMM base stats: all skills level 1, except Hitpoints which is 10
	private static final int DMM_BASE_LEVEL = 1;
	private static final int DMM_BASE_HITPOINTS = 10;

	// DMM autocompleted quests (set via setDmmAutoCompletedQuests)
	private Set<String> dmmAutoCompletedQuests = Collections.emptySet();

	// Map skill name aliases to Skill enum
	private static final Map<String, Skill> SKILL_ALIASES = new HashMap<>();

	// Map quest name aliases to Quest enum
	private static final Map<String, Quest> QUEST_ALIASES = new HashMap<>();

	static
	{
		// Standard names (from Skill enum)
		for (Skill skill : Skill.values())
		{
			if (skill != Skill.OVERALL)
			{
				SKILL_ALIASES.put(skill.getName().toLowerCase(), skill);
			}
		}

		// Common variations and aliases
		SKILL_ALIASES.put("hitpoints", Skill.HITPOINTS);
		SKILL_ALIASES.put("hp", Skill.HITPOINTS);
		SKILL_ALIASES.put("health", Skill.HITPOINTS);

		SKILL_ALIASES.put("range", Skill.RANGED);
		SKILL_ALIASES.put("ranging", Skill.RANGED);

		SKILL_ALIASES.put("rc", Skill.RUNECRAFT);
		SKILL_ALIASES.put("runecrafting", Skill.RUNECRAFT);

		SKILL_ALIASES.put("wc", Skill.WOODCUTTING);
		SKILL_ALIASES.put("woodcut", Skill.WOODCUTTING);

		SKILL_ALIASES.put("fm", Skill.FIREMAKING);

		SKILL_ALIASES.put("con", Skill.CONSTRUCTION);

		SKILL_ALIASES.put("pray", Skill.PRAYER);

		SKILL_ALIASES.put("mage", Skill.MAGIC);

		SKILL_ALIASES.put("str", Skill.STRENGTH);

		SKILL_ALIASES.put("atk", Skill.ATTACK);
		SKILL_ALIASES.put("att", Skill.ATTACK);

		SKILL_ALIASES.put("def", Skill.DEFENCE);
		SKILL_ALIASES.put("defense", Skill.DEFENCE);

		SKILL_ALIASES.put("herb", Skill.HERBLORE);

		SKILL_ALIASES.put("fletch", Skill.FLETCHING);

		SKILL_ALIASES.put("agi", Skill.AGILITY);

		SKILL_ALIASES.put("thieve", Skill.THIEVING);

		SKILL_ALIASES.put("craft", Skill.CRAFTING);

		SKILL_ALIASES.put("smith", Skill.SMITHING);

		SKILL_ALIASES.put("slay", Skill.SLAYER);

		SKILL_ALIASES.put("farm", Skill.FARMING);

		SKILL_ALIASES.put("hunt", Skill.HUNTER);

		// Build quest name map from Quest enum
		for (Quest quest : Quest.values())
		{
			String name = quest.getName();
			if (name != null && !name.isEmpty())
			{
				// Add exact name
				QUEST_ALIASES.put(name.toLowerCase(), quest);

				// Add without apostrophes
				String noApostrophe = name.replace("'", "").toLowerCase();
				if (!noApostrophe.equals(name.toLowerCase()))
				{
					QUEST_ALIASES.put(noApostrophe, quest);
				}

				// Add common variations
				String simplified = name.toLowerCase()
					.replace("'", "")
					.replace("-", " ")
					.replaceAll("\\s+", " ")
					.trim();
				if (!simplified.equals(name.toLowerCase()))
				{
					QUEST_ALIASES.put(simplified, quest);
				}
			}
		}

		// Add specific quest aliases (only for quests that exist in Quest enum)
		// Note: Quest enum names must match exactly - check net.runelite.api.Quest
		addQuestAlias("rfd", Quest.RECIPE_FOR_DISASTER);
		addQuestAlias("recipe for disaster", Quest.RECIPE_FOR_DISASTER);
		addQuestAlias("mm1", Quest.MONKEY_MADNESS_I);
		addQuestAlias("mm2", Quest.MONKEY_MADNESS_II);
		addQuestAlias("monkey madness", Quest.MONKEY_MADNESS_I);
		addQuestAlias("dt1", Quest.DESERT_TREASURE_I);
		addQuestAlias("desert treasure", Quest.DESERT_TREASURE_I);
		addQuestAlias("ds1", Quest.DRAGON_SLAYER_I);
		addQuestAlias("ds2", Quest.DRAGON_SLAYER_II);
		addQuestAlias("dragon slayer", Quest.DRAGON_SLAYER_I);
		addQuestAlias("sote", Quest.SONG_OF_THE_ELVES);
		addQuestAlias("sotf", Quest.SINS_OF_THE_FATHER);
		addQuestAlias("tob", Quest.A_TASTE_OF_HOPE);
		addQuestAlias("frem trials", Quest.THE_FREMENNIK_TRIALS);
		addQuestAlias("frem isles", Quest.THE_FREMENNIK_ISLES);
		addQuestAlias("frem exiles", Quest.THE_FREMENNIK_EXILES);
		addQuestAlias("roving elves", Quest.ROVING_ELVES);
		addQuestAlias("regicide", Quest.REGICIDE);
		addQuestAlias("mep1", Quest.MOURNINGS_END_PART_I);
		addQuestAlias("mep2", Quest.MOURNINGS_END_PART_II);
		addQuestAlias("legends quest", Quest.LEGENDS_QUEST);
		addQuestAlias("horror from the deep", Quest.HORROR_FROM_THE_DEEP);
		addQuestAlias("lunar diplomacy", Quest.LUNAR_DIPLOMACY);
		addQuestAlias("dream mentor", Quest.DREAM_MENTOR);
		addQuestAlias("kings ransom", Quest.KINGS_RANSOM);
		addQuestAlias("king's ransom", Quest.KINGS_RANSOM);
		addQuestAlias("a night at the theatre", Quest.A_NIGHT_AT_THE_THEATRE);
	}

	private static void addQuestAlias(String alias, Quest quest)
	{
		QUEST_ALIASES.put(alias.toLowerCase(), quest);
	}

	@Inject
	public RequirementChecker(Client client)
	{
		this.client = client;
	}

	/**
	 * Refreshes cached skill/quest data. Must be called on the client thread.
	 */
	public void refreshSnapshot()
	{
		if (client == null || !client.isClientThread())
		{
			return;
		}

		if (client.getLocalPlayer() == null)
		{
			clearSnapshot();
			return;
		}

		int[] real = buildEmptySkillCache();
		int[] boosted = buildEmptySkillCache();
		for (Skill skill : Skill.values())
		{
			if (skill == Skill.OVERALL)
			{
				continue;
			}
			real[skill.ordinal()] = client.getRealSkillLevel(skill);
			boosted[skill.ordinal()] = client.getBoostedSkillLevel(skill);
		}

		EnumMap<Quest, QuestState> questStates = new EnumMap<>(Quest.class);
		for (Quest quest : Quest.values())
		{
			questStates.put(quest, quest.getState(client));
		}

		cachedRealLevels = real;
		cachedBoostedLevels = boosted;
		cachedQuestStates = questStates;
		gameStateAvailable = true;
	}

	/**
	 * Clears cached snapshot data (safe to call from any thread).
	 */
	public void clearSnapshot()
	{
		gameStateAvailable = false;
		cachedRealLevels = buildEmptySkillCache();
		cachedBoostedLevels = buildEmptySkillCache();
		cachedQuestStates = Collections.emptyMap();
	}

	private static int[] buildEmptySkillCache()
	{
		int[] levels = new int[Skill.values().length];
		Arrays.fill(levels, -1);
		return levels;
	}

	/**
	 * Sets the DMM autocompleted quests for fallback when not logged in.
	 * @param quests Set of quest names that are autocompleted in DMM
	 */
	public void setDmmAutoCompletedQuests(Set<String> quests)
	{
		this.dmmAutoCompletedQuests = quests != null ? quests : Collections.emptySet();
	}

	/**
	 * Gets the DMM base level for a skill (1 for all skills, 10 for Hitpoints).
	 */
	private int getDmmBaseLevel(Skill skill)
	{
		if (skill == Skill.HITPOINTS)
		{
			return DMM_BASE_HITPOINTS;
		}
		return DMM_BASE_LEVEL;
	}

	/**
	 * Checks if a quest is autocompleted in DMM (case-insensitive).
	 */
	private boolean isDmmAutoCompleted(String questName)
	{
		if (questName == null || dmmAutoCompletedQuests.isEmpty())
		{
			return false;
		}
		String lower = questName.toLowerCase().trim();
		for (String auto : dmmAutoCompletedQuests)
		{
			if (auto.toLowerCase().equals(lower))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the Skill enum for a skill name, handling aliases.
	 * @param skillName The skill name (case-insensitive)
	 * @return The Skill enum, or null if not found
	 */
	private Skill getSkill(String skillName)
	{
		if (skillName == null || skillName.isEmpty())
		{
			return null;
		}
		return SKILL_ALIASES.get(skillName.toLowerCase().trim());
	}

	/**
	 * Checks if the player meets a single skill requirement.
	 * @param skillName The skill name (case-insensitive, aliases supported)
	 * @param requiredLevel The required level
	 * @param boostable Whether boosts count (uses boosted vs real level)
	 * @return true if met, false if not met. Returns true for unknown skills (fail open).
	 */
	public boolean meetsSkillRequirement(String skillName, int requiredLevel, boolean boostable)
	{
		Skill skill = getSkill(skillName);
		if (skill == null)
		{
			// Unknown skill - fail open (don't filter out)
			return true;
		}

		if (!gameStateAvailable)
		{
			// Not logged in - use DMM base stats as fallback
			return getDmmBaseLevel(skill) >= requiredLevel;
		}

		int playerLevel = boostable
			? cachedBoostedLevels[skill.ordinal()]
			: cachedRealLevels[skill.ordinal()];
		if (playerLevel < 0)
		{
			return getDmmBaseLevel(skill) >= requiredLevel;
		}
		return playerLevel >= requiredLevel;
	}

	/**
	 * Checks if the player meets all skill requirements in a list.
	 * @param requirements List of skill requirements
	 * @return true if ALL requirements are met (or list is empty/null)
	 */
	public boolean meetsAllSkillRequirements(List<DiarySkillRequirement> requirements)
	{
		if (requirements == null || requirements.isEmpty())
		{
			return true;
		}

		for (DiarySkillRequirement req : requirements)
		{
			if (!meetsSkillRequirement(req.getSkill(), req.getLevel(), req.isBoostable()))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if the player is currently logged in and game state is available.
	 * @return true if skill levels can be checked
	 */
	public boolean isGameStateAvailable()
	{
		return gameStateAvailable;
	}

	/**
	 * Gets the player's current level for a skill.
	 * @param skillName The skill name
	 * @param boostable Whether to return boosted or real level
	 * @return The level, or -1 if unavailable
	 */
	public int getPlayerSkillLevel(String skillName, boolean boostable)
	{
		if (!gameStateAvailable)
		{
			return -1;
		}

		Skill skill = getSkill(skillName);
		if (skill == null)
		{
			return -1;
		}

		int level = boostable
			? cachedBoostedLevels[skill.ordinal()]
			: cachedRealLevels[skill.ordinal()];
		return level >= 0 ? level : -1;
	}

	// ============ Quest Requirement Checking ============

	/**
	 * Gets the Quest enum for a quest name, handling aliases.
	 * @param questName The quest name (case-insensitive)
	 * @return The Quest enum, or null if not found
	 */
	private Quest getQuest(String questName)
	{
		if (questName == null || questName.isEmpty())
		{
			return null;
		}
		return QUEST_ALIASES.get(questName.toLowerCase().trim());
	}

	/**
	 * Checks if a quest is completed.
	 * @param questName The quest name (case-insensitive, aliases supported)
	 * @return true if completed, false if not. Returns true for unknown quests (fail open).
	 */
	public boolean isQuestCompleted(String questName)
	{
		if (!gameStateAvailable)
		{
			// Not logged in - check against DMM autocompleted quests
			return isDmmAutoCompleted(questName);
		}

		Quest quest = getQuest(questName);
		if (quest == null)
		{
			// Unknown quest - fail open (don't filter out)
			return true;
		}

		QuestState state = cachedQuestStates.get(quest);
		if (state == null)
		{
			// Unknown quest state - fail open
			return true;
		}
		return state == QuestState.FINISHED;
	}

	/**
	 * Checks if a quest is at least started (in progress or finished).
	 * @param questName The quest name (case-insensitive, aliases supported)
	 * @return true if started or finished, false if not started. Returns true for unknown quests.
	 */
	public boolean isQuestStarted(String questName)
	{
		if (!gameStateAvailable)
		{
			// Autocompleted quests count as started+finished
			return isDmmAutoCompleted(questName);
		}

		Quest quest = getQuest(questName);
		if (quest == null)
		{
			return true;
		}

		QuestState state = cachedQuestStates.get(quest);
		if (state == null)
		{
			return true;
		}
		return state == QuestState.IN_PROGRESS || state == QuestState.FINISHED;
	}

	/**
	 * Checks if the player meets a single quest requirement.
	 * @param req The quest requirement
	 * @return true if met, false if not met. Returns true for unknown quests (fail open).
	 */
	public boolean meetsQuestRequirement(DiaryQuestRequirement req)
	{
		if (req == null || req.getQuest() == null)
		{
			return true;
		}

		// If partial requirement, just check if started
		if (req.isPartial())
		{
			return isQuestStarted(req.getQuest());
		}

		// Otherwise, check if completed
		return isQuestCompleted(req.getQuest());
	}

	/**
	 * Checks if the player meets all quest requirements in a list.
	 * @param requirements List of quest requirements
	 * @return true if ALL requirements are met (or list is empty/null)
	 */
	public boolean meetsAllQuestRequirements(List<DiaryQuestRequirement> requirements)
	{
		if (requirements == null || requirements.isEmpty())
		{
			return true;
		}

		for (DiaryQuestRequirement req : requirements)
		{
			if (!meetsQuestRequirement(req))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if the player meets all requirements (skills AND quests) for a diary task.
	 * @param skillReqs List of skill requirements
	 * @param questReqs List of quest requirements
	 * @return true if ALL requirements are met
	 */
	public boolean meetsAllRequirements(List<DiarySkillRequirement> skillReqs, List<DiaryQuestRequirement> questReqs)
	{
		return meetsAllSkillRequirements(skillReqs) && meetsAllQuestRequirements(questReqs);
	}

	// ============ Boss/Monster Requirement Checking ============

	/**
	 * Checks if the player meets a slayer level requirement.
	 * @param requiredLevel Required slayer level (null means no requirement)
	 * @return true if requirement is met or not applicable
	 */
	public boolean meetsSlayerRequirement(Integer requiredLevel)
	{
		if (requiredLevel == null)
		{
			return true;
		}
		return meetsSkillRequirement("slayer", requiredLevel, false);
	}

	/**
	 * Checks if the player meets slayer and quest requirements for a boss.
	 * @param slayerLevel Required slayer level (nullable)
	 * @param questName Required quest name (nullable)
	 * @return true if all requirements are met
	 */
	public boolean meetsBossRequirements(Integer slayerLevel, String questName)
	{
		if (!meetsSlayerRequirement(slayerLevel))
		{
			return false;
		}
		if (questName == null || questName.isEmpty())
		{
			return true;
		}
		return meetsQuestRequirement(new DiaryQuestRequirement(questName));
	}
}
