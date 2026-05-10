# Changelog

## 0.1.0 - Phase 6T automated test harness / regression tests
- Added JUnit 5 + NeoForge unit-test harness configuration for local automated regression checks.
- Added effect preview regression tests for:
  - below-threshold block behavior;
  - threshold behavior with no/partial armor;
  - full-armor block behavior.
- Added shielding math regression tests for:
  - single-hit multiplier (`0.5`);
  - minimum multiplier cap (`0.1`).
- Added bundled data smoke tests for:
  - `dev_rotten_flesh.json`;
  - `dev_gold_block.json`;
  - `dev_water.json`.
- Added shielding tag and shielding diagnostics contract tests for dev + optional shielding candidate semantics.
- Updated local test policy to automation-first for core logic; repeated manual local Minecraft checks are no longer default.
- Kept Phase 6T non-goals: no gameplay mechanics, no auto-apply changes, no damage/exhaustion, no ticking accumulation, no cache/invalidation, and no optional integration/dependency additions.

## 0.1.0 - Phase 6E controlled manual radiation effect application command
- Added `/radworks effect` subcommands:
  - `apply`, `apply <player>`
  - `clear`, `clear <player>`
  - `status`, `status <player>`
- `apply/clear` require permission level 2; `status` is available to normal players.
- Added graceful console errors for missing target player:
  - `player required; use /radworks effect <subcommand> <player>`.
- `apply` now uses preview-gated control:
  - blocks when `effectPreview.wouldApply=false` with preview reason;
  - applies only `radworks:radiation` with `durationTicks=20`, `amplifier=0` when preview allows.
- `clear` removes only `radworks:radiation`.
- `status` reports registration state, active state, remaining duration/amplifier when active, and preview gate fields.
- Added command diagnostics timings: `performance.effect_apply`, `performance.effect_clear`, `performance.effect_status`.
- Kept Phase 6E non-goals: no auto-apply, no damage/effects gameplay logic, no ticking accumulation, no cache/invalidation, no source-provider changes, no shielding math changes, and no optional integration/dependency changes.

## 0.1.0 - Phase 5B real shielding rules + external tester package
- Added optional real shielding candidates to `#radworks:shielding_blocks`: `tfmg:raw_lead_block`, `tfmg:lead_block`, `tfmg:lead_ore` and `createnuclear:reinforced_glass`.
- Kept `minecraft:iron_block` as the dev/test shielding entry.
- Used `required:false` for optional external shielding entries so missing TFMG/Create Nuclear blocks do not crash clean dev environments.
- Added diagnostics-only shielding candidate reporting to `/radworks validate`.
- Added compact shielding candidate diagnostics to `/radworks dump`.
- Added `TESTER_HANDOFF.md` for external modpack testing.
- Kept Phase 5B diagnostic-only: no shielding math changes, damage/effects, armor protection, ticking accumulation, cache/invalidation, custom blocks/items, Create, Aeronautics or KubeJS.

## 0.1.0 - Phase 5A.1 shielding manual verification
- Marked Phase 5A shielding diagnostics as manually verified.
- Recorded successful no-shield dump review: `totalExposure=15.0`, gold block `finalContribution=5.0`, `shielding=clear`.
- Recorded successful shielded dump review: `totalExposure=12.5`, gold block `rawContribution=5.0`, `finalContribution=2.5`, `shieldingBlocksHit=1`, `shieldingMultiplier=0.5`.
- Confirmed player inventory shielding remains `not_applicable` and inventory contribution remains unchanged.
- Kept Phase 5A.1 docs-only: no gameplay code changes, new features, damage/effects, ticking, cache, armor protection, Create, Aeronautics or KubeJS.

## 0.1.0 - Phase 5A shielding diagnostics
- Added diagnostic-only shielding calculation for positioned source rows.
- Added `#radworks:shielding_blocks` with temporary/dev-only `minecraft:iron_block`.
- Added shielding fields: `respectsShielding`, `rawContribution`, `shielding`, `shieldingBlocksHit`, `shieldingMultiplier`, `shieldingReduction` and `finalContribution`.
- Kept compatibility `contribution` equal to `finalContribution`.
- Added command diagnostics timing as `performance.shielding`.
- Added shielding counters to `sourceScanSummary`.
- Kept Phase 5A diagnostic-only: no armor protection, damage/effects, hunger/exhaustion, ticking accumulation, cache/invalidation, Create, Aeronautics or KubeJS.

## 0.1.0 - Phase 4D block FluidHandler capability sources
- Added `block_fluid_handler` diagnostic source type.
- Added command-only source discovery for nearby block fluids exposed through NeoForge `Capabilities.FluidHandler.BLOCK`.
- Added temporary dev-only `minecraft:water` fluid rule for validation smoke testing.
- Added bucket-scaled diagnostic contribution: `strength * amountMb / 1000.0`.
- Added command diagnostics timing as `performance.fluidHandlerScan`.
- Added fluid handler counters to `sourceScanSummary`.
- Kept Phase 4D diagnostic-only: no item/entity fluid capabilities, buckets, inventory fluid containers, registry tanks, shielding, damage/effects, ticking accumulation, cache, Create, Aeronautics or KubeJS.

## 0.1.0 - Phase 4C.1 source diagnostics cleanup
- Added `sourceScanSummary` to `/radworks dump` for the most recent `/radworks sources` or `/radworks exposure`.
- Added provider counters for inventory, static block, vanilla container and block item handler scans.
- Added bounded diagnostic note when item handler scan skips vanilla `Container` block entities to avoid double counting.
- Documented what to send Codex when source discovery is wrong.
- Kept Phase 4C.1 diagnostic-only with no exposure formula or gameplay changes.

## 0.1.0 - Phase 4C NeoForge ItemHandler block capability sources
- Added `block_item_handler` diagnostic source type.
- Added command-only source discovery for nearby block inventories exposed through NeoForge `Capabilities.ItemHandler.BLOCK`.
- Skips vanilla `Container` block entities during item handler scan to avoid double counting with Phase 4B.
- Extended `/radworks sources`, `/radworks exposure` and dump snapshots to include item handler source rows.
- Added command diagnostics timing as `performance.itemHandlerScan`.
- Kept Phase 4C diagnostic-only: no entity capabilities, item stack capabilities, nested containers, fluids, energy, shielding, damage/effects, ticking accumulation, cache, Create, Aeronautics or KubeJS.

## 0.1.0 - Phase 4B vanilla container block entity item sources
- Added `block_entity_inventory` diagnostic source type.
- Added command-only source discovery for nearby block entities implementing vanilla `Container`.
- Extended `/radworks sources`, `/radworks exposure` and dump snapshots to include container item source rows.
- Added command diagnostics timing as `performance.blockEntityInventoryScan`.
- Kept Phase 4B diagnostic-only: no NeoForge capabilities, `IItemHandler`, nested containers, fluids, entities, shielding, damage/effects, ticking accumulation, cache, Create, Aeronautics or KubeJS.

## 0.1.0 - Phase 4A static block radiation sources
- Added temporary dev-only `minecraft:gold_block` block rule.
- Added command-only static block source discovery from ordinary block states.
- Extended `/radworks sources` and `/radworks exposure` to combine inventory and static block sources.
- Added bounded block scan diagnostics timing as `performance.blockScan`.
- Kept Phase 4A diagnostic-only: no block entity inventories, containers, tanks, fluids, capabilities, cache, shielding, damage, effects, ticking accumulation, Create, Aeronautics or KubeJS.

## 0.1.0 - Phase 3 exposure diagnostics framework
- Added `/radworks debug on`, `/radworks debug off`, and `/radworks debug status`.
- Added `/radworks sources` and `/radworks sources <player>` for player inventory item sources only.
- Added bounded recent warning buffer to dumps.
- Added command diagnostics performance stats for validate, exposure, sources and dump.
- Added debug state to dumps.
- Kept Phase 3 diagnostic-only and inventory-only.

## 0.1.0 - Phase 2 player inventory radiation diagnostics
- Added `/radworks exposure` and `/radworks exposure <player>`.
- Added diagnostic-only inventory exposure from active item rules.
- Scans only server-side main inventory and offhand.
- Added bounded `lastExposureSnapshot` to `/radworks dump` after exposure runs.
- Kept Phase 2 diagnostic-only: no damage, effects, hunger/exhaustion, particles, sounds, ticking accumulation, shielding, fluids or world/container/entity scans.

## 0.1.0 - Phase 1 data-driven radiation rules
- Added datapack radiation rule loading from `data/radworks/radiation_rules/*.json`.
- Added temporary dev-only smoke rule for `minecraft:rotten_flesh`.
- Added `/radworks validate`.
- Added rules validation summary to `/radworks version` and `/radworks dump`.
- Kept Phase 1 diagnostic-only: no exposure, scanning, shielding, effects, damage or optional integrations.

## 0.1.0 - Phase 0 repository foundation
- Created minimal NeoForge mod scaffold for RadWorks.
- Added `/radworks version`.
- Added `/radworks dump`.
- Added migration, testing and diagnostics documentation.
- Confirmed no radiation gameplay is implemented in Phase 0.
