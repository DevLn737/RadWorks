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
Status: implemented and manually verified.

Verified by user:
- `./gradlew build` passed.
- `./gradlew runClient` started.
- `/radworks validate` works.
- `/radworks dump` shows `rules.loaded=true`.
- Active test rule: `minecraft:rotten_flesh`.
- `validationMode=lenient/dev`.
- `errors=0 warnings=0`.

## Phase 2 - Player inventory radiation diagnostics
Status: implemented and manually verified.

Verified by user:
- `./gradlew build` passed.
- `/radworks validate` works.
- `/radworks exposure` works.
- 10 `minecraft:rotten_flesh` in inventory gives `totalExposure=10.0`.
- `/radworks dump` contains `lastExposureSnapshot` with a source row.
- Gameplay effects are absent.

## Phase 3 - Exposure diagnostics framework
Status: implemented and manually verified.

Verified by user:
- `/radworks debug` works.
- `/radworks sources` works.
- `/radworks exposure` works.
- `/radworks dump` contains debug, performance, recentWarnings and lastExposureSnapshot.
- 10 `minecraft:rotten_flesh` gives `totalExposure=10.0`.
- Gameplay effects are absent.

## Phase 4A - Static block radiation sources
Status: implemented locally, build passed, pending manual Minecraft verification.

## Phase 4B - Vanilla container block entity item sources
Status: implemented and manually verified.

Verified by user:
- Vanilla `Container` block entity item sources work.
- Chest/barrel with 10 `minecraft:rotten_flesh` gives `block_entity_inventory contribution=10.0`.
- 10 `minecraft:rotten_flesh` in player inventory plus nearby `minecraft:gold_block` plus chest with 10 `minecraft:rotten_flesh` gives `totalExposure=25.0`.
- Phase 4B remains diagnostic-only.

## Phase 4C - NeoForge ItemHandler block capability sources
Status: implemented locally, build passed, pending manual Minecraft verification.

## Phase 4C.1 - Source diagnostics cleanup
Status: implemented and manually verified.

Verified by user:
- `sourceScanSummary` was added to `/radworks dump`.
- Exposure formulas did not change.
- No new mechanics were added.
- `./gradlew build` passed.

## Phase 4D - Block FluidHandler capability sources
Status: implemented and manually verified.

Verified by user:
- `fluidRules=1`.
- `performance.fluidHandlerScan` is present.
- `sourceScanSummary` contains fluid counters.
- Baseline scenario remains `totalExposure=25.0`.
- Gameplay effects are absent.

## Phase 5A - Shielding diagnostics
Status: implemented and manually verified.

Verified by user in Phase 5A.1:
- Shielded dump: `radworks-dump-20260510-070002-Dev.json`.
- No-shield dump: `radworks-dump-20260510-070129-Dev.json`.
- No-shield scenario: 10 `minecraft:rotten_flesh` plus nearby `minecraft:gold_block` gives `totalExposure=15.0`.
- No-shield gold block row: `rawContribution=5.0`, `finalContribution=5.0`, `shielding=clear`, `shieldingBlocksHit=0`, `shieldingMultiplier=1.0`.
- Shielded scenario with `minecraft:iron_block`: total exposure is reduced to `12.5`.
- Shielded gold block row: `rawContribution=5.0`, `finalContribution=2.5`, `shielding=reduced`, `shieldingBlocksHit=1`, `shieldingMultiplier=0.5`, `shieldingReduction=2.5`.
- Player inventory `minecraft:rotten_flesh` remains `finalContribution=10.0`.
- `sourceScanSummary.shieldingSourcesApplicable=1` in both scenarios.
- `sourceScanSummary.shieldingSourcesReduced=0` without shield and `1` with shield.

## Phase 5A.1 - Shielding manual verification / dump review
Status: complete.

Conclusion:
- Phase 5A shielding diagnostics are manually verified.
- No gameplay code changes were required.
- No docs-only limitations update, diagnostics-only fix, or bugfix was required from the reported results.

## Phase 5B - Real shielding rules + external tester package
Status: implemented locally, pending external modpack verification.

Implemented:
- Kept Phase 5A shielding algorithm unchanged.
- Kept `minecraft:iron_block` as a temporary/dev-only shielding test entry.
- Added optional real shielding candidates to `#radworks:shielding_blocks` using `required:false`:
  - `tfmg:raw_lead_block`;
  - `tfmg:lead_block`;
  - `tfmg:lead_ore`;
  - `createnuclear:reinforced_glass`.
- Added diagnostics-only shielding candidate reporting to `/radworks validate`.
- Added compact shielding candidate diagnostics to `/radworks dump`.
- Added `TESTER_HANDOFF.md` for an external Minecraft/modpack-aware tester.

Verification status:
- Local clean dev environment is expected to report absent TFMG/Create Nuclear candidates as INFO, not ERROR.
- External tester verification is required for real TFMG/Create Nuclear shielding blocks.
- Phase 5B remains diagnostic-only.

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
- `/radworks exposure`.
- `/radworks sources`.
- `/radworks debug on/off/status`.
- Diagnostic-only static block source discovery for ordinary block states.
- Diagnostic-only item source discovery inside nearby vanilla `Container` block entities.
- Diagnostic-only item source discovery through NeoForge block `IItemHandler` capability.
- Diagnostic-only fluid source discovery through NeoForge block `IFluidHandler` capability.
- Diagnostic-only shielding calculation for positioned sources.
- Diagnostics-only shielding candidate reporting for optional real shielding blocks.
- Bounded `sourceScanSummary` diagnostics for the most recent `/radworks sources` or `/radworks exposure`.
- Rules summary in `/radworks version`.
- Rules validation summary in `/radworks dump`.
- Last diagnostic-only exposure snapshot in `/radworks dump` after `/radworks exposure` runs.
- Server-wide in-memory debug state in `/radworks dump`.
- Bounded recent warning buffer in `/radworks dump`.
- Command diagnostics performance stats in `/radworks dump`.
- Documentation: `AGENTS.md`, `README.md`, `MIGRATION_STATUS.md`, `TESTING.md`, `DIAGNOSTICS.md`, `CHANGELOG.md`.

## Phase 1 temporary/dev-only rule
- `src/main/resources/data/radworks/radiation_rules/dev_rotten_flesh.json`
- Rule ID: `minecraft:rotten_flesh`
- Purpose: smoke-test the loader and validation without requiring Create, Create Nuclear, TFMG or a full modpack.
- Status: temporary/dev-only; not final gameplay balance.

## Phase 4A temporary/dev-only rule
- `src/main/resources/data/radworks/radiation_rules/dev_gold_block.json`
- Rule ID: `minecraft:gold_block`
- Strength: `5.0`
- Radius: `6.0`
- Purpose: smoke-test static block source diagnostics without requiring Create, Create Nuclear, TFMG or a full modpack.
- Status: temporary/dev-only; not final gameplay balance.

## Phase 4D temporary/dev-only rule
- `src/main/resources/data/radworks/radiation_rules/dev_water.json`
- Rule ID: `minecraft:water`
- Strength: `1.0`
- Radius: `2.0`
- Purpose: smoke-test fluid rule validation without requiring Create, Create Nuclear, TFMG or a full modpack.
- Status: temporary/dev-only; not final gameplay balance. It only becomes a source if a block fluid handler containing water exists.

## Phase 5A temporary/dev-only shielding tag
- `src/main/resources/data/radworks/tags/block/shielding_blocks.json`
- Tag ID: `#radworks:shielding_blocks`
- Temporary block: `minecraft:iron_block`
- Purpose: smoke-test shielding diagnostics without requiring custom RadWorks shielding blocks or a full modpack.
- Status: temporary/dev-only; not final gameplay balance.

## Phase 5B optional real shielding candidates
- `tfmg:raw_lead_block`
- `tfmg:lead_block`
- `tfmg:lead_ore`
- `createnuclear:reinforced_glass`

These entries are added to `#radworks:shielding_blocks` with `required:false`, so missing optional mods do not crash the local dev environment. They are final candidates from the old KubeJS content registry, pending external modpack verification.

## Explicitly not implemented in Phase 0
- Radiation mechanics.
- Gameplay use of radioactive items, blocks, fluids or effects.
- Shielding.
- Capabilities or attachments.
- Radiation configs under `config/radworks/`.
- Gameplay exposure application.
- World, block, entity or container scanning.
- Armor protection logic.
- Curios/Trinkets, nested container, NBT or component scanning.
- Fluids.
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
- Phase 2 must run `./gradlew build` after implementation.
- Phase 2 `./gradlew build` passed on 2026-05-08.
- Phase 2 `./gradlew runClient` was requested but not run because the approval flow rejected the launch.
- Phase 3 must run `./gradlew build` after implementation.
- Phase 3 `./gradlew build` passed on 2026-05-08.
- Phase 3 `./gradlew runClient` was requested but not run because the approval flow rejected the launch.
- Phase 4A must run `./gradlew build` after implementation.
- Phase 4A `./gradlew build` passed on 2026-05-08.
- Phase 4A `timeout 60s ./gradlew runClient` launched the client, listed `RadWorks 0.1.0`, and started an integrated server; the process then ended with timeout code `124`, so manual command verification is still pending.
- Phase 4B must run `./gradlew build` after implementation.
- Phase 4B `./gradlew build` passed on 2026-05-08.
- Phase 4B `timeout 60s ./gradlew runClient` launched the client, listed `RadWorks 0.1.0`, and started an integrated server; the process then ended with timeout code `124`, so full manual command verification is still pending.
- Phase 4B was manually verified by user after implementation.
- Phase 4C `./gradlew build` passed on 2026-05-08.
- Phase 4C `timeout 60s ./gradlew runClient` launched the client, listed `RadWorks 0.1.0`, and started an integrated server; smoke log showed the existing Phase 4B scenario still reports `totalExposure=25.0` with player inventory + gold block + chest, not `35.0`. The process then ended with timeout code `124`, so full manual command verification is still pending.
- Phase 4C.1 `./gradlew build` passed on 2026-05-08.
- Phase 4D `./gradlew build` passed on 2026-05-08.
- Phase 5A `./gradlew build` passed on 2026-05-08.
- Phase 5A.1 manual verification was completed by user on 2026-05-10.
- Phase 5B `./gradlew build` passed on 2026-05-10.

## Phase 2 implementation notes
- `/radworks exposure` scans only server-side player main inventory and offhand.
- `/radworks exposure <player>` targets an online `ServerPlayer`.
- Diagnostic formula: `contribution = stack.count * rule.strength`.
- Inventory source distance is `0`.
- Inventory shielding is reported as `not_applicable`.
- Final exposure is the sum of contributions.
- Chat output is bounded to 10 source rows.
- Dump `lastExposureSnapshot` is bounded to 20 source rows.
- Dump `lastExposureSnapshot` is `null` until `/radworks exposure` has run.
- Snapshot includes `stale=true` when its rules checksum differs from current loaded rules.

## Phase 3 implementation notes
- `/radworks sources` uses only Phase 2 player inventory sources.
- `/radworks sources` does not scan blocks, block entities, containers, dropped items, entities, fluids, NBT/components, Create or Aeronautics.
- `/radworks sources` explains why each source matched an active item rule.
- `/radworks debug on/off` require permission level 2; `/radworks debug status` is readable by normal players.
- Debug state is server-wide, in-memory, and resets on restart.
- `WarningBuffer` is bounded to 100 entries.
- `PerformanceStats` measures command diagnostics only: `validate`, `exposure`, `sources`, `dump`.
- Performance stats are not TPS or server performance metrics.

## Phase 4A implementation notes
- Static block source discovery is command-only.
- `/radworks sources` and `/radworks exposure` combine Phase 2 player inventory sources with Phase 4A static block sources.
- Block discovery reads ordinary block states through `getBlockState`.
- Block discovery does not read block entity data, inventories, fluids, NBT/components or capabilities.
- Active block source formula: `distance = player position to block center`; source is active only when `distance <= rule.radius`.
- Phase 4A block contribution is `rule.strength`; no falloff and no shielding result is applied.
- Effective command scan radius is `min(max active block rule radius, 8)`.
- Block scan timing is command diagnostics only and appears as `performance.blockScan`.
- Chat output remains bounded to 10 source rows.
- Dump `lastExposureSnapshot` remains bounded to 20 source rows and can include both inventory and block source rows.

## Phase 4B implementation notes
- Vanilla container item discovery is command-only.
- `/radworks sources` and `/radworks exposure` combine player inventory, static block and vanilla `Container` block entity inventory sources.
- Block entity inventory discovery scans nearby block positions and reads a block entity only when it implements `net.minecraft.world.Container`.
- Container slots are read only through `getContainerSize()` and `getItem(slot)`.
- No NeoForge capabilities, `IItemHandler`, modded capability containers, nested containers, shulker contents, backpacks, fluids, tanks, entities or dropped items are scanned.
- Active container item source formula: `distance = player position to container block center`; source is active only when `distance <= item rule.radius`.
- Phase 4B container contribution is `stack.count * itemRule.strength`; no falloff and no shielding result is applied.
- Effective command scan radius is `min(max active item rule radius, 8)`.
- Block entity inventory scan timing is command diagnostics only and appears as `performance.blockEntityInventoryScan`.
- Chat output remains bounded to 10 source rows.
- Dump `lastExposureSnapshot` remains bounded to 20 source rows and can include inventory, block and block entity inventory source rows.

## Phase 4C implementation notes
- NeoForge block item handler discovery is command-only.
- `/radworks sources` and `/radworks exposure` combine player inventory, static block, vanilla `Container` block entity inventory and block `IItemHandler` capability sources.
- Source type: `block_item_handler`.
- Capability lookup uses `Capabilities.ItemHandler.BLOCK`.
- Lookup order is unsided first, then `UP`, `DOWN`, `NORTH`, `SOUTH`, `WEST`, `EAST`; only the first available handler per block position is scanned.
- Vanilla `Container` block entities are skipped by item handler scan to avoid double counting with Phase 4B.
- Handler slots are read only through `getSlots()` and `getStackInSlot(slot)`.
- Active item handler source formula: `distance = player position to block center`; source is active only when `distance <= item rule.radius`.
- Phase 4C item handler contribution is `stack.count * itemRule.strength`; no falloff and no shielding result is applied.
- Effective command scan radius is `min(max active item rule radius, 8)`.
- Item handler scan timing is command diagnostics only and appears as `performance.itemHandlerScan`.
- Chat output remains bounded to 10 source rows.
- Dump `lastExposureSnapshot` remains bounded to 20 source rows and can include `block_item_handler` source rows.

## Phase 4C.1 implementation notes
- `/radworks dump` includes `sourceScanSummary` for the most recent `/radworks sources` or `/radworks exposure`.
- The summary counts checked inventory slots, scanned block positions, container block entities, container slots, item handler scan positions, item handlers, item handler slots and source matches.
- The summary includes `sourcesShown` and `sourcesOmitted` from the last command output.
- The summary includes a bounded diagnostic note when `itemHandlerScan` skips vanilla `Container` block entities to avoid double counting.
- Phase 4C.1 does not change source discovery mechanics or exposure formulas.

## Phase 4D implementation notes
- NeoForge block fluid handler discovery is command-only.
- `/radworks sources` and `/radworks exposure` include block `IFluidHandler` capability sources.
- Source type: `block_fluid_handler`.
- Capability lookup uses `Capabilities.FluidHandler.BLOCK`.
- Lookup order is unsided first, then `UP`, `DOWN`, `NORTH`, `SOUTH`, `WEST`, `EAST`; only the first available handler per block position is scanned.
- Fluid handler tanks are read only through `getTanks()` and `getFluidInTank(tank)`.
- Active fluid handler source formula: `distance = player position to block center`; source is active only when `distance <= fluid rule.radius`.
- Phase 4D fluid handler contribution is `fluidRule.strength * amountMb / 1000.0`; no falloff and no shielding result is applied.
- Effective command scan radius is `min(max active fluid rule radius, 8)`.
- Fluid handler scan timing is command diagnostics only and appears as `performance.fluidHandlerScan`.
- `sourceScanSummary` includes fluid handler position, handler, tank and match counters.

## Phase 5A implementation notes
- Shielding is diagnostic-only.
- Shielding applies only to sources with a world position and `respectsShielding=true`.
- Player inventory sources report `shielding=not_applicable`; their final contribution remains equal to raw contribution.
- Shielding blocks are read from `#radworks:shielding_blocks`.
- Temporary/dev-only shielding block is `minecraft:iron_block`.
- Shielding samples a simple line from source block/container center to player body center.
- Sampling uses `sampleStep=0.25`, `maxSamples=64`, and skips source/player endpoint samples.
- Unique shielding block positions are counted once.
- Each shielding block hit multiplies contribution by `0.5`; minimum multiplier is capped at `0.1`.
- `rawContribution`, `shieldingReduction`, `finalContribution` and compatibility `contribution` are shown in source rows and dump snapshots.
- `contribution` mirrors `finalContribution`.
- Shielding timing is command diagnostics only and appears as `performance.shielding`.
- `sourceScanSummary` includes shielding source, sample, block-hit and reduced-source counters.
- Phase 5B adds shielding candidate diagnostics only; it does not change shielding math.
- `/radworks validate` reports optional external shielding block status.
- `/radworks dump` includes a compact `shielding` diagnostics section with tag id/path, dev/test entries, optional entries and required:false notes.

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
- Phase 2 formula is intentionally simple and diagnostic-only; old dynamic radius math is not ported yet.
- Offhand is included, but armor, Curios/Trinkets and nested containers are not included.
- Phase 3 debug state is not persisted.
- Phase 3 warning/performance buffers are in-memory and reset on restart.
- Phase 4A scans a bounded cube on command execution only; it is intentionally not cached.
- Phase 4A does not scan block entities, containers, tanks, fluids, dropped items, entities, Create contraptions or Aeronautics ships.
- Phase 4A does not implement shielding, damage, effects or exposure accumulation.
- Phase 4B only supports vanilla-style `Container` block entities; capability-only modded inventories are intentionally unsupported for now.
- Phase 4B scans a bounded cube on command execution only; it is intentionally not cached.
- Phase 4B does not scan nested item contents, fluids, tanks, dropped items, entities, Create contraptions or Aeronautics ships.
- Phase 4B does not implement shielding, damage, effects or exposure accumulation.
- Phase 4C works without extra mod dependencies, but the dev environment may not contain a non-vanilla block that exposes `IItemHandler`.
- Phase 4C does not scan entity capabilities, item stack capabilities, Curios/Trinkets, nested item contents, fluids, tanks, energy, dropped items, entities, Create contraptions or Aeronautics ships.
- Phase 4C does not implement shielding, damage, effects, exposure accumulation, cache or invalidation.
- Phase 4C only scans the first available item handler context per block position, which may miss side-distinct inventories in some modded blocks; this is intentional to avoid accidental double counting in this diagnostic phase.
- Phase 4C.1 counters are command diagnostics only and reset to the most recent `/radworks sources` or `/radworks exposure` result; they are not cumulative server metrics.
- Phase 4D works without extra mod dependencies, but the clean dev environment may not contain a block that exposes `IFluidHandler`.
- Phase 4D does not scan item fluid capabilities, entity fluid capabilities, buckets, fluid containers in player inventory, item NBT/components, Create tanks, Create contraptions or Aeronautics ships.
- Phase 4D does not implement shielding, damage, effects, exposure accumulation, cache or invalidation.
- Phase 5A uses simple line sampling, not the old 3-ray binary model and not complex voxel raytracing.
- Phase 5A does not implement armor protection, material-specific shielding strength, cache/invalidation, damage/effects or ticking accumulation.
- Partial blocks, fluids and transparent-block behavior are not specially modeled in Phase 5A.
- Phase 5B real shielding candidates require external TFMG/Create Nuclear testing because those mods are not installed in the local clean dev environment.
