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

# Credits and Attribution

This file is intended to track third-party inspirations, libraries, and assets used by the plugin. Update it whenever new dependencies or assets are added.
License texts for credited items live in `docs/THIRD_PARTY_LICENSES.md`.

## Platforms and frameworks

- RuneLite client and RuneLite Plugin Hub

## Libraries (direct)

From `pom.xml` / `build.gradle`:

- Gson (`com.google.code.gson:gson`)
- PF4J (`org.pf4j:pf4j`) (provided)
- JUnit (`junit:junit`) (test)

Note: OkHttp is used via the RuneLite client dependency (transitive).

## Design and UX inspirations

- Quest Helper (route overlay patterns)
- Combat Achievements Tracker (panel layout inspiration)
- WikiSync (sync concept inspiration)

## Code and feature attributions

- Wilderness Player Alarm (Alex) — player alarm feature adapted and extended beyond Wilderness (BSD 2-Clause; https://github.com/adhansen/plugin-repo)

## Assets

- `src/main/resources/com/dmmtracker/icons/*` are derived from Old School RuneScape/RuneScape game assets and iconography. Those assets are © Jagex Ltd. This plugin uses them solely for in-game UI context. If any icon is not sourced from Jagex assets, list its source and license here.

## Datasets

- `dmmscape-data.json` is generated internally. If any external datasets are used, list source and license.
- Diary completion rules embedded in `dmmscape-data.json` were derived from Quest Helper (BSD 2-Clause).
- Teleport locations are sourced from RuneLite's `TeleportLocationData` (BSD 2-Clause).
