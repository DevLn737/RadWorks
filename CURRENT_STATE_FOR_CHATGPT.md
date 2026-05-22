# Current State for ChatGPT — RadWorks NeoForge Migration

> Update (2026-05-22): Beta 0.6.2 applies `exclude` source overrides on existing source rows (post-discovery, pre-shielding) with diagnostics visibility. `contain` and `force` remain not-applied and diagnostics-only.

## 1. One-paragraph summary

RadWorks is a clean NeoForge Minecraft mod rebuilt from an old KubeJS prototype. The old KubeJS project is the behavior/specification source, but not the architecture to copy. Current baseline is Beta 0.5 + Beta 0.6.2: bounded source discovery (world fluids, entity carriers, living targets), target-aware shielding for living entities, bounded vanilla nested-container extraction via item data components (`DataComponents.CONTAINER`, `DataComponents.BUNDLE_CONTENTS`), source-override rule loading (`exclude`/`contain`/`force`), and active exclusion application (`exclude` only) before shielding/effect decisions.

## 2. Target environment

| Item | Current value |
|---|---|
| Minecraft | `1.21.1` |
| NeoForge | `21.1.228` |
| Java | `21` |
| Mod ID | `radworks` |
| Main package | `dev.radworks` |
| Mod version | `0.1.0` |
| Gradle setup | Gradle wrapper, Java library plugin, NeoForge ModDev Gradle plugin |
| NeoGradle/ModDev | `net.neoforged.moddev` `2.0.141` |
| Artifact | `build/libs/radworks-0.1.0.jar` exists in the local workspace |
| Runtime dependency policy | No KubeJS, Create, Aeronautics, or Create Nuclear dependency yet |
| UNKNOWN | Final target modpack MC/NeoForge version may still require revisiting the baseline |

## 3. Current repository structure summary

| Area | Important paths | Purpose |
|---|---|---|
| Docs | `AGENTS.md`, `README.md`, `MIGRATION_STATUS.md`, `TESTING.md`, `DIAGNOSTICS.md`, `CHANGELOG.md` | Human-readable project rules, status, tests, diagnostics, and history |
| Migration source | `migration_export/` | Source of truth from the old KubeJS project |
| Commands | `src/main/java/dev/radworks/command` | `/radworks` command tree |
| Diagnostics | `src/main/java/dev/radworks/diagnostics` | Dump state, warning buffer, command performance, source scan summary |
| Radiation core | `src/main/java/dev/radworks/radiation` | Rules, source providers, exposure pipeline |
| Shielding | `src/main/java/dev/radworks/radiation/shielding` | Diagnostic-only shielding line sampling |
| Data rules | `src/main/resources/data/radworks/radiation_rules/` | Dev-only JSON radiation rules |
| Tags | `src/main/resources/data/radworks/tags/block/shielding_blocks.json` | Dev-only shielding block tag |
| Mod metadata | `src/main/resources/META-INF/neoforge.mods.toml` | NeoForge metadata |
| Pack metadata | `src/main/resources/pack.mcmeta` | Resource/data pack metadata |
| Generated/runtime evidence | `build/libs/radworks-0.1.0.jar`, `run/radworks_dumps/`, `run/saves/` | Evidence only; not source of truth |

Do not treat `build/classes`, `build/tmp`, `build/reports`, `build/moddev`, `.gradle`, generated `run/config`, `run/resourcepacks`, `run/defaultconfigs`, or old compressed logs as source of truth.

## 4. Current phase status

| Phase | Status | What is implemented | What is still missing |
|---|---|---|---|
| Beta 0.5 nested containers | DONE (local) / EXTERNAL_RETEST_PENDING | Bounded nested extraction core + provider integration + nested diagnostics/config caps | External modpack confirmation for nested scenarios (shulker/bundle and unsupported modded formats) |
| Beta 0.4 closure baseline | DONE (local) | World fluid + stable clusters, entity carriers, living-target auto-apply, target-aware living shielding, dedicated-server hardening | Closed as baseline for Beta 0.5 follow-up |
| Phase 0 — Repository foundation | DONE | Minimal NeoForge mod, docs, `/radworks version`, `/radworks dump`, build baseline | Nothing planned for Phase 0 |
| Phase 1 — Data-driven radiation rules | DONE | JSON rules from `data/radworks/radiation_rules/*.json`, validation, checksum, lenient/dev mode | Real modpack rules not migrated |
| Phase 2 — Player inventory radiation | DONE / DIAGNOSTIC_ONLY | Server-side main inventory and offhand item source diagnostics | No gameplay effects, armor, Curios, nested containers |
| Phase 3 — Exposure diagnostics | DONE / DIAGNOSTIC_ONLY | `/radworks debug`, `/radworks sources`, warning buffer, performance stats, bounded snapshots | No new source discovery outside existing inventory pipeline |
| Phase 4A — Static block sources | PARTIAL / DIAGNOSTIC_ONLY / DEV_ONLY | Static block state source scanning, dev `minecraft:gold_block` rule | Manual status is less explicit than later combined tests |
| Phase 4B — Vanilla container/block entity inventory sources | DONE / DIAGNOSTIC_ONLY | Vanilla `Container` block entity slot scan, chest/barrel manual tests | No nested containers or modded capability inventories |
| Phase 4C — Block item handler capability sources | PARTIAL / DIAGNOSTIC_ONLY | NeoForge `Capabilities.ItemHandler.BLOCK`, skip vanilla `Container` to avoid double counting | Positive modded `IItemHandler` block test is UNKNOWN without extra mods |
| Phase 4D — Block fluid handler capability sources | DONE / DIAGNOSTIC_ONLY / DEV_ONLY | NeoForge `Capabilities.FluidHandler.BLOCK`, dev `minecraft:water` rule, fluid counters | Positive runtime fluid handler test is UNKNOWN without a tank block/mod |
| Phase 5A — Shielding diagnostics | DONE / DIAGNOSTIC_ONLY / DEV_ONLY | Diagnostic shielding engine, dev `minecraft:iron_block` tag, shielding fields/counters, manual dump review passed | No final balance model |
| Phase 5A.1 — Shielding manual verification / dump review | DONE | Confirmed no-shield total `15.0`, shielded total `12.5`, and expected shielding counters | Nothing further planned for 5A.1 |
| Phase 5B — Real shielding rules + external tester package | PARTIAL / DIAGNOSTIC_ONLY | Optional TFMG/Create Nuclear shielding candidates, validate/dump candidate diagnostics, `TESTER_HANDOFF.md` | External modpack verification pending |
| Phase 6 (6A/6C/6D/6E/6T) | DONE (beta baseline) | Armor diagnostics (6A), effect preview diagnostics (6C), own `radworks:radiation` registration (6D), controlled manual effect command (6E), automated local regression harness (6T), config-gated auto-apply baseline | No new gameplay systems beyond baseline scope |
| Phase 7 candidate | NOT_STARTED | Candidate for performance/cache planning before gameplay tick logic | Cache/invalidation strategy not decided |
| Phase 8+ Create/Aeronautics future work | NOT_STARTED / BLOCKED_BY_DECISION | No integrations yet | Requires optional integration architecture and API research |

## 5. Implemented commands

| Command | Status | Purpose | Important output fields |
|---|---|---|---|
| `/radworks version` | DONE | Show mod/runtime version info | mod id/version, Minecraft, NeoForge, Java |
| `/radworks dump` | DONE | Write bounded diagnostics JSON to `run/radworks_dumps/` | mod/world/player, rules, debug, performance, sourceScanSummary, lastExposureSnapshot, warnings |
| `/radworks validate` | DONE | Validate loaded radiation rules | loaded counts, checksum, validation mode, errors/warnings/infos |
| `/radworks sources` | DONE | Show current source rows for command target | source type, position/slot/tank, rule values, contribution, match reason, shielding fields |
| `/radworks sources <player>` | DONE | Same as sources, targeting an online server player | target player, bounded rows |
| `/radworks exposure` | DONE | Calculate diagnostic exposure for command player | total exposure, matched stacks/sources, bounded source rows |
| `/radworks exposure <player>` | DONE | Calculate diagnostic exposure for an online server player | target player, total exposure, bounded rows |
| `/radworks effect apply` | DONE | Controlled manual apply of `radworks:radiation` for command player (preview-gated) | reason if blocked; duration/amplifier when applied |
| `/radworks effect apply <player>` | DONE | Controlled manual apply for an online server player | target player, gate reason, applied/changed |
| `/radworks effect clear` | DONE | Remove only `radworks:radiation` from command player | removed flag |
| `/radworks effect clear <player>` | DONE | Remove only `radworks:radiation` from an online server player | target player, removed flag |
| `/radworks effect status` | DONE | Show runtime effect status and preview gate for command player | selectedEffectRegistered, active, duration/amplifier, wouldApply/reason |
| `/radworks effect status <player>` | DONE | Same status for an online server player | target player, active + preview gate |
| `/radworks debug status` | DONE | Show server-wide in-memory debug state | enabled/disabled |
| `/radworks debug on` | DONE | Enable server-wide debug state; permission level 2 | debug enabled |
| `/radworks debug off` | DONE | Disable server-wide debug state; permission level 2 | debug disabled |

## 6. Implemented source types

| Source type | Status | Provider/class | Rule type | Position model | Shielding behavior |
|---|---|---|---|---|---|
| `player_inventory` | DONE / DIAGNOSTIC_ONLY | `PlayerInventorySourceProvider` | `item` | No world position; inventory slot/offhand slot | `not_applicable`; final equals raw |
| `block` | PARTIAL / DIAGNOSTIC_ONLY / DEV_ONLY | `BlockSourceProvider` | `block` | Block center near target player | Applies if `respectsShielding=true` |
| `block_entity_inventory` | DONE / DIAGNOSTIC_ONLY | `BlockEntityInventorySourceProvider` | `item` | Container block position | Applies if `respectsShielding=true` |
| `block_item_handler` | PARTIAL / DIAGNOSTIC_ONLY | `BlockItemHandlerSourceProvider` | `item` | Capability block position and context | Applies if `respectsShielding=true` |
| `block_fluid_handler` | DONE baseline / DIAGNOSTIC_ONLY / DEV_ONLY | `BlockFluidHandlerSourceProvider` | `fluid` | Capability block position and context | Applies if `respectsShielding=true` |

Contribution formulas:

| Source type | Formula |
|---|---|
| `player_inventory` | `stack.count * itemRule.strength` |
| `block` | `blockRule.strength` |
| `block_entity_inventory` | `stack.count * itemRule.strength` |
| `block_item_handler` | `stack.count * itemRule.strength` |
| `block_fluid_handler` | `fluidRule.strength * amountMb / 1000.0` |

All source types are still diagnostic-only. No damage, effects, hunger/exhaustion, particles, sounds, ticking accumulation, or gameplay mutation are implemented.

## 7. Current radiation rule set

### Dev/test rules currently present

| Rule/tag | Type | Purpose | Old migration relation | Final status |
|---|---|---|---|---|
| `minecraft:rotten_flesh` | item rule | Smoke test for item sources and scaling | Placeholder for radioactive items | DEV_ONLY; replace with real item rules later |
| `minecraft:gold_block` | block rule | Smoke test for static block source scanning | Placeholder for radioactive blocks | DEV_ONLY; not balance |
| `minecraft:water` | fluid rule | Smoke test for fluid rule loading and future block fluid handlers | Placeholder for radioactive fluid support | DEV_ONLY; not balance |
| `minecraft:iron_block` | block tag entry in `#radworks:shielding_blocks` | Smoke test for shielding line sampling | Placeholder for shielding blocks | DEV_ONLY; keep for tests |
| `tfmg:raw_lead_block` | optional block tag entry | Real shielding candidate from old KubeJS config | TFMG lead shielding | Final candidate; external verification pending |
| `tfmg:lead_block` | optional block tag entry | Real shielding candidate from old KubeJS config | TFMG lead shielding | Final candidate; external verification pending |
| `tfmg:lead_ore` | optional block tag entry | Real shielding candidate from old KubeJS config | TFMG lead shielding | Final candidate; external verification pending |
| `createnuclear:reinforced_glass` | optional block tag entry | Real shielding candidate from old KubeJS config | Create Nuclear shielding glass | Final candidate; external verification pending |

### Real old KubeJS rules not fully migrated yet

Summary only; see `migration_export/04_CONTENT_REGISTRY.md`, `migration_export/05_BEHAVIOR_SPEC.md`, and related migration docs for full context.

| Area | Old content summary | Current status |
|---|---|---|
| Radioactive items | Create Nuclear uranium-related items such as raw uranium, crushed uranium, yellowcake, enriched yellowcake, uranium rod/bucket/powder | NOT_STARTED as real rules |
| Radioactive blocks | Create Nuclear uranium ore/block/fluid-like blocks and enrichment blocks | NOT_STARTED as real rules |
| Radioactive fluid | `createnuclear:uranium` | NOT_STARTED as real rule |
| Shielding blocks | TFMG lead blocks/ore and Create Nuclear reinforced glass | Added as optional `required:false` candidates; external verification pending |
| Armor set | Old prototype used full diamond armor as protective placeholder | NOT_STARTED; strategy decision required |
| Container exceptions | Specific Simulated/Create/TFMG blocks were excluded or special-cased in old logic | NOT_STARTED; decision required |

## 8. Shielding Phase 5A status

Phase 5A adds diagnostic-only shielding calculation for positioned sources. It does not apply gameplay effects.

| Item | Current behavior |
|---|---|
| Classes | `ShieldingEngine`, `ShieldingResult` |
| Tag path | `src/main/resources/data/radworks/tags/block/shielding_blocks.json` |
| Tag id | `#radworks:shielding_blocks` |
| Temporary block | `minecraft:iron_block` |
| Applicability | Only sources with world position and `respectsShielding=true` |
| Inventory source behavior | `shielding=not_applicable`, final equals raw |
| Algorithm | Simple sample line from source center/container center to player body center |
| Sampling | `sampleStep=0.25`, `maxSamples=64`, skip source/player endpoints |
| Block counting | Unique shielding block positions counted once |
| Multiplier | Each shielding block multiplies by `0.5`, minimum cap `0.1` |
| Compatibility field | `contribution` mirrors `finalContribution` |

Fields now exposed on source rows and dump rows:

- `respectsShielding`
- `rawContribution`
- `shielding`
- `shieldingBlocksHit`
- `shieldingMultiplier`
- `shieldingReduction`
- `finalContribution`
- `contribution`

Diagnostics additions:

- `performance.shielding`
- `sourceScanSummary.shieldingSourcesChecked`
- `sourceScanSummary.shieldingSourcesApplicable`
- `sourceScanSummary.shieldingSamplesChecked`
- `sourceScanSummary.shieldingBlocksHit`
- `sourceScanSummary.shieldingSourcesReduced`

Phase 5A.1 manual verification passed on 2026-05-10:

- No-shield dump `radworks-dump-20260510-070129-Dev.json`: `totalExposure=15.0`, gold block `finalContribution=5.0`, `shielding=clear`, `shieldingBlocksHit=0`.
- Shielded dump `radworks-dump-20260510-070002-Dev.json`: `totalExposure=12.5`, gold block `rawContribution=5.0`, `finalContribution=2.5`, `shielding=reduced`, `shieldingBlocksHit=1`, `shieldingMultiplier=0.5`.
- Player inventory `minecraft:rotten_flesh` remained `finalContribution=10.0`.
- `sourceScanSummary.shieldingSourcesApplicable=1` in both scenarios; `shieldingSourcesReduced` changed from `0` to `1` when the iron block was placed.

Future work remains: final shielding block list, balance model, old KubeJS model comparison, partial/transparent block behavior, armor protection, and cache/invalidation strategy.

Phase 5B adds:

- optional `required:false` tag entries for `tfmg:raw_lead_block`, `tfmg:lead_block`, `tfmg:lead_ore`, `createnuclear:reinforced_glass`;
- `/radworks validate` shielding candidate status;
- `/radworks dump.shielding` with tag path, dev/test entries, optional entries and present/missing status;
- `TESTER_HANDOFF.md` for external modpack testing.

## 9. Diagnostics and dump fields

`/radworks dump` currently writes bounded JSON to `run/radworks_dumps/`. The dump is intended for Codex/ChatGPT diagnostics and should not be pasted in full unless needed.

Current high-level sections:

- `schemaVersion`
- `createdAt`
- `mod`
- `world`
- `player`
- `rules`
- `debug`
- `integrations`
- `performance`
- `sourceScanSummary`
- `lastExposureSnapshot`
- `recentWarnings`

Current diagnostics include:

- rules loaded state, checksum, counts, validation mode, errors/warnings/infos;
- shielding candidate diagnostics including optional external block status;
- Create/Aeronautics integration placeholders showing not loaded/enabled;
- command diagnostics performance stats, including validate/exposure/sources/dump/block/item/fluid/shielding scans;
- bounded source rows with `sourcesShown` and `sourcesOmitted`;
- warning buffer capped at 100 entries;
- shielding fields for each source row after Phase 5A.

Missing by design:

- no full world scan snapshot;
- no cache state;
- no gameplay tick exposure history;
- no damage/effect application history;
- no Create/Aeronautics integration diagnostics beyond placeholders.

## 10. Manual verification status

| Test | Status | Expected result | Last known result |
|---|---|---|---|
| `./gradlew build` | DONE | Build succeeds | Last known builds passed; rerun after this handoff doc update |
| `./gradlew test` | DONE | Local automated regression suite executes real tests | Phase 6T adds non-`NO-SOURCE` unit tests for core diagnostics logic/data contracts |
| `./gradlew runClient` smoke | DONE as smoke | Client launches enough to confirm mod loads | Previously started client/integrated server; timeout smoke is not full manual verification |
| `/radworks validate` | DONE | Rules loaded, no validation errors for dev rules | User verified through Phase 4D |
| Inventory-only exposure | DONE | 10 `minecraft:rotten_flesh` gives `totalExposure=10.0` | User verified |
| Gold block static block exposure | DONE | Nearby `minecraft:gold_block` contributes `5.0` before shielding | Phase 5A.1 no-shield dump confirmed `finalContribution=5.0` |
| Chest/container source | DONE | Chest/barrel with 10 rotten flesh contributes `10.0` | User verified |
| Block item handler source | PARTIAL / UNKNOWN | No double counting with vanilla chest; modded handler positive test if available | Baseline no-double-counting was planned; positive modded handler test depends on external mod |
| Block fluid handler source baseline | DONE / UNKNOWN positive | No handler keeps baseline total unchanged; positive test requires tank/fluid handler | User verified baseline and counters; no custom tank added |
| Shielding with iron block | DONE | Iron block between gold block and player reduces block raw `5.0` to final `2.5` | Phase 5A.1 shielded dump confirmed `totalExposure=12.5` |
| Real TFMG/Create Nuclear shielding candidates | NOT_RUN / UNKNOWN | Available real blocks should reduce like iron block in same geometry | Pending external tester package results |
| `/radworks dump` contains performance/sourceScanSummary | DONE | Dump contains performance, sourceScanSummary, recentWarnings, lastExposureSnapshot | User verified Phase 5A shielding fields and counters |

## 11. Known limitations / UNKNOWN

- No damage/effects are implemented.
- No hunger/exhaustion changes are implemented.
- No ticking exposure accumulation is implemented.
- No cache/invalidation is implemented.
- No armor protection is implemented.
- No entity, dropped item, minecart, mob, item frame, or projectile sources are implemented.
- No Create, Create Nuclear, TFMG, Simulated, Aeronautics, or KubeJS runtime dependencies are added.
- No real modpack optional dependency environment has been tested yet; Phase 5B created an external tester package for this.
- Current radioactive rules are dev-only vanilla placeholders.
- Current shielding tag is a dev-only vanilla placeholder.
- Item handler positive tests may need a modded block exposing `IItemHandler`.
- Fluid handler positive tests may need a modded tank/block exposing `IFluidHandler`.
- Nested containers are currently limited to supported vanilla data-component formats (`CONTAINER`, `BUNDLE_CONTENTS`).
- Create toolbox and Sophisticated nested formats are not implemented yet (research-first follow-up).
- No Curios/Trinkets container integration.
- No container exception list is migrated yet.
- Shielding model is diagnostic and may not match final intended balance.
- Old KubeJS behavior is only partially migrated.

## 12. Important migration decisions still open

- Effect strategy: own `radworks:radiation` effect vs compatibility with `createnuclear:radiation`.
- Armor strategy: old diamond placeholder vs custom/tag-driven armor protection.
- Shielding strategy: current attenuation line sampling vs old three-ray binary model.
- Final shielding block/tag content, especially TFMG/Create Nuclear lead/reinforced glass.
- Real radiation rules migration when optional mods are absent from the dev environment.
- Container exception model and exact excluded block list.
- Entity source scope and timing.
- Cache/invalidation timing before any tick-based gameplay exposure.
- Create contraption API approach.
- Aeronautics/Simulated research and isolation strategy.

## 13. Recommended next steps

| Option | Why now | Risks | Plan Mode required? |
|---|---|---|---|
| Phase 5B.1 — external tester results review | Recommended because Phase 5B needs real modpack verification | Requires tester dumps/log snippets | Yes |
| Phase 6A — armor protection diagnostics only | Builds on verified shielding without applying damage/effects | Armor strategy is undecided | Yes |
| Phase 6B — effect strategy decision document | Clarifies future gameplay effect implementation before coding | Requires compatibility decision | Yes |
| Phase 4E — container exclusions | Addresses old KubeJS exception behavior before broader gameplay | Needs careful old behavior review | Yes |

Recommended next phase: Phase 5B.1 external tester results review. This keeps real shielding candidate work grounded in actual TFMG/Create Nuclear registry behavior before moving into armor or gameplay effects.

## 14. What ChatGPT should help with next

Please read this file first, then help the project owner choose the next Codex prompt. The likely task is to review external tester results for Phase 5B or prepare a precise Plan Mode prompt for Phase 5B.1. ChatGPT should preserve the constraints: no hidden rewrites, no KubeJS dependency, no Create/Aeronautics implementation yet, and no damage/effects until the diagnostic pipeline remains fully understood.

## 15. Compact "state fingerprint"

```text
Current build: Phase 5B ./gradlew build passed on 2026-05-10
Current artifact: build/libs/radworks-0.1.0.jar exists
Current latest completed phase: Phase 5A manually verified; Phase 5B implemented locally pending external verification
Current recommended next phase: Phase 5B.1 external tester results review
Gameplay damage/effects: not implemented
Create/Aeronautics: not implemented
KubeJS dependency: no
Main risk: optional TFMG/Create Nuclear shielding IDs need external modpack verification.
Need from ChatGPT: choose next Codex prompt
```
