# EXTRA_STAGE_3B_TEST_AUDIT_CLEANUP_PLAN

## Stage purpose

Extra Stage 3B фиксирует **план чистки и усиления тестов** на базе:
- `RADWORKS_BEHAVIOR_SPEC.md`
- `RADWORKS_TEST_MATRIX.md`

Ограничения Stage 3B:
- без изменений в `src/main/**`;
- без изменений в `src/test/**`;
- без удаления тестов;
- без адаптации спецификации под текущие баги реализации.

---

## 1) Existing test suite inventory (grouped actions)

### A. Rules
- Scope: `SourceOverrideRulesLoaderTest`, `RulesDataFilesSmokeTest`, `FlowingFluidRuleResolutionTest`
- Action:
  - `KEEP`: loader + fallback logic
  - `REVIEW`: smoke-only data tests
  - `ADD (high)`: malformed/optional-safe rule behavior cases

### B. Source providers
- Strong coverage now:
  - `WorldFluidSourceProviderTest` (`KEEP`)
  - `EntityCarrierExtractionTest` (`KEEP`)
  - `EntityInventoryCarrierAdapterTest` (`KEEP`)
  - `NestedProviderRegressionAuditTest` (`KEEP`)
  - `CreateTransientCarrierExtractorTest` / `CreateTransientCarrierAggregationTest` (`KEEP`)
- Needs review:
  - `NestedProviderIntegrationAuditTest` (`REVIEW`, formatting-sensitive checks)
- Missing blocker/high provider contracts:
  - `PlayerInventorySourceProviderContractTest` (blocker)
  - `BlockSourceProviderContractTest` (blocker)
  - `BlockEntityInventorySourceProviderContractTest` (blocker)
  - `BlockItemHandlerSourceProviderContractTest` (high)
  - `BlockFluidHandlerSourceProviderContractTest` (high)

### C. Dynamic radius
- Current: `DynamicRadiusModelTest`, plus partial coverage in fluid/create tests.
- Action:
  - `KEEP`
  - `ADD (normal)`: cross-provider monotonicity/consistency tests

### D. Shielding
- Current: `ShieldingResultTest`, `ShieldingEngineTargetAwarePolicyTest`, diagnostics/tag tests
- Action:
  - `KEEP`
  - `ADD (high)`: integration contract `contain/force -> shielding -> final contribution`

### E. Living targets / effect strategy
- Current: `LivingTargetSelectionTest`, `LivingEntityEffectDecisionTest`, `EffectStrategyServiceTest`
- Action:
  - `KEEP`
  - `ADD (high)`: end-to-end exposure-to-decision contract (player + living targets)

### F. Source overrides
- Current: `SourceOverrideEngineTest` (monolith)
- Action:
  - `SPLIT (high)`:
    - `SourceOverrideExcludeContractTest`
    - `SourceOverrideContainContractTest`
    - `SourceOverrideForceContractTest`
    - `SourceOverridePipelineOrderContractTest`
  - existing monolith: `REWRITE_CANDIDATE`, remove only after equivalent replacement and explicit approval

### G. Diagnostics
- Current section-level tests are present.
- Action:
  - `KEEP`
  - `ADD (high)`: `DiagnosticsDumpSchemaContractTest` for mandatory dump sections/counters

### H. Config
- Current: `RadWorksConfigTest`, `RadWorksConfigServerPolicyTest`, `RadWorksConfigLivingTargetsTest`
- Action:
  - `KEEP`
  - `ADD (high)`: `ConfigExposureThresholdClampIntentTest` (mismatch-candidate area)

### I. Command / server compatibility
- Current: `ForbiddenClientImportsTest`, `RadiusVisualizationServerSafetyTest`
- Action:
  - `KEEP`
  - `ADD (high)`: `CommandOutputContractTest` (stable semantic tokens, not full-string fragile assertions)
  - `ADD (normal)`: documented runServer smoke policy check path

### J. Delete candidates
- Stage 3B rule: `DELETE_CANDIDATE = none`.
- Any delete/rename only:
  - after replacement coverage exists,
  - and explicit approval in Stage 3D.

---

## 2) Prioritization and execution order (Stage 3C/3D)

### Stage 3C strict order

#### 3C.1 Blocker provider contracts
1. `PlayerInventorySourceProviderContractTest`
2. `BlockSourceProviderContractTest`
3. `BlockEntityInventorySourceProviderContractTest`
4. `ExposureEnginePipelineContractTest` (minimum pipeline-order contract)

#### 3C.2 High provider/capability contracts
1. `BlockItemHandlerSourceProviderContractTest`
2. `BlockFluidHandlerSourceProviderContractTest`
3. `OverrideSelectorCarrierBlockSemanticsTest`

#### 3C.3 High diagnostics/command/config contracts
1. `DiagnosticsDumpSchemaContractTest`
2. `CommandOutputContractTest`
3. `ConfigExposureThresholdClampIntentTest`

#### 3C.4 Split/cleanup monoliths
1. Split `SourceOverrideEngineTest` into focused suites
2. Sanitize formatting-coupled checks in `NestedProviderIntegrationAuditTest`
3. Keep smoke tests but pair with behavior-first replacements where needed

#### 3C.5 Normal/low gaps + matrix sync
1. Additional monotonicity/integration tests
2. Update `RADWORKS_TEST_MATRIX.md` with actual post-3C coverage

---

## 3) Mismatch handling policy

### Mismatch A: exposureThreshold clamp semantics
- Type: `SPEC_CODE_MISMATCH_CANDIDATE`
- Stage 3C test strategy:
  1. Characterization test captures current runtime fact.
  2. Intent/spec test documents expected contract with `NEEDS_DECISION` marker.
- Runtime behavior changes are out-of-scope for Stage 3B/3C.
- If spec wins, open a separate implementation phase after audit.

### Mismatch B: carrierBlockId selector semantics
- Type: `SPEC_CODE_MISMATCH_CANDIDATE`
- Stage 3C test strategy:
  1. Characterization test for current selector matching.
  2. Spec-intent test with explicit mismatch marker.
- Runtime fix only in a dedicated post-3C behavior-fix phase.

### Global mismatch rule
- Never mutate spec to hide bug behavior.
- First record fact and divergence, then make separate product decision.

---

## 4) Test naming/style and package organization

### Naming rule (mandatory)
- Format: `methodName_scenario_expectedBehavior`
- Example: `collect_whenRuleMissing_shouldNotCreateSourceRow`

### Style rules
- Tests assert behavior contract, not private implementation shape.
- Avoid duplicating production logic in expected-value generation.
- JSON formatting/order assertions only when schema explicitly requires it.
- Command output tests should assert stable semantic tokens, not whole chat strings.

### Target package layout for new Stage 3C tests
- `radiation/providers/**`
- `radiation/pipeline/**`
- `radiation/overrides/**`
- `radiation/nested/**`
- `diagnostics/**`
- `config/**`
- `gameplay/**`
- `command/**`

Current packages remain valid; migration is incremental.

---

## 5) Stage 3C commit split

1. **3C.1** Provider blocker contracts  
   (inventory/block/block-entity + minimal pipeline order)
2. **3C.2** Capability + selector mismatch contracts  
   (item/fluid handler + carrierBlock semantics)
3. **3C.3** Diagnostics/command/config contracts  
   (dump schema + command output + threshold mismatch tests)
4. **3C.4** Split/rewrite candidates  
   (`SourceOverrideEngineTest` split, sanitize integration-audit fragility)
5. **3C.5** Gap closure and matrix refresh  
   (normal/low tests + update test matrix)

---

## 6) Risks

- New contract tests may expose real bugs (expected).
- Overly strict command-output tests can be brittle.
- Provider tests may require careful synthetic fixtures/runtime wrappers.
- Synthetic-only coverage may miss integration runtime behavior.
- Overfitting to current class layout may reduce refactor resilience.

---

## 7) Stage 3C acceptance criteria

- `./gradlew test` PASS
- `./gradlew build` PASS
- No gameplay behavior change unless explicitly approved as separate fix phase
- Blocker tests added first
- Existing tests kept or split with replacement coverage
- No deletions without explicit approval and replacement proof
- `RADWORKS_TEST_MATRIX.md` updated after implementation

---

## 8) Exact next implementation prompt (Stage 3C.1)

```text
Реализуй только Stage 3C.1 — Blocker Provider Contract Tests.

Цель:
Добавить blocker-level behavior tests без изменений gameplay кода.

Добавить тесты:
1) PlayerInventorySourceProviderContractTest
2) BlockSourceProviderContractTest
3) BlockEntityInventorySourceProviderContractTest
4) ExposureEnginePipelineContractTest (минимальный pipeline-order contract)

Требования:
- Тесты проверяют behavior contract из RADWORKS_BEHAVIOR_SPEC.md, а не private implementation детали.
- Никаких изменений в src/main/**.
- Никаких удалений существующих тестов.
- Если выявлен mismatch/spec ambiguity, маркировать как NEEDS_VERIFICATION или SPEC_CODE_MISMATCH_CANDIDATE в комментарии теста.
- Сохранить deterministic assertions и минимизировать brittle string checks.

Run:
- ./gradlew test
- ./gradlew build

Final report:
- changed files;
- какие blocker-tests добавлены;
- какие contract assertions покрыты;
- test/build results;
- найденные mismatches/uncertainties.
```

---

## 9) Assumptions

- `RADWORKS_BEHAVIOR_SPEC.md` и `RADWORKS_TEST_MATRIX.md` — рабочий baseline контрактов Stage 3A.
- Stage 3B/3C по умолчанию не меняет gameplay runtime behavior.
- Любой delete/rename legacy tests требует replacement coverage + explicit approval.

