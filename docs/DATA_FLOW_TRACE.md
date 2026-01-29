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

# Data Flow Trace

A step-by-step trace of the main data flows with code entry points.

## 1) Login full sync

1. `DMMTrackerPlugin.onGameStateChanged` schedules `performInitialSync`.
2. `performInitialSync` gathers all progress on the client thread.
3. `SyncService.sendSync` sends the payload.
4. `handleSyncResponse` merges owned unlocks and any progress returned.
5. `fetchWebappProgress` (optional) merges higher values from webapp.

Files:
- `DMMTrackerPlugin.java`
- `SyncService.java`
- `SyncData.java`

## 2) Delta sync (live updates)

- Varbits → diary tiers, CA completions, collection log
- StatChanged → skill level deltas
- ChatMessage → boss KC + clue completion

Files:
- `DMMTrackerPlugin.java`
- `BossNameMapper.java`

## 3) Target polling

1. `TargetService.startPolling` schedules `fetchTargets`.
2. `fetchTargets` selects URL or file source.
3. `parseTargets` updates in-memory list and timestamps.
4. `DMMTrackerPlugin.onGameTick` refreshes panel and world map markers.

Files:
- `TargetService.java`
- `DMMTrackerPlugin.java`

## 4) Target completion updates

1. User toggles completion in UI.
2. `DMMTrackerPlugin.handleTargetToggle` updates local state optimistically.
3. `SyncService.sendTargetUpdates` PATCHes to webapp.
4. Failure path reverts the local toggle.

Files:
- `DMMTrackerPlugin.java`
- `SyncService.java`

## 5) Overlays

- `DMMTargetOverlay`: scene tile highlights
- `DMMMinimapOverlay`: minimap markers + arrows
- `RouteOverlay`: scene route lines
- `WorldMapRouteOverlay`: world map route lines

Files:
- `DMMTargetOverlay.java`
- `DMMMinimapOverlay.java`
- `RouteOverlay.java`
- `WorldMapRouteOverlay.java`

## 6) Auth + token refresh

1. Device code flow: `DeviceAuthService.startLinking`.
2. Polling until authorized or expired.
3. `RuneliteTokenService.refreshIfNeeded` fetches short-lived sync tokens.
4. `DMMTrackerPlugin.syncAuthHeaderIfNeeded` updates `TargetService`.

Files:
- `DeviceAuthService.java`
- `RuneliteTokenService.java`
- `DMMTrackerPlugin.java`
