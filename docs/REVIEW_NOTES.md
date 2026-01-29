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

# RuneLite Review Notes

This document is intended for reviewers. It summarizes behavior that typically matters for Plugin Hub review: network access, data usage, storage, and security posture.

## What the plugin does

- Syncs in-game progress to the DMMScape web app.
- Fetches plan targets from the web app and renders overlays (scene/minimap/world map).
- Provides a UI panel for viewing targets and toggling completion (if enabled).
- Implements a player alarm feature for nearby players.

## Network requests

All network calls are performed via OkHttp. The host is determined by config URLs and may differ from defaults if a user changes settings.

| Purpose | Default URL / Path | Method | Source |
| --- | --- | --- | --- |
| Sync progress | `webhookUrl` (default `https://dmmscape.com/api/me/sync/runelite`) | POST | `SyncService.sendSync` |
| Webapp progress import | `/api/me/progress` (derived from `webhookUrl`) | GET | `SyncService.fetchProgress` |
| Fetch targets | `targetSyncUrl` (default `https://dmmscape.com/api/me/plan/targets`) | GET | `TargetService.fetchTargets` |
| Update targets | `targetSyncUrl` | PATCH | `SyncService.sendTargetUpdates` |
| Add target to plan | `/api/me/plan/targets` (derived from `targetSyncUrl`) | POST | `SyncService.addToPlan` |
| Device code | `/auth/device/code` | POST | `DeviceAuthService` |
| Device status | `/auth/device/status` | GET | `DeviceAuthService` |
| Guest auth | `/auth/guest` | POST | `DeviceAuthService` |
| Guest login link | `/auth/guest/login-link` | POST | `DeviceAuthService` |
| Device revoke | `/me/device/revoke` | POST | `DeviceAuthService` |
| Sync token | `/me/sync-token` | POST | `RuneliteTokenService` |
| Data catalog | `https://dmmscape.com/api/plugin-data` | GET | `DataLoader.checkForUpdates` |

Notes:
- The API base URL is derived from `webhookUrl`/`targetSyncUrl` when possible; otherwise it defaults to `https://dmmscape.com/api`.
- For localhost dev APIs, `DeviceAuthService` adds a Supabase anon key header (local dev only).

## Data usage

The plugin reads game state (varps/varbits, quest status, skill levels, chat messages) to create progress updates. See `docs/DATA_CONTRACTS.md` for exact payloads.

## Local storage

- `~/.runelite/dmmtracker/dmmscape-progress.json` (progress store)
- `~/.runelite/dmmtracker/dmmscape-data-cache.json` + `dmmscape-data-etag.txt` (data cache)
- RuneLite config group `dmmtracker` for auth tokens and settings

## Security posture

- No remote code execution or script injection.
- Remote data is JSON used for rendering/mapping only.
- No third-party analytics SDKs embedded.

## User-visible behavior

- Opens browser for device-code linking.
- Adds overlays and world map markers when plan sync is enabled.
- Sends notifications for player alarm if enabled.

## Review-friendly entry points

- Main plugin entry: `src/main/java/com/dmmtracker/DMMTrackerPlugin.java`
- Network services: `SyncService`, `TargetService`, `DeviceAuthService`, `RuneliteTokenService`
- Data loader: `com.dmmtracker.data.DataLoader`
