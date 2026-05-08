# Migration Status

## Phase 0 - Repository foundation
Status: implemented and manually verified.

Verified by user:
- `./gradlew build` passed.
- `./gradlew runClient` started.
- `/radworks version` works.
- `/radworks dump` works.
- Dump JSON was created and checked.

## Phase 1 - Data-driven radiation rules
Status: implemented locally, build passed, client smoke test passed, pending user manual `/reload` and external datapack tests.

## MIGRATION_DECISION_ACCEPTED
- Minecraft version: `1.21.1`
- NeoForge version: `21.1.228`
- Java version: `21`
- Mod ID: `radworks`
- Java package: `dev.radworks`
- Mod version: `0.1.0`

These choices are accepted for Phase 0. They can be revisited if the target modpack is later confirmed to use another Minecraft/NeoForge version.

## Implemented
- Minimal NeoForge Gradle project.
- Mod metadata for `radworks`.
- Java package `dev.radworks`.
- `/radworks version`.
- `/radworks dump`.
- Data-driven radiation rule loading from `data/radworks/radiation_rules/*.json`.
- Default validation mode: `lenient/dev`.
- `/radworks validate`.
- Rules summary in `/radworks version`.
- Rules validation summary in `/radworks dump`.
- Documentation: `AGENTS.md`, `README.md`, `MIGRATION_STATUS.md`, `TESTING.md`, `DIAGNOSTICS.md`, `CHANGELOG.md`.

## Phase 1 temporary/dev-only rule
- `src/main/resources/data/radworks/radiation_rules/dev_rotten_flesh.json`
- Rule ID: `minecraft:rotten_flesh`
- Purpose: smoke-test the loader and validation without requiring Create, Create Nuclear, TFMG or a full modpack.
- Status: temporary/dev-only; not final gameplay balance.

## Explicitly not implemented in Phase 0
- Radiation mechanics.
- Gameplay use of radioactive items, blocks, fluids or effects.
- Shielding.
- Capabilities or attachments.
- Radiation configs under `config/radworks/`.
- Exposure calculation.
- Player inventory, world, block, entity or container scanning.
- Create, Create Nuclear or Aeronautics dependencies.
- KubeJS dependency.
- Packages named `registry`, `integration/create` or `integration/aeronautics`.

## Build status
- Java 21 is available locally.
- A Gradle wrapper was added from an existing local wrapper jar and configured for Gradle `8.14.2`.
- `./gradlew build` passed on 2026-05-08.
- Phase 1 `./gradlew build` passed on 2026-05-08.
- Phase 1 `./gradlew runClient` launched on 2026-05-08.
- Phase 1 `/radworks validate` smoke check returned `loaded=1 enabled=1 disabled=0 errors=0 warnings=0`.

## Phase 1 implementation notes
- Reload implementation uses a direct `SimplePreparableReloadListener` instead of `SimpleJsonResourceReloadListener` so malformed external datapack JSON can be captured and reported by `/radworks validate`.
- Unknown registry IDs are warnings in `lenient/dev`, not fatal errors.
- Duplicate enabled entries are errors but do not crash the game.
- Disabled rules are not active, are counted in `disabledRules`, and produce info messages.
- Checksum is stable SHA-256 over normalized valid rules.

## UNKNOWN / risks
- `runServer` may stop for Minecraft EULA acceptance; this is expected and not a Phase 0 failure.
- The target modpack version may differ from the accepted Phase 0 versions.
- Phase 1 has no config override layer; rules are datapack resources only.
- Real Create/Create Nuclear/TFMG rule defaults are intentionally not bundled yet.
- External broken-rule datapack tests must be removed after verification.
