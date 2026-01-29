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

# ADR 0008 - Task mapping via lookup tables and name normalization

Status: Accepted (2026-01-28)

## Context

The plugin needs to map game-facing identifiers (varp IDs, quest names, diary task text) to the internal IDs used by the DMMScape dataset. Names in-game can vary (punctuation, aliasing, formatting), and Combat Achievements use bitfields without direct IDs.

## Decision

- Use `TaskLookup` to build mapping tables from the data catalog.
- Normalize diary task text and CA names to increase match rates.
- For CAs, use both a dynamic name mapping from the game and an index-based fallback.

## Consequences

- Mapping accuracy improves without hardcoding every alias.
- Dataset changes can be reflected by rebuilding the lookup.
- The mapping system adds some complexity but isolates normalization logic.

## Alternatives considered

- Hardcoded ID maps in code: rejected due to maintenance cost.
- Rely only on CA bit index: rejected due to naming mismatches with dataset.

## References

- `TaskLookup`
- `CAIDMapper`
- `DiaryNameUtils`
- `DMMTrackerPlugin.initializeCANameMapping`
