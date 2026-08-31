# DEV_JARS — not-mine-legacy

Local jar dependencies required to build and run this repo in dev.

| filename | scope | source location | sha256 |
|---|---|---|---|
| geckolib-forge-1.20.1-4.8.2.jar | compile | `C:\Users\Nathan\Documents\Minecraft\Temog3Workspace\temog-port\modern_1.20.1\run\mods_raw\` (or `run_cached\mods_raw`) | `a2e4bcc986ce360f4e85d545a86b04c8c9350543ebd17069b6a44c0a8939ffdc` |

**GeckoLib is NOT required to compile this repo.** Verified 2026-08-31: no source file imports
`software.bernie.*`; core's GeckoLib coupling is by modid string only
(`NotMineLegacyMixinPlugin` gates `GeckoLibCreeperRendererFallbackMixin` on `"geckolib"`, and the mixin
itself targets vanilla `EntityRenderers`). The `compileOnly fg.deobf(files(...))` line in `build.gradle`
mirrors the monorepo's declaration for provisioning/parity; `compileJava` succeeds with the jar absent.
Removing it is safe if you want a zero-local-jar build.

Additionally, **MixinExtras** is consumed via Maven coordinates (compileOnly + annotationProcessor) — it is not a
local jar and does not need staging in `run/mods_raw/`.

These jars are gitignored local dependencies. Copy them into `run/mods_raw/` before building. See
`../../_tools/workspace-build.ps1` (builds NML, fans the jar out to consumers, refreshes this repo's
cohesion environment) and `MIGRATION_MAP.md` section 4.

## Cohesion environment (this repo only)

`run/mods_raw/` additionally carries all 8 sibling module jars plus the third-party runtime union so
`runServer` here boots the whole ecosystem. Sources recorded 2026-08-31: `nml_concord-1.0.0.jar` from
`_tools/built-jars`; the other 7 module jars from `_tools/seed-jars` (swap to built-jars as leaders
deliver); third-party jars copied read-only from the monorepo's `modern_1.20.1/run/mods_raw/`.
The third-party union is 16 jars: the 14 direct runtime deps (Alex's Mobs, Citadel, Naturalist, Quark,
Zeta, VanillaBackport, Aether, GeckoLib, Jade, Curios, JEI, Buzzier Bees, Twilight Forest, Sea Dwellers)
plus two transitive libraries the 2026-08-31 smoke boot proved mandatory: `Platform-forge-1.20.1-1.2.11.3.jar`
(required by vanillabackport `[1.2.11,)`) and `blueprint-1.20.1-7.1.3.jar` (required by buzzier_bees `[7.1.0,)`).
Never stage `notminelegacy-1.0.0.jar` into this repo's own `run/mods_raw` (dev sources provide it),
and never stage the LSO jar here (not redistributable; building-bridges-only opt-in).

**Smoke-boot history (2026-08-31):**
- Blocker 1 (RESOLVED): FML mod-sorting cycle among buildingbridges/noaddedsugar/mixedblessings/
  remnantstars mods.toml `ordering="AFTER"` declarations. Fixed by the arbitrated buildingbridges
  rebuild (`ordering="NONE"` on its NAS/MB/RS deps); the re-boot with all-built module jars passed
  sorting and constructed all 9 modules (module jars now all sourced from `_tools/built-jars`,
  hash-verified against its SHA256SUMS.txt).
- Blocker 2 (OPEN, escalated to remnant-stars): headless `runServer` crashes in remnantstars
  `FMLCommonSetupEvent` — `RemnantStarsNetwork.register` (RemnantStarsNetwork.java:122, the
  `PromptSpellbookNaming` PLAY_TO_CLIENT message) loads client-only `net.minecraft.client.gui.screens.Screen`
  on DEDICATED_SERVER (BootstrapMethodError via RuntimeDistCleaner). Pre-existing: the file is
  byte-identical to the monorepo's; it never surfaced because the ecosystem was only ever booted
  via `runClient`. A production dedicated server would crash the same way. Re-run the smoke boot
  after remnant-stars delivers a side-safe jar.
