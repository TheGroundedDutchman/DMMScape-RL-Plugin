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

# DMMScape Plugin - Development Setup (macOS)

---
## ⚠️ CRITICAL: DO NOT USE SIDELOADED PLUGINS ⚠️

**Sideloaded plugins have BROKEN event handling!** RuneLite's event bus cannot create lambdas for external classloaders, so `@Subscribe` methods will NOT fire.

**You MUST use the built-in plugin location in RuneLite source.**

---

## Prerequisites

- Java 17+ installed (`brew install openjdk@17`)
- RuneLite source cloned at `/path/to/runelite`
- Plugin source at `/path/to/dmm-tracker-plugin`

## Complete Development Workflow

### Step 1: Sync Source Files

```bash
rsync -av --delete --exclude='*.class' \
  /path/to/dmm-tracker-plugin/src/main/java/com/dmmtracker/ \
  /path/to/runelite/runelite-client/src/main/java/net/runelite/client/plugins/dmmtracker/
```

### Step 2: Update Package Declarations (CRITICAL!)

RuneLite only scans `net.runelite.client.plugins.*` for built-in plugins. You MUST update all package references:

```bash
# Update package declarations
find /path/to/runelite/runelite-client/src/main/java/net/runelite/client/plugins/dmmtracker \
  -name "*.java" -exec sed -i '' 's/package com\.dmmtracker/package net.runelite.client.plugins.dmmtracker/g' {} \;

# Update imports
find /path/to/runelite/runelite-client/src/main/java/net/runelite/client/plugins/dmmtracker \
  -name "*.java" -exec sed -i '' 's/import com\.dmmtracker/import net.runelite.client.plugins.dmmtracker/g' {} \;

# Update fully qualified class references
find /path/to/runelite/runelite-client/src/main/java/net/runelite/client/plugins/dmmtracker \
  -name "*.java" -exec sed -i '' 's/com\.dmmtracker\./net.runelite.client.plugins.dmmtracker./g' {} \;
```

### Step 3: Sync Resources (Icons, Data)

```bash
rsync -av /path/to/dmm-tracker-plugin/src/main/resources/com/dmmtracker/ \
  /path/to/runelite/runelite-client/src/main/resources/net/runelite/client/plugins/dmmtracker/
```

### Step 4: Rebuild RuneLite

```bash
cd /path/to/runelite && ./gradlew :client:shadowJar -x test -x javadoc
```

### Step 5: Run RuneLite

```bash
java -ea \
  --add-opens java.desktop/com.apple.eawt=ALL-UNNAMED \
  --add-exports java.desktop/com.apple.eawt=ALL-UNNAMED \
  -jar /path/to/runelite/runelite-client/build/libs/client-1.12.14-SNAPSHOT-shaded.jar \
  --developer-mode
```

## Full One-Liner (For Rapid Development)

```bash
# Sync, update packages, rebuild, run
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

## Verify Correct Plugin Loaded

```bash
# Check console log (if using the one-liner redirect)
grep -i "dmmscape.*started" /tmp/runelite-run.log

# Or check RuneLite's persistent log
grep "DMMScape v" ~/.runelite/logs/client.log | tail -1
```

**Expected (correct):**
```
n.r.c.p.dmmtracker.DMMTrackerPlugin - DMMScape vXX started
```

**NO lambda conversion errors should appear.**

If you see `LambdaConversionException` errors, the package declarations weren't updated correctly.

## Version Bumping

When releasing a new version:

1. Update `PLUGIN_VERSION` in `src/main/java/com/dmmtracker/DMMTrackerPlugin.java`:
   ```java
   public static final String PLUGIN_VERSION = "v16";
   ```

2. Sync, rebuild, and verify the new version loads.

## Clean Build (If Wrong Version Persists)

If you've synced and rebuilt but the old version still shows:

```bash
# Full clean rebuild
cd /path/to/runelite && ./gradlew clean :client:shadowJar -x test -x javadoc
```

**Note:** Use `:client:` not `:runelite-client:` - the project module is named `client`.

## Required JVM Flags Explained

| Flag | Purpose |
|------|---------|
| `-ea` | Enable assertions (RuneLite requires this) |
| `--add-opens java.desktop/com.apple.eawt=ALL-UNNAMED` | macOS fullscreen support |
| `--add-exports java.desktop/com.apple.eawt=ALL-UNNAMED` | macOS fullscreen support |
| `--developer-mode` | Enable plugin panel (not needed for built-in plugins) |

## Troubleshooting

### "Fatal error starting RuneLite"
Missing `-ea` flag. Add it to java command.

### IllegalAccessError: OSXFullScreenAdapter
Missing `--add-opens` and `--add-exports` flags.

### Plugin not loading / No DMMScape in logs
Package declarations weren't updated. Re-run the sed commands in Step 2.

### Lambda conversion errors
You're running a sideloaded plugin. Follow this guide to use the built-in location instead.

### Plugin loads but events don't fire
Same as above - sideloaded plugin. Use built-in location.

## Why Not Sideloading?

1. **Classloader isolation**: Sideloaded JARs use separate classloader
2. **EventBus limitation**: `LambdaMetafactory` can't create method handles across classloaders
3. **Result**: `@Subscribe` event handlers silently fail

The built-in plugin location compiles directly into RuneLite's main JAR, avoiding all classloader issues.

## Log Locations

- RuneLite console: `/tmp/runelite-run.log` (if using redirect)
- RuneLite client: `~/.runelite/logs/client.log`
- Plugin data: `~/.runelite/dmmtracker/`
