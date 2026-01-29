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

import java.util.Map;

/**
 * Utilities for normalizing Achievement Diary names.
 */
public final class DiaryNameUtils
{
	private static final Map<String, String> DIARY_NAME_ALIASES = Map.ofEntries(
		Map.entry("ardougne", "Ardougne Diary"),
		Map.entry("desert", "Desert Diary"),
		Map.entry("falador", "Falador Diary"),
		Map.entry("fremennik", "Fremennik Diary"),
		Map.entry("kandarin", "Kandarin Diary"),
		Map.entry("karamja", "Karamja Diary"),
		Map.entry("kourend", "Kourend & Kebos Diary"),
		Map.entry("kourend & kebos", "Kourend & Kebos Diary"),
		Map.entry("lumbridge", "Lumbridge & Draynor Diary"),
		Map.entry("lumbridge & draynor", "Lumbridge & Draynor Diary"),
		Map.entry("morytania", "Morytania Diary"),
		Map.entry("varrock", "Varrock Diary"),
		Map.entry("western", "Western Provinces Diary"),
		Map.entry("western provinces", "Western Provinces Diary"),
		Map.entry("wilderness", "Wilderness Diary")
	);

	private DiaryNameUtils()
	{
	}

	public static String normalizeDiaryName(String name)
	{
		if (name == null)
		{
			return null;
		}

		String trimmed = name.trim();
		if (trimmed.isEmpty())
		{
			return null;
		}

		String cleaned = trimmed
			.replaceAll("(?i)\\s+diary$", "")
			.replaceAll("(?i)\\s+area\\s+tasks?$", "")
			.replaceAll("(?i)\\s+tasks?$", "")
			.trim();

		String key = cleaned.toLowerCase();
		String alias = DIARY_NAME_ALIASES.getOrDefault(key, DIARY_NAME_ALIASES.get(trimmed.toLowerCase()));
		if (alias != null)
		{
			return alias;
		}

		if (trimmed.toLowerCase().endsWith("diary"))
		{
			return trimmed;
		}

		return cleaned + " Diary";
	}
}
