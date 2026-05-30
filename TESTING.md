# Testing

## Local testing policy (Phase 6T)
- Repeated manual local Minecraft testing is no longer the default.
- For core logic changes, automated tests are required when practical.
- Manual local Minecraft testing is now reserved for:
  - command/UX sanity checks;
  - scenarios that cannot be covered reliably by local automation.
- External modpack and optional dependency behavior remains external-tester responsibility.

## Automated checks
Run:

```bash
./gradlew test
./gradlew build
```

## Extra Stage 3C spec-driven automated hardening
- Added blocker/high contract suites from `RADWORKS_TEST_MATRIX.md`:
  - `PlayerInventorySourceProviderContractTest`
  - `BlockSourceProviderContractTest`
  - `BlockEntityInventorySourceProviderContractTest`
  - `BlockItemHandlerSourceProviderContractTest`
  - `BlockFluidHandlerSourceProviderContractTest`
  - `ExposureEnginePipelineContractTest`
  - `DiagnosticsDumpSchemaContractTest`
  - `CommandOutputContractTest`
  - `OverrideSelectorCarrierBlockSemanticsTest`
  - `ConfigExposureThresholdClampIntentTest`
- Override test split started (without mass deletions):
  - `SourceOverrideExcludeContractTest`
  - `SourceOverrideContainContractTest`
  - `SourceOverrideForceContractTest`
  - `SourceOverridePipelineOrderContractTest`
- Mismatch tracking preserved as tests (no runtime mutation in this stage):
  - `SPEC_CODE_MISMATCH_CANDIDATE`: exposure threshold clamp intent.
  - `SPEC_CODE_MISMATCH_CANDIDATE`: carrierBlockId selector semantics.

## Beta 0.6.5 final override regression workflow
Run in order:

```bash
./gradlew test
./gradlew build
```

Expected:
- green gates with no gameplay behavior changes in this closure step;
- override pipeline remains `exclude -> contain -> force(observed candidates only) -> contain(forced) -> shielding`.

Recommended manual review artifacts (from external tester only when needed):
- `/radworks validate`
- `/radworks sources`
- `/radworks exposure`
- `/radworks dump`

## Beta 0.6.4 automated checks (force application on observed candidates)
- `SourceOverrideRulesLoaderTest`:
  - valid force rule with runtime fields loads;
  - missing `forceStrength` / `forceRadius` / `forceUnitMode` fails validation;
  - force rule without concrete selector fails validation.
- `SourceOverrideEngineTest`:
  - force creates source from observed item/block candidates when normal source is absent;
  - existing source identity prevents forced duplicate;
  - exclude identity blocks force;
  - forced rows can be contained by contain rules;
  - force-disabled config path skips application.
- Order/behavior checks:
  - pipeline uses `exclude -> contain -> force -> contain(forced) -> shielding`;
  - forced positioned rows remain shieldable;
  - forced non-positioned rows are `shielding=not_applicable`.

Behavior note:
- 0.6.4 applies `exclude` + `contain` + `force`.
- force uses observed candidates only (no new scanners/discovery paths).

### Beta 0.6 override rule examples (for retest)
Exclude by `itemId`:
```json
{
  "id": "radworks:exclude_uranium_item",
  "enabled": true,
  "type": "exclude",
  "selectors": { "itemId": "createnuclear:raw_uranium" }
}
```

Contain by `carrierBlockId` with scale:
```json
{
  "id": "radworks:contain_chest_scale_half",
  "enabled": true,
  "type": "contain",
  "selectors": { "carrierBlockId": "minecraft:chest", "sourceType": "block_entity_inventory" },
  "mode": "scale",
  "multiplier": 0.5
}
```

Force by `blockId`:
```json
{
  "id": "radworks:force_stone_block",
  "enabled": true,
  "type": "force",
  "selectors": { "blockId": "minecraft:stone", "sourceType": "block" },
  "forceStrength": 3.0,
  "forceRadius": 4.0,
  "forceUnitMode": "block",
  "forceRespectsShielding": true
}
```

## Beta 0.6.3 automated checks (contain application only)
- `SourceOverrideEngineTest`:
  - contain `suppress` by nested `containerItemId` suppresses matching nested rows;
  - contain `scale` by `carrierEntityType` scales matching entity inventory rows;
  - direct non-nested items do not match `containerItemId` containment;
  - precedence/conflicts are deterministic:
    - `exclude` wins over `contain`;
    - `suppress` wins over `scale`;
    - scale conflicts use lowest multiplier;
  - containment disable paths keep rows unchanged;
  - force rules remain not applied.
- Shielding-order contract checks:
  - scale is applied before shielding input;
  - suppress rows are excluded from shielding input.
- `SourceScanSummary` override counters:
  - `sourcesContainedByOverride`
  - `sourcesAfterContainment`

Behavior note:
- 0.6.3 applied `exclude` + `contain` before force phase.
- containment changes contribution only (radius model unchanged).

## Beta 0.6.1 automated checks (source override diagnostics-only)
- `SourceOverrideRulesLoaderTest`:
  - valid `exclude` / `contain` / `force` rules load;
  - disabled rule counters are correct;
  - invalid rule type/selector/multiplier produce validation issues;
  - missing optional mod target is non-fatal and reported diagnostically.
- `RulesDataFilesSmokeTest`:
  - source override example file exists and stays disabled.
- Config tests:
  - source override toggles/caps are present and bounded.

Behavior note:
- 0.6.1 does not apply overrides to exposure/sources yet; validate/dump visibility only.

## Beta 0.6.2 automated checks (exclude application only)
- `SourceOverrideEngineTest`:
  - exclude by `sourceType`, `itemId`, `blockId`, `fluidId`, `containerItemId`, `carrierEntityType`;
  - non-matching/disabled rules do not apply;
  - config-disable path keeps sources unchanged;
  - excluded rows are separated from shielding input;
  - target-kind scoped exclusion works for living-target path;
  - contain/force rules remain not applied.
- `SourceScanSummary` override counters:
  - `sourcesExcludedByOverride`
  - `sourcesAfterOverrides`

Behavior note:
- 0.6.2 applies only `exclude`.
- `contain`/`force` remain schema/diagnostics-only.

## Beta 0.5 automated checks (nested containers)
- `NestedContainerExtractorTest`:
  - vanilla container-component extraction;
  - bundle-contents extraction;
  - depth-cap behavior;
  - bounded diagnostics emission contract.
- `NestedProviderRegressionAuditTest`:
  - nested disable path (`nestedContainersEnabled=false`) keeps direct item behavior;
  - no double-count in direct+nested covered aggregate scenarios;
  - structured nested source fields are present in row JSON;
  - nested diagnostics counters are present.
- `NestedProviderIntegrationAuditTest`:
  - provider wiring guards for player/block/item-handler/entity paths;
  - chat rows stay compact and avoid raw NBT output.
- `EntityCarrierExtractionTest` nested case:
  - dropped/container-like stack nested contents aggregate into entity-carried sources.
- Config defaults/bounds:
  - nested extraction toggles/caps are positive and bounded.
- Keep local gates:
  - `./gradlew test`
  - `./gradlew build`

## Beta 0.5 final external retest focus
- Shulker/bundle nested radioactive contents in:
  - player inventory;
  - chest/container inventory;
  - dropped item;
  - item frame;
  - chest boat/pack animal when convenient.
- Modded container items (Create toolbox / Sophisticated):
  - expected to work only if they expose supported vanilla components;
  - otherwise expected outcome is diagnostics (`nestedContainerDiagnostics`) without crash.

## Beta 0.4.5 final regression workflow
Run in order:

```bash
./gradlew test
./gradlew build
./gradlew runServer
```

`runServer` smoke interpretation:
- acceptable: server starts and waits with no client-only/classloading crash;
- acceptable: EULA-related stop in fresh server run directory;
- blocker: startup exception, client-only reference crash, registry/config crash during startup.

Final closure gate expectations:
- `ForbiddenClientImportsTest` green;
- config bounds/default tests green;
- world fluid tests green;
- living target tests green;
- shielding tests green.

## Beta 0.4.4 automated checks
- Target-aware shielding:
  - player shielding regression remains unchanged;
  - non-player living target shielding is applied for external positioned sources.
- Self-carried policy:
  - self-carried source for the same target remains `shielding=not_applicable`;
  - source reason includes `self_carried_source_not_shielded`.
- Config defaults/bounds:
  - `applyShieldingToLivingEntities=true` by default.
- Source scan summary:
  - `livingShieldingSourcesChecked`, `livingShieldingSourcesReduced`, `livingShieldingSamplesChecked` are present.
- Keep local gates:
  - `./gradlew test`
  - `./gradlew build`

## Beta 0.4.3 automated checks
- Living-target decision policy:
  - disabled mode -> skip;
  - missing selected effect -> skip;
  - below threshold -> skip;
  - above threshold -> apply intent.
- Living-target selection policy:
  - armor stand skipped by default;
  - max living targets cap enforced.
- Config defaults/bounds:
  - living target toggles are server-safe;
  - `maxLivingTargetsPerScan` and `livingTargetScanRadius` are positive and bounded.
- Keep local gates:
  - `./gradlew test`
  - `./gradlew build`

## Beta 0.4.2 dedicated server smoke
Run:

```bash
./gradlew runServer
```

Expected outcomes:
- acceptable: process stops due to Minecraft EULA not yet accepted in fresh run directory;
- acceptable: server starts and waits without client-only classloading crash.

Fail this smoke only on:
- startup exception;
- client-only reference/classloading crash;
- missing registry/config crash during startup.

## Beta 0.4.1 automated checks
- World fluid source resolution:
  - exact `createnuclear:uranium` rule match;
  - exact `createnuclear:flowing_uranium` rule match;
  - fallback `flowing_uranium -> uranium` when exact rule is missing.
- World fluid source row creation:
  - `world_fluid` source descriptor creation path for matched fluids;
  - unknown fluid produces no source and remains diagnostics-visible.
- World fluid diagnostics:
  - bounded skip/match samples include rule match mode and skip reason.

## Beta 0.4.1F automated checks
- World fluid connected-cluster aggregation:
  - one block => one cluster/source with `aggregateAmountMb=1000`;
  - connected 8-block cluster => one source with `aggregateAmountMb=8000`, `finalContribution=8.0` for `strength=1`;
  - connected 19-block cluster => `finalContribution=19.0` and larger `effectiveRadius` than 8-block cluster.
- Disconnected world fluid clusters:
  - produce separate aggregate `world_fluid` rows.
- Distance model:
  - cluster distance uses nearest fluid block (not centroid-only).

## Beta 0.4.1G automated checks
- Stable world-fluid discovery radius:
  - scan window uses `integrations.worldFluidClusterDiscoveryRadius`, not base rule radius.
- Connected fluid mass stability:
  - `3x3` pool => `contributingFluidBlocks=9`;
  - `4x3x3` cube => `contributingFluidBlocks=36`;
  - slight player movement around same mass keeps block count/effectiveRadius stable.
- Mixed still/flowing behavior:
  - `createnuclear:uranium` + `createnuclear:flowing_uranium` in one connected mass produce deterministic normalized cluster behavior.

## Post-Beta 4B automated checks
- Entity inventory adapter/extraction:
  - chest boat / chest raft type-path classification;
  - pack animal type-path classification;
  - container-backed inventory extraction and aggregation.
- Generic capability path:
  - synthetic `ItemStackHandler` extraction and aggregation.
- Dedupe contract:
  - dedupe key differs by logical inventory group.
- Diagnostics:
  - entity inventory counters exposed in `entityCarrierDiagnostics`.
- Keep local gates:
  - `./gradlew test`
  - `./gradlew build`

## Post-Beta 4A automated checks
- Entity carrier extraction and aggregation:
  - dropped/item-frame radioactive stack descriptor extraction;
  - player aura aggregate counts from inventory stacks;
  - self-player aura skip contract.
- Entity carrier diagnostics:
  - bounded skip sample behavior in `entityCarrierDiagnostics`.
- Keep local gates:
  - `./gradlew test`
  - `./gradlew build`

## Post-Beta 3 automated checks
- Create transient extraction parsing:
  - placard `Item`;
  - mechanical arm `HeldItem`;
  - fluid pipe `side.Flow.Fluid`;
  - malformed payload handling.
- Create transient aggregation:
  - contribution sum preserved;
  - dynamic radius behavior retained;
  - 1 item produces descriptor;
  - 1 mB fluid produces descriptor.
- Fluid rule resolution:
  - exact `createnuclear:flowing_uranium`;
  - fallback `flowing_x -> x` when exact missing.

Phase 6T minimum local gate for new logic phases:
- `./gradlew test` must execute real tests (not `NO-SOURCE`);
- `./gradlew build` must pass.

## Post-Beta 2 automated regression checks
- Dynamic radius model:
  - 1 unit keeps base-like radius;
  - 64 units yields larger radius than 1 unit;
  - cap enforcement works;
  - fluid amount-to-units conversion works.
- Aggregation:
  - multiple stacks/tanks aggregate into canonical rows;
  - contribution sum is preserved;
  - `contributingStacks` is correct.
- Handler diagnostics:
  - non-matching samples can report `outside_dynamic_radius`;
  - dynamic context fields (`baseRadius`, `effectiveRadius`, `distance`) are present in diagnostics samples.
- Existing effect strategy, shielding and rules smoke tests remain green.

## Post-Beta 2V radius visualization note
- Radius visualization is a debug UX tool for external modpack validation.
- It is not required for local manual default testing.
- External tester can use:
  - `/radworks radius show 10`
  - `/radworks radius status`
  - `/radworks radius clear`
  together with `/radworks sources` and `/radworks exposure`.

## Phase 6T automated regression suite
Automated local coverage now includes:
- effect preview policy thresholds and armor gate;
- shielding reduction math and multiplier cap;
- radiation rules JSON smoke for bundled dev files;
- shielding tag data contract for dev + optional entries;
- shielding diagnostics contract for optional candidates in clean-dev semantics.

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
   - Phase 4D: `performance.fluidHandlerScan` is present after the updated mod is loaded.
   - Phase 5A: `performance.shielding` is present after the updated mod is loaded.

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
   - `loaded=3`;
   - `enabled=3`;
   - `disabled=0`;
   - `errors=0`;
   - a short checksum;
   - Phase 4D: the temporary/dev-only `minecraft:water` fluid rule is valid;
   - Phase 5A: `#radworks:shielding_blocks` includes temporary/dev-only `minecraft:iron_block`;
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
   - `fluidRules: 1`;
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
   - `shielding=not_applicable`.

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
   - `scope=player_inventory+static_blocks+vanilla_containers+block_item_handlers+block_fluid_handlers`;
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
   - `fluidHandlerPositionsChecked`;
   - `fluidHandlersFound`;
   - `fluidTanksChecked`;
   - `fluidMatches`;
   - `shieldingSourcesChecked`;
   - `shieldingSourcesApplicable`;
   - `shieldingSamplesChecked`;
   - `shieldingBlocksHit`;
   - `shieldingSourcesReduced`;
   - `sourcesShown`;
   - `sourcesOmitted`.

4. If a vanilla chest/barrel is in scan range, confirm `skippedContainerBlockEntitiesForItemHandler` is greater than `0` and `diagnosticNotes` explains that `itemHandlerScan` skipped vanilla `Container` block entities to avoid double counting.

## What to send Codex for source discovery bugs
When a source is missing, duplicated, or has the wrong contribution, send:
- the generated `/radworks dump` JSON;
- the exact `/radworks sources` and `/radworks exposure` chat output;
- where the player stood and what was nearby;
- contents of relevant inventory/container slots or fluid tanks;
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

## Manual Minecraft test - Phase 4D fluid handler baseline
1. Run:

   ```text
   /radworks validate
   /radworks sources
   /radworks exposure
   /radworks dump
   ```

2. Confirm validation includes the dev-only `minecraft:water` fluid rule and has `errors=0`.
3. In a clean dev environment with no block fluid handler, confirm:
   - existing Phase 4B/4C scenario remains `totalExposure=25.0`;
   - no `type=block_fluid_handler` row appears;
   - `/radworks dump` contains `performance.fluidHandlerScan`;
   - `sourceScanSummary` contains `fluidHandlerPositionsChecked`, `fluidHandlersFound`, `fluidTanksChecked` and `fluidMatches`.

## Manual Minecraft test - Phase 4D optional modded fluid handler block
If the dev instance later has a block that exposes `Capabilities.FluidHandler.BLOCK`:

1. Place that block within 2 blocks of the player.
2. Put exactly 1000 mB of `minecraft:water` in one tank.
3. Run:

   ```text
   /radworks sources
   /radworks exposure
   ```

4. Confirm output may include:
   - `type=block_fluid_handler`;
   - `blockId=`;
   - `position=`;
   - `capabilityContext=unsided` or a side name;
   - `tank=fluid_handler.`;
   - `fluidId=minecraft:water`;
   - `amountMb=1000`;
   - `ruleStrength=1.0`;
   - `ruleRadius=2.0`;
   - `contribution=1.0`.

If no such block exists, this is expected for the clean dev environment and is not a Phase 4D failure. Do not add a custom tank block for this phase.

## Manual Minecraft test - Phase 5A inventory shielding is not applicable
1. Remove nearby `minecraft:gold_block`, chest/barrel radioactive contents and test tank sources.
2. Put exactly 10 `minecraft:rotten_flesh` in player inventory.
3. Run:

   ```text
   /radworks exposure
   ```

4. Confirm output includes:
   - `totalExposure=10.0`;
   - `type=player_inventory`;
   - `rawContribution=10.0`;
   - `shielding=not_applicable`;
   - `finalContribution=10.0`;
   - `contribution=10.0`.

## Manual Minecraft test - Phase 5A no shielding baseline
1. Put exactly 10 `minecraft:rotten_flesh` in player inventory.
2. Stand within 6 blocks of one `minecraft:gold_block`.
3. Make sure there is no `minecraft:iron_block` between the player and the gold block.
4. Run:

   ```text
   /radworks exposure
   ```

5. Confirm output includes:
   - `totalExposure=15.0`;
   - gold block row with `rawContribution=5.0`;
   - gold block row with `shielding=clear`;
   - gold block row with `shieldingBlocksHit=0`;
   - gold block row with `finalContribution=5.0`;
   - no damage, effects, hunger/exhaustion, particles, sounds or ticking accumulation.

## Manual Minecraft test - Phase 5A gold block shielding
1. Put exactly 10 `minecraft:rotten_flesh` in player inventory.
2. Place one `minecraft:gold_block` within 6 blocks of the player.
3. Place one `minecraft:iron_block` directly between the player and the gold block.
4. Run:

   ```text
   /radworks exposure
   /radworks sources
   ```

5. Confirm output includes:
   - `totalExposure=12.5`;
   - gold block row with `rawContribution=5.0`;
   - gold block row with `shielding=reduced`;
   - gold block row with `shieldingBlocksHit=1`;
   - gold block row with `shieldingMultiplier=0.5`;
   - gold block row with `shieldingReduction=2.5`;
   - gold block row with `finalContribution=2.5`;
   - gold block row with `contribution=2.5`.

## Manual Minecraft test - Phase 5A container shielding
1. Remove player inventory `minecraft:rotten_flesh`.
2. Place a `minecraft:chest` or `minecraft:barrel` within 2 blocks of the player.
3. Put exactly 10 `minecraft:rotten_flesh` in one container slot.
4. Place one `minecraft:iron_block` directly between the player and the container.
5. Run:

   ```text
   /radworks exposure
   ```

6. Confirm the container row uses `containerPos=` and includes:
   - `rawContribution=10.0`;
   - `shielding=reduced`;
   - `shieldingBlocksHit=1`;
   - `shieldingMultiplier=0.5`;
   - `finalContribution=5.0`;
   - `contribution=5.0`.

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

## Manual Minecraft test - Phase 4D dump source rows
1. Run:

   ```text
   /radworks sources
   /radworks dump
   ```

2. Open the generated JSON under `radworks_dumps/`.
3. Confirm:
   - `performance.fluidHandlerScan` exists;
   - `sourceScanSummary.fluidHandlerPositionsChecked` exists;
   - `sourceScanSummary.fluidHandlersFound` exists;
   - `sourceScanSummary.fluidTanksChecked` exists;
   - `sourceScanSummary.fluidMatches` exists;
   - if a block fluid handler source exists, `lastExposureSnapshot.sources` after `/radworks exposure` may include `type: "block_fluid_handler"`, `position`, `capabilityContext`, `tank`, `fluidId`, `amountMb` and `contribution`.

## Manual Minecraft test - Phase 5A dump shielding fields
1. Run:

   ```text
   /radworks exposure
   /radworks dump
   ```

2. Open the generated JSON under `radworks_dumps/`.
3. Confirm source rows include:
   - `respectsShielding`;
   - `rawContribution`;
   - `shielding`;
   - `shieldingBlocksHit`;
   - `shieldingMultiplier`;
   - `shieldingReduction`;
   - `finalContribution`;
   - `contribution`.
4. Confirm the `performance` object contains `shielding`.
5. Confirm `sourceScanSummary` contains:
   - `shieldingSourcesChecked`;
   - `shieldingSourcesApplicable`;
   - `shieldingSamplesChecked`;
   - `shieldingBlocksHit`;
   - `shieldingSourcesReduced`.

## Manual Minecraft test - Phase 5B shielding candidate validation
1. Start a clean dev world without TFMG/Create Nuclear installed.
2. Run:

   ```text
   /radworks validate
   ```

3. Confirm output includes:
   - `RadWorks shielding candidates: tag=#radworks:shielding_blocks`;
   - `minecraft:iron_block` reported as present;
   - TFMG/Create Nuclear optional candidates reported as INFO, not ERROR;
   - no crash and no hard dependency on optional mods.

## Manual Minecraft test - Phase 5B dump shielding section
1. Run:

   ```text
   /radworks dump
   ```

2. Open the generated JSON under `radworks_dumps/`.
3. Confirm the dump contains `shielding` with:
   - `tagId: "#radworks:shielding_blocks"`;
   - `tagPath: "src/main/resources/data/radworks/tags/block/shielding_blocks.json"`;
   - `devTestEntries` containing `minecraft:iron_block`;
   - `optionalEntries` containing `tfmg:raw_lead_block`, `tfmg:lead_block`, `tfmg:lead_ore`, `createnuclear:reinforced_glass`;
   - per-entry `status`;
   - notes mentioning `required:false`.

## Manual Minecraft test - Phase 5B external tester package
1. Build the jar:

   ```bash
   ./gradlew build
   ```

2. Send the external tester:
   - `build/libs/radworks-0.1.0.jar`;
   - `TESTER_HANDOFF.md`.

3. Ask the tester to run:
   - startup check;
   - `/radworks version`;
   - `/radworks validate`;
   - no-shield baseline;
   - `minecraft:iron_block` shield;
   - available real shielding blocks: `tfmg:lead_block`, `tfmg:raw_lead_block`, `tfmg:lead_ore`, `createnuclear:reinforced_glass`.

4. Ask the tester to return:
   - `/radworks version` output;
   - `/radworks validate` output;
   - one dump without shield;
   - one dump with `minecraft:iron_block`;
   - one dump per available real shielding block;
   - installed mod versions if possible;
   - `latest.log` only for crash, warnings, or confusing results.

## Manual Minecraft test - Phase 6A armor protection diagnostics
1. No armor:

   ```text
   /radworks exposure
   ```

   Expected:
   - `armorProtection.status=none`;
   - `wouldBlockExposure=false`;
   - `applied=false`;
   - `totalExposure` unchanged.

2. Full diamond armor:

   ```text
   /item replace entity @s armor.head with minecraft:diamond_helmet
   /item replace entity @s armor.chest with minecraft:diamond_chestplate
   /item replace entity @s armor.legs with minecraft:diamond_leggings
   /item replace entity @s armor.feet with minecraft:diamond_boots
   /radworks exposure
   ```

   Expected:
   - `armorProtection.status=full`;
   - `missingPieces=[]`;
   - `wouldBlockExposure=true`;
   - `applied=false`;
   - `totalExposure` unchanged;
   - `hypotheticalExposureIfArmorApplied=0.0`.

3. Partial diamond armor:

   ```text
   /item replace entity @s armor.chest with air
   /radworks exposure
   ```

   Expected:
   - `armorProtection.status=partial`;
   - `missingPieces` includes `chest`;
   - `wouldBlockExposure=false`;
   - `applied=false`;
   - `totalExposure` unchanged.

4. Dump:

   ```text
   /radworks exposure
   /radworks dump
   ```

   Expected:
   - `lastExposureSnapshot.armorProtection` exists;
   - no gameplay damage/effects/ticking behavior is active.

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

## Phase 4D acceptance
- The project builds.
- `/radworks validate` reports the dev-only `minecraft:water` fluid rule with no validation errors.
- Existing Phase 4B/4C scenario remains `totalExposure=25.0` when no block fluid handler source is present.
- `/radworks dump` includes `performance.fluidHandlerScan`.
- `/radworks dump.sourceScanSummary` includes fluid handler position, handler, tank and match counters.
- Optional block fluid handler sources can be shown as `type=block_fluid_handler` when such a block exists in the instance.
- No item fluid capabilities, entity fluid capabilities, buckets, fluid containers in player inventory, item NBT/components, registry tanks/blocks/items, energy, shielding, damage/effects, ticking accumulation, cache, Create, Aeronautics or KubeJS logic exists.

## Phase 5A acceptance
- The project builds.
- `#radworks:shielding_blocks` contains temporary/dev-only `minecraft:iron_block`.
- Baseline Phase 4B/4C scenario remains `totalExposure=25.0` without shielding blocks between sources and player.
- 10 `minecraft:rotten_flesh` plus one nearby `minecraft:gold_block` remains `totalExposure=15.0` with no shielding block between source and player.
- One `minecraft:iron_block` between player and `minecraft:gold_block` reduces the block row from `rawContribution=5.0` to `finalContribution=2.5`, giving `totalExposure=12.5` with 10 rotten flesh in inventory.
- Player inventory rows report `shielding=not_applicable` and keep `finalContribution=rawContribution`.
- Container rows use `containerPos` for shielding.
- `/radworks dump` includes shielding fields, `performance.shielding`, and shielding counters in `sourceScanSummary`.
- No armor protection, damage/effects, hunger/exhaustion, particles, sounds, ticking accumulation, cache/invalidation, Create, Aeronautics or KubeJS logic exists.

## Phase 5B acceptance
- The project builds.
- `src/main/resources/data/radworks/tags/block/shielding_blocks.json` uses the singular `tags/block` path.
- `minecraft:iron_block` remains a dev/test shielding entry.
- Optional real candidates are present in the shielding tag with `required:false`:
  - `tfmg:raw_lead_block`;
  - `tfmg:lead_block`;
  - `tfmg:lead_ore`;
  - `createnuclear:reinforced_glass`.
- Clean local dev environment does not crash when optional mods are absent.
- `/radworks validate` reports shielding candidate status without errors for absent optional mods.
- `/radworks dump` includes compact shielding candidate diagnostics.
- `TESTER_HANDOFF.md` exists and explains how an external tester should test and what to return.
- No damage/effects, armor protection, ticking accumulation, cache/invalidation, custom blocks/items, external dependencies, Create/Aeronautics integration or KubeJS dependency exists.

## Phase 6A acceptance
- The project builds.
- `/radworks exposure` includes a compact `armorProtection` diagnostics line.
- No armor reports `status=none`.
- Full diamond armor reports `status=full`.
- Partial diamond armor reports `status=partial` and a missing slot.
- `totalExposure` remains unchanged by armor in Phase 6A.
- `/radworks dump` includes `lastExposureSnapshot.armorProtection`.
- No gameplay effects, damage, ticking accumulation, cache/invalidation, source provider changes, or integrations are added.

## Phase 6E acceptance
- The project builds.
- `/radworks validate` still reports:
  - `selectedEffectId=radworks:radiation`;
  - `selectedEffectRegistered=true`;
  - no new errors when `createnuclear:radiation` is absent.
- `/radworks effect apply` below threshold does not apply and returns reason `below_threshold`.
- `/radworks effect apply` at/above threshold with no armor applies `radworks:radiation` for `20` ticks at amplifier `0`.
- `/radworks effect apply` at/above threshold with full armor does not apply and returns reason `blocked_by_full_armor`.
- `/radworks effect clear` removes only `radworks:radiation`.
- `/radworks effect status` reports effect id, registration status, active state, active duration/amplifier when present, and preview gate fields.
- `/radworks exposure` remains diagnostic-only and does not auto-apply the effect.
- `/radworks dump.performance` includes `effect_apply`, `effect_clear`, and `effect_status`.
- No damage, hunger/exhaustion, particles/sounds, ticking accumulation, cache/invalidation, source provider changes, shielding math changes, or optional integration changes are introduced.

## Phase 6T acceptance
- `./gradlew test` runs real test classes (not `NO-SOURCE`).
- `./gradlew build` passes after test harness setup.
- Effect preview regression tests cover:
  - below threshold -> blocked;
  - threshold with no/partial armor -> would apply;
  - full armor -> blocked by armor.
- Shielding regression tests cover:
  - single-hit 0.5 multiplier behavior;
  - minimum 0.1 multiplier cap behavior.
- Bundled rule JSON smoke tests pass for:
  - `dev_rotten_flesh.json`;
  - `dev_gold_block.json`;
  - `dev_water.json`.
- Shielding tag and shielding diagnostics data-contract tests pass.
- Local policy is automation-first; repeated manual local testing is not default.

## Manual Minecraft test - `/radworks effect`
1. Build:

   ```bash
   ./gradlew build
   ```

2. Validate strategy registration:

   ```text
   /radworks validate
   ```

3. Below-threshold blocked apply:

   ```text
   /clear @s
   /item replace entity @s armor.head with air
   /item replace entity @s armor.chest with air
   /item replace entity @s armor.legs with air
   /item replace entity @s armor.feet with air
   /give @s minecraft:rotten_flesh 1
   /radworks effect apply
   ```

   Expected:
   - apply blocked with `reason=below_threshold`;
   - no active `radworks:radiation`.

4. Threshold apply without armor:

   ```text
   /clear @s
   /item replace entity @s armor.head with air
   /item replace entity @s armor.chest with air
   /item replace entity @s armor.legs with air
   /item replace entity @s armor.feet with air
   /give @s minecraft:rotten_flesh 10
   /radworks effect apply
   /radworks effect status
   ```

   Expected:
   - apply succeeds;
   - `active=true`;
   - active effect is `radworks:radiation`;
   - status includes preview gate with `wouldApply=true`, `reason=exposure_at_or_above_threshold`, `applied=false`.

5. Threshold blocked by full armor:

   ```text
   /item replace entity @s armor.head with minecraft:diamond_helmet
   /item replace entity @s armor.chest with minecraft:diamond_chestplate
   /item replace entity @s armor.legs with minecraft:diamond_leggings
   /item replace entity @s armor.feet with minecraft:diamond_boots
   /radworks effect apply
   ```

   Expected:
   - apply blocked with `reason=blocked_by_full_armor`.

6. Clear and status:

   ```text
   /radworks effect clear
   /radworks effect status
   ```

   Expected:
   - clear reports `removed=true` if active, otherwise `removed=false`;
   - status reports `active=false`.

7. Console target requirement:
   - from console, `/radworks effect apply` must fail with:

   ```text
   player required; use /radworks effect apply <player>
   ```

   - from console, `/radworks effect clear` must fail with:

   ```text
   player required; use /radworks effect clear <player>
   ```

   - from console, `/radworks effect status` must fail with:

   ```text
   player required; use /radworks effect status <player>
   ```

8. Optional manual sanity check:

   ```text
   /radworks effect apply
   /radworks exposure
   ```

   Expected:
   - `/radworks exposure` does not apply effects automatically;
   - no gameplay damage/effects logic is triggered by exposure diagnostics.

## Phase 5A.1 manual verification result
- Status: DONE on 2026-05-10.
- Shielded dump reviewed by user: `radworks-dump-20260510-070002-Dev.json`.
- No-shield dump reviewed by user: `radworks-dump-20260510-070129-Dev.json`.
- No-shield result:
  - `totalExposure=15.0`;
  - player inventory `minecraft:rotten_flesh` `finalContribution=10.0`;
  - `minecraft:gold_block` `rawContribution=5.0`;
  - `minecraft:gold_block` `finalContribution=5.0`;
  - `shielding=clear`;
  - `shieldingBlocksHit=0`;
  - `shieldingMultiplier=1.0`;
  - `sourceScanSummary.shieldingSourcesApplicable=1`;
  - `sourceScanSummary.shieldingSourcesReduced=0`.
- Shielded result:
  - `totalExposure=12.5`;
  - player inventory `minecraft:rotten_flesh` `finalContribution=10.0`;
  - `minecraft:gold_block` `rawContribution=5.0`;
  - `minecraft:gold_block` `finalContribution=2.5`;
  - `shielding=reduced`;
  - `shieldingBlocksHit=1`;
  - `shieldingMultiplier=0.5`;
  - `shieldingReduction=2.5`;
  - `sourceScanSummary.shieldingSourcesApplicable=1`;
  - `sourceScanSummary.shieldingSourcesReduced=1`.
- Conclusion: Phase 5A shielding diagnostics are manually verified.
