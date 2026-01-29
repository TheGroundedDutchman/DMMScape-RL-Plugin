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
import java.util.Collections;
import java.util.List;

/**
 * Individual diary task details.
 */
public class DiaryTaskData
{
	@SerializedName("id")
	private String id;

	@SerializedName("taskNumber")
	private int taskNumber;

	@SerializedName("description")
	private String description;

	@SerializedName("points")
	private int points;

	@SerializedName("skillRequirements")
	private List<DiarySkillRequirement> skillRequirements;

	@SerializedName("questRequirements")
	private List<DiaryQuestRequirement> questRequirements;

	@SerializedName("itemRequirements")
	private List<String> itemRequirements;

	@SerializedName("notes")
	private List<String> notes;

	@SerializedName("location")
	private LocationData location;

	@SerializedName("matchedFrom")
	private String matchedFrom;

	@SerializedName("completion")
	private DiaryTaskCompletionRule completion;

	public String getId()
	{
		return id;
	}
	public int getTaskNumber()
	{
		return taskNumber;
	}
	public String getDescription()
	{
		return description;
	}
	public int getPoints()
	{
		return points;
	}
	public List<DiarySkillRequirement> getSkillRequirements()
	{
		return skillRequirements != null ? skillRequirements : Collections.emptyList();
	}
	public List<DiaryQuestRequirement> getQuestRequirements()
	{
		return questRequirements != null ? questRequirements : Collections.emptyList();
	}
	public List<String> getItemRequirements()
	{
		return itemRequirements != null ? itemRequirements : Collections.emptyList();
	}
	public List<String> getNotes()
	{
		return notes != null ? notes : Collections.emptyList();
	}
	public String getMatchedFrom()
	{
		return matchedFrom;
	}

	public DiaryTaskCompletionRule getCompletion()
	{
		return completion;
	}

	public LocationData getLocation()
	{
		return location;
	}

	public WorldPoint getWorldPoint()
	{
		if (location == null) return null;
		return new WorldPoint(location.getX(), location.getY(), location.getZ());
	}
}
