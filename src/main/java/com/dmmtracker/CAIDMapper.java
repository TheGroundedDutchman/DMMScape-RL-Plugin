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
 * Maps between game CA IDs (from varps) and web app task identifiers.
 *
 * The game stores CA completion in VarPlayerID.CA_TASK_COMPLETED_0 through _19.
 * Each varp contains 32 bits, allowing for 640 total CAs.
 * The bit position corresponds to the CA's internal task ID.
 *
 * Our web app uses string IDs based on task names (e.g., "ca-easy-1").
 * This class provides mappings between the two systems.
 */
package com.dmmtracker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CAIDMapper
{
	// Map from game CA ID to web app task ID
	private static final Map<Integer, String> GAME_TO_WEB = new HashMap<>();

	// Map from web app task ID to game CA ID
	private static final Map<String, Integer> WEB_TO_GAME = new HashMap<>();

	// Tier info for each game ID
	private static final Map<Integer, String> GAME_ID_TIER = new HashMap<>();

	static
	{
		// This mapping needs to be populated with actual CA data
		// The game IDs are determined by the bit position in the varps
		// We would need to either:
		// 1. Manually map each CA (tedious but accurate)
		// 2. Use task names to match at runtime
		//
		// For now, we'll export the raw game IDs and let the web app
		// handle the mapping using task names (which are more stable)

		// TODO: Populate this with actual mappings from the game data
		// Example:
		// GAME_TO_WEB.put(0, "ca-easy-1");
		// WEB_TO_GAME.put("ca-easy-1", 0);
		// GAME_ID_TIER.put(0, "easy");
	}

	/**
	 * Converts a game CA ID to web app task ID.
	 */
	public static String gameToWebId(int gameId)
	{
		return GAME_TO_WEB.get(gameId);
	}

	/**
	 * Converts a web app task ID to game CA ID.
	 */
	public static Integer webToGameId(String webId)
	{
		return WEB_TO_GAME.get(webId);
	}

	/**
	 * Gets the tier for a game CA ID.
	 */
	public static String getTier(int gameId)
	{
		return GAME_ID_TIER.get(gameId);
	}

	/**
	 * Checks if a game ID has a known mapping.
	 */
	public static boolean hasMapping(int gameId)
	{
		return GAME_TO_WEB.containsKey(gameId);
	}

	/**
	 * Returns all known game IDs.
	 */
	public static Set<Integer> getAllGameIds()
	{
		return Collections.unmodifiableSet(GAME_TO_WEB.keySet());
	}

	/**
	 * Returns all known web IDs.
	 */
	public static Set<String> getAllWebIds()
	{
		return Collections.unmodifiableSet(WEB_TO_GAME.keySet());
	}

	/**
	 * Adds a mapping between game ID and web ID.
	 * Can be used to dynamically populate mappings.
	 */
	public static void addMapping(int gameId, String webId, String tier)
	{
		GAME_TO_WEB.put(gameId, webId);
		WEB_TO_GAME.put(webId, gameId);
		GAME_ID_TIER.put(gameId, tier);
	}
}
