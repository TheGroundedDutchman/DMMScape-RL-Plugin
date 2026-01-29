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

# Code Map

A file-level map to speed up code reviews and onboarding.

## Core plugin

- `src/main/java/com/dmmtracker/DMMTrackerPlugin.java` - main lifecycle, event handling, sync orchestration
- `src/main/java/com/dmmtracker/DMMTrackerConfig.java` - config keys and defaults
- `src/main/java/com/dmmtracker/ProgressStore.java` - local persistence for UI state

## Sync + auth

- `src/main/java/com/dmmtracker/SyncService.java` - HTTP sync + target update requests
- `src/main/java/com/dmmtracker/SyncData.java` - sync payload structure
- `src/main/java/com/dmmtracker/DeviceAuthService.java` - device code + guest auth flow
- `src/main/java/com/dmmtracker/RuneliteTokenService.java` - short-lived sync token refresh

## Targets + overlays

- `src/main/java/com/dmmtracker/TargetService.java` - polling + parsing targets
- `src/main/java/com/dmmtracker/TargetPoint.java` - target model
- `src/main/java/com/dmmtracker/TargetMapPoint.java` - world map marker wrapper
- `src/main/java/com/dmmtracker/DMMTargetOverlay.java` - scene highlights
- `src/main/java/com/dmmtracker/DMMMinimapOverlay.java` - minimap markers + arrows
- `src/main/java/com/dmmtracker/RouteOverlay.java` - scene route lines
- `src/main/java/com/dmmtracker/WorldMapRouteOverlay.java` - world map route lines

## UI

- `src/main/java/com/dmmtracker/ui/DMMTabbedPanel.java` - main panel + tabs
- `src/main/java/com/dmmtracker/ui/tabs/*` - tab panels (Targets, Bosses, Sigils, Diaries, CAs, Search)
- `src/main/java/com/dmmtracker/ui/components/*` - shared UI components

## Data catalog

- `src/main/java/com/dmmtracker/data/DataLoader.java` - bundled + cached + remote data loader
- `src/main/java/com/dmmtracker/data/*` - data models for bosses, diaries, CAs, sigils, etc.
- `src/main/resources/net/runelite/client/plugins/dmmtracker/data/dmmscape-data.json` - bundled dataset

## Mapping and utilities

- `src/main/java/com/dmmtracker/TaskLookup.java` - mapping game IDs/names to dataset IDs
- `src/main/java/com/dmmtracker/RequirementChecker.java` - skill/quest requirement checks
- `src/main/java/com/dmmtracker/BossNameMapper.java` - KC chat name normalization
- `src/main/java/com/dmmtracker/CAIDMapper.java` - CA ID helpers
- `src/main/java/com/dmmtracker/DiaryNameUtils.java` - diary name normalization
