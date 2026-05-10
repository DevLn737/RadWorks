# RadWorks External Tester Handoff

## Purpose

RadWorks is a clean NeoForge radiation diagnostics mod rebuilt from an old KubeJS prototype. This test package checks Phase 5B shielding data in a real/modded environment.

This phase is diagnostic-only:
- no damage;
- no potion/status effects;
- no armor protection;
- no ticking exposure accumulation;
- no Create/Aeronautics integration;
- no KubeJS dependency.

## Target Environment

Use:
- Minecraft: `1.21.1`
- NeoForge: `21.1.228`
- Java: `21`
- RadWorks artifact: `build/libs/radworks-0.1.0.jar`

Install:
1. Put `radworks-0.1.0.jar` into the instance `mods/` folder.
2. KubeJS is not required.
3. TFMG and Create Nuclear are optional for startup, but needed to test real shielding blocks:
   - `tfmg:lead_block`
   - `tfmg:raw_lead_block`
   - `tfmg:lead_ore`
   - `createnuclear:reinforced_glass`

## Basic Startup Checks

Open a test world with commands enabled and run:

```text
/radworks version
/radworks validate
```

Expected:
- mod starts;
- `/radworks version` reports RadWorks `0.1.0`;
- `/radworks validate` has no errors;
- missing optional TFMG/Create Nuclear shielding blocks are INFO, not errors, if those mods are absent.

## Shielding Test Setup

Use a clean flat area. The local verified geometry was:

```text
/gamemode creative
/fill -8 72 -8 8 88 8 minecraft:air
/fill -8 79 -8 8 79 8 minecraft:stone
/tp @s 0.5 80 0.5
/clear @s minecraft:rotten_flesh
/give @s minecraft:rotten_flesh 10
/setblock 0 80 4 minecraft:gold_block
```

## Test 1 — No Shield Baseline

```text
/setblock 0 80 2 minecraft:air
/tp @s 0.5 80 0.5
/radworks exposure
/radworks dump
```

Expected:
- `totalExposure=15.0`;
- inventory rotten flesh contributes `10.0`;
- `minecraft:gold_block` has `rawContribution=5.0`;
- `minecraft:gold_block` has `finalContribution=5.0`;
- gold block `shielding=clear`;
- gold block `shieldingBlocksHit=0`.

## Test 2 — Dev/Test Iron Block Shield

```text
/setblock 0 80 2 minecraft:iron_block
/tp @s 0.5 80 0.5
/radworks exposure
/radworks dump
```

Expected:
- `totalExposure=12.5`;
- inventory rotten flesh still contributes `10.0`;
- `minecraft:gold_block` has `rawContribution=5.0`;
- `minecraft:gold_block` has `finalContribution=2.5`;
- gold block `shielding=reduced`;
- gold block `shieldingBlocksHit=1`;
- gold block `shieldingMultiplier=0.5`.

## Test 3 — Real Shielding Blocks

Only run a test if the block exists in your modpack. Use the same position as the iron block:

```text
/setblock 0 80 2 <shielding_block_id>
/tp @s 0.5 80 0.5
/radworks exposure
/radworks dump
```

Try available blocks:
- `tfmg:lead_block`
- `tfmg:raw_lead_block`
- `tfmg:lead_ore`
- `createnuclear:reinforced_glass`

Expected for each available block:
- same pattern as iron block;
- `totalExposure=12.5`;
- gold block `finalContribution=2.5`;
- `shielding=reduced`;
- `shieldingBlocksHit=1`.

If a block is not detected, try adjacent placements before calling it a bug:

```text
/setblock 0 80 1 <shielding_block_id>
/setblock 0 80 2 <shielding_block_id>
/setblock 0 80 3 <shielding_block_id>
/radworks exposure
/radworks dump
```

## What To Send Back

Please send:
- `/radworks version` output;
- `/radworks validate` output;
- one dump without shield;
- one dump with `minecraft:iron_block`;
- one dump for each real shielding block available in your modpack;
- installed TFMG/Create Nuclear/Create/NeoForge versions if possible;
- `latest.log` only if there is a crash, warning, or confusing result.

Useful dump snippets:
- `shielding`;
- `lastExposureSnapshot.totalExposure`;
- relevant `lastExposureSnapshot.sources[]` rows for `player_inventory` and `minecraft:gold_block`;
- `sourceScanSummary`;
- `recentWarnings` if non-empty.

## Known Limitations

- Current shielding is diagnostic attenuation, not final gameplay balance.
- Current algorithm samples a simple line between source and player.
- Phase 5B does not add damage/effects, armor, ticking, cache, or integrations.
- Missing optional mods should not crash the game.
