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

# ADR 0001 - Do not use sideloaded plugins for development

Status: Accepted (2026-01-28)

## Context

RuneLite only scans built-in plugins under `net.runelite.client.plugins.*`. Sideloaded JARs run in a separate classloader, and RuneLite's EventBus lambda generation fails across classloader boundaries. The result is that `@Subscribe` handlers do not fire reliably, breaking the plugin.

## Decision

All development and testing must use the built-in plugin location inside the RuneLite source tree. The plugin source is synchronized into the RuneLite client tree, package declarations are rewritten, and RuneLite is rebuilt.

## Consequences

- Developers must keep the rsync/sed workflow in sync with source changes.
- The plugin codebase uses `com.dmmtracker` for the Plugin Hub build but must be rewritten to `net.runelite.client.plugins.dmmtracker` for local development.
- Documentation must emphasize the no-sideloading rule to avoid confusion.

## Alternatives considered

- Sideloaded JAR in `~/.runelite/sideloaded-plugins/`: rejected due to EventBus lambda failure.
- External plugin loader: rejected due to the same classloader boundary issue.

## References

- `AGENTS.md`
- `docs/DEVELOPMENT-SETUP.md`
- `dmm-tracker-plugin/README.md`
