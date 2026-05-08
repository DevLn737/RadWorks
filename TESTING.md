# Testing

## Automated checks
Run:

```bash
./gradlew build
```

Optional smoke runs:

```bash
./gradlew runClient
./gradlew runServer
```

If `runServer` stops because the Minecraft EULA is not accepted, this is expected for a fresh server run directory. Open the generated `eula.txt`, accept the EULA if appropriate, and run `./gradlew runServer` again.

## Manual Minecraft test - `/radworks version`
1. Start the client:

   ```bash
   ./gradlew runClient
   ```

2. Create or open a test world with commands enabled.
3. Run:

   ```text
   /radworks version
   ```

4. Confirm the chat output includes:
   - `RadWorks 0.1.0`
   - `Minecraft: 1.21.1`
   - `NeoForge: 21.1.228`
   - Java runtime version
   - `Integrations: create=absent, aeronautics=absent`
   - Phase 0 before a world loads: `Rules: not loaded`
   - Phase 1 inside a world: `Rules: loaded, checksum=<short>, mode=lenient/dev`

## Manual Minecraft test - `/radworks dump`
1. In the same world, run:

   ```text
   /radworks dump
   ```

2. Confirm chat reports a created JSON path.
3. Check the active Minecraft instance directory for:

   ```text
   radworks_dumps/radworks-dump-YYYYMMDD-HHMMSS-<player>.json
   ```

4. Open the JSON and confirm:
   - `schemaVersion` is `1`;
   - `player` contains your name, UUID and position;
   - Phase 1: `rules.loaded` is `true`;
   - Phase 1: `rules.validationMode` is `lenient/dev`;
   - Phase 1: `rules.itemRules` is `1`;
   - Phase 4A: `rules.blockRules` is `1`;
   - Phase 1: `rules.checksum` is a full SHA-256 string;
   - Create and Aeronautics integrations are disabled;
   - performance counters are present.
   - Phase 4C: `performance.itemHandlerScan` is present after the updated mod is loaded.

## Manual Minecraft test - clean `/radworks validate`
1. Start the client:

   ```bash
   ./gradlew runClient
   ```

2. Open a test world with commands enabled.
3. Run:

   ```text
   /radworks validate
   ```

4. Confirm the output includes:
   - `mode=lenient/dev`;
   - `loaded=2`;
   - `enabled=2`;
   - `disabled=0`;
   - `errors=0`;
   - a short checksum;
   - no gameplay effects are applied.

## Manual Minecraft test - `/reload`
1. In the test world, run:

   ```text
   /reload
   ```

2. After reload completes, run:

   ```text
   /radworks validate
   ```

3. Confirm the clean validation output still reports the `minecraft:rotten_flesh` smoke rule and does not crash.

## Manual Minecraft test - dump rules section
1. Run:

   ```text
   /radworks dump
   ```

2. Open the generated JSON under `radworks_dumps/`.
3. Confirm the `rules` object contains:
   - `loaded: true`;
   - `validationMode: "lenient/dev"`;
   - `itemRules: 1`;
   - `blockRules: 1`;
   - `fluidRules: 0`;
   - `disabledRules: 0`;
   - `errors: []`;
   - `warnings: []`;
   - `infos: []` or only expected disabled-rule info if a local test datapack is active.
   - Before running `/radworks exposure`, `lastExposureSnapshot` is `null`.

## Manual Minecraft test - `/radworks exposure` with no rotten flesh
1. Remove all `minecraft:rotten_flesh` from main inventory and offhand.
2. Run:

   ```text
   /radworks exposure
   ```

3. Confirm output includes:
   - `totalExposure=0.0`;
   - `matchedSources=0`;
   - `diagnostic only, no gameplay effect`.

## Manual Minecraft test - `/radworks exposure` with 1 rotten flesh
1. Put exactly 1 `minecraft:rotten_flesh` in main inventory.
2. Run:

   ```text
   /radworks exposure
   ```

3. Confirm output includes:
   - `totalExposure=1.0`;
   - `matchedSources=1`;
   - `minecraft:rotten_flesh`;
   - the slot name;
   - `count=1`;
   - `ruleStrength=1.0`;
   - `ruleRadius=2.0`;
   - `contribution=1.0`;
   - `shielding=not_applied`.

## Manual Minecraft test - scaling 1 vs 10 rotten flesh
1. Change the same stack to exactly 10 `minecraft:rotten_flesh`.
2. Run:

   ```text
   /radworks exposure
   ```

3. Confirm output includes:
   - `totalExposure=10.0`;
   - `count=10`;
   - `contribution=10.0`.

## Manual Minecraft test - offhand rotten flesh
1. Remove rotten flesh from main inventory.
2. Put exactly 1 `minecraft:rotten_flesh` in offhand.
3. Run:

   ```text
   /radworks exposure
   ```

4. Confirm output includes:
   - `totalExposure=1.0`;
   - `slot=offhand.0`;
   - `count=1`.

## Manual Minecraft test - `/radworks exposure <player>`
1. In single-player, run:

   ```text
   /radworks exposure Dev
   ```

   Replace `Dev` with the actual player name shown by the client.

2. Confirm it reports that player.
3. From server console, `/radworks exposure` without a player should fail with:

   ```text
   player required; use /radworks exposure <player>
   ```

## Manual Minecraft test - `/radworks debug`
1. Run:

   ```text
   /radworks debug status
   ```

2. Confirm it reports `disabled` by default.
3. As an operator or in a cheats-enabled world, run:

   ```text
   /radworks debug on
   /radworks debug status
   /radworks debug off
   /radworks debug status
   ```

4. Confirm status changes from enabled to disabled.

## Manual Minecraft test - `/radworks sources` without rotten flesh
1. Remove all `minecraft:rotten_flesh` from main inventory and offhand.
2. Make sure there is no `minecraft:gold_block` within 6 blocks of the player.
3. Make sure there is no nearby chest or barrel containing `minecraft:rotten_flesh`.
4. Run:

   ```text
   /radworks sources
   ```

5. Confirm output includes:
   - `matchedSources=0`;
   - `scope=player_inventory+static_blocks+vanilla_containers+block_item_handlers`;
   - no container/entity/fluid source rows.

## Manual Minecraft test - `/radworks sources` with 10 rotten flesh
1. Put exactly 10 `minecraft:rotten_flesh` in main inventory.
2. Run:

   ```text
   /radworks sources
   ```

3. Confirm output includes:
   - `matchedSources=1`;
   - `type=player_inventory`;
   - `itemId=minecraft:rotten_flesh`;
   - slot name;
   - `count=10`;
   - `ruleStrength=1.0`;
   - `ruleRadius=2.0`;
   - `contribution=10.0`;
   - `reason=active item rule matched type=item id=minecraft:rotten_flesh`.

## Manual Minecraft test - `/radworks sources` with no gold block nearby
1. Remove or move away from nearby `minecraft:gold_block` blocks.
2. Run:

   ```text
   /radworks sources
   ```

3. Confirm no row contains:
   - `type=block`;
   - `blockId=minecraft:gold_block`.

## Manual Minecraft test - `/radworks sources` with nearby gold block
1. Place one `minecraft:gold_block` within 6 blocks of the player.
2. Run:

   ```text
   /radworks sources
   ```

3. Confirm output includes:
   - `type=block`;
   - `blockId=minecraft:gold_block`;
   - `position=`;
   - `distance=`;
   - `ruleRadius=6.0`;
   - `ruleStrength=5.0`;
   - `contribution=5.0`;
   - `reason=active block rule matched type=block id=minecraft:gold_block`.

## Manual Minecraft test - gold block outside radius
1. Move more than 6 blocks away from the test `minecraft:gold_block`.
2. Run:

   ```text
   /radworks sources
   ```

3. Confirm the gold block source disappears.

## Manual Minecraft test - combined inventory and block exposure
1. Put exactly 10 `minecraft:rotten_flesh` in main inventory.
2. Stand within 6 blocks of one `minecraft:gold_block`.
3. Run:

   ```text
   /radworks exposure
   ```

4. Confirm output includes:
   - `totalExposure=15.0`;
   - one `type=player_inventory` row for `itemId=minecraft:rotten_flesh` with `contribution=10.0`;
   - one `type=block` row for `blockId=minecraft:gold_block` with `contribution=5.0`;
   - no damage, effects, hunger/exhaustion, particles, sounds or ticking accumulation.

## Manual Minecraft test - no vanilla container nearby
1. Remove nearby chests/barrels or make sure they contain no `minecraft:rotten_flesh`.
2. Run:

   ```text
   /radworks sources
   ```

3. Confirm no row contains:
   - `type=block_entity_inventory`;
   - `containerPos=`.

## Manual Minecraft test - empty chest nearby
1. Place an empty `minecraft:chest` or `minecraft:barrel` within 2 blocks of the player.
2. Run:

   ```text
   /radworks sources
   ```

3. Confirm there is no `type=block_entity_inventory` row.

## Manual Minecraft test - chest with rotten flesh
1. Place a `minecraft:chest` or `minecraft:barrel` within 2 blocks of the player.
2. Put exactly 10 `minecraft:rotten_flesh` in one container slot.
3. Run:

   ```text
   /radworks sources
   ```

4. Confirm output includes:
   - `type=block_entity_inventory`;
   - `blockId=minecraft:chest` or `blockId=minecraft:barrel`;
   - `containerPos=`;
   - `slot=container.`;
   - `itemId=minecraft:rotten_flesh`;
   - `count=10`;
   - `ruleStrength=1.0`;
   - `ruleRadius=2.0`;
   - `distance=`;
   - `contribution=10.0`;
   - `reason=vanilla Container slot matched active item rule type=item id=minecraft:rotten_flesh`.

## Manual Minecraft test - combined inventory, block and container exposure
1. Put exactly 10 `minecraft:rotten_flesh` in player inventory.
2. Stand within 6 blocks of one `minecraft:gold_block`.
3. Place a `minecraft:chest` or `minecraft:barrel` within 2 blocks of the player.
4. Put exactly 10 `minecraft:rotten_flesh` in one container slot.
5. Run:

   ```text
   /radworks exposure
   ```

6. Confirm output includes:
   - `totalExposure=25.0`;
   - one `type=player_inventory` row with `contribution=10.0`;
   - one `type=block` row with `contribution=5.0`;
   - one `type=block_entity_inventory` row with `contribution=10.0`.

## Manual Minecraft test - Phase 4C double-counting guard
1. Put exactly 10 `minecraft:rotten_flesh` in player inventory.
2. Stand within 6 blocks of one `minecraft:gold_block`.
3. Place a `minecraft:chest` or `minecraft:barrel` within 2 blocks of the player.
4. Put exactly 10 `minecraft:rotten_flesh` in one container slot.
5. Run:

   ```text
   /radworks exposure
   /radworks sources
   ```

6. Confirm output includes:
   - `totalExposure=25.0`, not `35.0`;
   - one `type=block_entity_inventory` row with `contribution=10.0`;
   - no extra `type=block_item_handler` row for the same vanilla chest/barrel;
   - a note that vanilla `Container` block entities are skipped by `itemHandlerScan` to avoid double counting.

## Manual Minecraft test - Phase 4C item handler performance field
1. Run:

   ```text
   /radworks sources
   /radworks dump
   ```

2. Open the generated JSON under `radworks_dumps/`.
3. Confirm the `performance` object contains:
   - `itemHandlerScan`;
   - `lastMillis`;
   - `count`;
   - `averageMillis`;
   - `maxMillis`.

## Manual Minecraft test - Phase 4C.1 source scan summary
1. Run:

   ```text
   /radworks sources
   /radworks dump
   ```

2. Open the generated JSON under `radworks_dumps/`.
3. Confirm `sourceScanSummary` exists and contains:
   - `inventoryStacksChecked`;
   - `inventoryMatches`;
   - `blockPositionsChecked`;
   - `blockMatches`;
   - `blockEntitiesChecked`;
   - `containerBlockEntitiesFound`;
   - `containerSlotsChecked`;
   - `containerMatches`;
   - `itemHandlerPositionsChecked`;
   - `itemHandlersFound`;
   - `itemHandlerSlotsChecked`;
   - `itemHandlerMatches`;
   - `skippedContainerBlockEntitiesForItemHandler`;
   - `sourcesShown`;
   - `sourcesOmitted`.

4. If a vanilla chest/barrel is in scan range, confirm `skippedContainerBlockEntitiesForItemHandler` is greater than `0` and `diagnosticNotes` explains that `itemHandlerScan` skipped vanilla `Container` block entities to avoid double counting.

## What to send Codex for source discovery bugs
When a source is missing, duplicated, or has the wrong contribution, send:
- the generated `/radworks dump` JSON;
- the exact `/radworks sources` and `/radworks exposure` chat output;
- where the player stood and what was nearby;
- contents of relevant inventory/container slots;
- whether `/reload` was run after changing datapacks;
- expected result versus actual result.

## Manual Minecraft test - Phase 4C optional modded item handler block
If the dev instance has a non-vanilla block that exposes `Capabilities.ItemHandler.BLOCK` and is not a vanilla `Container`:

1. Place that block within 2 blocks of the player.
2. Put exactly 10 `minecraft:rotten_flesh` in one item handler slot.
3. Run:

   ```text
   /radworks sources
   ```

4. Confirm output may include:
   - `type=block_item_handler`;
   - `blockId=`;
   - `position=`;
   - `capabilityContext=unsided` or a side name;
   - `slot=item_handler.`;
   - `itemId=minecraft:rotten_flesh`;
   - `count=10`;
   - `ruleStrength=1.0`;
   - `ruleRadius=2.0`;
   - `contribution=10.0`.

If no such block exists, this is expected for the clean dev environment and is not a Phase 4C failure.

## Manual Minecraft test - container outside item radius
1. Move more than 2 blocks away from the test chest or barrel that contains 10 `minecraft:rotten_flesh`.
2. Run:

   ```text
   /radworks sources
   ```

3. Confirm the `block_entity_inventory` source disappears.

## Manual Minecraft test - exposure dump snapshot
1. Before running `/radworks exposure`, run:

   ```text
   /radworks dump
   ```

2. Confirm `lastExposureSnapshot` is `null`.
3. Run:

   ```text
   /radworks exposure
   /radworks dump
   ```

4. Confirm `lastExposureSnapshot` exists and includes:
   - `rulesChecksum`;
   - `createdAt`;
   - `playerName`;
   - `playerUuid`;
   - `totalExposure`;
   - `matchedStacks`;
   - `sources`;
   - `sourcesShown`;
   - `sourcesOmitted`;
   - `notes`;
   - `stale`.

## Manual Minecraft test - Phase 4A dump source rows
1. Put exactly 10 `minecraft:rotten_flesh` in main inventory.
2. Stand within 6 blocks of one `minecraft:gold_block`.
3. Run:

   ```text
   /radworks exposure
   /radworks dump
   ```

4. Open the generated JSON under `radworks_dumps/`.
5. Confirm `lastExposureSnapshot.sources` contains:
   - one inventory row with `type: "player_inventory"` and `itemId: "minecraft:rotten_flesh"`;
   - one block row with `type: "block"`, `blockId: "minecraft:gold_block"` and a `position` object;
   - `sourcesShown`;
   - `sourcesOmitted`.

## Manual Minecraft test - Phase 4B dump source rows
1. Put exactly 10 `minecraft:rotten_flesh` in player inventory.
2. Stand within 6 blocks of one `minecraft:gold_block`.
3. Place a `minecraft:chest` or `minecraft:barrel` within 2 blocks of the player.
4. Put exactly 10 `minecraft:rotten_flesh` in one container slot.
5. Run:

   ```text
   /radworks exposure
   /radworks dump
   ```

6. Open the generated JSON under `radworks_dumps/`.
7. Confirm `lastExposureSnapshot.sources` contains:
   - one inventory row with `type: "player_inventory"` and `itemId: "minecraft:rotten_flesh"`;
   - one block row with `type: "block"` and `blockId: "minecraft:gold_block"`;
   - one container row with `type: "block_entity_inventory"`, `containerPos`, `blockId`, `itemId: "minecraft:rotten_flesh"`, `slot`, `count` and `contribution`;
   - `sourcesShown`;
   - `sourcesOmitted`.

## Manual Minecraft test - Phase 4C dump source rows
1. Run:

   ```text
   /radworks sources
   /radworks dump
   ```

2. Open the generated JSON under `radworks_dumps/`.
3. Confirm:
   - `performance.itemHandlerScan` exists;
   - if a non-vanilla item handler source exists, `lastExposureSnapshot.sources` after `/radworks exposure` may include `type: "block_item_handler"`, `position`, `capabilityContext`, `slot`, `itemId`, `count` and `contribution`;
   - vanilla chest/barrel contents remain represented as `type: "block_entity_inventory"`, not double-counted as `block_item_handler`.

## Manual Minecraft test - Phase 3 dump diagnostics
1. Run:

   ```text
   /radworks debug status
   /radworks sources
   /radworks exposure
   /radworks validate
   /radworks dump
   ```

2. Open the generated JSON and confirm it contains:
   - `debug.enabled`;
   - `performance.validate`;
   - `performance.exposure`;
   - `performance.sources`;
   - `performance.dump`;
   - `recentWarnings`;
   - existing `lastExposureSnapshot`.

3. Confirm performance fields use command diagnostics names:
   - `lastMillis`;
   - `count`;
   - `averageMillis`;
   - `maxMillis`.

4. Confirm no TPS/server performance wording and no block/world/source scan snapshot.

## Manual Minecraft test - Phase 2 reload
1. Run:

   ```text
   /reload
   /radworks validate
   /radworks exposure
   ```

2. Confirm validation remains clean and exposure still detects `minecraft:rotten_flesh`.
3. If a datapack changes the rules checksum, a previous dump snapshot should show `stale=true` until `/radworks exposure` is run again.

## Manual Minecraft test - external broken-rule datapack
Do not put malformed JSON in `src/main/resources`.

1. In a test world, create a temporary datapack under:

   ```text
   saves/<world>/datapacks/radworks_broken_rule_test/
   ```

2. Add `pack.mcmeta` for Minecraft `1.21.1`:

   ```json
   {
     "pack": {
       "pack_format": 48,
       "description": "Temporary RadWorks broken rule test"
     }
   }
   ```

3. Add an invalid-value rule:

   ```text
   data/radworks/radiation_rules/broken_radius.json
   ```

   ```json
   {
     "type": "item",
     "id": "minecraft:rotten_flesh",
     "strength": 1.0,
     "radius": 0,
     "respectsShielding": true,
     "enabled": true,
     "comment": "Temporary broken-rule test"
   }
   ```

4. Run:

   ```text
   /reload
   /radworks validate
   ```

5. Confirm validation reports `INVALID_RULE_VALUE` and the game does not crash.
6. Optional unknown-ID test: add a valid rule with `id: "missing_mod:missing_item"` and confirm `UNKNOWN_REGISTRY_ID` appears as a warning in `lenient/dev`.
7. Remove the entire `radworks_broken_rule_test` datapack after testing.
8. Run `/reload` and `/radworks validate` again to return to the clean state.

## Server console dump check
From a dedicated server console, run:

```text
radworks dump
```

The command should create a dump with `player` set to `null` and a filename ending in `server.json`.

## Phase 0 acceptance
- The project builds.
- The mod loads.
- `/radworks version` works.
- `/radworks dump` creates readable JSON.
- No radiation gameplay exists.

## Phase 1 acceptance
- The project builds.
- `/radworks validate` works after opening a world.
- `/radworks validate` still works after `/reload`.
- `/radworks dump` includes the rules validation summary.
- Broken external datapack rules report validation issues without crashing.
- No exposure calculation, inventory/world/entity scanning, shielding, effects, damage or gameplay radiation exists.

## Phase 2 acceptance
- The project builds.
- `/radworks exposure` works for the executing player.
- `/radworks exposure <player>` works for an online server player.
- No rotten flesh gives `totalExposure=0.0`.
- 1 rotten flesh gives `totalExposure=1.0`.
- 10 rotten flesh gives `totalExposure=10.0`.
- Offhand rotten flesh is counted.
- `/radworks dump` has `lastExposureSnapshot=null` before exposure and a bounded snapshot after exposure.
- `/reload`, `/radworks validate`, and `/radworks exposure` work together.
- No damage, effects, hunger/exhaustion, particles, sounds, ticking accumulation, shielding, fluids or world/container/entity scan exists.

## Phase 3 acceptance
- The project builds.
- `/radworks debug status/on/off` works with expected permissions.
- `/radworks sources` reports only player inventory item sources.
- `/radworks sources` with no rotten flesh reports 0 sources.
- `/radworks sources` with 10 rotten flesh reports one `player_inventory` source and match reason.
- `/radworks exposure` still reports `totalExposure=10.0` for 10 rotten flesh.
- `/radworks dump` includes debug state, command diagnostics performance stats, recent warnings and existing exposure snapshot.
- No damage, effects, hunger/exhaustion, particles, sounds, ticking accumulation, shielding, fluids, block entity, container, tank, dropped item or entity scan exists.

## Phase 4A acceptance
- The project builds.
- `/radworks validate` reports the dev-only item rule and dev-only block rule.
- `/radworks sources` reports no block source when no `minecraft:gold_block` is nearby.
- `/radworks sources` reports a `type=block` source when `minecraft:gold_block` is within radius.
- Moving outside the rule radius removes the block source.
- 10 `minecraft:rotten_flesh` plus one nearby `minecraft:gold_block` gives `totalExposure=15.0`.
- `/radworks dump` after exposure includes bounded inventory and block source rows.
- No damage, effects, hunger/exhaustion, particles, sounds, ticking accumulation, shielding, fluids, block entity, container, tank, dropped item, entity, Create, Aeronautics or KubeJS logic exists.

## Phase 4B acceptance
- The project builds.
- `/radworks sources` reports no `block_entity_inventory` source when no chest/barrel is nearby.
- `/radworks sources` reports no `block_entity_inventory` source for an empty nearby chest/barrel.
- `/radworks sources` reports a `block_entity_inventory` source when a nearby chest/barrel contains 10 `minecraft:rotten_flesh`.
- Moving outside the item rule radius removes the container contribution.
- 10 `minecraft:rotten_flesh` in inventory plus one nearby `minecraft:gold_block` plus 10 `minecraft:rotten_flesh` in a nearby chest/barrel gives `totalExposure=25.0`.
- `/radworks dump` after exposure includes bounded inventory, block and block entity inventory source rows.
- No NeoForge capabilities, `IItemHandler`, modded capability containers, nested containers, shulker contents, backpacks, fluids, tanks, entities, dropped items, shielding, damage/effects, ticking accumulation, cache, Create, Aeronautics or KubeJS logic exists.

## Phase 4C acceptance
- The project builds.
- Existing Phase 4B scenario remains `totalExposure=25.0`, not `35.0`.
- Vanilla chest/barrel sources are not double-counted through `IItemHandler`.
- `/radworks sources` explains that vanilla `Container` block entities are skipped by `itemHandlerScan` to avoid double counting.
- `/radworks dump` includes `performance.itemHandlerScan`.
- Optional non-vanilla block item handler sources can be shown as `type=block_item_handler` when such a block exists in the instance.
- No entity capabilities, item stack capabilities, nested containers, Curios/Trinkets, fluids/tanks, `IFluidHandler`, energy, shielding, damage/effects, ticking accumulation, cache, Create, Aeronautics or KubeJS logic exists.

## Phase 4C.1 acceptance
- The project builds.
- `/radworks dump` includes `sourceScanSummary` after `/radworks sources` or `/radworks exposure`.
- `sourceScanSummary` includes all requested checked/match counters and output bounds.
- `sourceScanSummary.diagnosticNotes` explains container skipping when item handler scan skips vanilla `Container` block entities.
- No formulas, source mechanics, gameplay effects, ticking, cache, fluids, shielding, Create, Aeronautics or KubeJS logic changes.
