# RadWorks Beta 0.6 Final Retest Handoff

## Beta 0.6.5 closure note
- Beta 0.6 override-rules baseline is functionally complete:
  - 0.6.1 schema/loader/validator
  - 0.6.2 exclude
  - 0.6.3 contain
  - 0.6.4 force
  - 0.6.5 closure/regression/handoff
- This closure step adds no gameplay changes; it consolidates regression and retest workflow.

## Beta 0.6.4 note (force application)
- Enabled `force` rules now create rows only from observed candidates passed by existing provider loops.
- No new scanners/discovery radius/capability lookups are introduced by force.
- Runtime precedence:
  - `exclude` wins over `force`;
  - forced rows can still be processed by `contain`;
  - positioned forced rows can be shielded (if `forceRespectsShielding=true`).
- Quick checks:
  - `/radworks validate` (force rules loaded/valid)
  - `/radworks sources` (forced rows show `overrideMode=forced` unless subsequently contained)
  - `/radworks exposure` (post-force totals)
  - `/radworks dump` (`sourceOverrideDiagnostics` force counters/samples)

## Beta 0.6.1 note (diagnostics-only)
- Source override rules are now loaded/validated from:
  - `data/radworks/source_override_rules/*.json`
- In Beta 0.6.1 they are **not applied** to exposure yet.
- Quick sanity checks:
  - `/radworks validate` (source override counts/warnings/errors)
  - `/radworks dump` (`sourceOverrideDiagnostics` section present)

## Beta 0.6.2 note (exclude application only)
- Enabled `exclude` override rules now suppress already discovered source rows before shielding/effect decisions.
- `contain` and `force` are still not applied in this phase.
- Quick checks:
  - `/radworks sources` (excluded rows should show `overrideMode=excluded`)
  - `/radworks exposure` (excluded rows contribute `0`)
  - `/radworks dump` (`sourceOverrideDiagnostics` + override counters in `sourceScanSummary`)

## Beta 0.6.3 note (containment application)
- Enabled `contain` override rules now apply on post-exclusion rows and before shielding.
- `mode=suppress` sets contribution to `0` and keeps row visible as `overrideMode=contained`.
- `mode=scale` reduces contribution by multiplier; radius model is unchanged.
- Deterministic conflict policy:
  - `suppress` wins over `scale`;
  - scale conflicts choose the lowest multiplier.
- `force` is still not applied in this phase.
- Quick checks:
  - `/radworks sources` (contained rows show `overrideMode=contained`, `overrideRuleId`)
  - `/radworks exposure` (post-containment contribution is used)
  - `/radworks dump` (`sourceOverrideDiagnostics` containment counters and samples)

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

## Beta 0.6 override-rules retest scenarios
1. Exclude:
   - pick a normally-radiating item/block/fluid;
   - add an `exclude` rule;
   - confirm source remains explainable while contribution becomes `0` and total exposure drops.
2. Contain:
   - container/tank content source with uranium;
   - test `mode=suppress` and `mode=scale`;
   - if convenient, test nested shulker/bundle in contained context.
3. Force:
   - use an observed non-radiating candidate (item/block/fluid);
   - add force rule with `forceStrength`, `forceRadius`, `forceUnitMode`;
   - confirm forced row appears;
   - confirm force does not create source for non-observed context.
4. Interaction:
   - verify `exclude` wins over `force`;
   - verify contained forced row is possible;
   - verify positioned forced row is still shielded;
   - verify player/living exposure uses post-override totals.

When reporting failures/confusing behavior, attach:
- dump file(s);
- exact `source_override_rules` JSON used;
- mod versions;
- `latest.log` only if crash/warning/confusing behavior appears.

## Beta 0.4 baseline regression scenarios
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

## Beta 0.5 nested-container scenarios
1. Shulker in player inventory with radioactive item inside.
2. Shulker inside chest with radioactive item inside.
3. Bundle in player inventory with radioactive item inside.
4. Dropped shulker/bundle item with radioactive item inside.
5. Item frame / glow item frame holding shulker or bundle with radioactive item inside.
6. If available: shulker with radioactive item inside chest boat/pack animal inventory.
7. Optional modded container items (Create toolbox / Sophisticated):
   - expected support only if vanilla container components are exposed;
   - otherwise expected result: `nestedContainerDiagnostics` explains unsupported format (no crash).

## What to send back
- Dump files only for failing or confusing cases.
- Short note with mod versions used.
- `latest.log` only if crash/warning/confusing behavior appears.

## Notes
- Beta 0.5 baseline supports vanilla component-based nested extraction only.
- Create toolbox / Sophisticated nested formats remain research-first follow-up.
- Non-player armor protection is not in Beta 0.4 baseline.
- Force stage (0.6.4) is candidate-based only; no new discovery path support is expected.
