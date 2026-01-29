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

# ADR 0002 - Hybrid data loader (bundled + cached + remote)

Status: Accepted (2026-01-28)

## Context

The plugin requires a data catalog (bosses, diaries, CAs, sigils, etc.) to render the UI. Bundled data ensures offline functionality and stable startup, but the dataset can become stale between releases. The plugin must support updates without forcing a client release.

## Decision

Implement a hybrid data loader that:

1. Loads cached data from `~/.runelite/dmmtracker/dmmscape-data-cache.json` if available.
2. Falls back to bundled JSON in the JAR if no cache exists.
3. Supports remote updates via ETag-based HTTP fetch (`/api/plugin-data`).
4. Chooses the newest dataset using the `meta.timestamp` field.

## Consequences

- The plugin can ship with a baseline dataset while still accepting remote updates.
- Cache and ETag files must be persisted in the RuneLite directory.
- Update checks can be scheduled without changing the plugin packaging.

## Alternatives considered

- Bundled-only data: rejected due to slow update cycles.
- Remote-only data: rejected due to startup failures when offline or API unreachable.

## References

- `com.dmmtracker.data.DataLoader`
- `src/main/resources/net/runelite/client/plugins/dmmtracker/data/dmmscape-data.json`
