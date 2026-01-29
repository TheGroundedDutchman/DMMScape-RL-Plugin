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

# ADR 0007 - Multi-surface overlays with route lines

Status: Accepted (2026-01-28)

## Context

Plan targets need to be visible in multiple contexts: in-game scene, minimap, and world map. The plugin must show both discrete targets and the ordered route between them.

## Decision

Use three overlay surfaces plus world map points:

- `DMMTargetOverlay` for scene tile highlights and labels.
- `DMMMinimapOverlay` for minimap markers and direction arrows.
- `RouteOverlay` for scene route lines with chevrons.
- `WorldMapRouteOverlay` for world map route lines.
- `TargetMapPoint` + `WorldMapPointManager` for world map markers.

## Consequences

- Users get consistent visual cues across surfaces.
- Render logic must handle instanced regions and map bounds.
- Config flags enable or disable surfaces independently.

## Alternatives considered

- Single surface overlay only: rejected due to limited navigation help.
- World map markers only: rejected due to lack of in-scene guidance.

## References

- `DMMTargetOverlay`
- `DMMMinimapOverlay`
- `RouteOverlay`
- `WorldMapRouteOverlay`
- `TargetMapPoint`
