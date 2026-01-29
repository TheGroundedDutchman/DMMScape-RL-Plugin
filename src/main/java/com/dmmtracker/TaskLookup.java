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

import com.dmmtracker.data.CATaskData;
import com.dmmtracker.data.DataLoader;
import com.dmmtracker.data.DiaryData;
import com.dmmtracker.data.DiaryTaskData;
import com.dmmtracker.data.DiaryTierData;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lookup helper for diary and combat achievement task IDs.
 */
@Singleton
public class TaskLookup
{
	private final DataLoader dataLoader;

	private final Map<String, String> diaryTaskIdByKey = new HashMap<>();
	private final Map<String, String> caTaskIdByName = new HashMap<>();
	private final List<String> caTaskIdsByIndex = new ArrayList<>();

	@Inject
	public TaskLookup(DataLoader dataLoader)
	{
		this.dataLoader = dataLoader;
		rebuild();
		dataLoader.addUpdateListener(this::rebuild);
	}

	public synchronized void rebuild()
	{
		diaryTaskIdByKey.clear();
		caTaskIdByName.clear();
		caTaskIdsByIndex.clear();

		buildDiaryLookup();
		buildCaLookup();
	}

	private void buildDiaryLookup()
	{
		for (DiaryData diary : dataLoader.getDiaries())
		{
			String diaryName = DiaryNameUtils.normalizeDiaryName(diary.getName());
			if (diaryName == null)
			{
				continue;
			}

			addDiaryTierTasks(diaryName, diary.getTiers().getEasy());
			addDiaryTierTasks(diaryName, diary.getTiers().getMedium());
			addDiaryTierTasks(diaryName, diary.getTiers().getHard());
			addDiaryTierTasks(diaryName, diary.getTiers().getElite());
		}
	}

	private void addDiaryTierTasks(String diaryName, DiaryTierData tierData)
	{
		if (tierData == null)
		{
			return;
		}

		for (DiaryTaskData task : tierData.getTasks())
		{
			if (task == null || task.getId() == null)
			{
				continue;
			}

			String descriptionKey = normalizeDiaryTaskText(task.getDescription());
			if (descriptionKey != null)
			{
				diaryTaskIdByKey.putIfAbsent(diaryName + "::" + descriptionKey, task.getId());
			}

			String matchedKey = normalizeDiaryTaskText(task.getMatchedFrom());
			if (matchedKey != null)
			{
				diaryTaskIdByKey.putIfAbsent(diaryName + "::" + matchedKey, task.getId());
			}
		}
	}

	private void buildCaLookup()
	{
		Map<String, com.dmmtracker.data.PluginData.CATierData> tiers = dataLoader.getCATiers();
		for (String tier : List.of("easy", "medium", "hard", "elite", "master", "grandmaster"))
		{
			com.dmmtracker.data.PluginData.CATierData tierData = tiers.get(tier);
			if (tierData == null)
			{
				continue;
			}

			for (CATaskData task : tierData.getTasks())
			{
				if (task == null || task.getId() == null)
				{
					continue;
				}
				caTaskIdsByIndex.add(task.getId());
				String normalized = normalizeCAName(task.getName());
				if (normalized != null)
				{
					caTaskIdByName.putIfAbsent(normalized, task.getId());
				}
			}
		}
	}

	public synchronized String findDiaryTaskId(String diaryName, String taskText)
	{
		String normalizedDiary = DiaryNameUtils.normalizeDiaryName(diaryName);
		String normalizedText = normalizeDiaryTaskText(taskText);
		if (normalizedDiary == null || normalizedText == null)
		{
			return null;
		}

		return diaryTaskIdByKey.get(normalizedDiary + "::" + normalizedText);
	}

	public synchronized String findCaTaskId(String caName)
	{
		String normalized = normalizeCAName(caName);
		if (normalized == null)
		{
			return null;
		}
		return caTaskIdByName.get(normalized);
	}

	public synchronized String findCaTaskId(int gameId, Map<Integer, String> nameMapping)
	{
		if (nameMapping != null)
		{
			String name = nameMapping.get(gameId);
			if (name != null)
			{
				String mapped = findCaTaskId(name);
				if (mapped != null)
				{
					return mapped;
				}
			}
		}

		if (gameId >= 0 && gameId < caTaskIdsByIndex.size())
		{
			return caTaskIdsByIndex.get(gameId);
		}

		return null;
	}

	public synchronized Set<String> mapCaTaskIds(Set<Integer> gameIds, Map<Integer, String> nameMapping)
	{
		Set<String> mapped = new HashSet<>();
		if (gameIds == null || gameIds.isEmpty())
		{
			return mapped;
		}

		for (Integer id : gameIds)
		{
			if (id == null)
			{
				continue;
			}
			String taskId = findCaTaskId(id, nameMapping);
			if (taskId != null)
			{
				mapped.add(taskId);
			}
		}
		return mapped;
	}

	static String normalizeDiaryTaskText(String text)
	{
		if (text == null)
		{
			return null;
		}

		String cleaned = text.toLowerCase()
			.replace('\u2019', '\'')
			.replace('\u2018', '\'')
			.replace("&", "and")
			.replaceAll("^\\d+\\.\\s*", "")
			.replaceAll("^[-\\u2022]\\s*", "")
			.replaceAll("[^a-z0-9\\s]", "")
			.replaceAll("\\s+", " ")
			.trim();

		return cleaned.isEmpty() ? null : cleaned;
	}

	static String normalizeCAName(String name)
	{
		if (name == null)
		{
			return null;
		}

		String cleaned = name.toLowerCase()
			.replace('\u2019', '\'')
			.replace('\u2018', '\'')
			.replace("&", "and")
			.replaceAll("[^a-z0-9\\s]", "")
			.replaceAll("\\s+", " ")
			.trim();

		return cleaned.isEmpty() ? null : cleaned;
	}
}
