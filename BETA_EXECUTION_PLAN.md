# Beta 0.1 Execution Plan

## Current State
- `radworks:radiation` is registered and inert.
- Source discovery, shielding, armor diagnostics and effect preview are implemented.
- Unit/contract tests exist and must stay green.
- Optional mod support must remain data-driven and safe when optional mods are absent.
- Beta default must keep vanilla dev radiation rules disabled (`rules.enableDevRules=false`).

## Beta Milestones
1. Normalize manual effect commands to self-only dev commands:
   - `/radworks effect apply-self`
   - `/radworks effect clear-self`
   - `/radworks effect status`
2. Add config-gated automatic effect application:
   - server-side only;
   - throttled by `scanIntervalTicks`;
   - applies only `radworks:radiation`;
   - blocked by full protection armor;
   - no damage/exhaustion.
3. Add beta diagnostics:
   - gameplay config snapshot;
   - bounded last auto-apply decisions;
   - validate summary line.
4. Package beta:
   - `./gradlew test`;
   - `./gradlew build`;
   - jar from `build/libs/`.

## POST_BETA
- Create contraptions/trains.
- Aeronautics/Simulated.
- Sophisticated deep inventory internals.
- Persistent accumulated dose.
- Custom armor/items/blocks.
- Real damage/exhaustion balancing.
- Client UI.
- Compatibility with `createnuclear:radiation`.

## External Tester Required
- Real TFMG/Create Nuclear shielding behavior.
- Any optional mod inventory/fluid behavior that cannot be reproduced in clean dev.
- Real modpack crash/log validation.
- Create Nuclear item/block/fluid radiation candidate behavior from `/radworks validate` and `/radworks dump`.
