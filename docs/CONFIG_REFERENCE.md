<!--
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
-->

# DMMScape RuneLite Plugin - Config Reference

This is the authoritative reference for all configuration keys in the `dmmtracker` config group. It reflects `DMMTrackerConfig` and internal keys stored by `DeviceAuthService`.

## Overview

- Config group: `dmmtracker`
- UI: RuneLite settings → DMMScape
- Storage: RuneLite `ConfigManager`
- Source of truth: `src/main/java/com/dmmtracker/DMMTrackerConfig.java`

## Profile

| Key | UI Label | Type | Default | Used By | Notes |
| --- | --- | --- | --- | --- | --- |
| `profileType` | Profile Type | enum `ProfileType` | `DMM` | (unused) | Currently not read in code; runtime profile type is derived from world types in `DMMTrackerPlugin.getProfileType()`. |

## Sync Settings

| Key | UI Label | Type | Default | Used By | Notes |
| --- | --- | --- | --- | --- | --- |
| `syncEnabled` | Enable Auto-Sync | boolean | `false` | `DMMTrackerPlugin` | Controls whether sync requests are sent automatically and whether webapp progress import runs. |
| `webhookUrl` | Sync URL | string | `https://dmmscape.com/api/me/sync/runelite` | `SyncService`, `DMMTrackerPlugin` | Primary endpoint for progress sync; also used to fetch webapp progress. |
| `apiKey` | API Key (Advanced) | string (secret) | empty | `DeviceAuthService`, `RuneliteTokenService` | Fallback credential when not linked via device code. |
| `autoImportWebapp` | Auto-Import from Webapp | boolean | `false` | `DMMTrackerPlugin` | If true, webapp progress differences auto-merge without prompting. |

## Tracking Options

| Key | UI Label | Type | Default | Used By | Notes |
| --- | --- | --- | --- | --- | --- |
| `trackCAs` | Track Combat Achievements | boolean | `true` | `DMMTrackerPlugin` | Controls CA varp tracking and initial CA sync. |
| `trackBossKC` | Track Boss Kill Count | boolean | `true` | `DMMTrackerPlugin` | Enables boss KC parsing from chat messages. |
| `trackDiaries` | Track Diary Tiers | boolean | `true` | `DMMTrackerPlugin` | Enables diary varbit tracking and journal parsing. |
| `trackQuests` | Track Quest Points | boolean | `true` | `DMMTrackerPlugin` | Enables quest point and completion tracking. |
| `trackCollectionLog` | Track Collection Log | boolean | `true` | `DMMTrackerPlugin` | Enables collection log varp tracking. |

Notes:
- Skill level tracking and clue completion tracking are always active when syncing; there is no config toggle for them.

## Display

| Key | UI Label | Type | Default | Used By | Notes |
| --- | --- | --- | --- | --- | --- |
| `showOverlay` | Show Points Overlay | boolean | `false` | (unused) | Currently not wired to any overlay; reserved for future points overlay. |
| `defaultShowCompleted` | Show Completed by Default | boolean | `false` | `TargetsTab` | Sets initial Plan tab filter state. |
| `defaultReqMetFilter` | Req Met Filter by Default | boolean | `false` | `CombatAchievementsTab` | Sets initial requirement filter state. |
| `defaultIncompleteFilter` | Incomplete Filter by Default | boolean | `false` | `CombatAchievementsTab` | Sets initial “incomplete only” filter state. |

## Player Alarm

| Key | UI Label | Type | Default | Used By | Notes |
| --- | --- | --- | --- | --- | --- |
| `playerAlarmEnabled` | Enable Player Alarm | boolean | `false` | `DMMTrackerPlugin` | Enables alerting when nearby players appear. |
| `alarmSound` | Sound Alert | boolean | `true` | `DMMTrackerPlugin` | Plays a sound when a player is detected. |
| `alarmGameMessage` | Game Message | boolean | `true` | `DMMTrackerPlugin` | Sends a chat message in-game when triggered. |
| `alarmTrayNotification` | Tray Notification | boolean | `false` | `DMMTrackerPlugin` | Shows a system tray notification. |
| `ignoreFriends` | Ignore Friends | boolean | `true` | `DMMTrackerPlugin` | Suppresses alerts for friends. |
| `ignoreClanMembers` | Ignore Clan Members | boolean | `true` | `DMMTrackerPlugin` | Suppresses alerts for clan members. |
| `ignoreFriendsChat` | Ignore Friends Chat | boolean | `true` | `DMMTrackerPlugin` | Suppresses alerts for friends chat members. |
| `alarmCooldown` | Alarm Cooldown (seconds) | int | `30` | `DMMTrackerPlugin` | Minimum seconds between alerts for the same player. |
| `showCombatLevel` | Show Combat Level | boolean | `true` | `DMMTrackerPlugin` | Adds combat level to alert message. |
| `alarmFlash` | Flash Screen | boolean | `true` | `DMMTrackerPlugin` | Flashes the client view until focus or interaction. |
| `alarmFlashColor` | Flash Color | color (RGBA) | `rgba(255,30,30,120)` | `DMMTrackerPlugin` | Color used for flash overlay. |

## Plan (Target Sync)

| Key | UI Label | Type | Default | Used By | Notes |
| --- | --- | --- | --- | --- | --- |
| `enableTargetSync` | Enable Plan Sync | boolean | `true` | `DMMTrackerPlugin`, `TargetService` | Starts/stops polling and enables overlays. |
| `targetSyncUrl` | Plan Sync URL | string | `https://dmmscape.com/api/me/plan/targets` | `TargetService`, `SyncService` | Fetch targets and submit target updates. |
| `showTargetOverlay` | Show Plan Overlay | boolean | `true` | `DMMTargetOverlay`, `DMMMinimapOverlay`, `DMMTrackerPlugin` | Controls scene/minimap overlays. |
| `allowTargetEdits` | Allow In-Game Completion | boolean | `true` | `DMMTrackerPlugin`, `TargetsTab` | Allows toggling completion in the plugin panel. |
| `showWorldMapTargets` | Show World Map Markers | boolean | `true` | `DMMTrackerPlugin` | Controls world map markers. |
| `showWorldMapLegend` | Show World Map Legend | boolean | `true` | `WorldMapLegendOverlay` | Toggle the legend panel on the world map. |
| `worldMapLegendCollapsed` | Collapse World Map Legend | boolean | `false` | `WorldMapLegendOverlay` | Whether the legend starts collapsed. |
| `maxMapTargets` | Max World Map Items | int | `25` | `DMMTrackerPlugin` | Caps world map markers. |
| `maxOverlayTargets` | Max Overlay Items | int | `3` | `DMMTargetOverlay`, `DMMMinimapOverlay` | Caps scene/minimap overlays. |
| `targetOverlayColor` | Overlay Color | color (RGBA) | `rgba(0,255,255,180)` | `DMMTargetOverlay`, `DMMMinimapOverlay` | Color for markers and highlights. |
| `targetPollInterval` | Sync Interval (seconds) | int | `5` | `TargetService` | Clamped to 5-300 seconds. |
| `showTargetInfoText` | Show Item Name | boolean | `false` | (unused) | Currently not used by overlays. |
| `showTargetDistance` | Show Distance | boolean | `false` | (unused) | Currently not used by overlays. |

## Route Display

| Key | UI Label | Type | Default | Used By | Notes |
| --- | --- | --- | --- | --- | --- |
| `enableRouteDrawing` | Enable Route Drawing | boolean | `true` | `RouteOverlay`, `WorldMapRouteOverlay`, `DMMMinimapOverlay` | Master toggle for route lines. |
| `showSceneRoute` | Show Scene Route | boolean | `true` | `RouteOverlay` | Scene (3D world) lines. |
| `showMinimapRoute` | Show Minimap Route | boolean | `true` | `DMMMinimapOverlay` | Minimap route lines. |
| `showWorldMapRoute` | Show World Map Route | boolean | `true` | `WorldMapRouteOverlay` | World map route lines. |
| `routeColor` | Route Color | color (RGBA) | `rgba(255,128,0,200)` | All route overlays | Line color. |
| `routeLineWidth` | Route Line Width | int | `2` | All route overlays | Pixel width. |
| `showRouteArrows` | Show Direction Arrows | boolean | `true` | All route overlays | Draws chevrons on lines. |
| `maxRouteSegments` | Max Route Segments | int | `10` | All route overlays | `0` means unlimited. |

## Internal keys (not exposed in UI)

These are stored in the same `dmmtracker` config group and used internally:

| Key | Type | Owner | Purpose |
| --- | --- | --- | --- |
| `accessToken` | string | `DeviceAuthService` | Stored device-code access token. |
| `linkedUsername` | string | `DeviceAuthService` | Display name for linked account. |
| `linkedIsGuest` | boolean (string) | `DeviceAuthService` | Whether the linked account is a guest. |
| `worldMapLegendX` | int | `WorldMapLegendOverlay` | Stored legend x position (canvas coords). |
| `worldMapLegendY` | int | `WorldMapLegendOverlay` | Stored legend y position (canvas coords). |

Do not set these manually unless you know what you are doing.

## Config change side-effects

`DMMTrackerPlugin.onConfigChanged` responds to several keys:

- `enableTargetSync`: starts/stops polling and adds/removes overlays.
- `targetPollInterval`: reschedules polling.
- `showTargetOverlay`: toggles scene/minimap overlays.
- `enableRouteDrawing`, `showSceneRoute`, `showMinimapRoute`, `showWorldMapRoute`: route overlays update at render time.
- `targetSyncUrl`, `apiKey`, `accessToken`: clears cached auth and refreshes target data.
- `showWorldMapTargets`, `maxMapTargets`: refreshes world map points.
- `allowTargetEdits`: refreshes panel toggles.
- `syncEnabled`: triggers a manual sync when enabled.
