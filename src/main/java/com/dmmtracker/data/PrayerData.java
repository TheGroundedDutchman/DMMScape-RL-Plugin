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

/**
 * Ruinous prayer data for DMM point tracking.
 */
public class PrayerData
{
	@SerializedName("id")
	private String id;

	@SerializedName("name")
	private String name;

	@SerializedName(value = "level", alternate = { "points" })
	private int level;

	@SerializedName("effect")
	private String effect;

	@SerializedName("drainRate")
	private String drainRate;

	@SerializedName("drainEffect")
	private int drainEffect;

	@SerializedName("icon")
	private String icon;

	@SerializedName("wikiUrl")
	private String wikiUrl;

	@SerializedName("notes")
	private String notes;

	// Getters
	public String getId()
	{
		return id;
	}
	public String getName()
	{
		return name;
	}
	public int getLevel()
	{
		return level;
	}
	public int getPoints()
	{
		return level;
	}
	public String getEffect()
	{
		return effect;
	}
	public String getDrainRate()
	{
		return drainRate;
	}
	public int getDrainEffect()
	{
		return drainEffect;
	}
	public String getIcon()
	{
		return icon;
	}
	public String getWikiUrl()
	{
		if (wikiUrl != null && !wikiUrl.isEmpty()) return wikiUrl;
		if (name != null && !name.isEmpty())
		{
			return "https://oldschool.runescape.wiki/w/" + name.replace(" ", "_");
		}
		return "https://oldschool.runescape.wiki/w/Ruinous_Powers";
	}
	public String getNotes()
	{
		return notes;
	}

	@Override
	public String toString()
	{
		return name + " (level " + level + ")";
	}
}
