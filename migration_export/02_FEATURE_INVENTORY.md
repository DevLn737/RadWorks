# Feature Inventory

## Feature: Data/config driven radiation rules

### Status
IMPLEMENTED / FRAGILE

### Source files
- `config.js`
- `radiation_common.js`

### Current KubeJS behavior
`config.js` defines `RADIATION_CONFIG`. `radiation_common.js` converts config arrays into runtime maps: `blockRadii`, `dangerItemRadii`, `dangerFluidMap`, `radiationShielding`, `containerExceptions`.

### Intended behavior
The Java mod should load equivalent rules from data/config files, validate them, report invalid ids and avoid hardcoding modpack-specific content in Java logic.

### Known bugs
- UNKNOWN: no validation for missing item/block/fluid ids in current KubeJS.
- FRAGILE: optional mod ids are accepted silently.

### Missing parts
- No JSON/TOML validation command.
- No config migration system.
- No tags.

### IDs involved
Items:
- `createnuclear:raw_uranium`
- `create:crushed_raw_uranium`
- `createnuclear:raw_uranium_block`
- `createnuclear:uranium_ore`
- `createnuclear:deepslate_uranium_ore`
- `createnuclear:enriched_soul_soil`
- `createnuclear:enriching_campfire`
- `createnuclear:uranium_powder`
- `createnuclear:uranium_bucket`
- `createnuclear:uranium_rod`
- `createnuclear:yellowcake`
- `createnuclear:enriched_yellowcake`

Blocks:
- all block ids listed in `BLOCK_CONFIGS`, `RADIATION_SHIELDING_BLOCKS`, `CONTAINER_EXCEPTIONS`.

Fluids:
- `createnuclear:uranium`

Effects:
- `createnuclear:radiation`

Tags:
- None.

Recipes:
- None.

Commands:
- None in old project.

### Dependencies
Create Nuclear, Create, TFMG, Sophisticated Storage/Backpacks, Simulated/Aeronautics ids.

### Migration notes
Implement `RadiationRules` and `RadiationRulesLoader`; support config JSON/TOML plus optional datapack tags. Add `/radworks validate`.

### Diagnostics required
- `/radworks validate` should report unknown ids, duplicate entries, invalid radius/k values and missing optional mods.

## Feature: Static radioactive block sources

### Status
IMPLEMENTED

### Source files
- `config.js`
- `radiation_world_scan.js`

### Current KubeJS behavior
`findAllRadiationSources(player)` scans blocks around the player. If block id exists in `blockRadii`, it adds a source at block coordinates if player is within that block's radius.

### Intended behavior
Configured radioactive blocks emit fixed-radius radiation in the world.

### Known bugs
- PERFORMANCE: per-player sphere scan is naive.

### Missing parts
- No chunk/source cache.
- No exact block-source registry/index.

### IDs involved
Blocks:
- `createnuclear:uranium_ore` radius 2
- `createnuclear:enriched_soul_soil` radius 2
- `createnuclear:enriching_fire` radius 4
- `createnuclear:deepslate_uranium_ore` radius 2
- `createnuclear:raw_uranium_block` radius 8
- `createnuclear:uranium` radius 8
- `createnuclear:flowing_uranium` radius 8
- `createnuclear:enriching_campfire` radius 4

### Dependencies
Create Nuclear.

### Migration notes
Use `BlockSourceProvider`. Prefer tag/config lookup and source cache invalidated by block changes.

### Diagnostics required
- `/radworks sources` should list static block id, position, configured radius, distance to player and shielding result.

## Feature: Dynamic radioactive item sources

### Status
IMPLEMENTED / FRAGILE

### Source files
- `config.js`
- `radiation_math.js`
- `radiation_items.js`
- `radiation_world_scan.js`

### Current KubeJS behavior
Radioactive item counts are summed, max radioactivity tier is selected, radius is calculated via `calculateDynamicRadius`. Nested items are extracted from several known containers/components.

### Intended behavior
Any real inventory or transport containing radioactive items should emit radiation according to rules.

### Known bugs
- FRAGILE: broad NBT and component heuristics can miss items or read non-real items.
- UNKNOWN: Curios/Trinkets are not handled.

### Missing parts
- Full "any inventory" support is not complete.
- No stable adapter model.

### IDs involved
Items:
- radioactive item list from `config.js`.

### Dependencies
Minecraft inventory APIs, Forge capabilities, Create, Sophisticated.

### Migration notes
Create `InventorySourceProvider` and `NestedStackScanner`. Use item capabilities/data components where possible. Keep broad NBT only as diagnostics fallback.

### Diagnostics required
- `/radworks dump item <slot>` or `/radworks sources` should show item path: player inventory -> nested container -> radioactive item.

## Feature: Radioactive fluid sources

### Status
IMPLEMENTED / FRAGILE

### Source files
- `config.js`
- `radiation_fluids.js`
- `contraption.js`

### Current KubeJS behavior
Recursive NBT scanner finds compounds with `id` and `amount`, then applies configured fluid formula.

### Intended behavior
Fluid tanks and fluid handlers emit only if actual stored fluid is radioactive, with correct owner container context.

### Known bugs
- BUG/FRAGILE: broad scan can misattribute fluid inside nested contraption/container.

### Missing parts
- Typed fluid handler support.
- Ownership/context tracking.

### IDs involved
Fluids:
- `createnuclear:uranium`

### Dependencies
Create Nuclear, Create/TFMG fluid storage APIs.

### Migration notes
Use fluid capabilities/handlers and source ownership. For contraptions, each fluid source should know the containing block id and exception status.

### Diagnostics required
- `/radworks sources` should show fluid id, amount, tank/container block id, radius and whether exception suppressed it.

## Feature: Shielding

### Status
IMPLEMENTED

### Source files
- `config.js`
- `radiation_shielding.js`
- `events.js`

### Current KubeJS behavior
Three line traces with step 0.3 check configured shielding blocks. All three lines must be blocked for shielded=true.

### Intended behavior
Radiation can be fully blocked by configured shielding materials.

### Known bugs
- FRAGILE: ignores collision shape, block opacity and partial geometry; checks only block id.

### Missing parts
- No diagnostics explaining which ray failed.

### IDs involved
Blocks:
- `tfmg:raw_lead_block`
- `tfmg:lead_block`
- `tfmg:lead_ore`
- `createnuclear:reinforced_glass`

### Migration notes
Implement `ShieldingEngine` with same default behavior first. Add optional debug ray output.

### Diagnostics required
- `/radworks exposure <player>` should show shield rays and blocking blocks.

## Feature: Armor protection

### Status
IMPLEMENTED / PLANNED

### Source files
- `config.js`
- `radiation_common.js`
- `events.js`
- `TODO.md`

### Current KubeJS behavior
Full configured armor set prevents new radiation application. Current set is vanilla diamond armor.

### Intended behavior
Eventually custom radiation armor should exist, but core logic must first support configured armor set.

### Known bugs
- UNKNOWN: exact slot ordering should be verified in target Java implementation.

### Missing parts
- Custom armor ids/stats/textures/recipes.

### IDs involved
Items:
- `minecraft:diamond_boots`
- `minecraft:diamond_leggings`
- `minecraft:diamond_chestplate`
- `minecraft:diamond_helmet`

### Migration notes
Implement `ArmorProtectionService` before custom armor registry.

### Diagnostics required
- `/radworks exposure <player>` should show armor check result and missing/wrong pieces.

## Feature: Radiation effect, damage and exhaustion

### Status
BUGGY / FRAGILE / PARTIAL

### Source files
- `radiation_modifiers.js`
- `events.js`
- `TODO.md`

### Current KubeJS behavior
Startup script creates/modifies `createnuclear:radiation` attributes. Player tick applies magic damage and exhaustion while effect is active.

### Intended behavior
New mod should have a deliberate effect strategy: own effect or safe integration with Create Nuclear's effect.

### Known bugs
- FRAGILE: possible duplicate/override conflict with Create Nuclear.
- TODO: damage should be implemented "normally", not as ad-hoc KubeJS tick if Java effect tick is possible.

### Missing parts
- No decision on effect id ownership.

### IDs involved
Effects:
- `createnuclear:radiation`

### Migration notes
Add `MIGRATION_DECISION_REQUIRED`: reuse external effect vs own effect. Do not implement until decision in new repo.

### Diagnostics required
- `/radworks version` should report effect strategy.
- Boot logs should report whether Create Nuclear effect was found/reused.

## Feature: Player inventory radiation

### Status
IMPLEMENTED

### Source files
- `events.js`
- `radiation_world_scan.js`
- `radiation_items.js`

### Current KubeJS behavior
Player inventory is scanned every `PLAYER_INVENTORY_CHECK_COOLDOWN` ticks. Radiation source affects only carrying player.

### Intended behavior
Carrying radioactive items or contraptions should be personally hazardous.

### Known bugs
- UNKNOWN: offhand/Curios/Trinkets support not present.

### Missing parts
- Better diagnostics.
- Optional equipment slots beyond inventory.

### IDs involved
Items:
- all radioactive item ids
- `create:minecart_contraption`

### Migration notes
Implement early Phase 2 after rules. This is the safest first gameplay feature.

### Diagnostics required
- `/radworks exposure <player>` with item contribution paths.

## Feature: Block entity and container radiation

### Status
PARTIAL / FRAGILE

### Source files
- `radiation_items.js`
- `radiation_fluids.js`
- `radiation_world_scan.js`

### Current KubeJS behavior
Reads KubeJS inventory wrappers, Forge capabilities, Create NBT keys, Sophisticated storage, generic NBT lists.

### Intended behavior
All real inventories/tanks in supported mods should emit radiation when containing radioactive content.

### Known bugs
- FRAGILE: broad NBT scanning.
- TODO: "any inventory/tank" is not guaranteed.

### Missing parts
- Adapter registry.
- Test matrix for concrete blocks.

### Migration notes
Implement adapter/provider model and diagnostics before expanding integrations.

### Diagnostics required
- `/radworks dump block` looking at targeted block: block id, block entity class, inventory handlers, fluid handlers, recognized items/fluids.

## Feature: Entity radiation

### Status
PARTIAL / FRAGILE

### Source files
- `radiation_items.js`
- `radiation_world_scan.js`

### Current KubeJS behavior
Handles dropped item-like `ent.item`, item frames, armor stands, chest minecarts, chest boats, chested animals and generic entity inventories if KubeJS exposes them.

### Intended behavior
Entities carrying radioactive items should emit radiation and living carriers should receive radiation if detected.

### Known bugs
- UNKNOWN: chain-moving Create packages and other entity states not confirmed.

### Missing parts
- Complete entity adapter set.

### Migration notes
Use entity type/capability adapters and avoid `getType().toString()` string matching where possible.

### Diagnostics required
- `/radworks dump entity` for looked-at/nearby entity.

## Feature: Create minecart contraptions

### Status
PARTIAL / FRAGILE

### Source files
- `contraption.js`
- `radiation_world_scan.js`
- `radiation_items.js`

### Current KubeJS behavior
Reads `create:minecart_contraption_data`, palette, block list, actors, nested items/fluids, Sophisticated UUIDs. Placed contraption detection uses NBT `Contraption`.

### Intended behavior
Contraptions should emit from radioactive blocks/items/fluids inside them and respect exceptions.

### Known bugs
- FRAGILE: no exact source positions.
- FRAGILE: dependent on Create NBT shape.

### Missing parts
- Per-block positions.
- Create train support.

### Migration notes
Create integration should be optional and isolated. Use Create APIs if available.

### Diagnostics required
- `/radworks dump contraption` should output palette, block list, detected source nodes and ignored exception nodes.

## Feature: Create trains

### Status
PLANNED

### Source files
- `TODO.md`

### Current KubeJS behavior
Not implemented.

### Intended behavior
Trains should emit from blocks/containers/fluids in carriages and respect exceptions.

### Known bugs
Not implemented.

### Missing parts
All implementation.

### Migration notes
Do not start until core engine and Create contraption scanner are stable.

### Diagnostics required
- `/radworks dump train` or `/radworks dump entity` must collect train/carriage data.

## Feature: Create Aeronautics / Simulated sublevels

### Status
PLANNED / UNKNOWN

### Source files
- `TODO.md`
- `config.js` contains `simulated:*` exception ids.

### Current KubeJS behavior
Not implemented.

### Intended behavior
Aeronautics moving structures/sublevels should support radiation sources.

### Missing parts
All implementation and API research.

### Migration notes
Research spike after core engine.

### Diagnostics required
- `/radworks dump aeronautics` only after confirming API.

## Feature: Debug tooling

### Status
IMPLEMENTED / INSUFFICIENT FOR MIGRATION

### Source files
- `radiation_debug.js`
- `events.js`
- `config.js`

### Current KubeJS behavior
Shift+RightClick debug if `DEBUG_CONTAINERS` is true.

### Intended behavior
New mod should have robust commands: `/radworks version`, `/radworks dump`, `/radworks sources`, `/radworks exposure`, `/radworks debug`, `/radworks validate`.

### Missing parts
All command-based diagnostics in old project.

### Migration notes
Diagnostic-first design is mandatory.

### Diagnostics required
See `08_DIAGNOSTICS_REQUIREMENTS.md`.
