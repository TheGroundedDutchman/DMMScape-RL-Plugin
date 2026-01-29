# Contributing to DMMScape RuneLite Plugin

Thanks for your interest in contributing! This guide covers the basics for working on the plugin.

## Quick Start

Follow the authoritative setup in:
- `docs/DEVELOPMENT-SETUP.md`

## Repository Guidelines

- Do **not** develop via sideloaded plugins (RuneLite events won’t fire).
- Keep config keys documented in `docs/CONFIG_REFERENCE.md`.
- Update `docs/DATA_CONTRACTS.md` if payloads change.

## Testing

Manual checks (see `docs/QA_CHECKLIST.md`):
- Plugin loads in RuneLite
- Progress sync sends payloads
- Targets overlay renders
- Config toggles behave as expected

## Pull Request Checklist

- [ ] Manual QA checklist completed
- [ ] Docs updated when behavior changes
- [ ] No breaking changes without migration notes

## Where to Look

- Architecture + diagrams: `docs/ARCHITECTURE.md`
- Data contracts: `docs/DATA_CONTRACTS.md`
- Review guide: `docs/REVIEW_GUIDE.md`
