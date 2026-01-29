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
package com.dmmtracker.ui;

import java.awt.Color;

/**
 * Centralized color constants for the DMMScape plugin UI.
 * Provides consistent theming across all UI components.
 */
public final class DmmColors
{
	private DmmColors()
	{
	}

	// ============ Status Colors ============
	/** Success/completed state - darker green */
	public static final Color SUCCESS = new Color(0x27AE60);
	/** Success/completed state - brighter green */
	public static final Color SUCCESS_BRIGHT = new Color(0x2ECC71);
	/** Error/failed state - red */
	public static final Color ERROR = new Color(0xE74C3C);
	/** Warning/pending state - orange */
	public static final Color WARNING = new Color(0xE67E22);
	/** Pending/in-progress state - amber */
	public static final Color PENDING = new Color(0xF39C12);
	/** Info/link state - blue */
	public static final Color INFO = new Color(0x3498DB);
	/** Guest/special state - light blue */
	public static final Color GUEST = new Color(0x3498DB);
	/** Code display color - yellow */
	public static final Color CODE = new Color(0xF1C40F);
	/** Disabled/inactive state - gray */
	public static final Color DISABLED = new Color(0x95A5A6);

	// ============ Tier Colors ============
	/** Easy tier - green */
	public static final Color TIER_EASY = new Color(0x2ECC71);
	/** Medium tier - yellow */
	public static final Color TIER_MEDIUM = new Color(0xF1C40F);
	/** Hard tier - orange */
	public static final Color TIER_HARD = new Color(0xE67E22);
	/** Elite tier - purple */
	public static final Color TIER_ELITE = new Color(0x9B59B6);
	/** Master tier - red */
	public static final Color TIER_MASTER = new Color(0xE74C3C);
	/** Grandmaster tier - teal */
	public static final Color TIER_GRANDMASTER = new Color(0x1ABC9C);

	// ============ Category Colors ============
	/** Boss category - red */
	public static final Color CATEGORY_BOSS = new Color(0xE74C3C);
	/** Boss type alias (legacy) */
	public static final Color TYPE_BOSS = CATEGORY_BOSS;
	/** Slayer category - purple */
	public static final Color CATEGORY_SLAYER = new Color(0x9B59B6);
	/** Wilderness category - red */
	public static final Color CATEGORY_WILDERNESS = new Color(0xE74C3C);
	/** God Wars category - blue */
	public static final Color CATEGORY_GODWARS = new Color(0x3498DB);
	/** Raids category - orange/gold */
	public static final Color CATEGORY_RAIDS = new Color(0xF39C12);
	/** Desert Treasure 2 category - teal */
	public static final Color CATEGORY_DT2 = new Color(0x1ABC9C);
	/** Other/misc category - gray */
	public static final Color CATEGORY_OTHER = new Color(0x95A5A6);

	// ============ Content Type Colors ============
	/** Combat sigils - red */
	public static final Color TYPE_COMBAT = new Color(0xE74C3C);
	/** Skilling sigils - green */
	public static final Color TYPE_SKILLING = new Color(0x27AE60);
	/** Utility sigils - blue */
	public static final Color TYPE_UTILITY = new Color(0x3498DB);
	/** Prayer content - purple */
	public static final Color TYPE_PRAYER = new Color(0x9B59B6);
	/** Sigil content - orange/gold */
	public static final Color TYPE_SIGIL = new Color(0xF39C12);
	/** Lamp content - yellow */
	public static final Color TYPE_LAMP = new Color(0xF1C40F);
	/** Diary content - purple */
	public static final Color TYPE_DIARY = new Color(0x9B59B6);
	/** Combat Achievement content - teal */
	public static final Color TYPE_CA = new Color(0x1ABC9C);
	/** Skill content - purple */
	public static final Color TYPE_SKILL = new Color(0x9B59B6);
	/** Teleport content - blue */
	public static final Color TYPE_TELEPORT = new Color(0x4DA3FF);

	// ============ UI Element Colors ============
	/** Gold/currency display - yellow */
	public static final Color GOLD = new Color(0xF1C40F);
	/** High value points - gold/orange */
	public static final Color POINTS_HIGH = new Color(0xF39C12);
	/** Medium value points - green */
	public static final Color POINTS_MEDIUM = new Color(0x27AE60);
	/** Low value points - blue */
	public static final Color POINTS_LOW = new Color(0x3498DB);
	/** Muted/secondary text */
	public static final Color TEXT_MUTED = new Color(0x808080);

	// ============ Tab Colors ============
	/** Plan tab - green */
	public static final Color TAB_PLAN = new Color(0x27AE60);
	/** Boss tab - red */
	public static final Color TAB_BOSS = new Color(0xE74C3C);
	/** Sigil tab - orange/gold */
	public static final Color TAB_SIGIL = new Color(0xF39C12);
	/** Diary tab - purple */
	public static final Color TAB_DIARY = new Color(0x9B59B6);
	/** Combat Achievement tab - teal */
	public static final Color TAB_CA = new Color(0x1ABC9C);

	// ============ Hover/Interactive Colors ============
	/** Default hover background */
	public static final Color HOVER_BACKGROUND = new Color(0x3C3F41);
	/** Pressed/active background */
	public static final Color PRESSED_BACKGROUND = new Color(0x2B2D30);
	/** Selected item background */
	public static final Color SELECTED_BACKGROUND = new Color(0x4B6EAF);

	/**
	 * Get tier color by tier name.
	 * @param tier Tier name (easy, medium, hard, elite, master, grandmaster)
	 * @return The color for the tier, or DISABLED for unknown tiers
	 */
	public static Color getTierColor(String tier)
	{
		if (tier == null)
		{
			return DISABLED;
		}
		switch (tier.toLowerCase())
		{
			case "easy":
				return TIER_EASY;
			case "medium":
				return TIER_MEDIUM;
			case "hard":
				return TIER_HARD;
			case "elite":
				return TIER_ELITE;
			case "master":
				return TIER_MASTER;
			case "grandmaster":
				return TIER_GRANDMASTER;
			default:
				return DISABLED;
		}
	}

	/**
	 * Get boss category color by category name.
	 * @param category Category name (slayer, wilderness, godwars, raids, dt2, other)
	 * @return The color for the category, or CATEGORY_OTHER for unknown categories
	 */
	public static Color getBossCategoryColor(String category)
	{
		if (category == null)
		{
			return CATEGORY_OTHER;
		}
		switch (category.toLowerCase())
		{
			case "slayer":
				return CATEGORY_SLAYER;
			case "wilderness":
				return CATEGORY_WILDERNESS;
			case "godwars":
				return CATEGORY_GODWARS;
			case "raids":
				return CATEGORY_RAIDS;
			case "dt2":
				return CATEGORY_DT2;
			default:
				return CATEGORY_OTHER;
		}
	}

	/**
	 * Get sigil category color by category name.
	 * @param category Category name (combat, skilling, utility, prayer)
	 * @return The color for the category, or CATEGORY_OTHER for unknown categories
	 */
	public static Color getSigilCategoryColor(String category)
	{
		if (category == null)
		{
			return CATEGORY_OTHER;
		}
		switch (category.toLowerCase())
		{
			case "combat":
				return TYPE_COMBAT;
			case "skilling":
				return TYPE_SKILLING;
			case "utility":
				return TYPE_UTILITY;
			case "prayer":
				return TYPE_PRAYER;
			default:
				return CATEGORY_OTHER;
		}
	}

	/**
	 * Get search result type color.
	 * @param type Result type (Boss, Sigil, Lamp, Prayer, Skill, Diary, CA)
	 * @return The color for the type
	 */
	public static Color getSearchResultColor(String type)
	{
		if (type == null)
		{
			return DISABLED;
		}
		switch (type)
		{
			case "Boss":
				return CATEGORY_BOSS;
			case "Sigil":
				return TYPE_SIGIL;
			case "Lamp":
				return TYPE_LAMP;
			case "Prayer":
				return TYPE_PRAYER;
			case "Skill":
				return TYPE_SKILL;
			case "Diary":
				return TYPE_DIARY;
			case "CA":
				return TYPE_CA;
			default:
				return DISABLED;
		}
	}

	/**
	 * Get target type color.
	 * @param type Target type (boss, diary, ca, sigil, lamp, prayer, teleport)
	 * @return The color for the target type
	 */
	public static Color getTargetTypeColor(String type)
	{
		if (type == null)
		{
			return DISABLED;
		}
		switch (type.toLowerCase())
		{
			case "boss":
				return CATEGORY_BOSS;
			case "diary":
				return TYPE_DIARY;
			case "ca":
				return TYPE_CA;
			case "sigil":
				return TYPE_SIGIL;
			case "lamp":
				return TYPE_LAMP;
			case "prayer":
				return TYPE_PRAYER;
			case "teleport":
				return TYPE_TELEPORT;
			default:
				return DISABLED;
		}
	}
}
