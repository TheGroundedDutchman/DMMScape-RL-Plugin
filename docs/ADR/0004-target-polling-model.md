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

# ADR 0004 - Polling-based target sync

Status: Accepted (2026-01-28)

## Context

The plugin must keep plan targets in sync with the DMMScape webapp. RuneLite plugins do not have a native push channel, and long-lived sockets can be unreliable in a game client environment.

## Decision

Use scheduled polling in `TargetService`:

- Poll interval is configurable and clamped (5s to 300s).
- Targets are fetched via HTTP GET when `enableTargetSync` is true.
- An optional local file (`~/.runelite/dmm-targets.json`) can be used for testing.

## Consequences

- Simpler operational model, no persistent connection management.
- Target updates are eventually consistent (up to poll interval latency).
- UI and overlays refresh when `lastFetchTimestamp` changes.

## Alternatives considered

- WebSocket or SSE: rejected due to complexity and RuneLite runtime constraints.
- Push from server to plugin: rejected due to lack of reliable transport.

## References

- `TargetService.startPolling`, `TargetService.fetchTargets`
- `DMMTrackerPlugin.onGameTick` (refresh handling)
