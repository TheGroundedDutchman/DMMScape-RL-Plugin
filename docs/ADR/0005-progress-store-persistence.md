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

# ADR 0005 - Local ProgressStore for UI state

Status: Accepted (2026-01-28)

## Context

The plugin UI needs fast access to progress details (completed tasks, owned unlocks, diary counts, boss KC) that may not always be available from live game state or sync responses. Storing this state only in memory would lose data between sessions.

## Decision

Implement `ProgressStore` as a local JSON-backed cache persisted under `~/.runelite/dmmtracker/dmmscape-progress.json`. The store:

- Tracks completed diary and CA tasks, owned unlocks, diary task counts, and boss KC.
- Notifies listeners on changes with source metadata (LOCAL, REMOTE, GAME).
- Saves immediately on updates to keep UI consistent across restarts.

## Consequences

- UI can render even without immediate live events.
- Progress is resilient to client restarts.
- The store must merge remote data carefully to avoid regressing local state.

## Alternatives considered

- RuneLite config storage for all progress: rejected due to size and structure limitations.
- Pure in-memory state: rejected due to loss on restart.

## References

- `ProgressStore`
- `DMMTrackerPlugin.handleSyncResponse`
