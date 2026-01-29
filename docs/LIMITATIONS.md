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

# Known Limitations and Constraints

This list is intentionally candid to help reviewers and contributors understand current boundaries.

## Technical constraints

- Sideloaded plugins do not work reliably due to RuneLite EventBus lambda/classloader limitations. Development must use the built-in plugin location.
- Target updates are polling-based (no realtime push), so updates are eventually consistent.
- Overlay rendering is limited to the current plane; cross-plane targets are not visible in the scene.

## Data coverage

- Diary per-task completion is only available when the player opens the diary journal UI.
- CA mapping depends on the dataset shipped by the web app; missing entries can drop completions.
- Boss KC is parsed from chat messages and may miss KC changes not emitted in chat.
- Collection log is a total count only; per-item completion is not tracked.

## UI/UX limitations

- Large plans can feel noisy without grouping or search (some grouping exists but deep filtering is limited).
- Route lines are approximations and do not account for actual pathing or obstacles.
- Some display toggles exist but are not currently wired (see `docs/CONFIG_REFERENCE.md` for unused keys).

## Compatibility

- Requires Java 11+ for build; development uses Java 17.
- Requires RuneLite source for local development runs.
