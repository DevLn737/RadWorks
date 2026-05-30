# RADWORKS_TEST_MATRIX

## 1. Purpose

Эта матрица связывает поведенческий контракт (`RADWORKS_BEHAVIOR_SPEC.md`) с проверками:
- что уже покрыто автоматически;
- где есть пробелы;
- что требуется вынести в Stage 3B/3C.

Принцип: это не список “как устроен код”, а карта “какое поведение чем проверяется”.

---

## 2. Test categories

- **unit/logic tests**: чистая логика, формулы, policy-решения.
- **provider-level tests**: проверка extraction/discovery contract отдельных source paths.
- **diagnostics tests**: структура и bounded-контракты diagnostics sections.
- **config tests**: defaults, bounds, disable-path semantics.
- **command-format tests**: консольный контракт команд по стабильным маркерам (без brittle full-string checks).
- **server smoke**: запуск сервера без classloading/client-only проблем.
- **external modpack tests**: то, что сложно уверенно автоматизировать локально.
- **future GameTest**: кандидаты in-game сценариев (план, без реализации).

---

## 3. Coverage matrix

| Behavior area | Expected behavior | Current automated coverage | Missing tests / gaps | Test type | Priority | Notes |
|---|---|---|---|---|---|---|
| Radiation rules loading | JSON load/validate, optional-safe | `SourceOverrideRulesLoaderTest`, `RulesDataFilesSmokeTest`, `FlowingFluidRuleResolutionTest` | dedicated behavior-first tests для malformed optional branches | unit | high | good base, но частично smoke-style |
| Player inventory source | item rules -> aggregated source | `PlayerInventorySourceProviderContractTest`, `AggregatedSourceAccumulatorTest`, override tests | full in-world integration for real `ServerPlayer` inventory loop | provider | normal | blocker gap закрыт, остался integration-level gap |
| Static block source | block rules -> positioned source | `BlockSourceProviderContractTest` + override/scan summary tests | in-world provider integration path (runtime world scan) | provider | high | `NEEDS_VERIFICATION`: contract test пока hybrid (behavior + source-contract markers) |
| World fluid source | cluster discovery/aggregation/stability | `WorldFluidSourceProviderTest`, `WorldFluidDiagnosticsTest` | edge tests при mixed rules + override interaction | provider/diag | normal | покрытие сильное |
| Block entity inventory | container content discovery | `BlockEntityInventorySourceProviderContractTest`, nested regressions, override selector tests | additional in-world container access scenarios | provider | normal | blocker gap закрыт |
| Block item handler | capability content discovery | `BlockItemHandlerSourceProviderContractTest`, `HandlerDiagnosticsDynamicRadiusTest` | in-world capability edge scenarios (runtime block entities) | provider | normal | high gap закрыт |
| Block fluid handler | fluid capability discovery | `BlockFluidHandlerSourceProviderContractTest` + diagnostics/override tests | additional runtime handler edge scenarios | provider | normal | high gap закрыт |
| Create transient carriers | known-path extraction | `CreateTransientCarrierExtractorTest`, `CreateTransientCarrierAggregationTest`, `CreateCarrierDiagnosticsTest` | больше integration-level contract tests на full provider flow | provider/diag | normal | good parser coverage |
| Entity dropped/item frame/aura | entity source extraction | `EntityCarrierExtractionTest`, `EntityCarrierDiagnosticsTest` | живые runtime edge cases в modpack | provider/ext | normal | core covered |
| Entity inventories | chest boat/pack animal/generic capability | `EntityInventoryCarrierAdapterTest`, `EntityCarrierDiagnosticsTest` | provider integration tests с real entity states | provider/ext | high | partial synthetic |
| Nested containers | supported vanilla components + limits | `NestedContainerExtractorTest`, `NestedProviderRegressionAuditTest`, `NestedProviderIntegrationAuditTest` | deep scenario matrix for nested+override+entity carriers | provider | high | база есть |
| Dynamic radius | formula, cap, units | `DynamicRadiusModelTest`, `CreateTransientCarrierAggregationTest`, `WorldFluidSourceProviderTest` | monotonicity across all source types | unit | normal | mostly covered |
| Shielding | post-override attenuation semantics | `ShieldingResultTest`, `ShieldingEngineTargetAwarePolicyTest`, `ShieldingDiagnosticsContractTest`, `SourceScanSummaryLivingShieldingTest` | integration tests “contain/force then shielding” в provider context | unit/provider | high | partial via SourceOverrideEngineTest |
| Living targets | bounded scan, decision reasons | `LivingTargetSelectionTest`, `LivingEntityEffectDecisionTest` | end-to-end gameplay contract tests с exposure path | gameplay | high | current tests policy-level |
| Effect strategy | mode + threshold + armor block | `EffectStrategyServiceTest` | mode/selection contract against runtime registry states | unit | normal | reasonable |
| Source overrides exclude/contain/force | precedence + dedupe + disable paths | `SourceOverrideEngineTest`, `SourceOverrideExcludeContractTest`, `SourceOverrideContainContractTest`, `SourceOverrideForceContractTest`, `SourceOverridePipelineOrderContractTest`, `SourceOverrideRulesLoaderTest` | further monolith reduction in future cleanup phase | unit | normal | Stage 3C split started |
| Commands contract | intended output/semantics | `CommandOutputContractTest` + existing command usage tests | runtime command execution format in real command dispatcher | command | normal | `NEEDS_VERIFICATION`: current coverage is stable-token contract |
| Dump diagnostics contract | required sections + key counters | `DiagnosticsDumpSchemaContractTest` + section-level diagnostics tests | runtime dump generation E2E assertions with real server context | diagnostics | normal | high gap закрыт |
| Config contract | defaults/bounds/server-safe | `RadWorksConfigTest`, `RadWorksConfigServerPolicyTest`, `RadWorksConfigLivingTargetsTest`, `ConfigExposureThresholdClampIntentTest` | mismatch resolution decision for threshold clamp semantics | config | high | `SPEC_CODE_MISMATCH_CANDIDATE` intentionally preserved |
| Dedicated server compatibility | no client-only imports + safe side | `ForbiddenClientImportsTest`, `RadiusVisualizationServerSafetyTest` | repeatable runServer smoke automation policy | compat/smoke | normal | smoke mostly manual workflow |

---

## 4. Existing test audit table

> Stage 3C: добавлены новые contract-tests; delete/cleanup всё ещё консервативный (без массовых удалений).

| Test class | Purpose | Behavior area | Status | Reason |
|---|---|---|---|---|
| `compat/ForbiddenClientImportsTest` | guard against client-only imports | dedicated server compatibility | KEEP | критичный safety guard |
| `diagnostics/WorldFluidDiagnosticsTest` | world fluid diagnostics shape | world fluids diagnostics | KEEP | контракт dump-полей |
| `diagnostics/SourceScanSummaryLivingShieldingTest` | living shielding counters serialized | shielding diagnostics | KEEP | фиксирует summary contract |
| `diagnostics/RadiusVisualizationServerSafetyTest` | no client path + bounded caps | radius visualization safety | KEEP | server-safe гарантия |
| `diagnostics/HandlerDiagnosticsDynamicRadiusTest` | handler sample includes dynamic context | handler diagnostics | KEEP | важен для explainability |
| `diagnostics/CreateCarrierDiagnosticsTest` | bounded create carrier samples | create diagnostics | KEEP | bounded contract |
| `diagnostics/EntityCarrierDiagnosticsTest` | entity diagnostics counters/samples | entity diagnostics | KEEP | coverage полезная |
| `diagnostics/RadiusVisualizationSamplesTest` | visualization sampling/caps | radius visualization | KEEP | deterministic helper contract |
| `gameplay/LivingTargetSelectionTest` | selection filters/cap | living target policy | KEEP | policy-level must-have |
| `gameplay/LivingEntityEffectDecisionTest` | decision reasons | effect apply policy | KEEP | must-have |
| `radiation/NestedContainerExtractorTest` | component extraction + limits | nested containers | KEEP | core behavior |
| `radiation/NestedProviderIntegrationAuditTest` | wiring/compact command rows | nested integration | REVIEW | строковые/интеграционные проверки хрупкие |
| `radiation/CreateTransientCarrierAggregationTest` | transient aggregation + dynamic radius | create transient carrier | KEEP | value tests |
| `radiation/AggregatedSourceAccumulatorTest` | aggregate item/fluid math | aggregation | KEEP | pure logic |
| `radiation/EntityInventoryCarrierAdapterTest` | classify/adapt entity inventories | entity inventories | KEEP | adapter contract |
| `radiation/CreateTransientCarrierExtractorTest` | known-path payload parse | create transient extraction | KEEP | high-signal parser tests |
| `radiation/DynamicRadiusModelTest` | formula/cap/units | dynamic radius | KEEP | baseline formula guard |
| `radiation/WorldFluidSourceProviderTest` | cluster discovery and behavior | world fluid provider | KEEP | strong provider coverage |
| `radiation/EntityCarrierExtractionTest` | dropped/frame/aura/nested extraction | entity sources | KEEP | key behavior |
| `radiation/SourceOverrideEngineTest` | exclude+contain+force precedence | overrides pipeline | SPLIT | coverage сохранено, но монолит остаётся до дальнейшего cleanup |
| `radiation/PlayerInventorySourceProviderContractTest` | direct/nested/disable-path + candidate emission | player inventory provider | KEEP | закрывает blocker contract gap |
| `radiation/BlockSourceProviderContractTest` | scan-radius clamp + contract markers for match/candidate path | block provider | KEEP | закрывает blocker gap частично (runtime gap остаётся) |
| `radiation/BlockEntityInventorySourceProviderContractTest` | container + nested + candidate behavior | block entity inventory provider | KEEP | закрывает blocker contract gap |
| `radiation/BlockItemHandlerSourceProviderContractTest` | handler match/nested/candidate behavior | block item handler provider | KEEP | закрывает high gap |
| `radiation/BlockFluidHandlerSourceProviderContractTest` | handler fluid match/1mB/candidate behavior | block fluid handler provider | KEEP | закрывает high gap |
| `radiation/ExposureEnginePipelineContractTest` | override-order + post-shielding totals | pipeline contract | KEEP | закрывает blocker gap |
| `radiation/OverrideSelectorCarrierBlockSemanticsTest` | carrierBlockId selector behavior | override selector semantics | KEEP | `SPEC_CODE_MISMATCH_CANDIDATE` tracked explicitly |
| `radiation/SourceOverrideExcludeContractTest` | focused exclude behavior contract | override split suite | KEEP | split start |
| `radiation/SourceOverrideContainContractTest` | focused contain behavior contract | override split suite | KEEP | split start |
| `radiation/SourceOverrideForceContractTest` | focused force behavior contract | override split suite | KEEP | split start |
| `radiation/SourceOverridePipelineOrderContractTest` | focused override pipeline order contract | override split suite | KEEP | split start |
| `radiation/FlowingFluidRuleResolutionTest` | exact vs fallback | fluid rules semantics | KEEP | concise and critical |
| `radiation/NestedProviderRegressionAuditTest` | disable path, no double-count, fields | nested regression | KEEP | regression value |
| `config/RadWorksConfigTest` | baseline defaults | config | KEEP | sanity baseline |
| `config/RadWorksConfigLivingTargetsTest` | living defaults safe | config/living | KEEP | safety defaults |
| `config/RadWorksConfigServerPolicyTest` | server-safe bounds | config/server policy | KEEP | compatibility guard |
| `config/ConfigExposureThresholdClampIntentTest` | threshold clamp characterization + spec-intent marker | config mismatch handling | KEEP | `SPEC_CODE_MISMATCH_CANDIDATE` captured without runtime mutation |
| `radiation/RulesDataFilesSmokeTest` | bundled data files presence/fields | data contracts | REVIEW | mostly smoke; может требовать behavior split |
| `radiation/effects/EffectStrategyServiceTest` | threshold/armor effect preview | effect strategy | KEEP | core behavior |
| `radiation/SourceOverrideRulesLoaderTest` | loader/validation for overrides | override schema | KEEP | critical |
| `radiation/shielding/ShieldingEngineTargetAwarePolicyTest` | self-carried policy | shielding policy | KEEP | contract-critical |
| `radiation/shielding/ShieldingResultTest` | multiplier math | shielding math | KEEP | deterministic math |
| `radiation/shielding/ShieldingDiagnosticsContractTest` | shielding diagnostics structure | shielding diagnostics | KEEP | diagnostics contract |
| `radiation/shielding/ShieldingTagDataContractTest` | shielding tag entries | shielding data contract | KEEP | data consistency |
| `diagnostics/DiagnosticsDumpSchemaContractTest` | mandatory dump sections + override/summary counters schema | diagnostics dump contract | KEEP | закрывает high gap |
| `command/CommandOutputContractTest` | stable command output markers + registration tokens | command contract | KEEP | avoids brittle full-message assertions |

---

## 5. Former high-priority gaps (Stage 3C status)

| Candidate test | Stage 3C result | Residual gap / note |
|---|---|---|
| `PlayerInventorySourceProviderContractTest` | Added | in-world server-player execution path remains external/runtime |
| `BlockSourceProviderContractTest` | Added | `NEEDS_VERIFICATION`: full world-scan runtime behavior coverage remains |
| `BlockEntityInventorySourceProviderContractTest` | Added | more integration scenarios can be added later |
| `BlockItemHandlerSourceProviderContractTest` | Added | runtime capability edge-cases remain |
| `BlockFluidHandlerSourceProviderContractTest` | Added | runtime capability edge-cases remain |
| `ExposureEnginePipelineContractTest` | Added | full ExposureEngine+ShieldingEngine E2E with real level remains |
| `DiagnosticsDumpSchemaContractTest` | Added | runtime dump generation E2E remains optional |
| `CommandOutputContractTest` (stable markers) | Added | command dispatcher/runtime formatting remains `NEEDS_VERIFICATION` |
| `OverrideSelectorCarrierBlockSemanticsTest` | Added | `SPEC_CODE_MISMATCH_CANDIDATE` remains open for product decision |
| `ConfigExposureThresholdClampIntentTest` | Added | `SPEC_CODE_MISMATCH_CANDIDATE` remains open for product decision |

---

## 6. Obsolete or implementation-coupled tests

На Stage 3A только маркировка:

| Test class | Mark | Why |
|---|---|---|
| `radiation/SourceOverrideEngineTest` | REWRITE_CANDIDATE (split) | один класс покрывает слишком много поведения, сложно локализовать регрессии |
| `radiation/NestedProviderIntegrationAuditTest` | REVIEW | часть проверок зависит от формата строк/текста, а не контракта поведения |
| `radiation/RulesDataFilesSmokeTest` | REVIEW | smoke важен, но недостаточен как behavior contract |

`DELETE_CANDIDATE` на Stage 3A: **нет**.

---

## 7. External tester matrix

Локально трудно полноценно автоматизировать:
- real Create toolbox nested formats;
- Sophisticated Backpacks/Storage nested formats;
- Create contraptions/trains;
- Aeronautics/Simulated;
- комплексные modpack interactions (много модов одновременно).

Что обязательно собирать извне:
- `dump` (только confusing/failing cases);
- правила (`source_override_rules`), использованные в тесте;
- список версий модов;
- `latest.log` только при crash/неочевидных warning.

---

## 8. Future GameTest section (outline only)

GameTest пока не обязателен, но кандидаты:
- in-world shielding layout checks (источник/стена/цель);
- world fluid cluster deterministic checks;
- living target apply/no-apply сценарии;
- override precedence in-world (`exclude` > `force`, contain on forced rows).

На Stage 3A реализация GameTest не выполняется.

---

## 9. Post-Stage 3C recommendation

Следующий шаг после Stage 3C:
1. Перейти к Stage 3D cleanup: аккуратно сокращать монолит `SourceOverrideEngineTest` без потери coverage.
2. Добавить runtime-focused integration tests для блоковых/provider path, где сейчас coverage hybrid.
3. Подготовить один-два command-runtime сценария (не string-brittle) для закрытия `NEEDS_VERIFICATION`.
4. Зафиксировать product-решение по двум mismatch-темам:
   - `exposureThreshold` clamp intent;
   - `carrierBlockId` selector semantics.

Порядок приоритета:
- blocker/runtime-risk -> high -> normal -> low.
