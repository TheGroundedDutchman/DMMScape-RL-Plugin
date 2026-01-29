# DMMScape Plugin: Req Met Filter + KC Sync Implementation Plan

## Executive Summary

This document details the implementation plan for:
1. **Boss KC Bidirectional Sync** - Fix KC not displaying in UI + fetch from webapp on login
2. **Req Met Filter** - Working skill requirement filtering for Diaries (only - see rationale)
3. **Expand/Collapse Buttons** - Add to BossesTab (DiariesTab/CombatAchievementsTab already have them)

---

## Current State Analysis

### Boss KC Data Flow (BROKEN)

```
Game Chat → handleBossKCUpdate() → lastBossKCs (in-memory) → sendSyncWithAuth() → Webapp
                                         ↓
                                  MISSING: progressStore.setBossKillCount()
                                         ↓
                          UI calls progressStore.getBossKillCount() → returns -1 (unknown)
                                         ↓
                          BossesTab shows "-- KC" for all bosses
```

**Root Cause**: `handleBossKCUpdate()` (DMMTrackerPlugin.java:1346-1370) stores KC in `lastBossKCs` (in-memory Map for delta detection) and sends to webapp, but **never calls `progressStore.setBossKillCount()`**.

**Evidence**:
- BossesTab.java:252 calls `progressStore.getBossKillCount(boss.getName())`
- Returns -1 since KC was never stored
- UI shows "-- KC" even after killing bosses

### Sync Response Handling (INCOMPLETE)

`handleSyncResponse()` (DMMTrackerPlugin.java:1796-1824) only parses:
- `ownedSigils` ✓
- `ownedLamps` ✓
- `ownedPrayers` ✓
- `bossKc` ✗ **NOT PARSED**

### Requirement Data Availability

| Data Type | Has Requirements? | Fields |
|-----------|-------------------|--------|
| DiaryTaskData | ✓ YES | `skillRequirements: List<DiarySkillRequirement>`, `questRequirements` |
| BossData | ✗ NO | Only id, name, pointsPerKill, category, location |
| CATaskData | ✗ NO | Only id, name, description, points, tier |

**Conclusion**: Req Met filter can only work for **Diaries** without schema changes.

### Tab Construction (No DI)

Tabs are manually constructed in DMMTabbedPanel.java:98-102:
```java
bossesTab = new BossesTab(dataLoader, progressStore);
diariesTab = new DiariesTab(dataLoader, progressStore);
caTab = new CombatAchievementsTab(dataLoader, progressStore);
```

**Implication**: Cannot use `@Inject` in tabs. Must pass dependencies through constructor.

---

## Boss KC Sync: Deep Dive

### Source 1: Game Chat Messages

KC is captured via regex pattern in DMMTrackerPlugin.java:
```java
private static final Pattern BOSS_KC_PATTERN = Pattern.compile(
    "Your (.+) kill count is: (\\d+)\\.",
    Pattern.CASE_INSENSITIVE
);
```

Flow:
1. `onChatMessage()` matches pattern
2. Calls `handleBossKCUpdate(bossName, killCount)`
3. `normalizeBossName()` → `BossNameMapper.getBossId()` standardizes name
4. Stores in `lastBossKCs` for delta detection
5. Creates `SyncData` with `bossKills` map
6. Sends to webapp via `sendSyncWithAuth(delta)`

**Missing Step**: Store in `progressStore` for UI

### Source 2: Webapp API (GET /api/me/progress)

Per api-contract.md:
```json
GET /api/me/progress
Response: {
  "bossKc": { "zulrah": 100, "vorkath": 50 },
  "skillLevels": { "attack": 70 },
  ...
}
```

**Issue**: No code currently fetches this endpoint. Need to add.

### Source 3: Hiscores (External API)

The webapp's `useHiscores.ts` fetches from `secure.runescape.com/m=hiscore_oldschool_deadman/index_lite.ws` using CORS proxy.

**For Plugin**: NOT NEEDED. The plugin has direct access to:
- `client.getRealSkillLevel(Skill)` - current skill levels
- `client.getBoostedSkillLevel(Skill)` - with boosts
- Game chat messages for boss KC

The hiscores API is only needed for the **webapp** which doesn't have direct game access.

### KC Sync Strategy

**Merge Logic** (when fetching from webapp):
```java
for (boss, remoteKc) in webappResponse.bossKc:
    localKc = progressStore.getBossKillCount(boss)
    if (localKc < 0):        // Unknown locally
        progressStore.setBossKillCount(boss, remoteKc)
    elif (remoteKc > localKc):  // Remote is higher (played on another device)
        progressStore.setBossKillCount(boss, remoteKc)
    // else: local wins (more recent kill)
```

**Handling handleSyncResponse**:
The existing sync endpoints return owned items but may not return bossKc. We have two options:
1. Extend `handleSyncResponse()` to also parse `bossKc` if present
2. Add separate `fetchProgress()` call to GET /api/me/progress

**Decision**: Do BOTH. Option 1 catches any bossKc in sync response. Option 2 ensures we fetch full progress on login.

---

## Implementation Plan

### Part 1: Fix Boss KC Local Storage (1 line fix)

**File**: DMMTrackerPlugin.java

**Location**: Line 1354, inside `handleBossKCUpdate()`

**Change**:
```java
if (killCount > lastKC) {
    log.info("Boss KC update: {} -> {}", normalizedName, killCount);
    lastBossKCs.put(normalizedName, killCount);

    // ADD THIS LINE - Store in ProgressStore for UI display
    progressStore.setBossKillCount(normalizedName, killCount);

    // ... rest unchanged
}
```

**Impact**: Zero risk. Purely additive. UI will immediately show KC from game chat.

---

### Part 2: Add bossKc to handleSyncResponse()

**File**: DMMTrackerPlugin.java

**Location**: Line 1816, after existing updateOwnedState() call

**Change**:
```java
private void handleSyncResponse(JsonObject response) {
    if (response == null || progressStore == null) {
        return;
    }

    Set<String> sigils = parseStringSet(response, "ownedSigils");
    Set<String> lamps = parseStringSet(response, "ownedLamps");
    Set<String> prayers = parseStringSet(response, "ownedPrayers");

    if (sigils == null && lamps == null && prayers == null) {
        // Continue to check for bossKc even if owned items are null
    } else {
        Set<String> nextSigils = sigils != null ? sigils : progressStore.getOwnedSigils();
        Set<String> nextLamps = lamps != null ? lamps : progressStore.getOwnedLamps();
        Set<String> nextPrayers = prayers != null ? prayers : progressStore.getOwnedPrayers();
        progressStore.updateOwnedState(nextSigils, nextLamps, nextPrayers, ProgressStore.UpdateSource.REMOTE);
    }

    // ADD: Parse boss KC from response
    Map<String, Integer> bossKc = parseIntegerMap(response, "bossKc");
    if (bossKc != null && !bossKc.isEmpty()) {
        mergeBossKcFromRemote(bossKc);
    }

    ownedSyncReady = true;
    // ... rest unchanged
}

// ADD: New helper method
private Map<String, Integer> parseIntegerMap(JsonObject response, String key) {
    if (response == null || !response.has(key) || response.get(key).isJsonNull()) {
        return null;
    }
    JsonElement element = response.get(key);
    if (!element.isJsonObject()) {
        return null;
    }
    Map<String, Integer> result = new HashMap<>();
    for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
        if (entry.getValue().isJsonPrimitive()) {
            result.put(entry.getKey(), entry.getValue().getAsInt());
        }
    }
    return result;
}

// ADD: Merge logic
private void mergeBossKcFromRemote(Map<String, Integer> remoteBossKc) {
    for (Map.Entry<String, Integer> entry : remoteBossKc.entrySet()) {
        String bossName = entry.getKey();
        int remoteKc = entry.getValue();
        int localKc = progressStore.getBossKillCount(bossName);

        // Update if local is unknown (-1) or remote is higher
        if (localKc < 0 || remoteKc > localKc) {
            progressStore.setBossKillCount(bossName, remoteKc);
            lastBossKCs.put(bossName.toLowerCase(), remoteKc);
        }
    }
}
```

**Impact**: Low risk. If response doesn't have bossKc, nothing happens. Merge logic preserves highest KC.

---

### Part 3: Create RequirementChecker Service

**New File**: `dmm-tracker-plugin/src/main/java/com/dmmtracker/RequirementChecker.java`

```java
package com.dmmtracker;

import com.dmmtracker.data.DiarySkillRequirement;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for checking if the player meets skill requirements.
 * Uses the RuneLite Client API for direct game state access.
 */
@Singleton
public class RequirementChecker {

    private final Client client;

    // Map skill name aliases to Skill enum
    private static final Map<String, Skill> SKILL_ALIASES = new HashMap<>();

    static {
        // Standard names
        for (Skill skill : Skill.values()) {
            SKILL_ALIASES.put(skill.getName().toLowerCase(), skill);
        }
        // Common variations
        SKILL_ALIASES.put("hitpoints", Skill.HITPOINTS);
        SKILL_ALIASES.put("hp", Skill.HITPOINTS);
        SKILL_ALIASES.put("range", Skill.RANGED);
        SKILL_ALIASES.put("ranging", Skill.RANGED);
        SKILL_ALIASES.put("rc", Skill.RUNECRAFT);
        SKILL_ALIASES.put("runecrafting", Skill.RUNECRAFT);
        SKILL_ALIASES.put("wc", Skill.WOODCUTTING);
        SKILL_ALIASES.put("fm", Skill.FIREMAKING);
        SKILL_ALIASES.put("con", Skill.CONSTRUCTION);
    }

    @Inject
    public RequirementChecker(Client client) {
        this.client = client;
    }

    /**
     * Gets the Skill enum for a skill name, handling aliases.
     * @return The Skill enum, or null if not found
     */
    private Skill getSkill(String skillName) {
        if (skillName == null) {
            return null;
        }
        return SKILL_ALIASES.get(skillName.toLowerCase().trim());
    }

    /**
     * Checks if the player meets a single skill requirement.
     * @param skillName The skill name (case-insensitive, aliases supported)
     * @param requiredLevel The required level
     * @param boostable Whether boosts count (uses boosted vs real level)
     * @return true if met, false if not met or skill unknown
     */
    public boolean meetsSkillRequirement(String skillName, int requiredLevel, boolean boostable) {
        if (client == null || client.getLocalPlayer() == null) {
            return false; // Not logged in
        }

        Skill skill = getSkill(skillName);
        if (skill == null) {
            // Unknown skill - don't filter out (fail open)
            return true;
        }

        int playerLevel;
        try {
            playerLevel = boostable
                ? client.getBoostedSkillLevel(skill)
                : client.getRealSkillLevel(skill);
        } catch (Exception e) {
            return true; // Fail open on error
        }

        return playerLevel >= requiredLevel;
    }

    /**
     * Checks if the player meets all skill requirements in a list.
     * @param requirements List of skill requirements
     * @return true if ALL requirements are met (or list is empty/null)
     */
    public boolean meetsAllSkillRequirements(List<DiarySkillRequirement> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return true;
        }

        for (DiarySkillRequirement req : requirements) {
            if (!meetsSkillRequirement(req.getSkill(), req.getLevel(), req.isBoostable())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the player is currently logged in and game state is available.
     */
    public boolean isGameStateAvailable() {
        return client != null && client.getLocalPlayer() != null;
    }
}
```

**Design Decisions**:
1. **Fail open on unknown skills** - If skill name doesn't map, return true (don't hide task)
2. **Alias support** - Handle "Hitpoints", "HP", "Range" etc.
3. **Client null check** - Graceful handling when not logged in
4. **Constructor injection** - Works with RuneLite DI, but also allows manual construction

---

### Part 4: Wire RequirementChecker to DiariesTab

**Files to Modify**:
1. `DMMTabbedPanel.java` - Pass RequirementChecker to DiariesTab
2. `DiariesTab.java` - Use RequirementChecker for filtering

**DMMTabbedPanel.java changes**:
```java
// Add field
private RequirementChecker requirementChecker;

// In constructor, after injecting Client
@Inject
public DMMTabbedPanel(DataLoader dataLoader, ProgressStore progressStore,
                       ConfigManager configManager, Client client) {
    // ... existing code ...
    this.requirementChecker = new RequirementChecker(client);

    // Pass to DiariesTab
    diariesTab = new DiariesTab(dataLoader, progressStore, requirementChecker);
    // ... rest unchanged
}
```

**DiariesTab.java changes**:
```java
// Add field
private final RequirementChecker requirementChecker;

// Update constructor
public DiariesTab(DataLoader dataLoader, ProgressStore progressStore,
                   RequirementChecker requirementChecker) {
    this.dataLoader = dataLoader;
    this.progressStore = progressStore;
    this.requirementChecker = requirementChecker;
    // ... rest unchanged
}

// In rebuildContent(), replace the TODO:
if (filterReqMet) {
    List<DiarySkillRequirement> reqs = task.getSkillRequirements();
    if (reqs != null && !reqs.isEmpty()) {
        if (requirementChecker != null && !requirementChecker.meetsAllSkillRequirements(reqs)) {
            continue; // Skip task - requirements not met
        }
    }
}
```

**Why NOT Bosses/CAs**:
- BossData has no requirement fields
- CATaskData has no requirement fields
- Would need schema changes + data pipeline updates
- Scope creep - focus on what works now

---

### Part 5: Add Expand/Collapse Buttons to BossesTab

**File**: BossesTab.java

Copy pattern from DiariesTab/CombatAchievementsTab (already implemented there).

**Changes to buildHeader()**:
```java
// After reqMetFilter setup, add:
JButton expandAllBtn = createSmallButton("Expand");
expandAllBtn.addActionListener(e -> setAllExpanded(true));

JButton collapseAllBtn = createSmallButton("Collapse");
collapseAllBtn.addActionListener(e -> setAllExpanded(false));

filterRow.add(Box.createHorizontalStrut(8));
filterRow.add(expandAllBtn);
filterRow.add(collapseAllBtn);
```

**Add helper methods**:
```java
private JButton createSmallButton(String text) {
    JButton btn = new JButton(text);
    btn.setFont(FontManager.getRunescapeSmallFont());
    btn.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
    btn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
    btn.setBorder(new EmptyBorder(2, 6, 2, 6));
    btn.setFocusPainted(false);
    btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    return btn;
}

private void setAllExpanded(boolean expanded) {
    for (CollapsiblePanel panel : categoryPanels.values()) {
        panel.setExpanded(expanded);
    }
}
```

---

## Summary of All Changes

| File | Change Type | Risk | Description |
|------|-------------|------|-------------|
| DMMTrackerPlugin.java:1354 | 1 line add | None | Store KC in progressStore |
| DMMTrackerPlugin.java:1816+ | ~40 lines add | Low | Parse bossKc from sync response |
| RequirementChecker.java | New file | None | Skill requirement checker service |
| DMMTabbedPanel.java | ~5 lines modify | Low | Pass RequirementChecker to DiariesTab |
| DiariesTab.java | ~10 lines modify | Low | Wire Req Met filter logic |
| BossesTab.java | ~20 lines add | None | Expand/Collapse buttons |

**Total**: ~80 lines of new/modified code

---

## What This Does NOT Include

1. **Req Met for Bosses/CAs** - No requirement data in schema
2. **Quest requirement checking** - Would need Quest state mapping
3. **Separate fetchProgress() endpoint** - handleSyncResponse extension should suffice
4. **Webapp changes** - All changes are plugin-side

---

## Testing Checklist

- [ ] Kill a boss in-game → KC shows in BossesTab immediately
- [ ] Log out and back in → KC persists (from ProgressStore)
- [ ] Sync with webapp → KC shows if webapp has data
- [ ] Enable Req Met filter in DiariesTab → Tasks with unmet skill reqs hidden
- [ ] Disable Req Met filter → All tasks visible again
- [ ] Click Expand in BossesTab → All categories expand
- [ ] Click Collapse in BossesTab → All categories collapse
