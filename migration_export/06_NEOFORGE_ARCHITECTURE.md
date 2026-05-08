# NeoForge Architecture Proposal

## Target
- Minecraft version: UNKNOWN
- NeoForge version: UNKNOWN
- Java version: UNKNOWN
- Mod ID: `radworks` is recommended, final value UNKNOWN until the new repository chooses it.
- Main package: `com.<owner>.radworks` or `dev.<owner>.radworks`; exact package UNKNOWN.

## Core principle
Новый мод строится не как порт KubeJS-скриптов, а как нормальная Java-архитектура. Старый проект используется как прототип поведения, набор балансовых значений, список edge cases и источник диагностики.

KubeJS не должен быть runtime-зависимостью нового мода.

## Suggested package structure

```text
src/main/java/<package>/
  RadWorks.java
  registry/
    RadItems.java
    RadBlocks.java
    RadEffects.java
    RadCreativeTabs.java
  config/
    RadWorksConfig.java
    RadiationRuleConfig.java
  command/
    RadWorksCommand.java
    VersionCommand.java
    DumpCommand.java
    SourcesCommand.java
    ExposureCommand.java
    ValidateCommand.java
  diagnostics/
    DiagnosticsService.java
    DiagnosticsDump.java
    WarningBuffer.java
    PerformanceStats.java
  radiation/
    RadiationSource.java
    RadiationSourceType.java
    RadiationSourceSnapshot.java
    RadiationRules.java
    RadiationRulesLoader.java
    ExposureEngine.java
    RadiationEffectApplier.java
  radiation/source/
    RadiationSourceProvider.java
    PlayerInventorySourceProvider.java
    BlockSourceProvider.java
    BlockEntityInventorySourceProvider.java
    BlockEntityFluidSourceProvider.java
    EntitySourceProvider.java
    DroppedItemSourceProvider.java
  radiation/shielding/
    ShieldingEngine.java
    ShieldingResult.java
  radiation/cache/
    SourceCache.java
    ChunkSourceIndex.java
    EntitySourceIndex.java
  integration/
    IntegrationManager.java
    OptionalMod.java
  integration/create/
    CreateIntegration.java
    CreateContraptionSourceProvider.java
    CreateTrainSourceProvider.java
  integration/aeronautics/
    AeronauticsIntegration.java
    AeronauticsSourceProvider.java
  client/
    RadWorksClient.java
    TooltipHandlers.java
    DebugRenderers.java
  data/
    RadDataGenerators.java
    RadItemTagProvider.java
    RadBlockTagProvider.java
    RadRecipeProvider.java
```

## Data/resource structure

```text
src/main/resources/
  META-INF/
    neoforge.mods.toml
  assets/radworks/
    lang/
    models/
    textures/
    sounds/
  data/radworks/
    radiation_rules/
    tags/
    recipes/
    loot_tables/
    advancements/
```

## Core engine

### `RadiationSource`
Immutable server-side representation of one source contribution:
- source type;
- ID;
- position or entity UUID;
- radius;
- strength;
- distance to target;
- optional container path;
- optional nested item/fluid summary.

### `RadiationSourceType`
Enum or registry-like type for:
- `PLAYER_INVENTORY`;
- `BLOCK`;
- `BLOCK_ENTITY_INVENTORY`;
- `BLOCK_ENTITY_FLUID`;
- `ENTITY_INVENTORY`;
- `DROPPED_ITEM`;
- `CREATE_CONTRAPTION`;
- `CREATE_TRAIN`;
- `AERONAUTICS_OBJECT`.

### `RadiationSourceProvider`
Interface for all source discovery:

```java
public interface RadiationSourceProvider {
    void collectSources(RadiationSourceContext context, RadiationSourceCollector collector);
}
```

### `RadiationRules`
Validated runtime rules for items, blocks, fluids, armor protection and shielding.

### `RadiationRulesLoader`
Loads JSON/data/config rules, validates registry IDs and provides checksum for diagnostics.

### `ExposureEngine`
Combines sources, distance, shielding and armor protection into final exposure.

### `ShieldingEngine`
Owns ray/block protection logic. It should preserve old behavior initially only if the user confirms that exact binary 3-ray behavior is required.

### `RadiationEffectApplier`
Applies effect/damage/exhaustion. It must not silently hijack `createnuclear:radiation` unless compatibility mode explicitly requires that.

### `SourceCache`
Caches expensive source discovery. Required because the old KubeJS code scans blocks/entities around every player.

### `DiagnosticsService`
Single service used by commands, warning capture and JSON dumps.

## Source provider model

All sources must come through `RadiationSourceProvider`.

Initial providers:
- player inventory provider;
- dropped item provider;
- entity inventory provider;
- block provider;
- block entity inventory provider;
- block entity fluid provider.

Later providers:
- Create contraption provider;
- Create train provider;
- Aeronautics/Simulated provider.

## Diagnostic-first design

The new mod must implement diagnostics before complex gameplay:
- `/radworks version`;
- `/radworks dump`;
- `/radworks sources`;
- `/radworks exposure <player>`;
- `/radworks debug on/off`;
- `/radworks validate`.

Phase 0 must include `/radworks version` and `/radworks dump`. Radiation gameplay must not start before that foundation exists.

## Data-driven design

Rules from `config.js` should become JSON/config entries:
- radioactive item rules;
- radioactive block rules;
- radioactive fluid rules;
- shielding blocks;
- armor protection rules;
- effect consequence rules.

Hardcoded Java constants are acceptable only as defaults and must be surfaced in diagnostics.

## Registry strategy

Items/blocks/fluids:
- The current KubeJS project does not define custom items/blocks/fluids itself.
- Most IDs are external mod IDs such as `createnuclear:*`, `create:*`, `tfmg:*`.
- New mod should not register duplicate external content.

Effects:
- Prefer own `radworks:radiation` effect unless compatibility requires mapping to `createnuclear:radiation`.
- Old registration in `radiation_modifiers.js` is FRAGILE and must not be copied blindly.

Commands:
- Use Brigadier through NeoForge command registration.

## Event strategy

KubeJS `PlayerEvents.tick` from `events.js` maps to a server player tick event or server tick system with per-player scheduling.

KubeJS `PlayerEvents.loggedIn` maps to player login/clone lifecycle handlers, but persistent reset behavior from `events.js` must be reviewed because wiping data on login may not be desirable in a real mod.

KubeJS `BlockEvents.rightClicked` debug path maps to a debug command or optional interaction handler, not always-on gameplay.

## Recipe strategy

No recipes were found in the current KubeJS project. If recipes are later added, prefer data generation and JSON recipes. Use custom recipe serializers only for behavior that cannot be expressed by vanilla/NeoForge recipe types.

## Tag strategy

No custom tags were found. Shielding and armor should probably use block/item tags in the new mod:
- `#radworks:radiation_shielding`;
- `#radworks:radiation_protection_armor`.

These tags should be generated from defaults and optionally extended by data packs.

## Client strategy

Current project has no `kubejs/client_scripts`, no assets and no tooltip scripts.

New client work should be minimal at first:
- command feedback;
- optional debug particles/renderers;
- optional tooltips later showing radioactive items/blocks only after core mechanics are stable.

JEI/REI/EMI integration is currently UNKNOWN and should not be implemented in Phase 0.

## Config strategy

Config should cover:
- tick intervals;
- source scan radius limits;
- radiation level curves;
- effect application;
- shielding behavior;
- debug verbosity;
- optional integrations.

Radiation source data should be data-driven where possible so modpacks can override it.

## Compatibility strategy

External IDs found:
- Create: `create:*`;
- Create Nuclear: `createnuclear:*`;
- TFMG: `tfmg:*`;
- Sophisticated Core/Storage: referenced in `radiation_items.js`;
- Simulated/Aeronautics-like IDs: `simulated:*`;
- Minecraft vanilla.

Optional integrations must be isolated:
- core engine cannot directly depend on Create/Aeronautics classes;
- load integration only when mod is present;
- provide diagnostics when an integration is absent or unsupported.

## Testing strategy

Required:
- compile/build test;
- dedicated server startup;
- command registration test;
- manual world tests from `09_TESTING_PLAN.md`;
- diagnostics snapshot comparison against expected source IDs.

For later phases:
- fake/source unit tests for rule validation;
- game tests for exposure calculations if project setup supports them;
- performance smoke test with many sources and players.
