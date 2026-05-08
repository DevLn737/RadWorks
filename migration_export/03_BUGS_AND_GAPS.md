# Bugs and Gaps

## BUG/FRAGILE: Sophisticated Storage packed items may not emit reliably

### Type
BUG / FRAGILE / COMPATIBILITY

### Source
- `radiation_items.js`: `extractItemsFromStack`, `addSophisticatedUuidContentsToMap`, `getItemsFromContainer`
- `contraption.js`: `getItemsFromSophisticatedUUID`
- User-provided prompt referenced packed ids: `sophisticatedstorage:chest`, `sophisticatedstorage:limited_barrel`, `sophisticatedstorage:barrel`.

### Current behavior
Code attempts to read `block_entity_data`, `storageWrapper.contents.inventory.Items`, and UUID-backed storage through optional Sophisticated classes. This is unverified in live Minecraft after the latest changes.

### Expected behavior
Packed Sophisticated Storage items should emit radiation in inventories/containers if they contain radioactive items or contraptions.

### Why it matters
Sophisticated storage is a major storage mod; missing it undermines "any container" radiation.

### Suggested migration handling
Do not port KubeJS heuristics blindly. Implement proper optional integration using Sophisticated public API. Add diagnostics first.

### Required test
Place `createnuclear:raw_uranium` inside each packed Sophisticated Storage item; carry it in inventory; run `/radworks sources` and `/radworks exposure <player>`.

## BUG/FRAGILE: Create processing blocks with contraption items are fragile

### Type
BUG / FRAGILE / COMPATIBILITY

### Source
- `radiation_world_scan.js`: `processedDirectContraption`, `hasContraption`, fluid-skip behavior.
- `radiation_items.js`: direct `ItemStack` preservation.
- `contraption.js`: `getContraptionRadiationFromCreateBlock`.

### Current behavior
Code tries to preserve direct `ItemStack`s from handlers and prevent fluid inside a contraption from being counted as fluid in the host block. User previously reported item drain, belts and basins issues.

### Expected behavior
Create belt/basin/depot/item drain should emit radiation if they contain radioactive item/contraption, but should respect container exceptions inside contraption.

### Why it matters
Create logistics are central to the project.

### Suggested migration handling
Use Create APIs/handlers where possible. Add explicit test fixtures for belt, basin, depot, item drain.

### Required test
Put radioactive items and `create:minecart_contraption` into belt/basin/depot/item drain and inspect `/radworks sources`.

## BUG/FRAGILE: Broad NBT fluid scan can misattribute nested contraption fluid

### Type
BUG / FRAGILE

### Source
- `radiation_fluids.js`: `getFluidFromContainer`
- `radiation_world_scan.js`: `if (!hasContraption) getFluidFromContainer(...)`

### Current behavior
Generic recursive NBT scan finds any `{id, amount}` fluid. A guard skips host fluid scan when contraption was found.

### Expected behavior
Fluid inside contraption should be evaluated as contraption content, not as host block content; exception blocks inside contraption should suppress it.

### Why it matters
Without this, exception blocks like `tfmg:steel_fluid_tank` can incorrectly irradiate when inside contraption.

### Suggested migration handling
Use typed source ownership: each found fluid must have an owning block/container id and exception context.

### Required test
Contraption with `tfmg:steel_fluid_tank` containing uranium inside item drain must not emit due to exception.

## TODO: Create trains

### Type
TODO / UNKNOWN / COMPATIBILITY

### Source
- `TODO.md`: "Поезда Create"
- No implementation files.

### Current behavior
Not implemented.

### Expected behavior
Train carriage blocks/items/fluids should emit radiation and respect exceptions, ideally from exact source positions.

### Why it matters
Create trains are major long-distance automation/transport.

### Suggested migration handling
Implement later after core engine and diagnostics. Requires real train NBT/API research.

### Required test
Create train with radioactive block, radioactive container, radioactive fluid tank and exception tank; dump train data and validate sources.

## TODO: Create Aeronautics / Simulated sublevels

### Type
TODO / UNKNOWN / COMPATIBILITY

### Source
- `TODO.md`: "Create Aeronautics / sublevels"
- `config.js`: `simulated:*` exception ids.

### Current behavior
Not implemented.

### Expected behavior
Aeronautics/Simulated moving structures/sublevels should be scanned for radiation sources.

### Why it matters
Project requirements mention full support for Aeronautics constructions.

### Suggested migration handling
Research spike after core engine. Possibly needs dedicated integration or bridge if sublevels are invisible to standard world APIs.

### Required test
Provide NBT/API dump of a sublevel with radioactive blocks and run `/radworks dump`.

## FRAGILE: Exact positions inside contraptions are not implemented

### Type
FRAGILE / TODO

### Source
- `contraption.js`: `getPlacedContraptionRadiation`
- `radiation_world_scan.js`: placed source uses entity position.

### Current behavior
Placed contraption entity emits from entity coordinates.

### Expected behavior
Radiation should originate from actual block/container/fluid positions inside the contraption when possible.

### Why it matters
Large contraptions/trains should not irradiate as one sphere from center.

### Suggested migration handling
Represent source local position and transform to world position using Create API rather than manual NBT if possible.

### Required test
Contraption with radioactive source at known offset in four orientations.

## TODO: Custom radiation armor

### Type
TODO

### Source
- `TODO.md`
- `config.js`: current `ARMOR_SET` uses vanilla diamond armor.

### Current behavior
No custom armor. Full diamond set blocks new script radiation.

### Expected behavior
New mod may add its own armor set with defined stats/repair material/textures.

### Why it matters
Needed for full product identity and balance.

### Suggested migration handling
Do not implement in Phase 0. Require design decision: ids, stats, textures, recipes.

### Required test
Wear full set; verify new radiation is blocked but existing effect is not removed.

## FRAGILE: Radiation effect registration may conflict with Create Nuclear

### Type
FRAGILE / COMPATIBILITY

### Source
- `radiation_modifiers.js`
- `events.js`

### Current behavior
Startup script creates `createnuclear:radiation` and event script applies damage/exhaustion.

### Expected behavior
New mod must intentionally decide whether to replace Create Nuclear effect, attach behavior to it, or use its own effect.

### Why it matters
Duplicate effect registration can crash or override external mod behavior.

### Suggested migration handling
Add migration decision required before implementation.

### Required test
Boot with Create Nuclear installed and verify no duplicate registry errors.

## PERFORMANCE: Naive per-player scanning

### Type
PERFORMANCE

### Source
- `radiation_world_scan.js`: block sphere scan and `world.getEntities()`.
- `TODO.md`: optimization for 30+ players.

### Current behavior
Every player scan iterates nearby blocks and all entities, with repeated NBT parsing.

### Expected behavior
New mod should support 30+ players with caching, chunk/source indexing and bounded scans.

### Why it matters
Server TPS/MSPT.

### Suggested migration handling
Design cache early, but implement after Phase 1/2 diagnostics.

### Required test
Performance smoke test with many sources and players; record scan time.

## FRAGILE: `create:cardboard_package` support is heuristic

### Type
FRAGILE / UNKNOWN

### Source
- `radiation_items.js`: `extractItemsFromPackageStack`, `collectPackageStacksFromData`.
- `TODO.md`.

### Current behavior
Searches guessed keys like `package_contents`, `contents`, `items`, `storage`, `value`.

### Expected behavior
Use exact Create package API/data components.

### Why it matters
Broad search may miss real contents or detect non-real/ghost/filter items.

### Suggested migration handling
Add diagnostics first and implement after receiving real dumps.

### Required test
Package on ground, in inventory, in container and in contraption with radioactive contents.

## UNKNOWN: Exact modpack metadata

### Type
UNKNOWN

### Source
- Project scan: no `mods/`, manifest, modlist, `config/`, `defaultconfigs/`, datapacks or resourcepacks found.

### Current behavior
Scripts imply Create, Create Nuclear, TFMG, Sophisticated Storage/Backpacks and Simulated/Aeronautics, but versions are unknown.

### Expected behavior
New mod setup needs exact Minecraft/NeoForge/dependency versions.

### Why it matters
APIs and class names depend heavily on version.

### Suggested migration handling
Ask user for target MC version, NeoForge version and mod list before Phase 0 Gradle dependency locking.

### Required test
None; data request.
