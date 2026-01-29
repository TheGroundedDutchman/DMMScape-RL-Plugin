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

import com.dmmtracker.ApiConfig;
import net.runelite.client.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages and caches icons for the DMMScape plugin UI.
 * Icons are loaded from resources on first access and cached for reuse.
 */
public class IconManager
{
	private static final Logger log = LoggerFactory.getLogger(IconManager.class);
	private static final String[] ICON_PATHS =
	{
		"/com/dmmtracker/icons/",
		"/net/runelite/client/plugins/dmmtracker/icons/"
	};
	private static final Map<String, BufferedImage> imageCache = new HashMap<>();
	private static final Map<String, ImageIcon> iconCache = new HashMap<>();
	private static final Map<String, ImageIcon> remoteIconCache = new ConcurrentHashMap<>();
	private static final Set<String> remoteLoading = ConcurrentHashMap.newKeySet();
	private static final ExecutorService REMOTE_EXECUTOR = Executors.newFixedThreadPool(2);

	// Icon names
	public static final String NAV_ICON = "nav_icon";
	public static final String DMMSCAPE_LOGO = "dmmscape_logo";
	public static final String PLACEHOLDER = "placeholder";

	// Section icons
	public static final String SECTION_CA = "section_ca";
	public static final String SECTION_DIARY = "section_diary";
	public static final String SECTION_PLAN = "section_plan";
	public static final String SECTION_QUEST = "section_quest";
	public static final String SECTION_SIGIL = "section_sigil";
	public static final String LAW_RUNE_DETAIL = "law_rune_detail";

	// CA tier icons
	public static final String CA_EASY = "ca_easy";
	public static final String CA_MEDIUM = "ca_medium";
	public static final String CA_HARD = "ca_hard";
	public static final String CA_ELITE = "ca_elite";
	public static final String CA_MASTER = "ca_master";
	public static final String CA_GRANDMASTER = "ca_grandmaster";

	// Skill icons
	public static final String SKILL_ATTACK = "skill_attack";
	public static final String SKILL_DEFENCE = "skill_defence";
	public static final String SKILL_STRENGTH = "skill_strength";
	public static final String SKILL_HITPOINTS = "skill_hitpoints";
	public static final String SKILL_RANGED = "skill_ranged";
	public static final String SKILL_MAGIC = "skill_magic";
	public static final String SKILL_PRAYER = "skill_prayer";
	public static final String SKILL_SLAYER = "skill_slayer";

	// Skill icon URL base (hosted on webapp)
	private static final String SKILL_ICON_BASE = "/assets/external/oldschool.runescape.wiki/images/";

	// Map skill names to icon URLs (loaded from webapp)
	private static final Map<String, String> SKILL_ICON_URLS = new HashMap<>();
	static
	{
		SKILL_ICON_URLS.put("attack", SKILL_ICON_BASE + "Attack_icon.png");
		SKILL_ICON_URLS.put("hitpoints", SKILL_ICON_BASE + "Hitpoints_icon.png");
		SKILL_ICON_URLS.put("mining", SKILL_ICON_BASE + "Mining_icon.png");
		SKILL_ICON_URLS.put("strength", SKILL_ICON_BASE + "Strength_icon.png");
		SKILL_ICON_URLS.put("agility", SKILL_ICON_BASE + "Agility_icon.png");
		SKILL_ICON_URLS.put("smithing", SKILL_ICON_BASE + "Smithing_icon.png");
		SKILL_ICON_URLS.put("defence", SKILL_ICON_BASE + "Defence_icon.png");
		SKILL_ICON_URLS.put("herblore", SKILL_ICON_BASE + "Herblore_icon.png");
		SKILL_ICON_URLS.put("fishing", SKILL_ICON_BASE + "Fishing_icon.png");
		SKILL_ICON_URLS.put("ranged", SKILL_ICON_BASE + "Ranged_icon.png");
		SKILL_ICON_URLS.put("thieving", SKILL_ICON_BASE + "Thieving_icon.png");
		SKILL_ICON_URLS.put("cooking", SKILL_ICON_BASE + "Cooking_icon.png");
		SKILL_ICON_URLS.put("prayer", SKILL_ICON_BASE + "Prayer_icon.png");
		SKILL_ICON_URLS.put("crafting", SKILL_ICON_BASE + "Crafting_icon.png");
		SKILL_ICON_URLS.put("firemaking", SKILL_ICON_BASE + "Firemaking_icon.png");
		SKILL_ICON_URLS.put("magic", SKILL_ICON_BASE + "Magic_icon.png");
		SKILL_ICON_URLS.put("fletching", SKILL_ICON_BASE + "Fletching_icon.png");
		SKILL_ICON_URLS.put("woodcutting", SKILL_ICON_BASE + "Woodcutting_icon.png");
		SKILL_ICON_URLS.put("runecraft", SKILL_ICON_BASE + "Runecraft_icon.png");
		SKILL_ICON_URLS.put("slayer", SKILL_ICON_BASE + "Slayer_icon.png");
		SKILL_ICON_URLS.put("farming", SKILL_ICON_BASE + "Farming_icon.png");
		SKILL_ICON_URLS.put("construction", SKILL_ICON_BASE + "Construction_icon.png");
		SKILL_ICON_URLS.put("hunter", SKILL_ICON_BASE + "Hunter_icon.png");
		SKILL_ICON_URLS.put("sailing", SKILL_ICON_BASE + "Sailing_icon.png");
	}

	// Category icons
	public static final String CATEGORY_BOSS = "category_boss";

	/**
	 * Gets a BufferedImage for the given icon name.
	 * Images are cached after first load.
	 */
	public static BufferedImage getImage(String name)
	{
		return imageCache.computeIfAbsent(name, IconManager::loadImage);
	}

	/**
	 * Gets an ImageIcon for the given icon name, optionally scaled.
	 */
	public static ImageIcon getIcon(String name)
	{
		return iconCache.computeIfAbsent(name, n ->
		{
			BufferedImage img = getImage(n);
			return img != null ? new ImageIcon(img) : null;
		});
	}

	/**
	 * Gets an ImageIcon scaled to the specified size.
	 */
	public static ImageIcon getIcon(String name, int width, int height)
	{
		String cacheKey = name + "_" + width + "x" + height;
		return iconCache.computeIfAbsent(cacheKey, k ->
		{
			BufferedImage img = getImage(name);
			if (img == null)
			{
				return null;
			}
			Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
			return new ImageIcon(scaled);
		});
	}

	/**
	 * Gets the CA tier icon for a given tier name.
	 */
	public static ImageIcon getCATierIcon(String tier)
	{
		if (tier == null)
		{
			return null;
		}
		switch (tier.toLowerCase())
		{
			case "easy": return getIcon(CA_EASY);
			case "medium": return getIcon(CA_MEDIUM);
			case "hard": return getIcon(CA_HARD);
			case "elite": return getIcon(CA_ELITE);
			case "master": return getIcon(CA_MASTER);
			case "grandmaster": return getIcon(CA_GRANDMASTER);
			default: return null;
		}
	}

	/**
	 * Gets the CA tier icon scaled to specified size.
	 */
	public static ImageIcon getCATierIcon(String tier, int size)
	{
		if (tier == null)
		{
			return null;
		}
		switch (tier.toLowerCase())
		{
			case "easy": return getIcon(CA_EASY, size, size);
			case "medium": return getIcon(CA_MEDIUM, size, size);
			case "hard": return getIcon(CA_HARD, size, size);
			case "elite": return getIcon(CA_ELITE, size, size);
			case "master": return getIcon(CA_MASTER, size, size);
			case "grandmaster": return getIcon(CA_GRANDMASTER, size, size);
			default: return null;
		}
	}

	/**
	 * Loads a remote icon with caching, returning a placeholder immediately.
	 */
	public static ImageIcon getRemoteIcon(String url, int width, int height, Runnable onLoad)
	{
		if (url == null || url.isEmpty())
		{
			return getIcon(PLACEHOLDER, width, height);
		}

		String resolved = resolveRemoteUrl(url);
		String cacheKey = resolved + "_" + width + "x" + height;
		ImageIcon cached = remoteIconCache.get(cacheKey);
		if (cached != null)
		{
			return cached;
		}

		if (remoteLoading.add(cacheKey))
		{
			REMOTE_EXECUTOR.submit(() ->
			{
				try
				{
					BufferedImage img = ImageIO.read(new URL(resolved));
					if (img != null)
					{
						Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
						remoteIconCache.put(cacheKey, new ImageIcon(scaled));
					}
				}
				catch (Exception e)
				{
					// Ignore loading failures
				}
				finally
				{
					remoteLoading.remove(cacheKey);
					if (onLoad != null)
					{
						SwingUtilities.invokeLater(onLoad);
					}
				}
			});
		}

		return getIcon(PLACEHOLDER, width, height);
	}

	private static String resolveRemoteUrl(String url)
	{
		if (url.startsWith("http://") || url.startsWith("https://"))
		{
			return url;
		}
		if (url.startsWith("/"))
		{
			return ApiConfig.webappBase() + url;
		}
		return ApiConfig.webappBase() + "/" + url;
	}

	/**
	 * Gets the icon for a target type (boss, diary, ca, quest, sigil, lamp, prayer, teleport).
	 */
	public static ImageIcon getTargetTypeIcon(String type)
	{
		if (type == null)
		{
			return getIcon(PLACEHOLDER);
		}
		switch (type.toLowerCase())
		{
			case "boss": return getIcon(CATEGORY_BOSS);
			case "diary": return getIcon(SECTION_DIARY);
			case "ca": return getIcon(SECTION_CA);
			case "quest": return getIcon(SECTION_QUEST);
			case "sigil": return getIcon(SECTION_SIGIL);
			case "lamp": return getIcon(SKILL_PRAYER); // Use prayer as lamp proxy
			case "prayer": return getIcon(SKILL_PRAYER);
			case "teleport": return getIcon(SKILL_MAGIC);
			default: return getIcon(PLACEHOLDER);
		}
	}

	/**
	 * Gets the icon for a target type, scaled.
	 */
	public static ImageIcon getTargetTypeIcon(String type, int size)
	{
		if (type == null)
		{
			return getIcon(PLACEHOLDER, size, size);
		}
		switch (type.toLowerCase())
		{
			case "boss": return getIcon(CATEGORY_BOSS, size, size);
			case "diary": return getIcon(SECTION_DIARY, size, size);
			case "ca": return getIcon(SECTION_CA, size, size);
			case "quest": return getIcon(SECTION_QUEST, size, size);
			case "sigil": return getIcon(SECTION_SIGIL, size, size);
			case "lamp": return getIcon(SKILL_PRAYER, size, size);
			case "prayer": return getIcon(SKILL_PRAYER, size, size);
			case "teleport": return getIcon(SKILL_MAGIC, size, size);
			default: return getIcon(PLACEHOLDER, size, size);
		}
	}

	/**
	 * Gets a colored category indicator for boss categories.
	 */
	public static Color getCategoryColor(String category)
	{
		if (category == null)
		{
			return DmmColors.TEXT_MUTED;
		}
		switch (category.toLowerCase())
		{
			case "slayer": return DmmColors.CATEGORY_SLAYER;
			case "wilderness": return DmmColors.CATEGORY_WILDERNESS;
			case "godwars": return DmmColors.CATEGORY_GODWARS;
			case "raids": return DmmColors.CATEGORY_RAIDS;
			case "dt2": return DmmColors.CATEGORY_DT2;
			default: return DmmColors.CATEGORY_OTHER;
		}
	}

	/**
	 * Creates a colored dot icon for category indicators.
	 */
	public static ImageIcon createCategoryDot(String category, int size)
	{
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(getCategoryColor(category));
		g.fillOval(0, 0, size, size);
		g.dispose();
		return new ImageIcon(img);
	}

	private static BufferedImage loadImage(String name)
	{
		for (String iconPath : ICON_PATHS)
		{
			try
			{
				BufferedImage img = ImageUtil.loadImageResource(IconManager.class, iconPath + name + ".png");
				if (img != null)
				{
					return img;
				}
			}
			catch (Exception e)
			{
				// Try next path
			}
		}
		log.warn("Failed to load icon: {}", name);
		return null;
	}

	/**
	 * Clears all cached icons. Useful for testing or memory management.
	 */
	public static void clearCache()
	{
		imageCache.clear();
		iconCache.clear();
	}

	/**
	 * Gets a skill icon by skill name.
	 * Loads from wiki URLs with caching.
	 * @param skillName The skill name (case-insensitive)
	 * @param size The icon size
	 * @param onLoad Optional callback when icon finishes loading
	 * @return The skill icon, or a placeholder if not yet loaded
	 */
	public static ImageIcon getSkillIcon(String skillName, int size, Runnable onLoad)
	{
		if (skillName == null || skillName.isEmpty())
		{
			return getIcon(PLACEHOLDER, size, size);
		}

		String normalizedName = skillName.toLowerCase().trim();
		String url = SKILL_ICON_URLS.get(normalizedName);

		if (url == null)
		{
			// Unknown skill, return placeholder
			return getIcon(PLACEHOLDER, size, size);
		}

		return getRemoteIcon(url, size, size, onLoad);
	}

	/**
	 * Gets a skill icon by skill name (no callback).
	 */
	public static ImageIcon getSkillIcon(String skillName, int size)
	{
		return getSkillIcon(skillName, size, null);
	}
}
