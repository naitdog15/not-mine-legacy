# Not Mine Legacy (NML) Core

## Identity

**Not Mine Legacy** (mod id `notminelegacy`, jar `notminelegacy-1.0.0.jar`) is the shared mandatory dependency of all 8 module mods: buildingbridges, mixedblessings, noaddedsugar, nml_concord, overdueoverhauls, remnantstars, tidalaccord, whatremains (each in a sibling repo under `../`).

- **Target**: MC 1.20.1 / Forge 47.4.10 / Java 17, ForgeGradle 6.0.54, Gradle 8.8 wrapper
- **Origin**: Split from temog-port monorepo @1cedd534
- **Core packages** (`src/main/java/com/temog/notminelegacy/`):
  - `client`: SpeechBubbleRenderer + client compat
  - `compat`: DonkeyChestBridge, EarlyModCheck
  - `mixin`: client/common mixins + NotMineLegacyMixinPlugin
  - `speech`: speech-bubble system
  - `structure`: village key system
- **Mixin config**: `notminelegacy.mixins.json` (registered in `mods.toml` AND as `-mixin.config` arg in `build.gradle` — missing from run arg silently never loads in dev)
- **Access transformer**: `src/main/resources/META-INF/accesstransformer.cfg`

## Build + Run

From repo root:

```
.\gradlew.bat stageRunMods              # Stages run/mods_raw, parses filenames → local.runmods coords
.\gradlew.bat compileJava --offline    # ~20s
.\gradlew.bat build --offline
.\gradlew.bat runClient / runServer
```

Or use the dev helper: `.\dev.ps1 -Action stage|compile|build|client|server` (same tasks, plus gradle-slot handling).

**Gradle slot protocol** (before manual gradle): acquire `..\..\_tools\.gradle-slot-A` or `-B` (write `<repo> <task> <timestamp>` inside; both fresh (<15 min) = wait 60s, retry; delete your slot after gradle exits). Daemon disabled; offline-first, retry online only on cache-miss failures.

## Dependency Mechanism

This repo is **upstream of everything**. Consumers stage `notminelegacy-1.0.0.jar` in their own `run/mods_raw` and declare:

```
compileOnly fg.deobf('local.runmods:notminelegacy:1.0.0')
```

The coordinate is parsed from the **filename** by stageRunMods — **never rename jars**. After changing NML: `..\..\_tools\workspace-build.ps1` rebuilds NML, publishes the jar to `..\..\_tools\built-jars\`, fans it out to every consumer repo's `run/mods_raw`, and refreshes this repo's cohesion env. NML source is **never** copied into other repos.

## Cohesion Environment

This repo's `run/mods_raw` holds all 8 sibling module jars (from `..\..\_tools\built-jars\`, seed-jars fallback) plus third-party runtime union (Alex's Mobs+Citadel, Naturalist, Quark+Zeta, VanillaBackport, Aether, GeckoLib, Jade, Curios, JEI, Buzzier Bees, Twilight Forest, Sea Dwellers, plus the transitive libraries Platform and Blueprint — exact 16-jar list in `DEV_JARS.md`) — so `runServer` here boots the entire mod ecosystem as the cohesion test.

- **Critical**: NML's own classes come from dev sources; **never** stage `notminelegacy-1.0.0.jar` into this repo's own `run/mods_raw`
- **LSO jar** (legendarysurvivaloverhaul) must **NEVER** be staged here — not redistributable; building-bridges-only gitignored opt-in
- **GeckoLib**: NOT a compile requirement — verified, see `DEV_JARS.md`
- All `run/` jars are gitignored; `DEV_JARS.md` documents required local jars

## Pointers

- **Workspace rules**: `../../_context/rules/` (workflow.md, forge-workspace.md, reference-first.md)
- **Reference material**: `../../_reference/not-mine-legacy/` (read-only)
- **Shared tooling**: `../../_tools/` (built-jars, seed-jars, workspace-build.ps1, repo-shells)
- **No path-scoped rules** apply to NML core (target module subpackages in sibling repos)

## Sibling Etiquette

One repo = one write boundary. Read sibling repos freely; never write to them — cross-repo changes go through the workspace orchestrator. **Module ownership**: enchant compat → overdueoverhauls, cross-mod bridges → buildingbridges; OO and BB must **NEVER** import each other, only NML. Never add remotes, never push.

## Git Rule (Critical)

**NEVER** run `git add`/`git commit` from the terminal in this repo for development work — the user commits exclusively via GitHub Desktop. (The one-time migration commit was a sanctioned exception.)
