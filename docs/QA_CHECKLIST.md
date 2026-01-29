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

# QA Checklist

This checklist is designed for manual testing before releases or Plugin Hub submissions.

## Setup

- [ ] Build RuneLite with the plugin in the built-in plugins directory (not sideloaded).
- [ ] Confirm `DMMScape vX started` appears in logs.
- [ ] Confirm no `LambdaConversionException` in logs.

## Sync

- [ ] Initial login triggers a full sync when `Enable Auto-Sync` is on.
- [ ] Manual sync button triggers full sync and reports status.
- [ ] Quest points delta triggers sync when `Track Quest Points` is enabled.
- [ ] Skill level-up triggers delta sync.
- [ ] Boss KC chat message triggers delta sync and UI updates.
- [ ] Diary tier completion syncs and updates UI.
- [ ] Collection log count syncs when enabled.

## Webapp Progress Import

- [ ] Webapp progress is fetched after initial sync (only when sync enabled).
- [ ] Prompt shows differences when webapp has higher values.
- [ ] Auto-import merges without prompting when enabled.

## Targets + Overlays

- [ ] Targets load from the webapp with polling enabled.
- [ ] World map markers show up (respect `Max World Map Items`).
- [ ] Scene highlight + minimap markers display for nearby targets.
- [ ] Route lines show in scene/minimap/world map when enabled.
- [ ] Target toggle updates webapp and reverts on failure.

## Focus Navigation (Diaries)

- [ ] Clicking a diary location sets a focus target.
- [ ] Focus target renders overlays even if plan sync is disabled.
- [ ] Clearing navigation removes focus overlays.

## Player Alarm

- [ ] Alarm triggers when a new player appears nearby.
- [ ] Friends/clan/friends-chat are ignored when toggled.
- [ ] Cooldown prevents repeated alerts for the same player.
- [ ] Notifications honor sound/tray/flash settings.

## Configuration

- [ ] Toggling `Enable Plan Sync` starts/stops polling.
- [ ] Changing `Sync Interval` reschedules polling.
- [ ] Toggling overlay flags updates overlays immediately.
- [ ] Changing API key/target URL resets auth and refreshes targets.
