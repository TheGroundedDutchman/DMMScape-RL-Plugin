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

# ADR 0003 - Full sync on login + delta sync on changes

Status: Accepted (2026-01-28)

## Context

RuneLite events are not guaranteed to replay historical state, so relying solely on deltas can miss progress that occurred before the plugin started. At the same time, sending the full dataset on every change is wasteful.

## Decision

Use a hybrid sync strategy:

- Full sync on login (`fullSync=true`) to capture the complete current state.
- Delta sync on subsequent changes (varp/varbit, stats, chat parsing).
- Manual sync triggers a new full sync on demand.

## Consequences

- Initial sync is heavier but only happens once per login session.
- Delta sync reduces payload sizes and network load after initialization.
- Local caches (`lastSkillLevels`, `lastBossKCs`, etc.) are required for delta detection.

## Alternatives considered

- Full-only sync: rejected due to bandwidth and server load.
- Delta-only sync: rejected due to missed progress when the plugin starts late.

## References

- `DMMTrackerPlugin.performInitialSync`
- `DMMTrackerPlugin.onVarbitChanged`
- `DMMTrackerPlugin.onStatChanged`
- `DMMTrackerPlugin.onChatMessage`
- `SyncData`
