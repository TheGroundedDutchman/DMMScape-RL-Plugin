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

# Code Conventions (RuneLite Plugin)

## Scope
- Applies to the RuneLite plugin in `dmm-tracker-plugin`.
- Treat this doc as the canonical source; keep `AGENTS.md` in sync.

## Formatting (RuneLite Code Conventions)
- Tabs, not spaces.
- Brace placement is always on the next line.
- Use a single blank line to separate logical code blocks.
- Imports are a single logical block (no blank lines between import groups).
- Avoid wildcard imports; allow static wildcard imports only when RuneLite conventions prefer them (for example, ItemIDs).
- Multi-line annotations should be indented with an extra tab.
- Add the RuneLite-style copyright header to the top of every new text file.

## Plugin Architecture (Developer Guide)
- Plugins are singletons that extend `Plugin` and are annotated with `@PluginDescriptor`.
- A plugin typically provides one or more of: event subscriptions, overlays, and plugin panels.
- Plugins are configurable via the RuneLite settings panel.

## Plugin Hub Packaging (If Shipping via Plugin Hub)
- Each plugin lives in its own folder in the `plugin-hub` repo.
- `runelite-plugin.properties` is required; the `plugins` key lists the plugin class(es).
- `description` and `tags` are optional; `name` and `provider` may be supplied but should match `@PluginDescriptor` when present.
- Include a README and a 32x32 PNG icon.

## Configuration and Panels
- Use a configuration interface that extends `Config` and annotate it with `@ConfigGroup`.
- Use `@ConfigItem` to define `keyName`, `name`, `description`, and `position`.
- Group related config with `@ConfigSection` and use `position` for ordering.

## Logging
- Use the built-in slf4j logging; RuneLite uses Lombok to generate the logger instance.
- Logs go to `~/.runelite/logs/client.log`.
- Use log levels intentionally; `debug` is hidden by default and should be used for verbose output.

## IntelliJ Integration (Recommended)
- Import RuneLite's IntelliJ code style XML and format with Ctrl+Alt+L.
- Install Checkstyle-IDEA and point it at RuneLite's `checkstyle.xml`.
- Enable the Checkstyle configuration as Active.

## VS Code Integration
- Install the EditorConfig extension so VS Code honors `.editorconfig`.
- Ensure the Java extension uses the repo formatter config:
  - `.vscode/settings.json` points to `config/formatter/runelite-eclipse-formatter.xml`.
- The formatter profile is named `RuneLite`.

## Import Settings
- Disable wildcard imports: set "Class count to use import with '*'" to 999.
- Disable wildcard static imports: set "Names count to use static imports with '*'" to 999.
- Remove the Import Layout section to prevent reordering.
- Remove entries in "Packages to Use Import with '*'" to prevent `java.awt.*` replacements.

## Repo Enforcement (IDE + Git)
- `.editorconfig` enforces tabs in Java and disables wildcard imports for IntelliJ.
- `scripts/check-conventions.sh` validates tabs, import blocks, wildcard imports, and license headers.
- `config/checkstyle/checkstyle.xml` + `config/checkstyle/suppressions.xml` provide the RuneLite Checkstyle ruleset.
- Git hook: `git config core.hooksPath githooks` to enable the pre-commit hook.
- Pre-commit runs: `scripts/check-conventions.sh` and `./gradlew checkstyleMain`.

## Tooling Notes (RuneLite IntelliJ Guide)
- Follow the RuneLite IntelliJ setup for project import and run configurations.
- Use the RuneLite Checkstyle configuration when working in the RuneLite source tree.

## Git and PR Workflow (RuneLite Wiki)
- Fork and clone the RuneLite repo, add an `upstream` remote, and create a feature branch.
- Rebase on `upstream/master` before opening a PR.
- PR titles and commit messages use the format `module: Summary` (example: `runelite-client: Remove WidgetInfo`).
- Use `git commit --amend` and `git rebase -i` to update commits, then force-push your branch.
- The Git cheat sheet is the quick reference for common commands (`status`, `diff`, `checkout -b`, `rebase -i`, `push -f`).

## Developer Tools and Accounts
- Use `--developer-mode` to access the Developer Tools and Client Debug plugins; `-ea` enables assertions and `--debug` keeps logs in the console.
- Jagex account users must use the Jagex Launcher at least once so RuneLite can read stored credentials, then run the dev client with `--developer-mode`.

## Client Scripts and Vars
- Client scripts are written in RS2ASM and are part of the game client, not Java code.
- VarPlayers are server-synced, VarBits are bit ranges within VarPlayers, and VarClients are client-only.
- Use the Var Inspector in Developer Tools to discover VarPlayers and VarBits.

## Feature and Ownership Policies
- Check the "Rejected or Rolled Back Features" list before proposing new functionality; features on that list require strong justification to revisit.
- Follow the plugin takeover policy when maintaining or adopting an existing plugin.

## Reference Docs
- RuneLite API Javadoc and client Javadoc are the primary references for public APIs and client classes.

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
