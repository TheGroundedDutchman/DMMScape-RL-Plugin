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

# Security and Privacy Notes

This document summarizes what data the plugin collects, how it is stored, and when it is transmitted. It is intended for reviewers and contributors.

## Data collected (from the game client)

The plugin reads game state to sync progress and render UI:

- Combat Achievements completion bitfields
- Quest points and completed quests
- Skill levels (from `StatChanged` events)
- Diary tier completion and per-tier task counts
- Boss KC (parsed from chat messages)
- Clue completion counts (parsed from chat messages)
- Collection log total count
- Player presence (for the player alarm feature)

No in-game chat history beyond specific completion/KC messages is processed.

## Data transmitted

When sync is enabled, the plugin sends progress to the configured sync endpoint. The payload is derived from `SyncData`:

- Account name
- Profile type (DMM/LEAGUES/MAIN derived from world type)
- Progress data (CAs, quests, skills, diaries, KC, clues, collection log)
- Optional mapping data (CA name mapping)
- Optional ownership unlocks (sigils, lamps, prayers)

Target sync requests include target IDs and completion flags when the user toggles completion in the UI.

See `docs/DATA_CONTRACTS.md` for payload structure.

## Network access

Network requests are made via OkHttp for:

- Progress sync (`webhookUrl`)
- Webapp progress fetch (`/api/me/progress` via `webhookUrl` base)
- Plan target fetch and updates (`targetSyncUrl`)
- Device linking and guest flows (`/auth/*` and `/me/device/*`)
- Sync token refresh (`/me/sync-token`)
- Data catalog updates (`/api/plugin-data`)

Defaults are DMMScape endpoints, but hosts may change if a user configures custom URLs.

## Local storage

The plugin stores non-sensitive and sensitive data locally:

- `~/.runelite/dmmtracker/dmmscape-progress.json` (UI state: completed tasks, owned unlocks, boss KC, diary counts)
- `~/.runelite/dmmtracker/dmmscape-data-cache.json` and `dmmscape-data-etag.txt` (data catalog cache)
- RuneLite config group `dmmtracker`:
  - `accessToken` (device-code access token)
  - `linkedUsername`
  - `linkedIsGuest`
  - user-facing config keys (sync URLs, toggles)

The access token is stored in RuneLite config in plaintext (RuneLite standard behavior). Do not share your RuneLite config directory.

## UI and browser actions

- The plugin opens the user’s default browser for device-code linking.
- The plugin may open the DMMScape website via the “Open DMMScape” button.

## Security characteristics

- The plugin does not execute remote code. Remote JSON data is used for rendering and mapping only.
- No third-party analytics SDKs are embedded in the plugin.
- All HTTP requests include a fixed User-Agent string: `DMMScape-Companion/1.0`.

## Review checklist (security)

- Confirm that all network requests are to intended endpoints.
- Confirm that no credentials are logged.
- Confirm that progress is only transmitted when sync is enabled.
- Confirm that local data storage paths are within `~/.runelite`.
