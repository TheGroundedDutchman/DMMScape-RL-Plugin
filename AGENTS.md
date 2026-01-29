# AGENTS.md

---
## ⚠️ IMPORTANT: PLUGIN LOADING - READ THIS FIRST ⚠️

**Sideloaded plugins DO NOT work properly!** RuneLite's event bus cannot create lambdas for sideloaded plugin classes, so `@Subscribe` event handlers will NOT fire.

**The ONLY way to develop this plugin is using the built-in plugin location in RuneLite source.**

### Required Setup (Every Time You Make Changes)

```bash
# 1. Sync plugin source to RuneLite
rsync -av --delete --exclude='*.class' \
  /path/to/dmm-tracker-plugin/src/main/java/com/dmmtracker/ \
  /path/to/runelite/runelite-client/src/main/java/net/runelite/client/plugins/dmmtracker/

# 2. Update package declarations from com.dmmtracker to net.runelite.client.plugins.dmmtracker
find /path/to/runelite/runelite-client/src/main/java/net/runelite/client/plugins/dmmtracker \
  -name "*.java" -exec sed -i '' 's/package com\.dmmtracker/package net.runelite.client.plugins.dmmtracker/g' {} \;

# 3. Update imports
find /path/to/runelite/runelite-client/src/main/java/net/runelite/client/plugins/dmmtracker \
  -name "*.java" -exec sed -i '' 's/import com\.dmmtracker/import net.runelite.client.plugins.dmmtracker/g' {} \;

# 4. Update fully qualified class references
find /path/to/runelite/runelite-client/src/main/java/net/runelite/client/plugins/dmmtracker \
  -name "*.java" -exec sed -i '' 's/com\.dmmtracker\./net.runelite.client.plugins.dmmtracker./g' {} \;

# 5. Sync resources (icons, data files)
rsync -av /path/to/dmm-tracker-plugin/src/main/resources/com/dmmtracker/ \
  /path/to/runelite/runelite-client/src/main/resources/net/runelite/client/plugins/dmmtracker/

# 6. Rebuild RuneLite
cd /path/to/runelite && ./gradlew :client:shadowJar -x test -x javadoc

# 7. Run (macOS)
java -ea --add-opens java.desktop/com.apple.eawt=ALL-UNNAMED --add-exports java.desktop/com.apple.eawt=ALL-UNNAMED \
  -jar /path/to/runelite/runelite-client/build/libs/client-1.12.14-SNAPSHOT-shaded.jar --developer-mode
```

### One-Liner (For Rapid Development)

```bash
rsync -av --delete --exclude='*.class' \
  /path/to/dmm-tracker-plugin/src/main/java/com/dmmtracker/ \
  /path/to/runelite/runelite-client/src/main/java/net/runelite/client/plugins/dmmtracker/ && \
find /path/to/runelite/runelite-client/src/main/java/net/runelite/client/plugins/dmmtracker \
  -name "*.java" -exec sed -i '' \
    -e 's/package com\.dmmtracker/package net.runelite.client.plugins.dmmtracker/g' \
    -e 's/import com\.dmmtracker/import net.runelite.client.plugins.dmmtracker/g' \
    -e 's/com\.dmmtracker\./net.runelite.client.plugins.dmmtracker./g' {} \; && \
rsync -av /path/to/dmm-tracker-plugin/src/main/resources/com/dmmtracker/ \
  /path/to/runelite/runelite-client/src/main/resources/net/runelite/client/plugins/dmmtracker/ && \
cd /path/to/runelite && ./gradlew :client:shadowJar -x test -x javadoc && \
pkill -f "client.*shaded" 2>/dev/null; sleep 2 && \
java -ea --add-opens java.desktop/com.apple.eawt=ALL-UNNAMED --add-exports java.desktop/com.apple.eawt=ALL-UNNAMED \
  -jar runelite-client/build/libs/client-1.12.14-SNAPSHOT-shaded.jar --developer-mode > /tmp/runelite-run.log 2>&1 &
echo "Started. Check: sleep 15 && grep 'DMMScape' /tmp/runelite-run.log"
```

### Why This Is Required

1. **RuneLite only scans `net.runelite.client.plugins.*`** for built-in plugins
2. **Sideloaded plugins have broken event handling** due to classloader isolation (`LambdaMetafactory` fails)
3. **Package must match directory structure** for Java compilation

### Verify It Works

```bash
# Check console log (if using the one-liner)
grep "DMMScape v" /tmp/runelite-run.log

# Or check RuneLite client log
grep "DMMScape v" ~/.runelite/logs/client.log | tail -1

# Should show: n.r.c.p.dmmtracker.DMMTrackerPlugin - DMMScape vXX started
# NO lambda errors or LambdaConversionException should appear
```

### Version Bumping

When releasing a new version:

1. Update `PLUGIN_VERSION` in `DMMTrackerPlugin.java`:
   ```java
   public static final String PLUGIN_VERSION = "v16";
   ```

2. Sync, rebuild, and verify the new version loads:
   ```bash
   grep "DMMScape v" ~/.runelite/logs/client.log | tail -1
   ```

### Clean Build (If Stale Version Persists)

If the wrong version is still showing after a normal build, do a full clean:

```bash
cd /path/to/runelite && ./gradlew clean :client:shadowJar -x test -x javadoc
```

---

## Scope
- Applies to the RuneLite plugin in `dmm-tracker-plugin`.
- Canonical conventions live in `docs/code-conventions.md`; keep this file in sync.
- See `docs/code-conventions.md` for expanded RuneLite wiki guidance (plugin hub, config panels, logging, git/PR workflow, dev tools, client scripts/vars, rejected features, takeover policy, Javadocs).

## RuneLite Code Conventions
- Tabs, not spaces.
- Brace placement is always on the next line.
- Add the RuneLite-style copyright header to the top of every new text file.
- Imports must be in a single logical block (no blank lines between import groups).
- Avoid wildcard imports; allow static wildcard imports only when RuneLite conventions prefer them (for example, ItemIDs).
- Multi-line annotations should be indented with an extra tab.
- Use the RuneLite copyright header template shown in the RuneLite wiki example.
- Use a single blank line to separate logical code blocks within methods/classes.

## Copyright Header Template
```
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
```

## Tooling Expectations
- Prefer formatting with RuneLite's IntelliJ code style when available.
- Do not auto-reorder imports; keep a single logical block.
- Use the repo hook setup (`git config core.hooksPath githooks`) and `.editorconfig` to enforce tabs and import rules.
- VS Code users should keep `.vscode/settings.json` pointing to `config/formatter/runelite-eclipse-formatter.xml`.

## IntelliJ Integration (Recommended)
- Import RuneLite's IntelliJ code style XML and format with Ctrl+Alt+L.
- Install Checkstyle-IDEA and point it at RuneLite's `checkstyle.xml`.
- Enable the Checkstyle configuration as Active.

## Import Settings
- Disable wildcard imports: set "Class count to use import with '*'" to 999.
- Disable wildcard static imports: set "Names count to use static imports with '*'" to 999.
- Remove the Import Layout section to prevent reordering.
- Remove entries in "Packages to Use Import with '*'" to prevent `java.awt.*` replacements.

---

## macOS-Specific JVM Flags

### Required Flags Explained

| Flag | Purpose |
|------|---------|
| `-ea` | Enable assertions (RuneLite requires this) |
| `--add-opens java.desktop/com.apple.eawt=ALL-UNNAMED` | macOS fullscreen support |
| `--add-exports java.desktop/com.apple.eawt=ALL-UNNAMED` | macOS fullscreen support |
| `--developer-mode` | Enable plugin panel visibility |

### Common Errors

**"Fatal error starting RuneLite" / Assertion Errors:**
```
Developers should enable assertions; Add --ea to your JVM arguments
```
**Solution:** Add `-ea` flag.

**IllegalAccessError: OSXFullScreenAdapter:**
```
java.lang.IllegalAccessError: class net.runelite.client.ui.OSXFullScreenAdapter cannot access class com.apple.eawt.FullScreenAdapter
```
**Solution:** Add both `--add-opens` and `--add-exports` flags.

---

## Troubleshooting

### Plugin not loading / No DMMScape in logs
Package declarations weren't updated. Re-run the sed commands to update all `com.dmmtracker` to `net.runelite.client.plugins.dmmtracker`.

### Lambda conversion errors / Events not firing
```
LambdaConversionException: Invalid caller class
```
You're running a sideloaded plugin. **Use the built-in location approach described at the top of this file.**

### Tabs are empty / No data loading
Check `~/.runelite/logs/client.log` for JSON parsing errors:
- **"Expected a string but was BEGIN_OBJECT"**: Data model mismatch
- **Solution**: Use custom Gson type adapters (like `QuestRequirementTypeAdapter`)

### Data model classes (for reference)
- `DiaryQuestRequirement.java` - Handles both string and object quest requirement formats
- `QuestRequirementTypeAdapter.java` - Custom Gson deserializer for flexible quest requirements
- `DataLoader.java` - Registers type adapters with GsonBuilder

### Plugin not visible in RuneLite sidebar
Plugin may be disabled. Enable it:
```bash
sed -i '' 's/runelite.dmmtrackerplugin=false/runelite.dmmtrackerplugin=true/' ~/.runelite/profiles2/default-*.properties
```

Or via UI: Configuration panel (wrench icon) → Search "DMMScape" → Toggle ON

### Log locations
- Console output: `/tmp/runelite-run.log` (when using the one-liner)
- RuneLite client log: `~/.runelite/logs/client.log`
- Plugin data: `~/.runelite/dmmtracker/`

---

## Common Issues Summary

| Issue | Symptom | Solution |
|-------|---------|----------|
| Assertion error | "Fatal error starting RuneLite" | Add `-ea` JVM flag |
| macOS module access | IllegalAccessError: OSXFullScreenAdapter | Add `--add-opens` and `--add-exports` flags |
| Plugin not in sidebar | Plugin not visible | Check `runelite.dmmtrackerplugin=true` in profile |
| Lambda conversion errors | Events don't fire, `LambdaConversionException` | **Use built-in plugin location** |
| Wrong version loading | Old version showing after changes | 1) Ensure packages updated after rsync, 2) Do clean build: `./gradlew clean :client:shadowJar` |
| Icon/resource warnings | "Failed to load icon" | Sync resources to correct location |
| Build uses wrong module | `:runelite-client` not found | Use `:client:` not `:runelite-client:` |
