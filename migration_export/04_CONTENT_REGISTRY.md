# Content Registry

## Items

| ID | Display name | Source file | Current behavior | Intended behavior | Migration notes |
|---|---|---|---|---|---|
| `createnuclear:raw_uranium` | UNKNOWN | `config.js` | Radioactive item radius value 2 | Emits dynamic radiation in inventories/entities/containers | External item; config/rules entry |
| `create:crushed_raw_uranium` | UNKNOWN | `config.js` | Radioactive item radius value 2 | Emits dynamic radiation | External Create/Create Nuclear item |
| `createnuclear:raw_uranium_block` | UNKNOWN | `config.js` | Radioactive item radius 8; static block radius 8 | Item/block both hazardous | External item/block |
| `createnuclear:uranium_ore` | UNKNOWN | `config.js` | Radioactive item radius 2; static block radius 2 | Item/block both hazardous | External item/block |
| `createnuclear:deepslate_uranium_ore` | UNKNOWN | `config.js` | Radioactive item radius 2; static block radius 2 | Item/block both hazardous | External item/block |
| `createnuclear:enriched_soul_soil` | UNKNOWN | `config.js` | Radioactive item radius 2; static block radius 2 | Item/block both hazardous | External item/block |
| `createnuclear:enriching_campfire` | UNKNOWN | `config.js` | Radioactive item radius 2; static block radius 4 | Item/block both hazardous | External item/block |
| `createnuclear:uranium_powder` | UNKNOWN | `config.js` | Radioactive item radius 3 | Emits dynamic radiation | External item |
| `createnuclear:uranium_bucket` | UNKNOWN | `config.js` | Radioactive item radius 12 | Emits dynamic radiation | External item |
| `createnuclear:uranium_rod` | UNKNOWN | `config.js` | Radioactive item radius 8 | Emits dynamic radiation | External item |
| `createnuclear:yellowcake` | UNKNOWN | `config.js` | Radioactive item radius 6 | Emits dynamic radiation | External item |
| `createnuclear:enriched_yellowcake` | UNKNOWN | `config.js` | Radioactive item radius 8 | Emits dynamic radiation | External item |
| `minecraft:diamond_boots` | Diamond Boots | `config.js` | Armor set element | Temporary/default radiation protection armor | New mod may replace with custom armor |
| `minecraft:diamond_leggings` | Diamond Leggings | `config.js` | Armor set element | Temporary/default radiation protection armor | New mod may replace with custom armor |
| `minecraft:diamond_chestplate` | Diamond Chestplate | `config.js` | Armor set element | Temporary/default radiation protection armor | New mod may replace with custom armor |
| `minecraft:diamond_helmet` | Diamond Helmet | `config.js` | Armor set element | Temporary/default radiation protection armor | New mod may replace with custom armor |
| `create:minecart_contraption` | UNKNOWN | `radiation_common.js`, `contraption.js` | Partial radiation from internal contents | Full contraption source support | Requires Create integration |
| `create:cardboard_package` | UNKNOWN | `radiation_items.js` | Heuristic nested content scan | Real package content support | Needs Create package API/data |

## Blocks

| ID | Display name | Source file | Current behavior | Intended behavior | Migration notes |
|---|---|---|---|---|---|
| `createnuclear:uranium_ore` | UNKNOWN | `config.js` | Static radius 2 | Emits fixed radiation | External block |
| `createnuclear:enriched_soul_soil` | UNKNOWN | `config.js` | Static radius 2 | Emits fixed radiation | External block |
| `createnuclear:enriching_fire` | UNKNOWN | `config.js` | Static radius 4 | Emits fixed radiation | External block |
| `createnuclear:deepslate_uranium_ore` | UNKNOWN | `config.js` | Static radius 2 | Emits fixed radiation | External block |
| `createnuclear:raw_uranium_block` | UNKNOWN | `config.js` | Static radius 8 | Emits fixed radiation | External block |
| `createnuclear:uranium` | UNKNOWN | `config.js` | Static radius 8 | Emits fixed radiation if block exists | Confirm block vs fluid |
| `createnuclear:flowing_uranium` | UNKNOWN | `config.js` | Static radius 8 | Emits fixed radiation if block exists | Confirm fluid block |
| `createnuclear:enriching_campfire` | UNKNOWN | `config.js` | Static radius 4 | Emits fixed radiation | External block |
| `tfmg:raw_lead_block` | UNKNOWN | `config.js` | Shielding block | Blocks radiation rays | External TFMG block |
| `tfmg:lead_block` | UNKNOWN | `config.js` | Shielding block | Blocks radiation rays | External TFMG block |
| `tfmg:lead_ore` | UNKNOWN | `config.js` | Shielding block | Blocks radiation rays | External TFMG block |
| `createnuclear:reinforced_glass` | UNKNOWN | `config.js` | Shielding block | Blocks radiation rays | External block |
| `simulated:linked_typewriter` | UNKNOWN | `config.js` | Container exception | Suppresses contained radiation | Optional mod |
| `simulated:directional_linked_receiver` | UNKNOWN | `config.js` | Container exception | Suppresses contained radiation | Optional mod |
| `simulated:modulating_linked_receiver` | UNKNOWN | `config.js` | Container exception | Suppresses contained radiation | Optional mod |
| `create:redstone_requester` | UNKNOWN | `config.js` | Container exception | Suppresses contained radiation | External Create/addon |
| `create:factory_gauge` | UNKNOWN | `config.js` | Container exception | Suppresses contained radiation | External Create/addon |
| `create:item_vault` | UNKNOWN | `config.js` | Container exception | Suppresses contained radiation | Important contraption edge case |
| `create:redstone_link` | UNKNOWN | `config.js` | Container exception | Suppresses contained radiation | External Create block |
| `create:lectern_controller` | UNKNOWN | `config.js` | Container exception | Suppresses contained radiation | External Create/addon |
| `create:creative_fluid_tank` | UNKNOWN | `config.js` | Container/fluid exception | Suppresses contained radiation | External Create block |
| `tfmg:steel_fluid_tank` | UNKNOWN | `config.js` | Container/fluid exception | Suppresses contained radiation | Critical edge case |
| `tfmg:steel_pipe` | UNKNOWN | `config.js` | Container/fluid exception | Suppresses contained radiation | External TFMG block |
| `tfmg:steel_smart_fluid_pipe` | UNKNOWN | `config.js` | Container/fluid exception | Suppresses contained radiation | External TFMG block |
| `tfmg:electric_pump` | UNKNOWN | `config.js` | Container/fluid exception | Suppresses contained radiation | External TFMG block |
| `tfmg:steel_mechanical_pump` | UNKNOWN | `config.js` | Container/fluid exception | Suppresses contained radiation | External TFMG block |
| `minecraft:air` | Air | `events.js`, `radiation_debug.js` | Debug skip check | Not content | No migration content |

## Fluids

| ID | Source file | Behavior | Migration notes |
|---|---|---|---|
| `createnuclear:uranium` | `config.js`, `radiation_fluids.js` | Radioactive fluid; start radius 2, +1 per 1000 mb, max 10 | External fluid; use fluid rules/config |

## Effects

| ID | Source file | Behavior | Migration notes |
|---|---|---|---|
| `createnuclear:radiation` | `radiation_modifiers.js`, `events.js` | Harmful, color 15453236, attribute debuffs, tick damage/exhaustion | MIGRATION_DECISION_REQUIRED: own effect vs Create Nuclear effect |

## Tags

| Tag | Values | Source file | Purpose |
|---|---|---|---|
| None found | N/A | N/A | Current project uses arrays, not tags |

## Recipes

| Output/ID | Type | Inputs | Output | Source file | Conditions | Migration notes |
|---|---|---|---|---|---|---|
| None found | N/A | N/A | N/A | N/A | N/A | No recipe migration from current repo |

## Commands

| Command | Source file | Purpose | Migration notes |
|---|---|---|---|
| None found | N/A | Current project has no commands | New mod must add `/radworks ...` diagnostics |

## Config values

| Name | Current location | Meaning | Default | Migration target |
|---|---|---|---|---|
| `BLOCK_CONFIGS` | `config.js` | Static radioactive blocks and fixed radius | see block table | JSON/config list |
| `ENABLE_DYNAMIC_CONTAINERS` | `config.js` | Enable dynamic container item radiation | `true` | server config |
| `DYNAMIC_CONTAINER_RADIUS` | `config.js` | Fallback item radioactivity | `4` | server config |
| `RADIOACTIVE_ITEM_CONFIGS` | `config.js` | Radioactive item ids and base radius/radioactivity | see item table | JSON/config list |
| `RADIATION_LEVELS` | `config.js` | Dynamic item radius tiers | `[{0..4,1,0.1},{5..6,2,0.2},{7..99,4,0.3}]` | server config |
| `RADIUS_FLOOR` | `config.js` | Minimum radius clamp | `1` | server config |
| `RADIUS_CEIL` | `config.js` | Maximum radius clamp/search cap | `10` | server config |
| `RADIOACTIVE_FLUIDS` | `config.js` | Radioactive fluid formulas | uranium entry | JSON/config list |
| `FLUID_START_RADIUS` | `config.js` | Default fluid start radius | `2` | server config |
| `FLUID_RADIUS_PER_1000` | `config.js` | Default fluid radius per 1000 mb | `1` | server config |
| `FLUID_MAX_RADIUS` | `config.js` | Default fluid max radius | `10` | server config |
| `ENABLE_RADIATION_SHIELDING` | `config.js` | Enable shielding checks | `true` | server config |
| `RADIATION_SHIELDING_BLOCKS` | `config.js` | Shielding block ids | lead/glass list | tag/config |
| `CONTAINER_EXCEPTIONS` | `config.js` | Blocks that never emit from contents | exception list | tag/config |
| `ENABLE_PLAYER_INVENTORY_RADIATION` | `config.js` | Enable player inventory radiation | `true` | server config |
| `PLAYER_INVENTORY_CHECK_COOLDOWN` | `config.js` | Player inventory scan interval | `100` ticks | server config |
| `ARMOR_SET` | `config.js` | Full set required for immunity | diamond armor | server config |
| `CHECK_COOLDOWN` | `config.js` | World scan interval per player | `20` ticks | server config |
| `RADIATION_DURATION` | `config.js` | Applied effect duration | `20` ticks | server config |
| `DEBUG_CONTAINERS` | `config.js` | Enable Shift+RightClick debug | `false` | command/config |
| `DEBUG_NESTED_ITEMS` | `config.js` | Intended nested debug flag | `true` | UNKNOWN: unused? |
| `SHOW_PARTICLES` | `config.js` | Show radius particles | `true` | server/client config |
| `PARTICLE_TYPE` | `config.js` | Particle id | `minecraft:end_rod` | config |
| `ANGLE_STEP` | `config.js` | Particle sphere angular step | `10` degrees | config |
