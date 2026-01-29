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
 * DMMScape Companion Configuration
 */
package com.dmmtracker;

import com.dmmtracker.ProfileType;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import java.awt.Color;

@ConfigGroup("dmmtracker")
public interface DMMTrackerConfig extends Config
{
	@ConfigSection(
		name = "Profile",
		description = "Profile type and gameplay mode settings",
		position = 0
	)
	String profileSection = "profile";

	@ConfigItem(
		keyName = "profileType",
		name = "Profile Type",
		description = "Select your gameplay mode to use the correct point values",
		position = 0,
		section = profileSection
	)
	default ProfileType profileType()
	{
		return ProfileType.DMM;
	}

	@ConfigSection(
		name = "Sync Settings",
		description = "Settings for syncing data to the web app",
		position = 0
	)
	String syncSection = "sync";

	@ConfigItem(
		keyName = "syncEnabled",
		name = "Enable Auto-Sync",
		description = "Automatically sync progress to the DMMScape web app",
		position = 1,
		section = syncSection
	)
	default boolean syncEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "webhookUrl",
		name = "Sync URL",
		description = "The URL to send sync data to (leave empty for manual export only)",
		position = 2,
		section = syncSection
	)
	default String webhookUrl()
	{
		return ApiConfig.syncUrl();
	}

	@ConfigItem(
		keyName = "apiKey",
		name = "API Key (Optional)",
		description = "Optional fallback key from DMMScape Settings > RuneLite API Key",
		position = 3,
		section = syncSection,
		secret = true
	)
	default String apiKey()
	{
		return "";
	}

	@ConfigItem(
		keyName = "autoImportWebapp",
		name = "Auto-Import from Webapp",
		description = "Automatically import progress from the webapp without prompting",
		position = 4,
		section = syncSection
	)
	default boolean autoImportWebapp()
	{
		return true;
	}

	@ConfigSection(
		name = "Tracking Options",
		description = "Choose what to track",
		position = 1
	)
	String trackingSection = "tracking";

	@ConfigItem(
		keyName = "trackCAs",
		name = "Track Combat Achievements",
		description = "Track Combat Achievement completions",
		position = 1,
		section = trackingSection
	)
	default boolean trackCAs()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackBossKC",
		name = "Track Boss Kill Count",
		description = "Track boss kill counts from chat messages",
		position = 2,
		section = trackingSection
	)
	default boolean trackBossKC()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackDiaries",
		name = "Track Diary Tiers",
		description = "Track Achievement Diary tier completions",
		position = 3,
		section = trackingSection
	)
	default boolean trackDiaries()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackQuests",
		name = "Track Quest Points",
		description = "Track total quest points",
		position = 4,
		section = trackingSection
	)
	default boolean trackQuests()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackCollectionLog",
		name = "Track Collection Log",
		description = "Track total Collection Log entries",
		position = 5,
		section = trackingSection
	)
	default boolean trackCollectionLog()
	{
		return true;
	}

	@ConfigSection(
		name = "Display",
		description = "Display options",
		position = 2
	)
	String displaySection = "display";

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show Points Overlay",
		description = "Show an overlay with your current DMM points",
		position = 1,
		section = displaySection
	)
	default boolean showOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = "defaultShowCompleted",
		name = "Show Completed by Default",
		description = "Start with 'Show completed' filter enabled in Plan tab",
		position = 2,
		section = displaySection
	)
	default boolean defaultShowCompleted()
	{
		return false;
	}

	@ConfigItem(
		keyName = "defaultReqMetFilter",
		name = "Req Met Filter by Default",
		description = "Start with 'Requirements Met' filter enabled in Combat Achievements tab",
		position = 3,
		section = displaySection
	)
	default boolean defaultReqMetFilter()
	{
		return false;
	}

	@ConfigItem(
		keyName = "defaultIncompleteFilter",
		name = "Incomplete Filter by Default",
		description = "Start with 'Incomplete Only' filter enabled in Combat Achievements tab",
		position = 4,
		section = displaySection
	)
	default boolean defaultIncompleteFilter()
	{
		return false;
	}

	// Player alarm configuration adapted from Wilderness Player Alarm by Alex (BSD 2-Clause).
	@ConfigSection(
		name = "Player Alarm",
		description = "Alert when other players appear nearby",
		position = 3
	)
	String playerAlarmSection = "playerAlarm";

	@ConfigItem(
		keyName = "playerAlarmEnabled",
		name = "Enable Player Alarm",
		description = "Alert when other players appear nearby",
		position = 1,
		section = playerAlarmSection
	)
	default boolean playerAlarmEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "alarmSound",
		name = "Sound Alert",
		description = "Play a sound when a player is detected",
		position = 2,
		section = playerAlarmSection
	)
	default boolean alarmSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alarmGameMessage",
		name = "Game Message",
		description = "Show a game message when a player is detected",
		position = 3,
		section = playerAlarmSection
	)
	default boolean alarmGameMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alarmTrayNotification",
		name = "Tray Notification",
		description = "Show a system tray notification when a player is detected",
		position = 4,
		section = playerAlarmSection
	)
	default boolean alarmTrayNotification()
	{
		return false;
	}

	@ConfigItem(
		keyName = "ignoreFriends",
		name = "Ignore Friends",
		description = "Don't alert for friends",
		position = 5,
		section = playerAlarmSection
	)
	default boolean ignoreFriends()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ignoreClanMembers",
		name = "Ignore Clan Members",
		description = "Don't alert for clan members",
		position = 6,
		section = playerAlarmSection
	)
	default boolean ignoreClanMembers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ignoreFriendsChat",
		name = "Ignore Friends Chat",
		description = "Don't alert for players in the same friends chat",
		position = 7,
		section = playerAlarmSection
	)
	default boolean ignoreFriendsChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alarmCooldown",
		name = "Alarm Cooldown (seconds)",
		description = "Minimum seconds between alarms for the same player",
		position = 8,
		section = playerAlarmSection
	)
	default int alarmCooldown()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "showCombatLevel",
		name = "Show Combat Level",
		description = "Include combat level in alert messages",
		position = 9,
		section = playerAlarmSection
	)
	default boolean showCombatLevel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alarmFlash",
		name = "Flash Screen",
		description = "Flash the game view until you interact with the client",
		position = 10,
		section = playerAlarmSection
	)
	default boolean alarmFlash()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "alarmFlashColor",
		name = "Flash Color",
		description = "Color used for the alarm flash",
		position = 11,
		section = playerAlarmSection
	)
	default Color alarmFlashColor()
	{
		return new Color(255, 30, 30, 120);
	}

	// ==================== TARGET OVERLAY SECTION ====================

	@ConfigSection(
		name = "Target Overlay",
		description = "Settings for target tracking overlay synced from webapp",
		position = 4
	)
	String targetSection = "target";

	@ConfigItem(
		keyName = "enableTargetSync",
		name = "Enable Target Sync",
		description = "Sync target list from webapp and show overlays",
		section = targetSection,
		position = 0
	)
	default boolean enableTargetSync()
	{
		return true;
	}

	@ConfigItem(
		keyName = "targetSyncUrl",
		name = "Target Sync URL",
		description = "URL to fetch target list from webapp (leave empty for file-based sync)",
		section = targetSection,
		position = 1
	)
	default String targetSyncUrl()
	{
		return ApiConfig.targetSyncUrl();
	}

	@ConfigItem(
		keyName = "showTargetOverlay",
		name = "Show Target Overlay",
		description = "Show minimap arrow and tile highlight for next target",
		section = targetSection,
		position = 2
	)
	default boolean showTargetOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "allowTargetEdits",
		name = "Allow In-Game Completion",
		description = "Allow toggling plan targets from the plugin panel",
		section = targetSection,
		position = 3
	)
	default boolean allowTargetEdits()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showWorldMapTargets",
		name = "Show World Map Markers",
		description = "Show plan targets on the world map",
		section = targetSection,
		position = 4
	)
	default boolean showWorldMapTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showWorldMapLegend",
		name = "Show World Map Legend",
		description = "Show legend and layer toggles on the world map",
		section = targetSection,
		position = 5
	)
	default boolean showWorldMapLegend()
	{
		return true;
	}

	@ConfigItem(
		keyName = "worldMapLegendCollapsed",
		name = "Collapse World Map Legend",
		description = "Collapse the world map legend panel by default",
		section = targetSection,
		position = 6
	)
	default boolean worldMapLegendCollapsed()
	{
		return false;
	}

	@ConfigItem(
		keyName = "worldMapLegendX",
		name = "World Map Legend X",
		description = "Stored x position for the world map legend",
		section = targetSection,
		position = 7,
		hidden = true
	)
	default int worldMapLegendX()
	{
		return -1;
	}

	@ConfigItem(
		keyName = "worldMapLegendY",
		name = "World Map Legend Y",
		description = "Stored y position for the world map legend",
		section = targetSection,
		position = 8,
		hidden = true
	)
	default int worldMapLegendY()
	{
		return -1;
	}

	@ConfigItem(
		keyName = "maxMapTargets",
		name = "Max World Map Targets",
		description = "Maximum targets to show on the world map",
		section = targetSection,
		position = 9
	)
	default int maxMapTargets()
	{
		return 25;
	}

	@ConfigItem(
		keyName = "showWorldMapTeleports",
		name = "Show Teleports (Map)",
		description = "Show teleport locations on the world map",
		section = targetSection,
		position = 10
	)
	default boolean showWorldMapTeleports()
	{
		return false;
	}

	@ConfigItem(
		keyName = "maxWorldMapTeleports",
		name = "Max Teleports (Map)",
		description = "Maximum teleport markers to show on the world map",
		section = targetSection,
		position = 11
	)
	default int maxWorldMapTeleports()
	{
		return 200;
	}

	@ConfigItem(
		keyName = "maxOverlayTargets",
		name = "Max Overlay Targets",
		description = "Maximum targets to highlight in the scene/minimap overlays",
		section = targetSection,
		position = 12
	)
	default int maxOverlayTargets()
	{
		return 3;
	}

	@Alpha
	@ConfigItem(
		keyName = "targetOverlayColor",
		name = "Overlay Color",
		description = "Color for target markers and tile highlights",
		section = targetSection,
		position = 13
	)
	default Color targetOverlayColor()
	{
		return new Color(0, 255, 255, 180);
	}

	@ConfigItem(
		keyName = "targetPollInterval",
		name = "Sync Interval (seconds)",
		description = "How often to sync plan from the webapp (5-300 seconds)",
		section = targetSection,
		position = 14
	)
	default int targetPollInterval()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "showTargetInfoText",
		name = "Show Item Name",
		description = "Display plan item name text in the overlay",
		section = targetSection,
		position = 11
	)
	default boolean showTargetInfoText()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showTargetDistance",
		name = "Show Distance",
		description = "Display distance to plan item in the overlay",
		section = targetSection,
		position = 12
	)
	default boolean showTargetDistance()
	{
		return false;
	}

	// ==================== MAP FILTERS SECTION ====================

	@ConfigSection(
		name = "Map Filters",
		description = "Filters for plan items shown on map/overlays",
		position = 6
	)
	String mapFilterSection = "mapFilters";

	@ConfigItem(
		keyName = "mapShowCompleted",
		name = "Show Completed",
		description = "Include completed plan items in map overlays",
		section = mapFilterSection,
		position = 0
	)
	default boolean mapShowCompleted()
	{
		return false;
	}

	@ConfigItem(
		keyName = "mapShowDiaryTargets",
		name = "Show Diary Targets",
		description = "Show diary targets on map overlays",
		section = mapFilterSection,
		position = 1
	)
	default boolean mapShowDiaryTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "mapShowQuestTargets",
		name = "Show Quest Targets",
		description = "Show quest targets on map overlays",
		section = mapFilterSection,
		position = 2
	)
	default boolean mapShowQuestTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "mapShowBossTargets",
		name = "Show Boss Targets",
		description = "Show boss targets on map overlays",
		section = mapFilterSection,
		position = 3
	)
	default boolean mapShowBossTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "mapShowCaTargets",
		name = "Show CA Targets",
		description = "Show combat achievement targets on map overlays",
		section = mapFilterSection,
		position = 4
	)
	default boolean mapShowCaTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "mapShowSigilTargets",
		name = "Show Sigil Targets",
		description = "Show sigil targets on map overlays",
		section = mapFilterSection,
		position = 5
	)
	default boolean mapShowSigilTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "mapShowLampTargets",
		name = "Show Lamp Targets",
		description = "Show lamp targets on map overlays",
		section = mapFilterSection,
		position = 6
	)
	default boolean mapShowLampTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "mapShowPrayerTargets",
		name = "Show Prayer Targets",
		description = "Show prayer targets on map overlays",
		section = mapFilterSection,
		position = 7
	)
	default boolean mapShowPrayerTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "mapShowTeleportTargets",
		name = "Show Teleport Targets",
		description = "Show teleport targets on map overlays",
		section = mapFilterSection,
		position = 8
	)
	default boolean mapShowTeleportTargets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "mapShowCustomTargets",
		name = "Show Custom Targets",
		description = "Show custom targets on map overlays",
		section = mapFilterSection,
		position = 9
	)
	default boolean mapShowCustomTargets()
	{
		return true;
	}

	// ==================== ROUTE DISPLAY SECTION ====================

	@ConfigSection(
		name = "Route Display",
		description = "Settings for drawing routes between plan targets",
		position = 5
	)
	String routeSection = "route";

	@ConfigItem(
		keyName = "enableRouteDrawing",
		name = "Enable Route Drawing",
		description = "Draw lines connecting plan targets in order",
		section = routeSection,
		position = 0
	)
	default boolean enableRouteDrawing()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSceneRoute",
		name = "Show Scene Route",
		description = "Draw route lines in the game scene (3D world)",
		section = routeSection,
		position = 1
	)
	default boolean showSceneRoute()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMinimapRoute",
		name = "Show Minimap Route",
		description = "Draw route lines on the minimap",
		section = routeSection,
		position = 2
	)
	default boolean showMinimapRoute()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showWorldMapRoute",
		name = "Show World Map Route",
		description = "Draw route lines on the world map",
		section = routeSection,
		position = 3
	)
	default boolean showWorldMapRoute()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "routeColor",
		name = "Route Color",
		description = "Color for route lines",
		section = routeSection,
		position = 4
	)
	default Color routeColor()
	{
		return new Color(255, 128, 0, 200);
	}

	@ConfigItem(
		keyName = "routeLineWidth",
		name = "Route Line Width",
		description = "Width of route lines in pixels",
		section = routeSection,
		position = 5
	)
	default int routeLineWidth()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "showRouteArrows",
		name = "Show Direction Arrows",
		description = "Show arrows indicating route direction",
		section = routeSection,
		position = 6
	)
	default boolean showRouteArrows()
	{
		return true;
	}

	@ConfigItem(
		keyName = "maxRouteSegments",
		name = "Max Route Segments",
		description = "Maximum number of route segments to draw (0 = unlimited)",
		section = routeSection,
		position = 7
	)
	default int maxRouteSegments()
	{
		return 10;
	}
}
