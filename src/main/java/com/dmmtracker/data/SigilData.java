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
import net.runelite.api.coords.WorldPoint;

/**
 * Sigil data for DMM point tracking.
 */
public class SigilData
{
	@SerializedName("id")
	private String id;

	@SerializedName("name")
	private String name;

	@SerializedName("shortDesc")
	private String shortDesc;

	@SerializedName("category")
	private String category;

	@SerializedName("tier")
	private int tier;

	@SerializedName("unlockType")
	private String unlockType;

	@SerializedName("combatStyle")
	private String combatStyle;

	@SerializedName(value = "cost", alternate = { "points" })
	private int cost;

	@SerializedName("icon")
	private String icon;

	@SerializedName("effectIcon")
	private String effectIcon;

	@SerializedName("location")
	private LocationData location;

	@SerializedName("wikiUrl")
	private String wikiUrl;

	// Getters
	public String getId()
	{
		return id;
	}
	public String getName()
	{
		return name;
	}
	public String getShortDesc()
	{
		return shortDesc;
	}
	public String getCategory()
	{
		return category;
	}
	public int getTier()
	{
		return tier;
	}
	public String getUnlockType()
	{
		return unlockType;
	}
	public String getCombatStyle()
	{
		return combatStyle;
	}
	public int getCost()
	{
		return cost;
	}
	public int getPoints()
	{
		return cost;
	}
	public String getIcon()
	{
		return icon;
	}
	public String getEffectIcon()
	{
		return effectIcon;
	}
	public LocationData getLocation()
	{
		return location;
	}
	public String getWikiUrl()
	{
		if (wikiUrl != null) return wikiUrl;
		return "https://oldschool.runescape.wiki/w/Sigil";
	}

	public WorldPoint getWorldPoint()
	{
		if (location == null) return null;
		return new WorldPoint(location.getX(), location.getY(), location.getZ());
	}

	@Override
	public String toString()
	{
		return name + " (" + cost + " pts)";
	}
}
