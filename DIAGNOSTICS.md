# Diagnostics

Phase 0 diagnostics exist so users and future Codex sessions can inspect the mod before radiation gameplay is implemented.

## `/radworks version`
Prints:

```text
RadWorks 0.1.0
Minecraft: 1.21.1
NeoForge: 21.1.228
Java: <runtime version>
Integrations: create=absent, aeronautics=absent
Rules: loaded, checksum=<short>, mode=lenient/dev
```

If an optional mod is present in the instance, its state may appear as `loaded_disabled`. Phase 0 never enables optional integrations.

Before a world has loaded rules, the final line may still be:

```text
Rules: not loaded
```

## `/radworks validate`
Validates datapack radiation rules from:

```text
data/radworks/radiation_rules/*.json
```

Phase 1 uses validation mode:

```text
lenient/dev
```

Clean output should look like:

```text
RadWorks rules validation: loaded=3 enabled=3 disabled=0 errors=0 warnings=0 mode=lenient/dev checksum=<short>
```

Validation categories:
- `UNKNOWN_REGISTRY_ID`: warning in `lenient/dev`.
- `INVALID_RULE_VALUE`: error for invalid fields like `radius <= 0` or `strength <= 0`.
- `DUPLICATE_RULE`: error for duplicate enabled `type:id`; warning for duplicate disabled rules.
- `MALFORMED_JSON`: error for malformed or unreadable JSON.
- `DISABLED_RULE`: info; disabled rules are counted but not active.

## `/radworks exposure`
Reports diagnostic-only exposure from active item rules in the executing player's server-side inventory, active static block rules around the player, active item rules inside nearby vanilla `Container` block entities, active item rules inside nearby block item handlers exposed through NeoForge `Capabilities.ItemHandler.BLOCK`, and active fluid rules inside nearby block fluid handlers exposed through NeoForge `Capabilities.FluidHandler.BLOCK`.

```text
/radworks exposure
```

This form works only when run by a player. From server console, use:

```text
/radworks exposure <player>
```

Inventory scan includes only:
- main inventory;
- offhand.

Phase 4A block scan includes only ordinary block states read with `getBlockState`.

Phase 4B container scan includes only block entities that implement `net.minecraft.world.Container`. Slots are read through `getContainerSize()` and `getItem(slot)`.

Phase 4C item handler scan includes only block capabilities queried through `Capabilities.ItemHandler.BLOCK`. It does not scan entity capabilities or item stack capabilities.

Phase 4D fluid handler scan includes only block capabilities queried through `Capabilities.FluidHandler.BLOCK`. It does not scan item fluid capabilities, entity fluid capabilities, buckets or fluid containers in player inventory.

Phase 5A shielding uses the block tag:

```text
#radworks:shielding_blocks
```

The bundled temporary/dev-only shielding block is `minecraft:iron_block`.

Vanilla `Container` block entities are skipped by item handler scan to avoid double counting with Phase 4B.

Phase 5A does not scan armor, Curios/Trinkets, nested containers, shulker contents, backpacks, dropped items, entities, item NBT/components, item fluid containers, energy, Create or Aeronautics.

Formula:

```text
contribution = stack.count * rule.strength
distance = 0
shielding = not_applicable
finalExposure = sum(contribution)
```

Static block formula:

```text
distance = player position to block center
active when distance <= rule.radius
contribution = rule.strength
```

Vanilla container item formula:

```text
distance = player position to container block center
active when distance <= itemRule.radius
contribution = stack.count * itemRule.strength
```

Block item handler formula:

```text
distance = player position to block center
active when distance <= itemRule.radius
contribution = stack.count * itemRule.strength
```

Block fluid handler formula:

```text
distance = player position to block center
active when distance <= fluidRule.radius
contribution = fluidRule.strength * amountMb / 1000.0
```

Shielding formula for positioned sources:

```text
rawContribution = provider contribution before shielding
sample line = source block/container center to player body center
sampleStep = 0.25
maxSamples = 64
each unique #radworks:shielding_blocks hit multiplies by 0.5
minimum shieldingMultiplier = 0.1
finalContribution = rawContribution * shieldingMultiplier
shieldingReduction = rawContribution - finalContribution
contribution = finalContribution
```

Inventory sources do not have a world line, so they use:

```text
shielding = not_applicable
finalContribution = rawContribution
contribution = finalContribution
```

Positioned sources with `respectsShielding=false` also use `shielding=not_applicable`.

Positioned sources with `respectsShielding=true` and no shielding block hit use:

```text
shielding = clear
finalContribution = rawContribution
```

Positioned sources with one or more shielding block hits use:

```text
shielding = reduced
finalContribution < rawContribution
```

The effective block command scan radius is:

```text
min(max active block rule radius, 8)
```

The effective container command scan radius is:

```text
min(max active item rule radius, 8)
```

The effective item handler command scan radius is also:

```text
min(max active item rule radius, 8)
```

The effective fluid handler command scan radius is:

```text
min(max active fluid rule radius, 8)
```

Block, container, item handler and fluid handler scans are command-only and are not tick scans.

Example:

```text
RadWorks exposure for Dev: totalExposure=25.0 matchedSources=3
- type=player_inventory itemId=minecraft:rotten_flesh slot=inventory.0 count=10 distance=0.0 ruleRadius=2.0 ruleStrength=1.0 respectsShielding=true rawContribution=10.0 contribution=10.0 shielding=not_applicable shieldingBlocksHit=0 shieldingMultiplier=1.0 shieldingReduction=0.0 finalContribution=10.0
- type=block blockId=minecraft:gold_block position=10,64,10 distance=1.2 ruleRadius=6.0 ruleStrength=5.0 respectsShielding=true rawContribution=5.0 contribution=5.0 shielding=clear shieldingBlocksHit=0 shieldingMultiplier=1.0 shieldingReduction=0.0 finalContribution=5.0
- type=block_entity_inventory itemId=minecraft:rotten_flesh blockId=minecraft:chest containerPos=11,64,10 slot=container.0 count=10 distance=1.6 ruleRadius=2.0 ruleStrength=1.0 respectsShielding=true rawContribution=10.0 contribution=10.0 shielding=clear shieldingBlocksHit=0 shieldingMultiplier=1.0 shieldingReduction=0.0 finalContribution=10.0
Note: diagnostic only, no gameplay effect
```

Chat output is bounded to 10 source rows.

## `/radworks sources`
Reports the diagnostic sources currently found for a player. Phase 4D combines Phase 2 player inventory sources, Phase 4A static block sources, Phase 4B nearby vanilla `Container` block entity inventory sources, nearby block item handler sources and nearby block fluid handler sources.

```text
/radworks sources
/radworks sources <player>
```

Difference from `/radworks exposure`:
- `sources` shows found source rows and why they matched.
- `exposure` shows the summed total and contribution math.

Phase 4D `sources` scope:
- main inventory;
- offhand;
- ordinary block states around the player;
- vanilla block entities implementing `Container`;
- block `IItemHandler` capability exposed through `Capabilities.ItemHandler.BLOCK`;
- block `IFluidHandler` capability exposed through `Capabilities.FluidHandler.BLOCK`;
- active `type=item`, `type=block` and `type=fluid` rules only.

It does not scan entity capabilities, item stack capabilities, item fluid capabilities, nested containers, shulker contents, backpacks, dropped items, entities, buckets, item NBT/components, energy, Create or Aeronautics.

Example:

```text
RadWorks sources for Dev: matchedSources=3 scope=player_inventory+static_blocks+vanilla_containers+block_item_handlers+block_fluid_handlers
- type=player_inventory itemId=minecraft:rotten_flesh slot=inventory.0 count=10 distance=0.0 ruleRadius=2.0 ruleStrength=1.0 respectsShielding=true rawContribution=10.0 shielding=not_applicable shieldingBlocksHit=0 shieldingMultiplier=1.0 shieldingReduction=0.0 finalContribution=10.0 contribution=10.0 reason=active item rule matched type=item id=minecraft:rotten_flesh
- type=block blockId=minecraft:gold_block position=10,64,10 distance=1.2 ruleRadius=6.0 ruleStrength=5.0 respectsShielding=true rawContribution=5.0 shielding=clear shieldingBlocksHit=0 shieldingMultiplier=1.0 shieldingReduction=0.0 finalContribution=5.0 contribution=5.0 reason=active block rule matched type=block id=minecraft:gold_block
- type=block_entity_inventory itemId=minecraft:rotten_flesh blockId=minecraft:chest containerPos=11,64,10 slot=container.0 count=10 distance=1.6 ruleRadius=2.0 ruleStrength=1.0 respectsShielding=true rawContribution=10.0 shielding=clear shieldingBlocksHit=0 shieldingMultiplier=1.0 shieldingReduction=0.0 finalContribution=10.0 contribution=10.0 reason=vanilla Container slot matched active item rule type=item id=minecraft:rotten_flesh
sourcesShown=3 sourcesOmitted=0
Note: diagnostic only, player inventory, static block, vanilla Container, block ItemHandler and block FluidHandler sources only
Note: vanilla Container block entities are skipped by itemHandlerScan to avoid double counting
```

If a non-vanilla block item handler source is present, a row can look like:

```text
- type=block_item_handler itemId=minecraft:rotten_flesh blockId=example:item_handler_block position=12,64,10 capabilityContext=unsided slot=item_handler.0 count=10 distance=1.4 ruleRadius=2.0 ruleStrength=1.0 respectsShielding=true rawContribution=10.0 shielding=clear shieldingBlocksHit=0 shieldingMultiplier=1.0 shieldingReduction=0.0 finalContribution=10.0 contribution=10.0 reason=NeoForge ItemHandler block capability matched active item rule type=item id=minecraft:rotten_flesh
```

If a block fluid handler source is present, a row can look like:

```text
- type=block_fluid_handler fluidId=minecraft:water blockId=example:tank position=12,64,10 capabilityContext=unsided tank=fluid_handler.0 amountMb=1000 distance=1.4 ruleRadius=2.0 ruleStrength=1.0 respectsShielding=true rawContribution=1.0 shielding=clear shieldingBlocksHit=0 shieldingMultiplier=1.0 shieldingReduction=0.0 finalContribution=1.0 contribution=1.0 reason=NeoForge FluidHandler block capability matched active fluid rule type=fluid id=minecraft:water
```

Chat output is bounded to 10 source rows.

## `/radworks debug`
Controls server-wide in-memory diagnostics debug state:

```text
/radworks debug status
/radworks debug on
/radworks debug off
```

Rules:
- `debug status` is available to normal players.
- `debug on/off` require permission level 2.
- Console can use all three commands if permission allows.
- Debug state resets on server restart.
- Debug state does not change gameplay balance or calculations.

## `/radworks dump`
Creates a JSON diagnostics file in:

```text
<minecraft-instance>/radworks_dumps/
```

Filename format:

```text
radworks-dump-YYYYMMDD-HHMMSS-<player>.json
```

If the command is run from a server console, the filename uses `server` and the JSON field `player` is `null`.

## JSON schema
```json
{
  "schemaVersion": 1,
  "createdAt": "ISO-8601",
  "mod": {
    "id": "radworks",
    "version": "0.1.0",
    "minecraftVersion": "1.21.1",
    "neoforgeVersion": "21.1.228",
    "javaVersion": "runtime"
  },
  "world": {
    "dimension": "minecraft:overworld",
    "serverType": "integrated_or_dedicated",
    "gameTime": 0
  },
  "player": {
    "name": "player",
    "uuid": "uuid",
    "position": {
      "x": 0,
      "y": 0,
      "z": 0
    }
  },
  "rules": {
    "loaded": true,
    "checksum": "full SHA-256",
    "validationMode": "lenient/dev",
    "itemRules": 1,
    "blockRules": 1,
    "fluidRules": 1,
    "disabledRules": 0,
    "errors": [],
    "warnings": [],
    "infos": []
  },
  "debug": {
    "enabled": false
  },
  "integrations": {
    "create": {
      "loaded": false,
      "enabled": false,
      "notes": ["Phase 0: not implemented"]
    },
    "aeronautics": {
      "loaded": false,
      "enabled": false,
      "notes": ["Phase 0: not implemented"]
    }
  },
  "performance": {
    "validate": {
      "lastMillis": 0,
      "count": 0,
      "averageMillis": 0,
      "maxMillis": 0
    },
    "exposure": {
      "lastMillis": 0,
      "count": 0,
      "averageMillis": 0,
      "maxMillis": 0
    },
    "sources": {
      "lastMillis": 0,
      "count": 0,
      "averageMillis": 0,
      "maxMillis": 0
    },
    "blockScan": {
      "lastMillis": 0,
      "count": 0,
      "averageMillis": 0,
      "maxMillis": 0
    },
    "blockEntityInventoryScan": {
      "lastMillis": 0,
      "count": 0,
      "averageMillis": 0,
      "maxMillis": 0
    },
    "itemHandlerScan": {
      "lastMillis": 0,
      "count": 0,
      "averageMillis": 0,
      "maxMillis": 0
    },
    "fluidHandlerScan": {
      "lastMillis": 0,
      "count": 0,
      "averageMillis": 0,
      "maxMillis": 0
    },
    "shielding": {
      "lastMillis": 0,
      "count": 0,
      "averageMillis": 0,
      "maxMillis": 0
    },
    "dump": {
      "lastMillis": 0,
      "count": 0,
      "averageMillis": 0,
      "maxMillis": 0
    }
  },
  "sourceScanSummary": null,
  "lastExposureSnapshot": null,
  "recentWarnings": []
}
```

After `/radworks exposure`, `lastExposureSnapshot` becomes:

```json
{
  "createdAt": "ISO-8601",
  "rulesChecksum": "full SHA-256",
  "stale": false,
  "playerName": "Dev",
  "playerUuid": "uuid",
  "totalExposure": 10.0,
  "matchedSources": 1,
  "matchedStacks": 1,
  "notes": "diagnostic only, no gameplay effect",
  "sources": [
    {
      "type": "player_inventory",
      "itemId": "minecraft:rotten_flesh",
      "slot": "inventory.0",
      "count": 10,
      "ruleStrength": 1.0,
      "ruleRadius": 2.0,
      "distance": 0.0,
      "respectsShielding": true,
      "rawContribution": 10.0,
      "shielding": "not_applicable",
      "shieldingBlocksHit": 0,
      "shieldingMultiplier": 1.0,
      "shieldingReduction": 0.0,
      "finalContribution": 10.0,
      "contribution": 10.0,
      "matchReason": "active item rule matched type=item id=minecraft:rotten_flesh"
    }
  ],
  "sourcesShown": 1,
  "sourcesOmitted": 0
}
```

After Phase 4A, source rows may include block sources:

```json
{
  "type": "block",
  "blockId": "minecraft:gold_block",
  "position": {
    "x": 10,
    "y": 64,
    "z": 10
  },
  "ruleStrength": 5.0,
  "ruleRadius": 6.0,
  "distance": 1.2,
  "respectsShielding": true,
  "rawContribution": 5.0,
  "shielding": "clear",
  "shieldingBlocksHit": 0,
  "shieldingMultiplier": 1.0,
  "shieldingReduction": 0.0,
  "finalContribution": 5.0,
  "contribution": 5.0,
  "matchReason": "active block rule matched type=block id=minecraft:gold_block"
}
```

After Phase 4B, source rows may include vanilla container block entity inventory sources:

```json
{
  "type": "block_entity_inventory",
  "itemId": "minecraft:rotten_flesh",
  "blockId": "minecraft:chest",
  "slot": "container.0",
  "count": 10,
  "containerPos": {
    "x": 11,
    "y": 64,
    "z": 10
  },
  "ruleStrength": 1.0,
  "ruleRadius": 2.0,
  "distance": 1.6,
  "respectsShielding": true,
  "rawContribution": 10.0,
  "shielding": "clear",
  "shieldingBlocksHit": 0,
  "shieldingMultiplier": 1.0,
  "shieldingReduction": 0.0,
  "finalContribution": 10.0,
  "contribution": 10.0,
  "matchReason": "vanilla Container slot matched active item rule type=item id=minecraft:rotten_flesh"
}
```

After Phase 4C, source rows may include block item handler sources:

```json
{
  "type": "block_item_handler",
  "itemId": "minecraft:rotten_flesh",
  "blockId": "example:item_handler_block",
  "slot": "item_handler.0",
  "count": 10,
  "position": {
    "x": 12,
    "y": 64,
    "z": 10
  },
  "capabilityContext": "unsided",
  "ruleStrength": 1.0,
  "ruleRadius": 2.0,
  "distance": 1.4,
  "respectsShielding": true,
  "rawContribution": 10.0,
  "shielding": "clear",
  "shieldingBlocksHit": 0,
  "shieldingMultiplier": 1.0,
  "shieldingReduction": 0.0,
  "finalContribution": 10.0,
  "contribution": 10.0,
  "matchReason": "NeoForge ItemHandler block capability matched active item rule type=item id=minecraft:rotten_flesh"
}
```

After Phase 4D, source rows may include block fluid handler sources:

```json
{
  "type": "block_fluid_handler",
  "fluidId": "minecraft:water",
  "blockId": "example:tank",
  "tank": "fluid_handler.0",
  "amountMb": 1000,
  "position": {
    "x": 12,
    "y": 64,
    "z": 10
  },
  "capabilityContext": "unsided",
  "ruleStrength": 1.0,
  "ruleRadius": 2.0,
  "distance": 1.4,
  "respectsShielding": true,
  "rawContribution": 1.0,
  "shielding": "clear",
  "shieldingBlocksHit": 0,
  "shieldingMultiplier": 1.0,
  "shieldingReduction": 0.0,
  "finalContribution": 1.0,
  "contribution": 1.0,
  "matchReason": "NeoForge FluidHandler block capability matched active fluid rule type=fluid id=minecraft:water"
}
```

Dump snapshot output is bounded to 20 source rows. If rules are reloaded and the checksum changes, an old snapshot reports `stale=true`.

## Source scan summary
After `/radworks sources` or `/radworks exposure`, `/radworks dump` includes `sourceScanSummary`.

Example:

```json
{
  "createdAt": "ISO-8601",
  "inventoryStacksChecked": 37,
  "inventoryMatches": 1,
  "blockPositionsChecked": 2197,
  "blockMatches": 1,
  "blockEntitiesChecked": 125,
  "containerBlockEntitiesFound": 1,
  "containerSlotsChecked": 27,
  "containerMatches": 1,
  "itemHandlerPositionsChecked": 125,
  "itemHandlersFound": 0,
  "itemHandlerSlotsChecked": 0,
  "itemHandlerMatches": 0,
  "skippedContainerBlockEntitiesForItemHandler": 1,
  "fluidHandlerPositionsChecked": 125,
  "fluidHandlersFound": 0,
  "fluidTanksChecked": 0,
  "fluidMatches": 0,
  "shieldingSourcesChecked": 3,
  "shieldingSourcesApplicable": 2,
  "shieldingSamplesChecked": 12,
  "shieldingBlocksHit": 0,
  "shieldingSourcesReduced": 0,
  "sourcesShown": 3,
  "sourcesOmitted": 0,
  "diagnosticNotes": [
    "itemHandlerScan skipped vanilla Container block entities to avoid double counting with block_entity_inventory sources"
  ]
}
```

How to read it:
- `*Checked` fields show how much of each provider's search space was inspected by the last command.
- `*Matches` fields show how many rows became radiation sources.
- `skippedContainerBlockEntitiesForItemHandler` should increase when vanilla `Container` block entities are near the player; those are handled by `block_entity_inventory`, not `block_item_handler`.
- `fluidMatches` only increases when a block fluid handler contains a fluid with an active `type=fluid` rule and the block is within that rule radius.
- `shieldingSourcesChecked` counts matched source rows that were passed through shielding diagnostics.
- `shieldingSourcesApplicable` excludes inventory sources and sources whose rule does not respect shielding.
- `shieldingBlocksHit` counts unique shielding block positions hit across applicable source lines.
- `shieldingSourcesReduced` counts source rows whose final contribution was reduced.
- `sourcesShown` and `sourcesOmitted` mirror the bounded chat output.

For source discovery bugs, send Codex:
- the full `/radworks dump` JSON;
- `/radworks sources` and `/radworks exposure` chat output;
- player position, nearby relevant blocks, and inventory/container contents;
- expected source row and actual source row or missing row.

## Recent warnings
`recentWarnings` is an in-memory bounded ring buffer with a maximum of 100 entries.

Warnings include real diagnostics issues such as:
- validation warnings/errors after `/radworks validate`;
- command misuse, such as using player-only commands from console;
- dump write failures;
- unexpected diagnostic exceptions if they occur.

Entries have:

```json
{
  "createdAt": "ISO-8601",
  "category": "COMMAND_MISUSE",
  "source": "sources",
  "message": "player required; use /radworks sources <player>"
}
```

Normal successful commands do not add warnings.

## Performance stats
`performance` contains command diagnostics timings only. It is not TPS and not general server performance.

Measured operations:
- `validate`
- `exposure`
- `sources`
- `blockScan`
- `blockEntityInventoryScan`
- `itemHandlerScan`
- `fluidHandlerScan`
- `shielding`
- `dump`

Fields:
- `lastMillis`
- `count`
- `averageMillis`
- `maxMillis`

## Not available in Phase 0
- Gameplay use of radiation rules
- Gameplay exposure effects

## Phase 1 rule format
```json
{
  "type": "item",
  "id": "minecraft:rotten_flesh",
  "strength": 1.0,
  "radius": 2.0,
  "respectsShielding": true,
  "enabled": true,
  "comment": "Temporary Phase 1 dev-only smoke rule"
}
```

Allowed `type` values:
- `item`
- `block`
- `fluid`

Phase 1 does not scan the world or apply these rules to gameplay.

Phase 2 uses item rules only for `/radworks exposure` diagnostics.

Phase 3 uses the same item-rule inventory provider for `/radworks sources` diagnostics.

Phase 4A adds static block source diagnostics.

Phase 4B adds vanilla `Container` block entity item source diagnostics.

Phase 4C adds NeoForge block `IItemHandler` capability item source diagnostics.

Phase 4D adds NeoForge block `IFluidHandler` capability fluid source diagnostics.

Phase 5A adds diagnostic-only shielding calculation for positioned sources.
