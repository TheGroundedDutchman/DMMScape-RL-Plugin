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

# ADR 0006 - Device code flow with short-lived sync tokens

Status: Accepted (2026-01-28)

## Context

The plugin requires authenticated API access without embedding a full browser login inside the client. Users should be able to link their account securely, and API keys should remain optional.

## Decision

- Use RFC 8628 device code flow for linking (`DeviceAuthService`).
- Store access tokens in RuneLite config.
- Request short-lived sync tokens for API calls via `RuneliteTokenService`.
- Allow API key as a fallback for advanced users.

## Consequences

- Linking can happen entirely within the plugin UI using a browser prompt.
- Tokens rotate to reduce long-lived credential exposure.
- Config changes that affect auth must invalidate cached auth headers.

## Alternatives considered

- Username/password login in-plugin: rejected for security and UX reasons.
- API key only: rejected due to high friction for normal users.

## References

- `DeviceAuthService`
- `RuneliteTokenService`
- `DMMTrackerPlugin.syncAuthHeaderIfNeeded`
