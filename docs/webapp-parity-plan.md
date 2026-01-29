# Web App Parity Plan for RuneLite Plugin

This document maps DMMScape web app features (from `App.tsx` and related UI) to feasible RuneLite plugin equivalents.

## Reality Check: Full UI Parity Is Not Possible
- RuneLite does not embed a browser or full React UI.
- The Leaflet map UI cannot be recreated exactly inside RuneLite.
- The best practical outcome is functional parity, not visual parity.

## Feature Parity Matrix

| Web App Feature | Current Plugin | Parity Target | Notes |
| --- | --- | --- | --- |
| Skill tracking | Partial | Full | Already captured via varps; expose in UI panel. |
| Quest points | Full | Full | Already captured. |
| Diary tier progress | Full | Full | Already captured; use counts in UI. |
| Diary task completion | Partial | Improved | Task updates only when user opens diary interface. |
| CA completion | Partial | Full | Depends on CA mapping coverage. |
| Boss KC | Full | Full | Already captured. |
| Clue tracking | Full | Full | Already captured. |
| Collection log | Full | Full | Already captured. |
| Task plan list | Partial | Full | Panel shows targets + completion toggles. |
| Plan reordering | None | Partial | Still web-only. |
| Map markers | Partial | Partial | Scene + minimap + world map markers added. |
| Route view | None | Partial | Ordered waypoints line still missing. |
| Viewer mode | Partial | Partial | “Open DMMScape” button links to full web UI. |

## Proposed Parity Approach

### 1. In-Game "Plan Panel" (Core Parity)
**Implemented:** The plugin panel now renders the active plan list, shows sync status, and supports completion toggles.

**Remaining:**
- Grouping by type/category and search.
- Requirements display (skills/quest points) once backend includes them.

### 2. Map Parity (Functional, Not Visual)
**Implemented:**
- World map markers
- Scene tile highlights + minimap markers

**Remaining:**
- Optional route line connecting ordered waypoints
- “Jump to map” from panel row

### 3. "View Full App" Shortcut
**Implemented:** Panel now includes "Open DMMScape" button.

### 4. Bidirectional Sync
**Implemented:** Completion toggles now PATCH `/api/me/plan/targets`.
**Remaining:** Add undo/confirm UX and change audit in backend.

## Required Backend Additions
**Implemented:**
- `GET /api/me/plan/targets` (API key auth)
- `PATCH /api/me/plan/targets` for completion updates

**Remaining:**
- Optional per-target requirements for richer panel display

## Current UI Layout (Plugin Panel)

```
┌──────────────────────────────┐
│ DMMScape                      │
│ [Sync Now] [Open DMMScape]   │
├──────────────────────────────┤
│ 1  [Diary Icon] Easy Desert  │
│    Catch a Golden Warbler    │
│    Req: Hunter 5             │
│                              │
│ 2  [Quest Icon] The Feud      │
│    Req: 15 QP                │
│                              │
│ 3  [Boss Icon] King Black    │
│    Req: 20 KC                │
└──────────────────────────────┘
```

## Risks and Limitations
- Task coordinates may be missing (no marker).
- Diary task updates are incomplete; rely on counts rather than per-task completion.
- World map overlays can become noisy; require filters and clustering.

## Recommended Implementation Order
1. Backend endpoint for plan targets (API key auth).
2. Plugin panel to list tasks (read-only).
3. World map markers for tasks with coordinates.
4. Route line overlay (optional).
5. Completion updates (optional).
