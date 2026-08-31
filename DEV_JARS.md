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
`../../_tools/workspace-build.ps1` (to be added) and `MIGRATION_MAP.md` section 4.
