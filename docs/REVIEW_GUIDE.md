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

# Review Guide (RuneLite Plugin Hub)

This guide is optimized for reviewers. It highlights the minimum set of documents and code entry points needed to validate behavior, safety, and compliance.

## 5-minute doc path

1) `docs/ARCHITECTURE.md` (diagrams + data flows)
2) `docs/SECURITY_PRIVACY.md` (data usage + storage)
3) `docs/DATA_CONTRACTS.md` (payload structure)
4) `docs/REVIEW_NOTES.md` (network call inventory)
5) `docs/QA_CHECKLIST.md` (manual test plan)

## Key code entry points

| Area | Files |
| --- | --- |
| Plugin entry + lifecycle | `src/main/java/com/dmmtracker/DMMTrackerPlugin.java` |
| Sync pipeline | `src/main/java/com/dmmtracker/SyncService.java`, `src/main/java/com/dmmtracker/SyncData.java` |
| Target sync | `src/main/java/com/dmmtracker/TargetService.java` |
| Auth + tokens | `src/main/java/com/dmmtracker/DeviceAuthService.java`, `src/main/java/com/dmmtracker/RuneliteTokenService.java` |
| Data loader | `src/main/java/com/dmmtracker/data/DataLoader.java` |
| Progress store | `src/main/java/com/dmmtracker/ProgressStore.java` |
| Overlays | `src/main/java/com/dmmtracker/DMMTargetOverlay.java`, `DMMMinimapOverlay.java`, `RouteOverlay.java`, `WorldMapRouteOverlay.java` |
| UI | `src/main/java/com/dmmtracker/ui/DMMTabbedPanel.java`, `src/main/java/com/dmmtracker/ui/tabs/*` |

For a full map, see `docs/CODE_MAP.md`.

## Network access overview

- Progress sync: `webhookUrl` (default `https://dmmscape.com/api/me/sync/runelite`)
- Target sync: `targetSyncUrl` (default `https://dmmscape.com/api/me/plan/targets`)
- Auth: `/auth/*` and `/me/device/*`
- Token refresh: `/me/sync-token`
- Data catalog update: `https://dmmscape.com/api/plugin-data`

All requests are via OkHttp with a fixed User-Agent `DMMScape-Companion/1.0`.

## Local storage

- `~/.runelite/dmmtracker/dmmscape-progress.json` (progress store)
- `~/.runelite/dmmtracker/dmmscape-data-cache.json` + `dmmscape-data-etag.txt` (data cache)
- RuneLite config group `dmmtracker` for auth tokens + settings

## Review constraints

- Development cannot use sideloaded plugins (EventBus lambdas fail). See `docs/DEVELOPER_GUIDE.md` for the required built-in plugin workflow.
- The plugin reads only specific game state and does not execute remote code.

## Recommended review flow

1) Confirm network inventory and data usage in `docs/SECURITY_PRIVACY.md` and `docs/DATA_CONTRACTS.md`.
2) Trace sync flow in `DMMTrackerPlugin.performInitialSync` and `SyncService.sendSync`.
3) Verify target polling in `TargetService.fetchTargets` and overlay rendering in `DMMTargetOverlay`/`RouteOverlay`.
4) Check auth state transitions in `DeviceAuthService` and token refresh in `RuneliteTokenService`.
5) Validate local storage in `ProgressStore` and `DataLoader`.

## Reviewer setup (optional)

If you want to run locally, use the built-in plugin location in RuneLite source (not sideloading). See `docs/DEVELOPER_GUIDE.md` for the exact commands.
