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

# Data Contracts

This document describes the JSON payloads the plugin sends or expects. It is based on `SyncData`, `SyncService`, and `TargetService`.

## Sync payload (Plugin → Webapp)

### Shape

`SyncData` is serialized directly with Gson. Fields are optional and only included when data is present.

| Field | Type | Notes |
| --- | --- | --- |
| `username` | string | RuneLite display name from `client.getLocalPlayer().getName()` |
| `profileType` | string | `DMM`, `LEAGUES`, or `MAIN` derived from world types |
| `timestamp` | number (ms) | `System.currentTimeMillis()` |
| `fullSync` | boolean | `true` on login/manual sync, `false` for deltas |
| `completedCAs` | number[] | CA game IDs (0–639) from varps |
| `caNameMapping` | object | Map of game ID → CA name (string) |
| `questPoints` | number | Total quest points |
| `completedQuests` | string[] | Quest names (RuneLite Quest enum names) |
| `skillLevels` | object | Skill name (lowercase) → level |
| `bossKills` | object | Boss ID → KC (BossNameMapper normalized, hyphenated) |
| `clueCompletions` | object | Tier → completion count |
| `collectionLogCount` | number | Total collection log entries |
| `diaryProgress` | object | Diary name → tier → boolean |
| `diaryTaskCounts` | object | Diary name → tier → count |
| `diaryTaskUpdates` | array | Completed task entries (from journal parsing) |
| `ownedSigils` | string[] | Owned sigil IDs (if present in sync response) |
| `ownedLamps` | string[] | Owned lamp IDs |
| `ownedPrayers` | string[] | Owned prayer IDs |

### Example (full sync)

```json
{
  "username": "PlayerName",
  "profileType": "DMM",
  "timestamp": 1706140800000,
  "fullSync": true,
  "completedCAs": [1, 2, 45, 67],
  "caNameMapping": {"1": "Abyssal Sire Speed-Trialist"},
  "questPoints": 44,
  "completedQuests": ["Cook's Assistant", "Sheep Shearer"],
  "skillLevels": {"attack": 40, "strength": 45},
  "bossKills": {"zulrah": 10, "king-black-dragon": 5},
  "clueCompletions": {"beginner": 5, "easy": 10},
  "collectionLogCount": 150,
  "diaryProgress": {"Varrock": {"easy": true, "medium": false}},
  "diaryTaskCounts": {"Varrock": {"easy": 12, "medium": 5}}
}
```

### Example (delta sync)

```json
{
  "username": "PlayerName",
  "profileType": "DMM",
  "timestamp": 1706140900000,
  "fullSync": false,
  "bossKills": {"zulrah": 11}
}
```

## Webapp progress response (Webapp → Plugin)

The plugin requests `/api/me/progress` and consumes a subset of fields:

| Field | Type | Notes |
| --- | --- | --- |
| `bossKc` | object | Boss ID → KC (merged with local using max strategy) |
| `skillLevels` | object | Skill name → level (merged if higher) |
| `clueCompletions` | object | Tier → count (merged if higher) |
| `collectionLogCount` | number | Merged if higher |
| `questPoints` | number | Merged if higher |
| `ownedSigils` | string[] | Used in sync response handling |
| `ownedLamps` | string[] | Used in sync response handling |
| `ownedPrayers` | string[] | Used in sync response handling |

## Target list payload (Webapp → Plugin)

Targets are fetched from `targetSyncUrl` and parsed by `TargetService`.

```json
{
  "timestamp": 1706140800000,
  "activeListId": "plan-id",
  "planName": "Early Route",
  "targets": [
    {
      "id": "cooks-assistant",
      "type": "quest",
      "name": "Cook's Assistant",
      "category": "quests",
      "completed": false,
      "completionSource": "none",
      "canToggle": true,
      "order": 0,
      "location": {"x": 3211, "y": 3217, "z": 0}
    }
  ]
}
```

Notes:
- `completionSource` is normalized to `none` if missing.
- `location` is optional; if omitted, overlays will not render for that target.

## Target update payload (Plugin → Webapp)

Used by `SyncService.sendTargetUpdates` when the user toggles completion.

```json
{
  "activeListId": "plan-id",
  "updates": [
    {"id": "cooks-assistant", "type": "quest", "completed": true}
  ]
}
```

## Add-to-plan payload (Plugin → Webapp)

Used by `SyncService.addToPlan` when adding a boss from the UI.

```json
{
  "type": "boss",
  "id": "zulrah",
  "name": "Zulrah",
  "category": "boss",
  "points": 16,
  "location": {"x": 2200, "y": 3050, "z": 0}
}
```

Fields `location`, `points`, `category`, and `tier` are optional.
