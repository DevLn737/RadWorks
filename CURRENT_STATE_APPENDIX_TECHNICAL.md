# Current State Appendix — Technical Details

## 1. Changed/created files by phase

This appendix is a compact technical map, not a full changelog. For exact history, see `CHANGELOG.md` and `MIGRATION_STATUS.md`.

### Phase 0 — Repository foundation

Created/established:

- `build.gradle`
- `settings.gradle`
- `gradle.properties`
- `src/main/java/dev/radworks/RadWorks.java`
- `src/main/java/dev/radworks/command/RadWorksCommands.java`
- `src/main/java/dev/radworks/diagnostics/DiagnosticsDump.java`
- `src/main/java/dev/radworks/diagnostics/DiagnosticsService.java`
- `src/main/resources/META-INF/neoforge.mods.toml`
- `src/main/resources/pack.mcmeta`
- `README.md`
- `MIGRATION_STATUS.md`
- `TESTING.md`
- `DIAGNOSTICS.md`
- `CHANGELOG.md`
- `AGENTS.md`

Purpose:

- Minimal NeoForge mod scaffold.
- `/radworks version`.
- `/radworks dump`.
- Documentation baseline.

### Phase 1 — Data-driven radiation rules

Created/modified:

- `src/main/java/dev/radworks/command/ValidateCommand.java`
- `src/main/java/dev/radworks/radiation/RadiationRule.java`
- `src/main/java/dev/radworks/radiation/RadiationRuleType.java`
- `src/main/java/dev/radworks/radiation/RadiationRules.java`
- `src/main/java/dev/radworks/radiation/RadiationRulesLoader.java`
- `src/main/java/dev/radworks/radiation/RadiationRuleValidationResult.java`
- `src/main/resources/data/radworks/radiation_rules/dev_rotten_flesh.json`
- Docs updated.

Purpose:

- Load JSON radiation rules from datapack resources.
- Validate item/block/fluid ids and rule structure.
- Add stable checksum and lenient/dev validation mode.

### Phase 2 — Player inventory radiation diagnostics

Created/modified:

- `src/main/java/dev/radworks/command/ExposureCommand.java`
- `src/main/java/dev/radworks/radiation/ExposureEngine.java`
- `src/main/java/dev/radworks/radiation/ExposureBreakdown.java`
- `src/main/java/dev/radworks/radiation/PlayerInventorySourceProvider.java`
- `src/main/java/dev/radworks/radiation/RadiationSource.java`
- `src/main/java/dev/radworks/radiation/RadiationSourceType.java`
- Diagnostics snapshot support.
- Docs updated.

Purpose:

- Scan only server-side player main inventory and offhand.
- Calculate diagnostic exposure from item rules.
- Store bounded `lastExposureSnapshot` after `/radworks exposure`.

### Phase 3 — Exposure diagnostics framework

Created/modified:

- `src/main/java/dev/radworks/command/DebugCommand.java`
- `src/main/java/dev/radworks/command/SourcesCommand.java`
- `src/main/java/dev/radworks/diagnostics/DiagnosticsState.java`
- `src/main/java/dev/radworks/diagnostics/PerformanceStats.java`
- `src/main/java/dev/radworks/diagnostics/WarningBuffer.java`
- Existing command/dump classes.
- Docs updated.

Purpose:

- `/radworks debug on|off|status`.
- `/radworks sources`.
- Recent warnings.
- Command diagnostics performance stats.

### Phase 4A — Static block sources

Created/modified:

- `src/main/java/dev/radworks/radiation/BlockSourceProvider.java`
- `src/main/resources/data/radworks/radiation_rules/dev_gold_block.json`
- `RadiationSourceType.java`
- `ExposureEngine.java`
- Command/dump output.
- Docs updated.

Purpose:

- Command-only scanning of nearby ordinary block states.
- Dev-only `minecraft:gold_block` block rule.

### Phase 4B — Vanilla container block entity inventory sources

Created/modified:

- `src/main/java/dev/radworks/radiation/BlockEntityInventorySourceProvider.java`
- `ExposureEngine.java`
- `RadiationSource.java`
- Diagnostics/dump classes.
- Docs updated.

Purpose:

- Command-only scanning of nearby block entities implementing `net.minecraft.world.Container`.
- Read slots using `getContainerSize()` and `getItem(slot)`.

### Phase 4C — NeoForge item handler block capability sources

Created/modified:

- `src/main/java/dev/radworks/radiation/BlockItemHandlerSourceProvider.java`
- `RadiationSourceType.java`
- `RadiationSource.java`
- `ExposureEngine.java`
- `PerformanceStats.java`
- Sources/exposure/dump diagnostics.
- Docs updated.

Purpose:

- Command-only scanning through `Capabilities.ItemHandler.BLOCK`.
- Skip vanilla `Container` block entities to avoid double counting.

### Phase 4C.1 — Source diagnostics cleanup

Created/modified:

- `src/main/java/dev/radworks/diagnostics/SourceScanSummary.java`
- `DiagnosticsDump.java`
- `DiagnosticsService.java`
- Providers updated to report scan counters.
- Docs updated.

Purpose:

- Add `sourceScanSummary`.
- Make source discovery diagnostics easier to send back to Codex.

### Phase 4D — Block fluid handler capability sources

Created/modified:

- `src/main/java/dev/radworks/radiation/BlockFluidHandlerSourceProvider.java`
- `src/main/resources/data/radworks/radiation_rules/dev_water.json`
- `RadiationSourceType.java`
- `RadiationSource.java`
- `RadiationRules.java`
- `ExposureEngine.java`
- `PerformanceStats.java`
- `SourceScanSummary.java`
- Commands/dump/docs.

Purpose:

- Command-only scanning through `Capabilities.FluidHandler.BLOCK`.
- Dev-only `minecraft:water` fluid rule.

### Phase 5A — Shielding diagnostics

Created/modified:

- `src/main/java/dev/radworks/radiation/shielding/ShieldingEngine.java`
- `src/main/java/dev/radworks/radiation/shielding/ShieldingResult.java`
- `src/main/resources/data/radworks/tags/block/shielding_blocks.json`
- `RadiationSource.java`
- `ExposureEngine.java`
- `PerformanceStats.java`
- `SourceScanSummary.java`
- Sources/exposure/dump output.
- Docs updated.

Purpose:

- Diagnostic-only shielding attenuation for positioned sources.
- Dev-only `minecraft:iron_block` shielding tag.

## 2. Current Java package/class map

```text
dev.radworks
  RadWorks
    Main mod entry point and NeoForge event registration.

dev.radworks.command
  DebugCommand
    /radworks debug status/on/off.
  ExposureCommand
    /radworks exposure [player].
  RadWorksCommands
    Registers the /radworks command tree.
  SourcesCommand
    /radworks sources [player].
  ValidateCommand
    /radworks validate.

dev.radworks.diagnostics
  DiagnosticsDump
    Writes bounded diagnostic JSON files.
  DiagnosticsService
    Shared diagnostics state facade.
  DiagnosticsState
    In-memory debug state, warnings, performance, latest snapshots.
  PerformanceStats
    Command diagnostic timing counters.
  SourceScanSummary
    Bounded counters for latest source scan.
  WarningBuffer
    Bounded recent warning list.

dev.radworks.radiation
  BlockEntityInventorySourceProvider
    Vanilla Container block entity item sources.
  BlockFluidHandlerSourceProvider
    NeoForge block fluid handler sources.
  BlockItemHandlerSourceProvider
    NeoForge block item handler sources.
  BlockSourceProvider
    Static block-state sources.
  ExposureBreakdown
    Total exposure and bounded source rows.
  ExposureEngine
    Coordinates providers, shielding, totals, snapshots.
  PlayerInventorySourceProvider
    Player inventory/offhand item sources.
  RadiationRule
    One loaded item/block/fluid rule.
  RadiationRules
    Loaded rule set, counts, lookup helpers, checksum.
  RadiationRulesLoader
    Datapack JSON loader and validation.
  RadiationRuleType
    Rule type enum: item, block, fluid.
  RadiationRuleValidationResult
    Validation messages and counters.
  RadiationSource
    One discovered source row with contribution/shielding diagnostics.
  RadiationSourceType
    Source type enum.

dev.radworks.radiation.shielding
  ShieldingEngine
    Diagnostic line sampling and attenuation.
  ShieldingResult
    Shielding result fields for one source.
```

No `config`, `registry`, `integration/create`, or `integration/aeronautics` package is currently implemented.

## 3. RadiationSource model

Current `RadiationSource` is a compact row model for diagnostics and exposure summing.

| Field | Purpose | Source types using it |
|---|---|---|
| `type` | Source type enum/string | all |
| `itemId` | Item id for item stack source | player inventory, container, item handler |
| `fluidId` | Fluid id for fluid source | block fluid handler |
| `blockId` | Block id for block or host block | block, container, item handler, fluid handler |
| `slot` | Inventory slot label | player inventory, container, item handler |
| `tank` | Fluid tank label | fluid handler |
| `count` | Item stack count | item-based sources |
| `amountMb` | Fluid amount in millibuckets | fluid handler |
| `position` | World position string | positioned sources |
| `capabilityContext` | `unsided` or side name for capability lookup | item handler, fluid handler |
| `ruleStrength` | Strength from matched rule | all |
| `ruleRadius` | Radius from matched rule | all |
| `distance` | Player-to-source distance | positioned sources |
| `matchReason` | Human-readable reason source was matched | all |

Shielding fields:

| Field | Purpose |
|---|---|
| `respectsShielding` | Copied from the matched radiation rule |
| `rawContribution` | Contribution before shielding |
| `shielding` | Status such as `not_applicable`, `not_applied`, `applied` |
| `shieldingBlocksHit` | Count of unique shielding block positions found |
| `shieldingMultiplier` | Final multiplier after attenuation and cap |
| `shieldingReduction` | `rawContribution - finalContribution` |
| `finalContribution` | Contribution after shielding |
| `contribution()` | Compatibility accessor; returns `finalContribution` |

## 4. RadiationRules model

Rule sources:

- JSON files in `src/main/resources/data/radworks/radiation_rules/*.json`.
- Data-pack loading path is `data/radworks/radiation_rules/`.
- Current rule types: `item`, `block`, `fluid`.

Rule fields:

- `type`
- `id`
- `strength`
- `radius`
- `respectsShielding`
- `enabled`
- optional `comment`/notes-style field depending on JSON.

Loaded rule state:

- active rules list;
- disabled rule count;
- item/block/fluid counts;
- validation result;
- stable SHA-256 checksum.

Validation behavior:

- default mode is `lenient/dev`;
- malformed JSON is reported, not allowed to crash commands;
- radius `<= 0` is invalid;
- strength `<= 0` is invalid;
- duplicate enabled entries are errors;
- unknown item/block/fluid registry IDs are warnings in lenient/dev;
- disabled rules are counted and reported as info, not active.

Current dev-only rules:

- item: `minecraft:rotten_flesh`
- block: `minecraft:gold_block`
- fluid: `minecraft:water`

Shielding uses a block tag, not a radiation rule:

- tag path: `src/main/resources/data/radworks/tags/block/shielding_blocks.json`
- tag id: `#radworks:shielding_blocks`
- current value: `minecraft:iron_block`

## 5. ExposureEngine flow

Actual current flow, summarized:

1. Commands call the exposure/source pipeline for a target `ServerPlayer`.
2. The engine reads the currently loaded `RadiationRules`.
3. Providers collect diagnostic source rows:
   - player inventory;
   - static blocks;
   - vanilla `Container` block entities;
   - NeoForge block item handlers;
   - NeoForge block fluid handlers.
4. Providers use bounded command-only scans; no tick loop is registered.
5. Providers filter by matching active rules and source distance/radius where applicable.
6. Providers calculate raw contribution:
   - item stack count times item rule strength;
   - block rule strength;
   - fluid strength times `amountMb / 1000.0`.
7. `ShieldingEngine` is applied to positioned sources that respect shielding.
8. Inventory sources remain `shielding=not_applicable`.
9. Total exposure is the sum of source final contributions.
10. Bounded chat output is produced.
11. Bounded `lastExposureSnapshot` and `sourceScanSummary` are stored for dump output.

No gameplay side effects occur in this flow.

## 6. Provider details

### PlayerInventorySourceProvider

| Item | Detail |
|---|---|
| Source type | `player_inventory` |
| Scans | Server-side player main inventory and offhand |
| Bounds | Only target player's own inventory/offhand |
| Formula | `stack.count * itemRule.strength` |
| Diagnostics | slot, item id, count, strength, radius, raw/final contribution |
| Limitations | No armor, Curios/Trinkets, nested containers, NBT/components, item capabilities |

### BlockSourceProvider

| Item | Detail |
|---|---|
| Source type | `block` |
| Scans | Nearby block states through level/block state lookup |
| Bounds | `min(max active block rule radius, 8)` |
| Formula | `blockRule.strength` |
| Diagnostics | block id, position, distance, radius, contribution, match reason |
| Limitations | No block entity data, no fluids, no cache, command-only cube scan |

### BlockEntityInventorySourceProvider

| Item | Detail |
|---|---|
| Source type | `block_entity_inventory` |
| Scans | Nearby block entities implementing `net.minecraft.world.Container` |
| Bounds | `min(max active item rule radius, 8)` |
| Formula | `stack.count * itemRule.strength` |
| Diagnostics | block id, container position, slot, item id, count, distance, contribution |
| Limitations | No capabilities, no nested container contents, no container exclusions yet |

### BlockItemHandlerSourceProvider

| Item | Detail |
|---|---|
| Source type | `block_item_handler` |
| Scans | Nearby `Capabilities.ItemHandler.BLOCK` |
| Bounds | `min(max active item rule radius, 8)` |
| Lookup | unsided first, then UP/DOWN/NORTH/SOUTH/WEST/EAST; first non-null handler only |
| Double counting | Skips block entities that are already vanilla `Container` |
| Formula | `stack.count * itemRule.strength` |
| Diagnostics | block id, position, capability context, slot, item id, count, contribution |
| Limitations | No entity capabilities, item stack capabilities, nested containers, or modded positive test in clean dev |

### BlockFluidHandlerSourceProvider

| Item | Detail |
|---|---|
| Source type | `block_fluid_handler` |
| Scans | Nearby `Capabilities.FluidHandler.BLOCK` |
| Bounds | `min(max active fluid rule radius, 8)` |
| Lookup | unsided first, then UP/DOWN/NORTH/SOUTH/WEST/EAST; first non-null handler only |
| Formula | `fluidRule.strength * amountMb / 1000.0` |
| Diagnostics | block id, position, capability context, tank, fluid id, amountMb, contribution |
| Limitations | No item/entity fluid capabilities, no buckets, no inventory fluid containers, no custom tank block |

## 7. Shielding technical details

Classes:

- `ShieldingEngine`
- `ShieldingResult`

Data:

- tag id: `#radworks:shielding_blocks`
- resource path: `src/main/resources/data/radworks/tags/block/shielding_blocks.json`
- current dev-only block: `minecraft:iron_block`

Algorithm:

- Applies only to sources with a world position.
- Applies only when the matched source rule has `respectsShielding=true`.
- Samples a simple line from source center/container center to player body center.
- `sampleStep=0.25`.
- `maxSamples=64`.
- Source endpoint and player endpoint samples are skipped.
- Unique shielding block positions are counted once.
- Each shielding block hit multiplies contribution by `0.5`.
- Minimum multiplier cap is `0.1`.
- `finalContribution = rawContribution * shieldingMultiplier`.
- `shieldingReduction = rawContribution - finalContribution`.
- `contribution` mirrors `finalContribution`.

Counters updated:

- `shieldingSourcesChecked`
- `shieldingSourcesApplicable`
- `shieldingSamplesChecked`
- `shieldingBlocksHit`
- `shieldingSourcesReduced`

Known technical limitations:

- No complex voxel ray tracing.
- No partial-block or transparency model.
- No armor protection.
- No cache/invalidation.
- No final shielding balance.

## 8. PerformanceStats and SourceScanSummary

Performance stats are command diagnostics only. They are not TPS profiling.

Current performance operations:

- `validate`
- `exposure`
- `sources`
- `dump`
- `blockScan`
- `blockEntityInventoryScan`
- `itemHandlerScan`
- `fluidHandlerScan`
- `shielding`

Each operation tracks:

- `lastMillis`
- `count`
- `averageMillis`
- `maxMillis`

Current `SourceScanSummary` counters:

| Area | Counters |
|---|---|
| Output bounds | `sourcesShown`, `sourcesOmitted` |
| Player inventory | `inventoryStacksChecked`, `inventoryMatches` |
| Static blocks | `blockPositionsChecked`, `blockMatches` |
| Block entities | `blockEntitiesChecked`, `containerBlockEntitiesFound` |
| Container slots | `containerSlotsChecked`, `containerMatches` |
| Item handlers | `itemHandlerPositionsChecked`, `itemHandlersFound`, `itemHandlerSlotsChecked`, `itemHandlerMatches`, `skippedContainerBlockEntitiesForItemHandler` |
| Fluid handlers | `fluidHandlerPositionsChecked`, `fluidHandlersFound`, `fluidTanksChecked`, `fluidMatches` |
| Shielding | `shieldingSourcesChecked`, `shieldingSourcesApplicable`, `shieldingSamplesChecked`, `shieldingBlocksHit`, `shieldingSourcesReduced` |

Diagnostics note:

- Item handler scan records a bounded note when vanilla `Container` block entities are skipped to avoid double counting.

## 9. Current data/resources

### `src/main/resources/data/radworks/radiation_rules/`

| File | Purpose | Status |
|---|---|---|
| `dev_rotten_flesh.json` | Item rule smoke test | DEV_ONLY |
| `dev_gold_block.json` | Static block source smoke test | DEV_ONLY |
| `dev_water.json` | Fluid rule and fluid handler smoke baseline | DEV_ONLY |

### `src/main/resources/data/radworks/tags/`

| File | Purpose | Status |
|---|---|---|
| `block/shielding_blocks.json` | Shielding block tag, currently `minecraft:iron_block` | DEV_ONLY |

### `src/main/resources/META-INF/`

| File | Purpose | Status |
|---|---|---|
| `neoforge.mods.toml` | NeoForge mod metadata and dependencies | Production metadata |

### `src/main/resources/assets/`

No assets directory is currently part of the relevant source set.

### Other resources

| File | Purpose |
|---|---|
| `pack.mcmeta` | Data/resource pack metadata; current pack format is for Minecraft 1.21.1 |

## 10. Current manual test commands

Gradle:

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
```

Core Minecraft commands:

```text
/radworks version
/radworks validate
/radworks dump
/radworks debug status
/radworks debug on
/radworks debug off
/radworks sources
/radworks sources <player>
/radworks exposure
/radworks exposure <player>
```

Useful manual fixtures:

```text
/give @s minecraft:rotten_flesh 10
/give @s minecraft:gold_block 1
/give @s minecraft:chest 1
/give @s minecraft:barrel 1
/give @s minecraft:iron_block 1
```

Expected current smoke scenarios:

- No rotten flesh and no nearby source blocks: exposure should be `0.0`.
- 10 rotten flesh in player inventory: exposure should be `10.0`.
- 10 rotten flesh plus nearby gold block before shielding: exposure should be `15.0`.
- 10 rotten flesh plus nearby gold block plus chest containing 10 rotten flesh: exposure should be `25.0`.
- Iron block between player and gold block should reduce gold block raw `5.0` to final `2.5` in Phase 5A.
- `/radworks dump` should contain `performance`, `sourceScanSummary`, and bounded source rows.

## 11. Last known build/run results

Known from project status and local evidence:

- `build/libs/radworks-0.1.0.jar` exists.
- `run/radworks_dumps/` contains diagnostic dump files.
- Earlier `./gradlew build` runs passed for the implemented phases.
- Earlier `./gradlew runClient` smoke runs launched the client/integrated server but should not be treated as full manual gameplay verification.
- `run/` world/save/log files may be modified as a side effect of `runClient`.

This appendix was created before the final `./gradlew build` rerun requested by the handoff task. Check the assistant's final response for the build result after these documentation files were added.

## 12. Known code smells / TODOs

TODO / UNKNOWN:

- Phase 5A shielding needs owner manual verification.
- Real radiation rules from the old KubeJS project are not migrated.
- Real shielding blocks are not migrated.
- Armor strategy is undecided.
- Effect strategy is undecided.
- Container exception list is not implemented.
- Entity/dropped item source scope is not implemented.
- Cache/invalidation strategy is not designed.
- Positive modded `IItemHandler` source test is UNKNOWN without an extra mod.
- Positive block fluid handler source test is UNKNOWN without a tank/fluid handler block.
- Create/Aeronautics integrations are intentionally not started.

Dev-only:

- `minecraft:rotten_flesh` item rule.
- `minecraft:gold_block` block rule.
- `minecraft:water` fluid rule.
- `minecraft:iron_block` shielding tag entry.

Fragile by design for now:

- Command-only bounded scans are acceptable for diagnostics but not final gameplay ticking.
- Shielding line sampling is intentionally simple.
- Side-specific capability scans take the first handler only to avoid duplicate rows.
- Current diagnostics prefer clarity and safety over final performance architecture.

Migration decisions required:

- Final effect id and compatibility strategy.
- Final armor/protection model.
- Final shielding model and balance.
- Optional mod rule loading when registries are absent.
- Create contraption and Aeronautics integration approach.
