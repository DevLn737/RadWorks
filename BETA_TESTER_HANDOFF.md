# RadWorks Beta Tester Handoff

## Target
- Minecraft: `1.21.1`
- NeoForge: `21.1.228`
- Java: `21`
- Jar: `build/libs/radworks-0.1.0.jar`
- KubeJS is not required.
- Create, Create Nuclear, TFMG, Sophisticated and Aeronautics are optional for this beta.

## Basic Commands
Run:

```text
/radworks version
/radworks validate
/radworks exposure
/radworks sources
/radworks effect status
/radworks radius show 10
/radworks dump
```

## Gameplay Smoke
1. Beta default has dev vanilla radiation rules OFF (`rules.enableDevRules=false`), so rotten flesh/gold block/water are not expected to irradiate.
2. Effect runtime mode default is `external_if_present`:
   - if `createnuclear:radiation` exists, RadWorks auto-apply should use it;
   - otherwise RadWorks should fall back to `radworks:radiation`.
3. Run `/radworks validate` and note which Create Nuclear/Create IDs are present or missing.
3. If present in this modpack, test actual radioactive candidates:
   - Create Nuclear items in player inventory.
   - Create Nuclear blocks near player.
   - `createnuclear:uranium` in a modded tank/fluid handler.
4. For each scenario, run:
   - `/radworks sources`
   - `/radworks exposure`
   - `/radworks effect status`
   - `/radworks dump`
   Confirm chat output is readable/compact (details should stay in dump).
5. Clear effect between scenarios:
   - `/radworks effect clear-self`
6. Equip full diamond armor and repeat one positive exposure scenario.
   Expected: full armor blocks auto-apply.
7. If item/fluid handlers are found but no match appears, inspect dump section:
   - `handlerDiagnostics.itemHandlerNonMatchingSamples`
   - `handlerDiagnostics.fluidHandlerNonMatchingSamples`
8. Dynamic radius retest is required:
   - chest full of `createnuclear:raw_uranium`: capture close / medium / just-outside distance dumps;
   - Create item vault with uranium: capture close / medium / just-outside distance dumps;
   - uranium bucket or uranium tank if available: capture close / medium / just-outside distance dumps.
   - verify reasons use `outside_dynamic_radius` where applicable.
9. Radius visualization sanity:
   - run `/radworks radius show 10`;
   - compare visual rings with `/radworks sources` `effectiveRadius` values;
   - use close/medium/outside positions for chest/vault/tank cases.
10. Create transient/internal carrier retest:
   - `create:placard` with `createnuclear:raw_uranium` in `Item`;
   - `create:mechanical_arm` with `createnuclear:raw_uranium` in `HeldItem`;
   - `create:fluid_pipe` and `create:glass_fluid_pipe` with `side.Flow.Fluid` uranium;
   - `fluid:pipette` if available in this pack.
   - Also check `createnuclear:flowing_uranium` if it appears in flow data.
   - In dump, inspect `createCarrierDiagnostics.fluidPathSamples` for:
     - `ruleMatchMode` (`none|exact|fallback`)
     - `skippedReason` (`path_missing`, `fluid_compound_missing`, `invalid_fluid_id`, `amount_missing`, `amount_non_positive`, `no_active_fluid_rule`, `outside_dynamic_radius`).
   - For each scenario run:
     - `/radworks validate`
     - `/radworks sources`
     - `/radworks exposure`
     - `/radworks radius show 10` (or keep always-on visualization)
     - `/radworks dump`
11. Entity carrier retest (Post-Beta 4A + 4B):
   - dropped `createnuclear:raw_uranium` stack with count `1` and `64` if possible;
   - `item_frame` with `createnuclear:raw_uranium`;
   - `glow_item_frame` with `createnuclear:raw_uranium`;
   - player aura test: one player with radioactive inventory near another player.
   - chest boat with radioactive inventory.
   - donkey/mule with chest inventory and radioactive stacks.
   - llama/trader llama inventory scenario if available.
   - optional: any modded mob/entity exposing `Capabilities.ItemHandler.ENTITY`.
   - Confirm self-player inventory is not double-counted (self remains from `player_inventory`, aura is from other players only).
   - For each scenario run:
     - `/radworks validate`
     - `/radworks sources`
     - `/radworks exposure`
     - `/radworks radius show 10` (or always-on visualization)
     - `/radworks dump`
12. World fluid retest (Beta 0.4.1):
   - place `createnuclear:uranium` in world and check close / medium / outside distance;
   - place `createnuclear:flowing_uranium` in world and repeat;
   - verify `world_fluid` rows appear in `/radworks sources` and `/radworks exposure`;
   - verify radius visualization works from the same source rows;
   - if a row is skipped, inspect `/radworks dump` -> `worldFluidDiagnostics` skip reason.

## Optional Shielding Checks
If TFMG/Create Nuclear are installed, test available shielding blocks:
- `tfmg:raw_lead_block`
- `tfmg:lead_block`
- `tfmg:lead_ore`
- `createnuclear:reinforced_glass`

## Return to Codex/User
- `/radworks validate` output.
- Dumps for: baseline, positive item/block/fluid scenario, armor-blocked scenario, shielding scenario.
- Include close/medium/outside dumps for chest/vault/tank dynamic radius checks.
- Include dumps for placard/mechanical arm/pipe/glass_pipe/pipette transient scenarios where available.
- Include dumps for dropped-item, item-frame, glow-item-frame, player-aura, chest-boat, and pack-animal scenarios.
- Include dumps for world-fluid still/flowing uranium scenarios.
- Screenshots or short video of radius visualization for transient scenarios are helpful.
- `latest.log` only if there is a crash/warning/confusing behavior.
- Installed mod versions if optional mods are involved.
