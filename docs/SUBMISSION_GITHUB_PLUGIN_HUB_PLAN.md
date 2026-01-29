---
title: Submission GitHub + Plugin Hub Plan
status: draft
owner: DMMScape
updated: 2026-01-29
---

# Submission GitHub + Plugin Hub Plan

This document proposes a **reviewer-friendly commit plan** for migrating the plugin repo to GitHub
and submitting it to RuneLite Plugin Hub. The goal is to make review easy by grouping changes into
logical, inspectable slices with clear intent and minimal cross-cutting noise.

## Goals

- Make the GitHub history easy to scan and review.
- Keep each commit focused and reviewable (one feature cluster per commit).
- Provide a consistent reviewer trail to explain *why* each chunk exists.
- Prepare the repo for a Plugin Hub PR without mixing in local/dev-only bits.

## Suggested Commit Strategy

Two viable approaches (pick one):

1) **Curated incremental history** (recommended): re-commit the repo in a clean, ordered sequence.
2) **Single snapshot commit**: fastest, but harder for reviewers to diff.

This plan assumes the **curated incremental history** approach.

## Reviewability Rules (for curated history)

These rules keep each commit inspectable and avoid reviewer fatigue:

- Each commit should build on its own (no missing types or partial refactors).
- Avoid large renames or formatting-only commits unless isolated and explained.
- Keep diffs small where possible; prefer multiple focused commits to one broad change.
- Don’t mix docs-only edits with code unless the docs explain the exact change.
- Avoid deep code motion; move files only in dedicated commits with explicit rationale.

## AI Agent Execution Guide (no gaps)

Use this as the **step-by-step checklist** to avoid missing items.

### Preconditions (before commit work)

- Decide the history strategy: curated incremental history vs single snapshot.
- Start from a clean working tree (no untracked build artifacts).
- Confirm required files exist:
  - `src/main/resources/runelite-plugin.properties`
  - `LICENSE`
  - `README.md`
  - `docs/THIRD_PARTY_LICENSES.md`
  - `docs/CREDITS.md`
- Confirm `ApiConfig.USE_LOCAL` policy for the final release commit:
  - `true` for local dev commits, `false` for the final submission snapshot.
- Confirm plugin package path: `com.dmmtracker.*` (Plugin Hub expects this repo structure).

### Commit Loop (repeat per commit)

For each planned commit:

1) **Select scope**: only the files listed in the plan for that commit.
2) **Apply changes**: avoid unrelated edits (no formatting-only noise).
3) **Update docs** (if using iterative docs):
   - Match doc updates using the “Documentation coupling matrix” below.
4) **Update commit note** (optional but recommended):
   - Create/update `docs/submission-commit-notes/NN-title.md`.
5) **Quick sanity check**:
   - Does the repo still build?
   - Does the plugin still load?
6) **Commit with clear message** (start with feature area).

### Finalization (before Plugin Hub PR)

- Ensure `ApiConfig.USE_LOCAL = false`.
- Ensure no local build outputs are tracked (`build/`, `target/`, `.gradle/`).
- Ensure README and credits are accurate and current.
- Pin a **specific commit hash** for the Plugin Hub submission.

### Guardrails (do not skip)

- Do not commit local RuneLite source sync artifacts.
- Do not include `.runelite/` data or caches.
- Do not include sideloaded plugin builds; Plugin Hub uses source.
- Do not include private backend secrets or keys.

## Documentation Strategy (to make review easier)

You can use the docs in two ways:

### Option A: Iterative docs per feature (recommended)
Each feature commit includes minimal docs that explain the change. This helps reviewers verify
each commit without jumping around later.

### Option B: Docs as a final batch
Fastest to write, but reviewers must wait until the end to understand intent.

### Recommended hybrid
Include **small, targeted doc edits in each relevant commit**, and keep big narrative docs
(`ARCHITECTURE.md`, `REVIEW_GUIDE.md`) as a later consolidation.

#### Documentation coupling matrix
Use this mapping to decide which docs change alongside which code:

- **Config changes** → `docs/CONFIG_REFERENCE.md`
- **API endpoints or payloads** → `docs/DATA_CONTRACTS.md`, `docs/SECURITY_PRIVACY.md`
- **New files or modules** → `docs/CODE_MAP.md`
- **UI/UX behavior** → `docs/REVIEW_GUIDE.md` (short bullet add)
- **License/attribution** → `docs/CREDITS.md`, `docs/THIRD_PARTY_LICENSES.md`
- **Testing steps** → `docs/QA_CHECKLIST.md`

## Minimum Plugin Hub Requirements (must be present)

- `LICENSE` with BSD 2-Clause.
- `runelite-plugin.properties` with correct `displayName`, `author`, `description`, `tags`, `plugins`.
- `README.md` with a short feature summary and high-level setup.
- Public GitHub repository URL (Plugin Hub requires GitHub for review).

## Recommended Reviewer Aids

- `docs/REVIEW_GUIDE.md` (fast review path)
- `docs/REVIEW_NOTES.md` (deeper context + known tradeoffs)
- `docs/QA_CHECKLIST.md` (manual verification checklist)

## Optional: Per-Commit Notes (recommended)

If you want to be extra explicit for reviewers, create a short MD note per commit under:

```
docs/submission-commit-notes/
```

Example filename: `docs/submission-commit-notes/03-core-plugin-wiring.md`

Suggested note template:

```
# Commit N - <title>
Intent:
Scope:
Non-goals:
Key files:
Public API/Schema impact:
UI impact:
Test notes (manual or automated):
Risk / rollback:
Review focus:
```

This keeps review breadcrumbs close to the code without bloating commit messages.
## Commit Plan (Detailed)

Below is a proposed commit sequence with **specific file-level scope**.
Adjust ordering as needed for your final story.

### 01) Repo foundation + build system

**Intent**: establish reproducible build + plugin metadata.

**Files**
- `build.gradle`
- `pom.xml`
- `gradlew`, `gradlew.bat`, `gradle/`
- `src/main/resources/runelite-plugin.properties`
- `.gitignore`, `.editorconfig`

**Docs (if iterative)**
- `docs/README.md` (repo quick-start entry)
- `docs/CODE_MAP.md` (baseline structure)

**Review focus**
- Build tasks compile plugin without RuneLite source.
- `runelite-plugin.properties` points at `com.dmmtracker.DMMTrackerPlugin`.

**Optional note**: `docs/submission-commit-notes/01-repo-foundation.md`

---

### 02) Core plugin skeleton (no features)

**Intent**: minimal plugin wiring, panel container, config skeleton.

**Files**
- `src/main/java/com/dmmtracker/DMMTrackerPlugin.java`  
  (basic plugin lifecycle, dependency injection stubs, plugin descriptor)
- `src/main/java/com/dmmtracker/DMMTrackerConfig.java`  
  (empty/placeholder sections)
- `src/main/java/com/dmmtracker/DMMTrackerPanel.java`  
  (bare panel with header/footer)
- `src/main/java/com/dmmtracker/ui/DMMTabbedPanel.java`  
  (tab host container)

**Docs (if iterative)**
- `docs/CODE_MAP.md` (core classes)
- `docs/ARCHITECTURE.md` (high-level module diagram)

**Review focus**
- Plugin loads without side effects.
- Config group registered, UI mounts.

**Optional note**: `docs/submission-commit-notes/02-core-skeleton.md`

---

### 03) Shared UI toolkit + style helpers

**Intent**: reusable UI components and styling utilities.

**Files**
- `src/main/java/com/dmmtracker/ui/DmmColors.java`
- `src/main/java/com/dmmtracker/ui/IconManager.java`
- `src/main/java/com/dmmtracker/ui/ScrollPaneUtils.java`
- `src/main/java/com/dmmtracker/ui/components/*`

**Docs (if iterative)**
- `docs/CODE_MAP.md` (UI toolkit section)

**Review focus**
- Reusable components are isolated.
- No feature logic yet (pure UI plumbing).

**Optional note**: `docs/submission-commit-notes/03-ui-toolkit.md`

---

### 04) API environment configuration

**Intent**: controlled toggle between local and production endpoints.

**Files**
- `src/main/java/com/dmmtracker/ApiConfig.java`
- `docs/CONFIG_REFERENCE.md` (if needed for env defaults)

**Docs (if iterative)**
- `docs/CONFIG_REFERENCE.md` (local vs prod defaults)
- `docs/DATA_CONTRACTS.md` (endpoint base)

**Review focus**
- `USE_LOCAL` toggle is centralized.
- All endpoints derived from `ApiConfig` (no hard-coded URLs elsewhere).

**Optional note**: `docs/submission-commit-notes/04-api-config.md`

---

### 05) Data models + JSON loader

**Intent**: core data structures and JSON parsing.

**Files**
- `src/main/java/com/dmmtracker/data/*`  
  (BossData, DiaryData, SigilData, TeleportData, etc.)
- `src/main/java/com/dmmtracker/data/DataLoader.java`
- `src/main/java/com/dmmtracker/data/QuestRequirementTypeAdapter.java`
- `src/main/java/com/dmmtracker/data/DiaryTaskCompletionRule.java`

**Docs (if iterative)**
- `docs/CODE_MAP.md` (data model inventory)
- `docs/ARCHITECTURE.md` (data layer description)
- `docs/DATA_CONTRACTS.md` (model schema references)

**Review focus**
- Gson adapters handle mixed schema safely.
- Data model matches bundled JSON shape.

**Optional note**: `docs/submission-commit-notes/05-data-models.md`

---

### 06) Bundled resources (icons + data)

**Intent**: ship assets and data needed for offline/plugin bootstrap.

**Files**
- `src/main/resources/com/dmmtracker/icons/*`
- `src/main/resources/com/dmmtracker/data/dmmscape-data.json`
- `src/main/resources/com/dmmtracker/data/dmmscape-data.min.json`

**Docs (if iterative)**
- `docs/CODE_MAP.md` (resource locations)
- `docs/REVIEW_GUIDE.md` (where data comes from)

**Review focus**
- No external fetch required on first load.
- Asset licenses addressed in `docs/THIRD_PARTY_LICENSES.md`.

**Optional note**: `docs/submission-commit-notes/06-resources.md`

---

### 07) Progress model + state tracking

**Intent**: store and diff local game state for sync.

**Files**
- `src/main/java/com/dmmtracker/ProgressStore.java`
- `src/main/java/com/dmmtracker/SyncData.java`
- `src/main/java/com/dmmtracker/DiaryTaskUpdate.java`
- `src/main/java/com/dmmtracker/ProfileType.java`

**Docs (if iterative)**
- `docs/DATA_CONTRACTS.md` (sync payload fields)
- `docs/DATA_FLOW_TRACE.md` (local → payload flow)

**Review focus**
- Diffing logic and update sources (local vs web).
- Clear separation of data vs transport.

**Optional note**: `docs/submission-commit-notes/07-progress-store.md`

---

### 08) Device auth + token handling

**Intent**: auth flow for device codes + token refresh.

**Files**
- `src/main/java/com/dmmtracker/DeviceAuthService.java`
- `src/main/java/com/dmmtracker/RuneliteTokenService.java`

**Docs (if iterative)**
- `docs/SECURITY_PRIVACY.md` (auth flows + tokens)
- `docs/DATA_FLOW_TRACE.md` (device code flow)

**Review focus**
- Device code flow and guest flow behavior.
- Token refresh cadence and failure handling.

**Optional note**: `docs/submission-commit-notes/08-auth.md`

---

### 09) Sync client + target updates

**Intent**: HTTP client for sync, target updates, and plan operations.

**Files**
- `src/main/java/com/dmmtracker/SyncService.java`
- `src/main/java/com/dmmtracker/TargetService.java`
- `src/main/java/com/dmmtracker/TaskLookup.java`

**Docs (if iterative)**
- `docs/DATA_CONTRACTS.md` (sync + target endpoints)
- `docs/DATA_FLOW_TRACE.md` (sync + target request flow)
- `docs/CONFIG_REFERENCE.md` (sync URLs)

**Review focus**
- Sync payloads and response parsing.
- Target polling and update methods.

**Optional note**: `docs/submission-commit-notes/09-sync.md`

---

### 10) Core plugin logic (events + orchestration)

**Intent**: wire game events to progress updates + sync triggers.

**Files**
- `src/main/java/com/dmmtracker/DMMTrackerPlugin.java`
- `src/main/java/com/dmmtracker/RequirementChecker.java`
- `src/main/java/com/dmmtracker/BossNameMapper.java`
- `src/main/java/com/dmmtracker/CAIDMapper.java`
- `src/main/java/com/dmmtracker/DiaryCompletionService.java`
- `src/main/java/com/dmmtracker/DiaryNameUtils.java`

**Docs (if iterative)**
- `docs/ARCHITECTURE.md` (event flow)
- `docs/DATA_FLOW_TRACE.md` (on-login, on-varbit)

**Review focus**
- Event handlers: login, varbit changes, chat parsing.
- Sync scheduling + throttling.

**Optional note**: `docs/submission-commit-notes/10-plugin-orchestration.md`

---

### 11) Overlay rendering (scene + minimap + world map)

**Intent**: draw targets/routes in game and on world map.

**Files**
- `src/main/java/com/dmmtracker/DMMTargetOverlay.java`
- `src/main/java/com/dmmtracker/DMMMinimapOverlay.java`
- `src/main/java/com/dmmtracker/RouteOverlay.java`
- `src/main/java/com/dmmtracker/WorldMapRouteOverlay.java`
- `src/main/java/com/dmmtracker/WorldMapLegendOverlay.java`
- `src/main/java/com/dmmtracker/TargetMapPoint.java`
- `src/main/java/com/dmmtracker/TeleportMapPoint.java`

**Docs (if iterative)**
- `docs/CONFIG_REFERENCE.md` (overlay settings)
- `docs/REVIEW_GUIDE.md` (overlay behavior + limits)

**Review focus**
- Map marker limits and filters.
- Route drawing and overlay density safeguards.

**Optional note**: `docs/submission-commit-notes/11-overlays.md`

---

### 12) Panel + tabs (Targets, Diaries, etc.)

**Intent**: feature UI for plan targets, diaries, bosses, sigils, search.

**Files**
- `src/main/java/com/dmmtracker/DMMTrackerPanel.java`
- `src/main/java/com/dmmtracker/ui/SettingsPanel.java`
- `src/main/java/com/dmmtracker/ui/tabs/TargetsTab.java`
- `src/main/java/com/dmmtracker/ui/tabs/DiariesTab.java`
- `src/main/java/com/dmmtracker/ui/tabs/BossesTab.java`
- `src/main/java/com/dmmtracker/ui/tabs/CombatAchievementsTab.java`
- `src/main/java/com/dmmtracker/ui/tabs/SigilsTab.java`
- `src/main/java/com/dmmtracker/ui/tabs/GlobalSearchTab.java`

**Docs (if iterative)**
- `docs/REVIEW_GUIDE.md` (panel flow)
- `docs/CONFIG_REFERENCE.md` (settings surfaced in UI)

**Review focus**
- Panel state sync with `ProgressStore`.
- UI actions map cleanly to sync APIs.

**Optional note**: `docs/submission-commit-notes/12-panel-tabs.md`

---

### 13) Player alarm feature

**Intent**: alerting for nearby players (extended beyond Wilderness).

**Files**
- `src/main/java/com/dmmtracker/DMMTrackerConfig.java`  
  (player alarm config section)
- `src/main/java/com/dmmtracker/DMMTrackerPlugin.java`  
  (alarm logic + flash handling)
- `src/main/java/com/dmmtracker/ui/SettingsPanel.java`  
  (alarm settings UI)

**Docs (if iterative)**
- `docs/CONFIG_REFERENCE.md` (alarm settings)
- `docs/REVIEW_NOTES.md` (feature scope + limitations)

**Review focus**
- Alert conditions and cooldowns.
- Respect user config and disabled states.

**Optional note**: `docs/submission-commit-notes/13-player-alarm.md`

---

### 14) Docs for reviewers + QA

**Intent**: give Plugin Hub reviewers a fast path.

**Files**
- `README.md`
- `docs/README.md`
- `docs/REVIEW_GUIDE.md`
- `docs/REVIEW_NOTES.md`
- `docs/QA_CHECKLIST.md`
- `docs/SECURITY_PRIVACY.md`
- `docs/DATA_CONTRACTS.md`
- `docs/DATA_FLOW_TRACE.md`
- `docs/CONFIG_REFERENCE.md`
- `docs/CODE_MAP.md`
- `docs/ARCHITECTURE.md`

**Review focus**
- Endpoints and payloads are documented.
- Test plan is explicit.

**Optional note**: `docs/submission-commit-notes/14-review-docs.md`

---

### 15) Attribution + licensing

**Intent**: ensure third-party usage is credited and licensed.

**Files**
- `LICENSE`
- `docs/CREDITS.md`
- `docs/THIRD_PARTY_LICENSES.md`
- `README.md` (acknowledgments section)

**Review focus**
- All external code/data/assets are attributed.
- License compatibility is spelled out.

**Optional note**: `docs/submission-commit-notes/15-licenses.md`

---

### 16) Tooling + code quality

**Intent**: align with RuneLite conventions and enable checks.

**Files**
- `config/checkstyle/checkstyle.xml`
- `config/formatter/runelite-eclipse-formatter.xml`
- `githooks/*`
- `scripts/check-conventions.sh`
- `docs/code-conventions.md`
- `.vscode/settings.json`

**Review focus**
- No auto-reordering of imports.
- Tabs enforced; format consistent.

## Optional Split Points (for even tighter reviews)

If you want smaller commits, consider splitting the larger ones:

- **Commit 05** → 05a Data models, 05b DataLoader + adapters
- **Commit 06** → 06a Icons, 06b Data JSON bundles
- **Commit 10** → 10a Event parsing + state updates, 10b Diary completion + varbit logic
- **Commit 11** → 11a Scene/minimap overlays, 11b World map overlays + legend
- **Commit 12** → 12a Targets tab, 12b Diaries tab, 12c Bosses + CA tabs, 12d Sigils + Search, 12e Settings panel

## Testing Gates (lightweight per stage)

- **After commit 02**: plugin loads and panel opens (no features).
- **After commit 05/06**: data loads; no JSON parse errors in log.
- **After commit 08/09**: device code flow reachable; API calls stubbed or disabled if not configured.
- **After commit 10–12**: basic sync triggers and overlay rendering operate without errors.
- **Before final submission**: run full manual QA from `docs/QA_CHECKLIST.md`.

**Optional note**: `docs/submission-commit-notes/16-tooling.md`

---

## Suggested Final Checklist (pre-PR)

- `ApiConfig.USE_LOCAL = false` for release build.
- Remove or ignore local build artifacts (`build/`, `target/`, `.gradle/`).
- Confirm `runelite-plugin.properties` has correct `displayName`, `author`, `description`, `tags`, `plugins`.
- Ensure README mentions data sent and how to opt-out.
- Verify license texts and attribution links.

## Submission Summary (high level)

- Push repo to GitHub.
- Submit Plugin Hub PR referencing a **specific commit hash**.
- Keep changes minimal and easy to review with this commit plan.
