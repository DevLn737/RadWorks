# Changelog

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
