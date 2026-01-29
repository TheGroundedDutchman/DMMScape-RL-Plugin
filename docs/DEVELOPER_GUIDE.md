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

# DMMScape RuneLite Plugin - Developer Guide

This guide consolidates development workflow and conventions. It is sourced from `AGENTS.md` and existing setup docs to reduce onboarding time.

## Critical: no sideloading

Sideloaded plugins do not work reliably for this codebase. RuneLite's EventBus cannot create lambdas across classloaders, so `@Subscribe` handlers do not fire. Development must use the built-in RuneLite plugin location under `net.runelite.client.plugins.*`.

## Beginner mental model (setup in RuneLite)

Three concepts people mix together:

1) Custom plugin
   - Your code is not in official RuneLite.
   - To run it, you either embed it in RuneLite source (our workflow) or sideload a jar.
   - For this plugin, sideloading breaks `@Subscribe` event handlers.

2) Dev/snapshot build
   - A client you compiled yourself from the RuneLite repo.
   - It is not the official release build. It is whatever current `master` is.

3) Developer mode flag (`--developer-mode`)
   - Enables dev UI tools and sideloading.
   - It does not mean "snapshot" by itself.

Do you "need dev/snapshot" to use our custom plugin?
- Yes, in practice. Our plugin must be built into the client (per `AGENTS.md`), which means a custom build.

But that does not block other plugins:
- Core plugins (Menu Entry Swapper, Ground Markers, etc.) still work.
- Plugin Hub plugins might fail on snapshot builds if they were built against a different release API. You can try:
  ```
  -Drunelite.pluginhub.version=<current RL version>
  ```
  This only works when there are no ABI changes between your snapshot and the release.
- Other dev plugins in jars (sideloaded) can break for the same EventBus lambda reason as this plugin.

Quick "what should I do" guide:
- Our plugin + core plugins only: use the built-in flow (this guide).
- Our plugin + Plugin Hub plugins: use a custom build + try the version override above.
- Our plugin + another dev jar: embed it as built-in too, or patch RuneLite's sideload classloader.

## Prerequisites

- Java 17
- RuneLite source at `/path/to/runelite`
- Plugin source at `/path/to/dmm-tracker-plugin`

## Required workflow (authoritative)

Every time you change code:

```bash
# 1. Sync plugin source to RuneLite
rsync -av --delete --exclude='*.class' \
  /path/to/dmm-tracker-plugin/src/main/java/com/dmmtracker/ \
  /path/to/runelite/runelite-client/src/main/java/net/runelite/client/plugins/dmmtracker/

# 2. Update package declarations
find /path/to/runelite/runelite-client/src/main/java/net/runelite/client/plugins/dmmtracker \
  -name "*.java" -exec sed -i '' 's/package com\.dmmtracker/package net.runelite.client.plugins.dmmtracker/g' {} \;

# 3. Update imports
find /path/to/runelite/runelite-client/src/main/java/net/runelite/client/plugins/dmmtracker \
  -name "*.java" -exec sed -i '' 's/import com\.dmmtracker/import net.runelite.client.plugins.dmmtracker/g' {} \;

# 4. Update fully qualified references
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

One-liner (sync, rebuild, run):

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

## Verify the correct plugin is loaded

```bash
# Console log (if using redirect)
grep "DMMScape" /tmp/runelite-run.log

# Or RuneLite persistent log
grep "DMMScape v" ~/.runelite/logs/client.log | tail -1
```

Expected:
- `n.r.c.p.dmmtracker.DMMTrackerPlugin - DMMScape vXX started`
- No `LambdaConversionException` errors

## Version bumping

1. Update `PLUGIN_VERSION` in `DMMTrackerPlugin.java`
2. Sync, rebuild, verify new version loads

## Clean build (if stale version persists)

```bash
cd /path/to/runelite && ./gradlew clean :client:shadowJar -x test -x javadoc
```

**Note:** Use `:client:` not `:runelite-client:` for the Gradle module.

## Common failures

- Assertion error: add `-ea` to JVM flags.
- `IllegalAccessError: OSXFullScreenAdapter`: add `--add-opens` and `--add-exports` flags.
- `LambdaConversionException`: you are sideloading or package declarations are wrong.
- Plugin not visible: enable it in config or ensure package sync step ran.
- Wrong version loading: do a clean build (`./gradlew clean :client:shadowJar`).

## Code conventions (summary)

Authoritative rules are in `docs/code-conventions.md`. Key points:

- Tabs, not spaces.
- Brace on the next line.
- Add the RuneLite-style copyright header to every new text file.
- Imports are a single logical block (no blank lines between groups).
- Avoid wildcard imports (static wildcard only when RuneLite conventions prefer it).
- Multi-line annotations are indented with an extra tab.

## IDE setup (IntelliJ)

- Import RuneLite code style XML.
- Install Checkstyle-IDEA and point it to RuneLite `checkstyle.xml`.
- Disable wildcard imports (set thresholds to 999).
- Ensure IntelliJ honors `.editorconfig` (tabs in Java, no wildcard imports).
- Enable repo hooks: `git config core.hooksPath githooks`.
- Optional: run `scripts/check-conventions.sh` and `./gradlew checkstyleMain` before pushing.

## IDE setup (VS Code)

- Install the EditorConfig extension so `.editorconfig` is respected.
- Ensure the Java formatter uses the repo config:
  - `.vscode/settings.json` points to `config/formatter/runelite-eclipse-formatter.xml`.
  - Formatter profile name: `RuneLite`.

## Data and log locations

- Plugin data: `~/.runelite/dmmtracker/`
- Sync/target logs: `/tmp/runelite-run.log` or `~/.runelite/logs/client.log`
- Optional target file: `~/.runelite/dmm-targets.json`

## See also

- `docs/ARCHITECTURE.md`
- `docs/ADR/README.md`
- `docs/DEVELOPMENT-SETUP.md`
