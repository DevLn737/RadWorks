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
   - Phase 1: `rules.checksum` is a full SHA-256 string;
   - Create and Aeronautics integrations are disabled;
   - performance counters are zero.

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
   - `loaded=1`;
   - `enabled=1`;
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
   - `blockRules: 0`;
   - `fluidRules: 0`;
   - `disabledRules: 0`;
   - `errors: []`;
   - `warnings: []`;
   - `infos: []` or only expected disabled-rule info if a local test datapack is active.

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
