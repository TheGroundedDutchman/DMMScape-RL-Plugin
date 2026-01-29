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
import java.util.List;

/**
 * Achievement Diary data with tiered task lists.
 */
public class DiaryData
{
	@SerializedName("id")
	private String id;

	@SerializedName("name")
	private String name;

	@SerializedName("area")
	private String area;

	@SerializedName("taskmaster")
	private String taskmaster;

	@SerializedName("itemReward")
	private String itemReward;

	@SerializedName("totalPoints")
	private Integer totalPoints;

	@SerializedName("tiers")
	private DiaryTiers tiers;

	public String getId()
	{
		return id;
	}
	public String getName()
	{
		return name;
	}
	public String getArea()
	{
		return area;
	}
	public String getTaskmaster()
	{
		return taskmaster;
	}
	public String getItemReward()
	{
		return itemReward;
	}
	public Integer getTotalPoints()
	{
		return totalPoints;
	}

	public DiaryTierData getTier(String tier)
	{
		if (tiers == null || tier == null)
		{
			return null;
		}
		switch (tier.toLowerCase())
		{
			case "easy": return tiers.easy;
			case "medium": return tiers.medium;
			case "hard": return tiers.hard;
			case "elite": return tiers.elite;
			default: return null;
		}
	}

	public DiaryTiers getTiers()
	{
		return tiers != null ? tiers : new DiaryTiers();
	}

	public static class DiaryTiers
	{
		@SerializedName("easy")
		private DiaryTierData easy;

		@SerializedName("medium")
		private DiaryTierData medium;

		@SerializedName("hard")
		private DiaryTierData hard;

		@SerializedName("elite")
		private DiaryTierData elite;

		public DiaryTierData getEasy()
		{
			return easy;
		}
		public DiaryTierData getMedium()
		{
			return medium;
		}
		public DiaryTierData getHard()
		{
			return hard;
		}
		public DiaryTierData getElite()
		{
			return elite;
		}

		public List<DiaryTierData> asList()
		{
			return List.of(
				easy != null ? easy : new DiaryTierData(),
				medium != null ? medium : new DiaryTierData(),
				hard != null ? hard : new DiaryTierData(),
				elite != null ? elite : new DiaryTierData()
			);
		}
	}
}
