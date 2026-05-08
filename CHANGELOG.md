# Changelog

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
