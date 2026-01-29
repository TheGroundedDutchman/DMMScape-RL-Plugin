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
 * Maps boss names from chat messages to standardized web app boss IDs.
 */
package com.dmmtracker;

import java.util.HashMap;
import java.util.Map;

public class BossNameMapper
{
	// Map from various name variants to standardized boss ID
	private static final Map<String, String> NAME_TO_ID = new HashMap<>();

	static
	{
		// GWD
		mapBoss("commander-zilyana", "Commander Zilyana", "Zilyana", "Sara", "Saradomin");
		mapBoss("general-graardor", "General Graardor", "Graardor", "Bandos");
		mapBoss("kreearra", "Kree'arra", "Kree", "Arma", "Armadyl");
		mapBoss("kril-tsutsaroth", "K'ril Tsutsaroth", "K'ril", "Kril", "Zammy", "Zamorak");

		// Slayer bosses
		mapBoss("abyssal-sire", "Abyssal Sire", "Sire");
		mapBoss("alchemical-hydra", "Alchemical Hydra", "Hydra");
		mapBoss("cerberus", "Cerberus");
		mapBoss("grotesque-guardians", "Grotesque Guardians", "GGs", "Guardians");
		mapBoss("kraken", "Kraken");
		mapBoss("thermonuclear-smoke-devil", "Thermonuclear Smoke Devil", "Smoke Devil", "Thermy");

		// Wilderness bosses
		mapBoss("artio", "Artio");
		mapBoss("callisto", "Callisto");
		mapBoss("calvarion", "Calvar'ion", "Calvarion");
		mapBoss("chaos-elemental", "Chaos Elemental");
		mapBoss("chaos-fanatic", "Chaos Fanatic");
		mapBoss("crazy-archaeologist", "Crazy Archaeologist");
		mapBoss("king-black-dragon", "King Black Dragon", "KBD");
		mapBoss("scorpia", "Scorpia");
		mapBoss("spindel", "Spindel");
		mapBoss("venenatis", "Venenatis");
		mapBoss("vetion", "Vet'ion", "Vetion");

		// Raids
		mapBoss("chambers-of-xeric", "Chambers of Xeric", "CoX", "Raids 1");
		mapBoss("chambers-of-xeric-cm", "Chambers of Xeric: Challenge Mode", "CoX CM", "Challenge Mode");
		mapBoss("theatre-of-blood-entry", "Theatre of Blood: Entry Mode", "ToB Entry", "Entry Mode Theatre");
		mapBoss("theatre-of-blood", "Theatre of Blood", "ToB", "Raids 2");
		mapBoss("theatre-of-blood-hard", "Theatre of Blood: Hard Mode", "ToB HM", "ToB Hard", "Hard Mode Theatre");
		mapBoss("tombs-of-amascut-entry", "Tombs of Amascut: Entry Mode", "ToA Entry", "Entry Mode ToA");
		mapBoss("tombs-of-amascut", "Tombs of Amascut", "ToA", "Raids 3");
		mapBoss("tombs-of-amascut-expert", "Tombs of Amascut: Expert Mode", "ToA Expert");

		// DT2 bosses (base)
		mapBoss("duke-sucellus", "Duke Sucellus", "Duke");
		mapBoss("leviathan", "The Leviathan", "Leviathan");
		mapBoss("whisperer", "The Whisperer", "Whisperer");
		mapBoss("vardorvis", "Vardorvis");
		// DT2 bosses (awakened)
		mapBoss("duke-sucellus-awakened", "Duke Sucellus (Awakened)", "Awakened Duke");
		mapBoss("leviathan-awakened", "The Leviathan (Awakened)", "Awakened Leviathan");
		mapBoss("vardorvis-awakened", "Vardorvis (Awakened)", "Awakened Vardorvis");
		mapBoss("whisperer-awakened", "The Whisperer (Awakened)", "Awakened Whisperer");

		// Other bosses
		mapBoss("araxxor", "Araxxor");
		mapBoss("barrows", "Barrows Chests", "Barrows", "Barrows Brothers");
		mapBoss("bryophyta", "Bryophyta");
		mapBoss("corporeal-beast", "Corporeal Beast", "Corp");
		mapBoss("dagannoth-prime", "Dagannoth Prime", "Prime");
		mapBoss("dagannoth-rex", "Dagannoth Rex", "Rex");
		mapBoss("dagannoth-supreme", "Dagannoth Supreme", "Supreme");
		mapBoss("deranged-archaeologist", "Deranged Archaeologist");
		mapBoss("giant-mole", "Giant Mole", "Mole");
		mapBoss("hespori", "Hespori");
		mapBoss("kalphite-queen", "Kalphite Queen", "KQ");
		mapBoss("mimic", "Mimic");
		mapBoss("nex", "Nex");
		mapBoss("nightmare", "Nightmare", "The Nightmare");
		mapBoss("phosanis-nightmare", "Phosani's Nightmare", "Phosani");
		mapBoss("obor", "Obor");
		mapBoss("phantom-muspah", "Phantom Muspah", "Muspah");
		mapBoss("sarachnis", "Sarachnis");
		mapBoss("scurrius", "Scurrius");
		mapBoss("skotizo", "Skotizo");
		mapBoss("tempoross", "Tempoross");
		mapBoss("crystalline-hunllef", "The Gauntlet", "Gauntlet", "Crystalline Hunllef");
		mapBoss("corrupted-hunllef", "The Corrupted Gauntlet", "Corrupted Gauntlet", "CG", "Corrupted Hunllef");
		mapBoss("tzkal-zuk", "TzKal-Zuk", "Zuk", "Inferno");
		mapBoss("tztok-jad", "TzTok-Jad", "Jad", "Fight Caves");
		mapBoss("vorkath", "Vorkath");
		mapBoss("wintertodt", "Wintertodt");
		mapBoss("zalcano", "Zalcano");
		mapBoss("zulrah", "Zulrah");

		// New bosses
		mapBoss("amoxliatl", "Amoxliatl");
		mapBoss("hueycoatl", "The Hueycoatl", "Hueycoatl");
		mapBoss("royal-titans", "Royal Titans");
		mapBoss("lunar-chests", "Lunar Chests");
		mapBoss("fortis-colosseum", "Fortis Colosseum", "Colosseum", "Sol Heredit");
		mapBoss("moons-of-peril", "Moons of Peril", "Moons");
		mapBoss("doom-of-mokhaiotil", "Doom of Mokhaiotil");
		mapBoss("yama", "Yama");
	}

	/**
	 * Maps multiple name variants to a single boss ID.
	 */
	private static void mapBoss(String id, String... names)
	{
		for (String name : names)
		{
			NAME_TO_ID.put(normalize(name), id);
		}
	}

	/**
	 * Normalizes a boss name for lookup.
	 */
	private static String normalize(String name)
	{
		return name.toLowerCase()
			.replace("'", "")
			.replace("\"", "")
			.replace(":", "")
			.replaceAll("\\s+", " ")
			.trim();
	}

	/**
	 * Gets the standardized boss ID for a chat message boss name.
	 */
	public static String getBossId(String chatName)
	{
		String normalized = normalize(chatName);
		String id = NAME_TO_ID.get(normalized);

		if (id == null)
		{
			// Try partial matching for names not in the map
			for (Map.Entry<String, String> entry : NAME_TO_ID.entrySet())
			{
				if (normalized.contains(entry.getKey()) || entry.getKey().contains(normalized))
				{
					return entry.getValue();
				}
			}

			// Fall back to generating an ID from the name
			return normalized.replaceAll("\\s+", "-");
		}

		return id;
	}

	/**
	 * Checks if a boss name has a known mapping.
	 */
	public static boolean hasMapping(String chatName)
	{
		return NAME_TO_ID.containsKey(normalize(chatName));
	}
}
