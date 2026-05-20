# RadWorks Beta 0.4 Final Retest Handoff

## Target
- Minecraft: `1.21.1`
- NeoForge: `21.1.228`
- Java: `21`
- Jar: `build/libs/radworks-0.1.0.jar`

## Setup
- Place RadWorks jar on the dedicated server.
- During beta retest, also place the same jar on clients.
- Optional mods (if available): Create, Create Nuclear, TFMG.

## Commands to run in each scenario
```text
/radworks validate
/radworks sources
/radworks exposure
/radworks radius show 10
/radworks dump
```

## Final Beta 0.4 retest scenarios
1. World fluid: `createnuclear:uranium`
   - 1 block
   - pool
   - waterfall
2. Dropped radioactive item
   - count 1
   - count 64
3. Item frame / glow item frame with radioactive item.
4. Chest boat with radioactive inventory.
5. Donkey/mule/llama with radioactive inventory.
6. Mob near radioactive source (effect should apply when threshold reached).
7. Mob carrying radioactive item (self-carried source behavior).
8. Mob with shield block between source and target (reduced shielding contribution expected).
9. Player shielding regression check (existing player shielding still works).
10. Two-player aura check (if possible): one player carries source, second player nearby.

## What to send back
- Dump files only for failing or confusing cases.
- Short note with mod versions used.
- `latest.log` only if crash/warning/confusing behavior appears.

## Notes
- No nested containers/containment are part of this beta closure.
- Non-player armor protection is not in Beta 0.4 baseline.
