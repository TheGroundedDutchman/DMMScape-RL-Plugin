# DMMScape Companion - RuneLite Plugin

[![RuneLite Plugin Hub](https://img.shields.io/badge/RuneLite-Plugin%20Hub-orange)](https://runelite.net/plugin-hub)
[![License](https://img.shields.io/badge/License-BSD%202--Clause-blue.svg)](LICENSE)

Syncs DMMScape progress and plan targets between RuneLite and the [DMMScape](https://dmmscape.com) web app. Build routes on the interactive map and track them in-game.

## Documentation (Start Here)

- `docs/README.md` (doc index)
- `CONTRIBUTING.md` (contribution guidelines)
- `docs/DEVELOPER_GUIDE.md` (setup + AGENTS summary)
- `docs/ARCHITECTURE.md` (in-depth architecture + diagrams)
- `docs/CONFIG_REFERENCE.md` (full config key reference)
- `docs/DATA_CONTRACTS.md` (sync and target payloads)
- `docs/DATA_FLOW_TRACE.md` (step-by-step data flow traces)
- `docs/CODE_MAP.md` (file map for reviewers)
- `docs/LIMITATIONS.md` (known constraints)
- `docs/CREDITS.md` (credits and attribution)
- `docs/SECURITY_PRIVACY.md` (data usage and storage)
- `docs/REVIEW_NOTES.md` (RuneLite review summary)
- `docs/REVIEW_GUIDE.md` (reviewer quick path)
- `docs/QA_CHECKLIST.md` (manual test checklist)
- `docs/ADR/README.md` (architecture decisions)

---
## ⚠️ IMPORTANT: Development Setup ⚠️

**DO NOT use sideloaded plugins for development!** Sideloaded plugins have broken event handling - `@Subscribe` methods will NOT fire.

**You MUST sync to RuneLite's built-in plugin location and update package declarations.**

See [AGENTS.md](AGENTS.md) for complete setup commands, or the quick version:

```bash
# 1. Sync source to RuneLite
rsync -av src/main/java/com/dmmtracker/ /path/to/runelite/.../plugins/dmmtracker/

# 2. Update ALL package declarations: com.dmmtracker -> net.runelite.client.plugins.dmmtracker
find /path/to/runelite/.../plugins/dmmtracker -name "*.java" -exec sed -i '' \
  -e 's/package com\.dmmtracker/package net.runelite.client.plugins.dmmtracker/g' \
  -e 's/import com\.dmmtracker/import net.runelite.client.plugins.dmmtracker/g' \
  -e 's/com\.dmmtracker\./net.runelite.client.plugins.dmmtracker./g' {} \;

# 3. Sync resources
rsync -av src/main/resources/com/dmmtracker/ /path/to/runelite/.../resources/.../dmmtracker/

# 4. Rebuild RuneLite
cd /path/to/runelite && ./gradlew :client:shadowJar -x test -x javadoc

# 5. Run with macOS flags
java -ea --add-opens java.desktop/com.apple.eawt=ALL-UNNAMED \
  --add-exports java.desktop/com.apple.eawt=ALL-UNNAMED \
  -jar client-*.jar --developer-mode
```
---

## Installation

### From Plugin Hub (Recommended)
1. Open RuneLite
2. Go to Configuration (wrench icon)
3. Click "Plugin Hub" at the bottom
4. Search for "DMMScape Companion"
5. Click Install

### Manual Installation (Sideloading)
Requires RuneLite to run from source with `--developer-mode`. Production RuneLite does not load sideloaded plugins.

```bash
# Build the plugin
./gradlew jar

# Copy to sideloaded plugins
cp build/libs/dmm-tracker-*.jar ~/.runelite/sideloaded-plugins/

# Enable the plugin in RuneLite settings (may be disabled by default)
sed -i '' 's/runelite.dmmtrackerplugin=false/runelite.dmmtrackerplugin=true/' ~/.runelite/profiles2/default-*.properties 2>/dev/null
```

After starting RuneLite, if the plugin doesn't appear in the sidebar:
1. Open Configuration panel (wrench icon)
2. Search for "DMMScape"
3. Toggle the plugin ON

## Features

### Progress Tracking (Automatic)

| Data Type | Source | Notes |
|-----------|--------|-------|
| Combat Achievements | Varps 3116-3135 | 640 CAs tracked via bitfields |
| Boss Kill Counts | Chat messages | Regex parsing of KC messages |
| Diary Completion | Varbits | All 12 diaries, tier-level tracking |
| Diary Task Counts | Varbits | Per-tier task completion counts |
| Quest Completion | Quest.getState() | All quests tracked |
| Skill Levels | Skill API | All 23 skills |
| Collection Log | Varbits | Total count only |
| Clue Completions | Varbits | All tiers (beginner → master) |

### Sync Behavior

- **Full sync on login**: Sends complete state when you log in
- **Delta sync on changes**: Only sends what changed (efficient)
- **Target polling**: Fetches targets from webapp every 5 seconds
- **Plugin panel status**: Shows last sync and target update results
- **Short-lived sync tokens**: Auto-refreshes 30-minute tokens for API calls
- **Completion toggles**: Optional in-panel updates PATCH back to the web app

### Target Overlay

When enabled, highlights tiles in-game for your current targets:
- Quest locations
- Diary task locations
- Boss locations
- Custom target locations

Also renders:
- Minimap markers + direction arrow
- World map markers (ordered)

### Player Alarm (DMM Feature)

Alerts when other players appear nearby - useful for DMM survival.

## Configuration

Found in RuneLite settings under "DMMScape Companion":

Tip: Use **Start as Guest** for instant sync without logging in, or **Link Account** to connect your web account. The API key field is an optional fallback.
Use **Unsync** in the panel header to disconnect and disable sync quickly.

### Environment Defaults (Local vs Production)

This repo defaults to local development endpoints via `ApiConfig.USE_LOCAL = true`.  
For Plugin Hub or release builds, set `USE_LOCAL = false` so default endpoints use `https://dmmscape.com`.
Any custom Sync URL or Target Sync URL you set in RuneLite will override these defaults.

### Sync Settings
| Setting | Default | Description |
|---------|---------|-------------|
| Enable Sync | true | Master toggle for all sync |
| Sync URL | Prod: `https://dmmscape.com/api/me/sync/runelite`<br>Local: `http://localhost:54321/functions/v1/api/me/sync/runelite` | URL to POST progress data |
| API Key (Optional) | (empty) | Optional fallback key from DMMScape Settings |
| Profile Type | DMM | DMM, LEAGUES, or MAIN |

### Target Overlay
| Setting | Default | Description |
|---------|---------|-------------|
| Enable Target Sync | true | Fetch targets from webapp |
| Target Sync URL | Prod: `https://dmmscape.com/api/me/plan/targets`<br>Local: `http://localhost:54321/functions/v1/api/me/plan/targets` | URL to fetch targets from |
| Show Target Overlay | true | Render tile highlights + minimap markers |
| Allow In-Game Completion | true | Toggle target completion from panel |
| Show World Map Markers | true | Render world map markers |
| Max World Map Targets | 25 | Cap map marker count |
| Max Overlay Targets | 3 | Cap scene/minimap highlights |
| Target Color | Cyan | Highlight color |

### Tracking Options
| Setting | Default | Description |
|---------|---------|-------------|
| Track Combat Achievements | true | Send CA completions |
| Track Diaries | true | Send diary progress |
| Track Quests | true | Send quest completions |
| Track Skills | true | Send skill levels |
| Track Boss KC | true | Send boss kill counts |
| Track Collection Log | true | Send collection log count |

## Data Formats

### Sync Payload (Plugin → Server)

```json
{
  "username": "PlayerName",
  "profileType": "DMM",
  "timestamp": 1706140800000,
  "fullSync": false,
  "completedCAs": [1, 2, 45, 67],
  "questPoints": 44,
  "completedQuests": ["Cook's Assistant", "Sheep Shearer"],
  "skillLevels": {
    "attack": 40,
    "strength": 45,
    "defence": 40
  },
  "bossKills": {
    "Barrows": 50,
    "Zulrah": 10
  },
  "diaryProgress": {
    "Varrock": {
      "easy": true,
      "medium": false,
      "hard": false,
      "elite": false
    }
  },
  "diaryTaskCounts": {
    "Varrock": {
      "easy": 12,
      "medium": 5,
      "hard": 0,
      "elite": 0
    }
  },
  "clueCompletions": {
    "beginner": 5,
    "easy": 10,
    "medium": 3
  },
  "collectionLogCount": 150
}
```

### Target Payload (Server → Plugin)

```json
{
  "timestamp": 1706140800000,
  "activeListId": "plan-id",
  "planName": "Early Route",
  "targets": [
    {
      "type": "quest",
      "id": "cooks-assistant",
      "name": "Cook's Assistant",
      "completed": false,
      "completionSource": "none",
      "canToggle": true,
      "order": 0,
      "location": { "x": 3211, "y": 3217, "z": 0 }
    },
    {
      "type": "diary",
      "id": "varrock-easy-1",
      "name": "Browse Thessalia's store",
      "completed": true,
      "completionSource": "progress",
      "canToggle": false,
      "order": 1,
      "location": { "x": 3206, "y": 3417, "z": 0 }
    },
    {
      "type": "boss",
      "id": "barrows",
      "name": "Barrows",
      "completed": false,
      "completionSource": "none",
      "canToggle": true,
      "order": 2,
      "location": { "x": 3565, "y": 3289, "z": 0 }
    }
  ]
}
```

### Target Update Payload (Plugin → Server)

```json
{
  "activeListId": "plan-id",
  "updates": [
    { "id": "varrock-easy-1", "type": "diary", "completed": true }
  ]
}
```

## Current Integration Status

### What Works Now

1. **Device code linking**: Link from the plugin panel using a short code
2. **Guest linking**: Start syncing immediately without web login
3. **API key fallback**: Optional manual key if you prefer
4. **Target sync**: Fetch plan targets via `/api/me/plan/targets`
5. **Completion updates**: Panel toggles PATCH back to the web app
6. **File-based targets**: Place `dmm-targets.json` in `.runelite` folder (fallback)

## File Structure

```
dmm-tracker-plugin/
├── src/main/java/com/dmmtracker/
│   ├── DMMTrackerPlugin.java    # Main plugin (1410 lines)
│   ├── DMMTrackerConfig.java    # Configuration options (332 lines)
│   ├── DMMTrackerPanel.java     # Panel UI (sync + plan list)
│   ├── SyncService.java         # HTTP sync client (173 lines)
│   ├── SyncData.java            # Sync payload structure (52 lines)
│   ├── TargetService.java       # Target fetching (310 lines)
│   ├── TargetMapPoint.java      # World map markers
│   └── DMMTargetOverlay.java    # Tile highlighting (132 lines)
└── README.md                    # This file
```

## Building

```bash
# Build with Gradle
./gradlew build

# Output JAR will be in build/libs/
```

## Development Troubleshooting

### macOS: RuneLite Won't Start
If you see "Fatal error starting RuneLite" or assertion errors:
```bash
java -ea \
  --add-opens java.desktop/com.apple.eawt=ALL-UNNAMED \
  --add-exports java.desktop/com.apple.eawt=ALL-UNNAMED \
  -jar client-1.12.14-SNAPSHOT-shaded.jar \
  --developer-mode
```

### Plugin Shows Wrong Version
If there's a built-in dmmtracker plugin in RuneLite source, disable it:
```bash
mv /path/to/runelite/.../plugins/dmmtracker /path/to/runelite/.../plugins/dmmtracker.disabled
cd /path/to/runelite && ./gradlew :client:shadowJar -x test -x javadoc
```

### Full Troubleshooting Guide
See [AGENTS.md](AGENTS.md) for comprehensive troubleshooting including:
- Built-in plugin conflict resolution
- macOS-specific JVM flags
- Complete development workflow
- Common issues and solutions

## Development Notes

### CA Tracking Implementation

Combat Achievements use 20 varps (3116-3135), each storing 32 completion bits:
```java
private static final int CA_VARP_START = 3116;
private static final int CA_VARP_COUNT = 20;
// CA index = (varpIndex * 32) + bitPosition
```

### Diary Varbits

Each diary has a varbit storing tier completion as a bitfield:
- Bit 0: Easy complete
- Bit 1: Medium complete
- Bit 2: Hard complete
- Bit 3: Elite complete

### Boss KC Detection

Boss kills detected via chat message regex:
```java
Pattern.compile("Your (.+) kill count is: (\\d+)")
```

## Code Conventions

See `docs/code-conventions.md` for the RuneLite formatting rules and IDE setup used by this repo.

## Integration with DMMScape Webapp

### For Users

1. Open the plugin panel and click **Start as Guest** for instant sync, or **Link Account** for full login
2. If linking an account: go to `https://dmmscape.com/link` and enter the code shown
3. Once linked (guest or account), enable auto-sync in the plugin settings
4. Optional: set an API key in settings if you prefer manual auth

### For Developers

See the DMMScape backend documentation for:
- Backend endpoint specifications
- Device code authentication flow
- Full integration architecture

## Privacy

- All data is sent only to your authenticated DMMScape account
- No data is shared with third parties
- You control what gets synced via plugin settings

## Support

- Website: [dmmscape.com](https://dmmscape.com)
- Issues: [GitHub Issues](https://github.com/dmmscape/dmm-tracker-plugin/issues)

## Acknowledgments

- RuneLite client and the RuneLite Code Conventions wiki for formatting and UI conventions.
- [Quest Helper](https://github.com/Zoinkwiz/quest-helper) (Zoinkwiz) for world/map route line overlay patterns (see `RouteOverlay` and `WorldMapRouteOverlay`).
- [Combat Achievements Tracker](https://github.com/ehubbartt/combat-achievements-tracker) (Ethan Hubbartt) for panel UX inspiration and boss grid layouts.
- [WikiSync](https://oldschool.runescape.wiki/w/RuneScape:WikiSync) (andmcadams) for player-data sync inspiration.
- Wilderness Player Alarm (Alex) for the player alarm feature inspiration (extended beyond Wilderness, BSD 2-Clause; https://github.com/adhansen/plugin-repo).
- See `docs/CREDITS.md` for full dependency and attribution tracking.
- See `docs/THIRD_PARTY_LICENSES.md` for third-party license texts.

## License

BSD 2-Clause License - see [LICENSE](LICENSE) for details.
