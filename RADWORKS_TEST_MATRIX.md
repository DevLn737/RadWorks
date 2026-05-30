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
- **command-format tests**: консольный контракт команд (сейчас ограничено, есть gaps).
- **server smoke**: запуск сервера без classloading/client-only проблем.
- **external modpack tests**: то, что сложно уверенно автоматизировать локально.
- **future GameTest**: кандидаты in-game сценариев (план, без реализации).

---

## 3. Coverage matrix

| Behavior area | Expected behavior | Current automated coverage | Missing tests / gaps | Test type | Priority | Notes |
|---|---|---|---|---|---|---|
| Radiation rules loading | JSON load/validate, optional-safe | `SourceOverrideRulesLoaderTest`, `RulesDataFilesSmokeTest`, `FlowingFluidRuleResolutionTest` | dedicated behavior-first tests для malformed optional branches | unit | high | good base, но частично smoke-style |
| Player inventory source | item rules -> aggregated source | косвенно через `AggregatedSourceAccumulatorTest`, override tests | прямой contract test для `PlayerInventorySourceProvider` | provider | high | нужен явный behavior test |
| Static block source | block rules -> positioned source | частично через override/scan summary counters | явный contract test на `BlockSourceProvider` | provider | high | сейчас coverage косвенная |
| World fluid source | cluster discovery/aggregation/stability | `WorldFluidSourceProviderTest`, `WorldFluidDiagnosticsTest` | edge tests при mixed rules + override interaction | provider/diag | normal | покрытие сильное |
| Block entity inventory | container content discovery | частично через override селекторы + nested regressions | явный provider contract test | provider | high | сейчас нет прямого класса-теста |
| Block item handler | capability content discovery | `HandlerDiagnosticsDynamicRadiusTest` (diag), override tests (synthetic) | прямой provider test на create/match/skip | provider | high | gap |
| Block fluid handler | fluid capability discovery | override tests (synthetic), часть diagnostics | прямой provider test на mb scaling + rule match | provider | high | gap |
| Create transient carriers | known-path extraction | `CreateTransientCarrierExtractorTest`, `CreateTransientCarrierAggregationTest`, `CreateCarrierDiagnosticsTest` | больше integration-level contract tests на full provider flow | provider/diag | normal | good parser coverage |
| Entity dropped/item frame/aura | entity source extraction | `EntityCarrierExtractionTest`, `EntityCarrierDiagnosticsTest` | живые runtime edge cases в modpack | provider/ext | normal | core covered |
| Entity inventories | chest boat/pack animal/generic capability | `EntityInventoryCarrierAdapterTest`, `EntityCarrierDiagnosticsTest` | provider integration tests с real entity states | provider/ext | high | partial synthetic |
| Nested containers | supported vanilla components + limits | `NestedContainerExtractorTest`, `NestedProviderRegressionAuditTest`, `NestedProviderIntegrationAuditTest` | deep scenario matrix for nested+override+entity carriers | provider | high | база есть |
| Dynamic radius | formula, cap, units | `DynamicRadiusModelTest`, `CreateTransientCarrierAggregationTest`, `WorldFluidSourceProviderTest` | monotonicity across all source types | unit | normal | mostly covered |
| Shielding | post-override attenuation semantics | `ShieldingResultTest`, `ShieldingEngineTargetAwarePolicyTest`, `ShieldingDiagnosticsContractTest`, `SourceScanSummaryLivingShieldingTest` | integration tests “contain/force then shielding” в provider context | unit/provider | high | partial via SourceOverrideEngineTest |
| Living targets | bounded scan, decision reasons | `LivingTargetSelectionTest`, `LivingEntityEffectDecisionTest` | end-to-end gameplay contract tests с exposure path | gameplay | high | current tests policy-level |
| Effect strategy | mode + threshold + armor block | `EffectStrategyServiceTest` | mode/selection contract against runtime registry states | unit | normal | reasonable |
| Source overrides exclude/contain/force | precedence + dedupe + disable paths | `SourceOverrideEngineTest`, `SourceOverrideRulesLoaderTest` | split monolithic test into behavior-focused suites (Stage 3B) | unit | high | coverage broad but tightly coupled |
| Commands contract | intended output/semantics | indirect only | отсутствуют command output tests | command | high | clear gap |
| Dump diagnostics contract | required sections + key counters | diagnostics tests exist by section | unified dump schema contract test | diagnostics | high | needs single contract test |
| Config contract | defaults/bounds/server-safe | `RadWorksConfigTest`, `RadWorksConfigServerPolicyTest`, `RadWorksConfigLivingTargetsTest` | threshold clamp intent test (`SPEC_CODE_MISMATCH_CANDIDATE`) | config | high | important |
| Dedicated server compatibility | no client-only imports + safe side | `ForbiddenClientImportsTest`, `RadiusVisualizationServerSafetyTest` | repeatable runServer smoke automation policy | compat/smoke | normal | smoke mostly manual workflow |

---

## 4. Existing test audit table

> Stage 3A: только классификация. Ничего не удаляется и не переписывается.

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
| `radiation/SourceOverrideEngineTest` | exclude+contain+force precedence | overrides pipeline | SPLIT | покрытие широкое, класс стал монолитным |
| `radiation/FlowingFluidRuleResolutionTest` | exact vs fallback | fluid rules semantics | KEEP | concise and critical |
| `radiation/NestedProviderRegressionAuditTest` | disable path, no double-count, fields | nested regression | KEEP | regression value |
| `config/RadWorksConfigTest` | baseline defaults | config | KEEP | sanity baseline |
| `config/RadWorksConfigLivingTargetsTest` | living defaults safe | config/living | KEEP | safety defaults |
| `config/RadWorksConfigServerPolicyTest` | server-safe bounds | config/server policy | KEEP | compatibility guard |
| `radiation/RulesDataFilesSmokeTest` | bundled data files presence/fields | data contracts | REVIEW | mostly smoke; может требовать behavior split |
| `radiation/effects/EffectStrategyServiceTest` | threshold/armor effect preview | effect strategy | KEEP | core behavior |
| `radiation/SourceOverrideRulesLoaderTest` | loader/validation for overrides | override schema | KEEP | critical |
| `radiation/shielding/ShieldingEngineTargetAwarePolicyTest` | self-carried policy | shielding policy | KEEP | contract-critical |
| `radiation/shielding/ShieldingResultTest` | multiplier math | shielding math | KEEP | deterministic math |
| `radiation/shielding/ShieldingDiagnosticsContractTest` | shielding diagnostics structure | shielding diagnostics | KEEP | diagnostics contract |
| `radiation/shielding/ShieldingTagDataContractTest` | shielding tag entries | shielding data contract | KEEP | data consistency |

---

## 5. Missing high-priority tests (Stage 3B/3C backlog)

| Candidate test | Why missing | Priority |
|---|---|---|
| `PlayerInventorySourceProviderContractTest` | прямой provider contract для player inventory отсутствует | blocker |
| `BlockSourceProviderContractTest` | прямой contract для static block discovery отсутствует | blocker |
| `BlockEntityInventorySourceProviderContractTest` | прямой container provider contract отсутствует | blocker |
| `BlockItemHandlerSourceProviderContractTest` | прямой capability provider contract отсутствует | high |
| `BlockFluidHandlerSourceProviderContractTest` | прямой fluid capability provider contract отсутствует | high |
| `ExposureEnginePipelineContractTest` | единый end-to-end contract pipeline отсутствует | blocker |
| `DiagnosticsDumpSchemaContractTest` | нет одного теста на обязательные dump sections | high |
| `CommandOutputContractTest` (минимум smoke-format) | нет автоматической проверки командного контракта | high |
| `OverrideSelectorCarrierBlockSemanticsTest` | риск `SPEC_CODE_MISMATCH_CANDIDATE` по carrierBlock selector | high |
| `ConfigExposureThresholdClampIntentTest` | риск `SPEC_CODE_MISMATCH_CANDIDATE` по threshold clamp | high |

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

## 9. Stage 3B recommendation

Рекомендуемый следующий шаг:
1. Зафиксировать `RADWORKS_BEHAVIOR_SPEC.md` как reference contract.
2. Разбить `SourceOverrideEngineTest` на 3-4 компактных behavior suites (`exclude`, `contain`, `force`, `pipeline-order`).
3. Добавить missing blocker/high provider contract tests.
4. Добавить единый `DiagnosticsDumpSchemaContractTest`.
5. Только после этого пересматривать `REVIEW/REWRITE_CANDIDATE` классы на замену.

Порядок приоритета для Stage 3B:
- blocker -> high -> normal -> low.

