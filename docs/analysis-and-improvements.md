# RuneLite Plugin Analysis and Improvements (dmm-tracker-plugin)

## Current Capabilities
- Collects progress for:
  - Combat Achievements (game IDs)
  - Diary tier progress and task counts
  - Quest points and completed quests
  - Skill levels
  - Boss KC
  - Clue completions
  - Collection log count
- Syncs to web app via POST to a configurable Sync URL.
- Supports API key header (`X-Api-Key`) and secret config storage.
- Provides manual JSON export for import into the web app.
- Fetches plan targets from the backend and shows them in a plugin panel.
- Shows multi-target overlays on the scene, minimap, and world map.
- Supports in-game completion toggles (PATCH back to web app).

## What Changed in This Iteration
- Added plugin panel with sync status, target list, and “Open DMMScape” shortcut.
- Added world map markers + multi-target overlays (scene + minimap).
- Added API key secret config storage and target sync auth support.
- Added target completion updates back to the backend.

## Critical Gaps and Risks (Be Very Critical)

### User Experience
1. **Target list UX is still basic.**
   - No grouping by category or search/filter beyond “show completed.”
   - Large plans can feel noisy without grouping or collapsing.

2. **No route-line visualization yet.**
   - Markers are shown, but there is no waypoint line or path overlay.

### Data Coverage
1. **Per-task diary completion is partial.**
   - Diary task updates only available if user visits diary interfaces.

2. **CA mapping depends on web app data.**
   - Missing CA entries in the frontend dataset means dropped completions.

### Integration
1. **Target polling is still interval-based.**
   - No realtime push; updates are every few seconds.
   - Could move to SSE/websocket later if needed.

## Improvement Roadmap

### Short Term (High Priority)
- Add grouping/collapsing in the target list (by type/category).
- Add error badges and retry buttons in the panel for failed syncs.

### Mid Term
- Route line overlay on world map (ordered path).
- Optional per-target “jump to map” behavior (open world map at target).

### Long Term
- Real-time target updates (SSE/websocket).
- In-game planner (reorder tasks, batch edits).

## Suggested Testing Checklist
- API key sync succeeds with correct key and fails with incorrect key.
- Sync errors show an in-game notification and panel status updates.
- Target list loads from `/api/me/plan/targets` and matches web app.
- Completion toggles update web app and reflect after refresh.
- World map markers align with target coordinates.

## Key Files to Reference
- `src/main/java/com/dmmtracker/DMMTrackerPlugin.java`
- `src/main/java/com/dmmtracker/SyncService.java`
- `src/main/java/com/dmmtracker/SyncData.java`
- `src/main/java/com/dmmtracker/DMMTrackerConfig.java`
