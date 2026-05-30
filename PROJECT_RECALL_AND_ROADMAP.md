# PROJECT_RECALL_AND_ROADMAP

## 1. Цель проекта
RadWorks — data-driven мод радиации для NeoForge, мигрированный с KubeJS-прототипа на standalone Java-архитектуру, с server-authoritative pipeline расчёта/экспозиции и ограниченной диагностикой.

Текущий baseline server-authoritative для discovery источников, расчёта exposure, shielding и effect decisions на стороне сервера. Мод расширяет механику радиации вокруг Create Nuclear/радиоактивных материалов, сохраняя optional-safe policy интеграций (без hard dependency на optional-моды, если это явно не требуется).

Источник:
- README.md (описание проекта, окружение, optional dependency policy)
- src/main/java/dev/radworks/radiation/ExposureEngine.java
- src/main/java/dev/radworks/diagnostics/DiagnosticsService.java

## 2. Сводка текущего baseline до Beta 0.6
- Baseline Beta 0.4: world fluid sources (clustered), entity-carried sources, living entity targets, target-aware shielding для living entities, dedicated-server hardening.
- Baseline Beta 0.5: bounded vanilla nested-container extraction (`DataComponents.CONTAINER`, `DataComponents.BUNDLE_CONTENTS`) интегрирован в source providers.
- Baseline Beta 0.6: слой source overrides (`exclude`, `contain`, `force`) с loader/validator + runtime application и diagnostics.

Источник:
- MIGRATION_STATUS.md (разделы Beta 0.4.x / 0.5 / 0.6.x)
- CHANGELOG.md (Unreleased entries для Beta 0.4/0.5/0.6)
- CURRENT_STATE_FOR_CHATGPT.md (таблица phase status)

## 3. Таймлайн реализованных возможностей

Примечание по evidence:
- commit hashes в этом разделе используются как **secondary evidence**;
- primary source-of-truth для статусов и scope: `CHANGELOG.md`, `MIGRATION_STATUS.md`, профильные классы в `src/main/java/dev/radworks/**`.

### Phase 1–5
- Phase 1: baseline data-driven radiation rules и validation.
- Phase 2: baseline диагностики player inventory sources.
- Phase 3: framework диагностики exposure (`sources`, `exposure`, bounded snapshots).
- Phase 4: block/static + container + capability providers, развиваемые по шагам (4A–4D).
- Phase 5: baseline shielding diagnostics и пакет real shielding candidates для внешнего ретеста.

Источник:
- CHANGELOG.md (Phase entries)
- MIGRATION_STATUS.md (Phase sections)
- примеры строк commit history: `c34b79f`, `a29599d`, `1ee17ec`, `7d8d434`, `14f2969`, `a5006ef`, `1aa1688`, `360a00c`

### Beta 0.4
- Добавлено стабильное покрытие world fluid sources, включая cluster discovery/aggregation.
- Добавлено покрытие entity sources (`entity_dropped_item`, `entity_item_frame`, `entity_player_inventory_aura`, `entity_inventory`).
- Добавлены bounded living entity effect targets.
- Добавлен target-aware shielding для living targets.
- Добавлены dedicated server compatibility audit и guard tests.

Источник:
- CHANGELOG.md (entries Beta 0.4.1..0.4.5)
- MIGRATION_STATUS.md (разделы Beta 0.4.x)
- примеры строк commit history: `25f283b`, `7d9e96f`, `ff6faa9`, `8bcb1c3`, `6501039`

### Beta 0.5
- Добавлено ядро nested extraction для vanilla data components.
- Добавлена nested-интеграция provider-ов по player/block/entity source paths.
- Добавлены nested diagnostics и config caps.
- Baseline закрыт, modded nested formats вынесены в research-first follow-up.

Источник:
- CHANGELOG.md (entries Beta 0.5)
- MIGRATION_STATUS.md (разделы Beta 0.5 / 0.5.3)
- примеры строк commit history: `4255e83`, `783ef1a`, `7aadbb7`

### Beta 0.6
- 0.6.1: source override schema/loader/validator/diagnostics.
- 0.6.2: runtime-применение `exclude`.
- 0.6.3: runtime-применение `contain`.
- 0.6.4: runtime-применение `force` только для observed candidates.
- 0.6.5: baseline closure/regression/handoff docs.

Источник:
- CHANGELOG.md (entries Beta 0.6)
- MIGRATION_STATUS.md (разделы Beta 0.6.1..0.6.5)
- примеры строк commit history: `e180c9f`, `1584e2d`, merge `7e49b12`

## 4. Текущее покрытие source discovery
- **player inventory** — **Implemented**. Прямая и nested-aware агрегация в player inventory path.
  - Источник: `PlayerInventorySourceProvider`, `ExposureEngine`, `NestedContainerExtractor`.
- **static blocks** — **Implemented** (исторически в документах встречается DEV/PARTIAL из-за эволюции old rule set).
  - Источник: `BlockSourceProvider`, `RadiationSourceType.BLOCK`.
- **world fluids** — **Implemented** с cluster aggregation и rule matching.
  - Источник: `WorldFluidSourceProvider`, `RadiationSourceType.WORLD_FLUID`.
- **vanilla containers** — **Implemented** через block entity inventory scan.
  - Источник: `BlockEntityInventorySourceProvider`.
- **block item handlers** — **Implemented** через NeoForge block item handler capability path.
  - Источник: `BlockItemHandlerSourceProvider`.
- **block fluid handlers** — **Implemented** через NeoForge block fluid handler capability path.
  - Источник: `BlockFluidHandlerSourceProvider`.
- **Create transient carriers** — **Implemented (optional-safe)** known-path extraction, без hard Create API dependency.
  - Источник: `CreateTransientCarrierSourceProvider`, optional policy в README.
- **entity sources** — **Implemented** (`dropped item`, `item frame`, `player aura`).
  - Источник: `EntityCarrierSourceProvider`, enum `RadiationSourceType`.
- **entity inventories** — **Implemented** (chest boats/pack animals/generic entity capability).
  - Источник: `EntityCarrierSourceProvider`, `EntityInventoryCarrierAdapter`.
- **nested vanilla containers** — **Implemented** (`DataComponents.CONTAINER`, `DataComponents.BUNDLE_CONTENTS`) с bounds.
  - Источник: `NestedContainerExtractor`.
- **forced candidates / override layer** — **Implemented** (`exclude` + `contain` + `force`), где force materialize-ится только из observed candidates.
  - Источник: `SourceOverrideEngine`, `SourceOverrideRulesLoader`, `ForceSourceCandidate`.

## 5. Текущая gameplay-механика
- **Exposure calculation**: total exposure — сумма contributions source-ов, собранных для target context.
  - Источник: `ExposureEngine.calculateForTarget`.
- **Dynamic radius**: используется provider-ами для aggregate-based расширения радиуса.
  - Источник: provider-ы, использующие `DynamicRadiusModel` (player/block/entity/world fluid paths).
- **Shielding**: target-aware shielding path выполняется после override processing.
  - Источник: `ShieldingEngine`, `ExposureEngine`.
- **Effect strategy**: runtime effect decision/apply path управляется config.
  - Источник: `RadiationGameplayService`, `EffectStrategyService`, `RadWorksConfig`.
- **Living entity targets**: присутствует bounded processing non-player living targets.
  - Источник: `RadiationGameplayService`, config keys в `RadWorksConfig`.
- **Override rules**: runtime rules загружаются из datapack path и применяются в engine.
  - Источник: `SourceOverrideRulesLoader`, `SourceOverrideEngine`.

Текущий override precedence:
1. discover/extract
2. aggregate
3. exclude
4. contain
5. force only from observed candidates
6. contain for forced rows
7. shielding
8. exposure/effect decision

Источник:
- `ExposureEngine.collectSourcesForTarget`
- `SourceOverrideEngine.applyForTargetKind`

## 6. Текущие команды и диагностика
- **`/radworks validate`**: подтверждает состояние validation rules + override loader.
  - Поддержка diagnostics: `sourceOverrideDiagnostics`, поля rule validation в dump/validate output.
- **`/radworks sources`**: показывает текущие source rows и контекст match/override/shielding.
  - Поддержка diagnostics: поля source row из `RadiationSource.toJson`, summary counters.
- **`/radworks exposure`**: вычисляет текущий target exposure из текущего source set.
  - Поддержка diagnostics: exposure rows + totals.
- **`/radworks dump`**: пишет consolidated diagnostics snapshot.
  - Разделы включают: `sourceScanSummary`, `handlerDiagnostics`, `worldFluidDiagnostics`, `createCarrierDiagnostics`, `entityCarrierDiagnostics`, `nestedContainerDiagnostics`, `sourceOverrideDiagnostics`.
- **`/radworks radius`**: управляет/проверяет radius visualization service.
  - Поддержка diagnostics: раздел `radiusVisualization` в dump.
- **`/radworks effect`**: текущая command tree включает `apply-self`, `clear-self`, `status`.
  - Поддержка diagnostics: статус gameplay/effect strategy в dump и command output.

Источник:
- `src/main/java/dev/radworks/command/RadWorksCommands.java`
- `src/main/java/dev/radworks/command/EffectCommand.java`
- `src/main/java/dev/radworks/diagnostics/DiagnosticsService.java`

NEEDS_VERIFICATION:
- Точные детали форматирования чата для каждой команды во всех ветках требуют runtime evidence (`/radworks ...` output + dump pair) после последних merge.

## 7. Текущие области конфигурации
- **gameplay**:
  - `enabled`, `autoApplyEffect`, `exposureThreshold`, `effectDurationTicks`, `scanIntervalTicks`,
  - living-target toggles/caps,
  - `applyShieldingToLivingEntities`,
  - `damageEnabled`,
  - `alwaysShowRadiusVisualization`,
  - `effectMode`.
- **rules**:
  - dev-rule toggle,
  - dynamic-radius settings,
  - nested-container settings,
  - source override toggles/cap (`sourceOverridesEnabled`, `sourceExclusionsEnabled`, `sourceContainmentEnabled`, `forcedSourcesEnabled`, `sourceOverrideDiagnosticSampleCap`).
- **integrations**:
  - настройки Create transient carrier,
  - настройки entity carrier,
  - world fluid discovery radius.
- **diagnostics/debug**:
  - Название отдельной config-группы в `RadWorksConfig` — **UNKNOWN**; поведение diagnostics/debug присутствует через runtime services/commands (`/radworks debug`, dump sections), а не как standalone `diagnostics` config category в текущем файле.

NEEDS_VERIFICATION:
- Перед Extra Stage 2 (Admin Guide) точный перечень config keys должен быть повторно свернут напрямую с `RadWorksConfig.java` (с учётом возможных изменений между ветками/merge).

Источник:
- `src/main/java/dev/radworks/config/RadWorksConfig.java`
- `src/main/java/dev/radworks/command/RadWorksCommands.java`
- `src/main/java/dev/radworks/diagnostics/DiagnosticsService.java`

## 8. Текущая тестовая стратегия
- Unit/logic-heavy стратегия по radiation, overrides, nested extraction и diagnostics assertions.
- Основной локальный gate: `./gradlew test` и `./gradlew build`.
- Optional smoke path: `./gradlew runServer` (документированная интерпретация EULA/startup).
- External tester scope остаётся обязательным для подтверждения modpack/runtime behavior и ambiguous cases.
- В Extra Stage 3 будет пересмотр hardening/spec coverage и закрытие оставшихся coverage gaps.

Источник:
- TESTING.md (automated checks и runServer smoke sections)
- MIGRATION_STATUS.md (dedicated server audit и staged validations)
- BETA_TESTER_HANDOFF.md

## 9. Известные ограничения
- no persistent dose
- no damage/exhaustion
- no mob armor policy
- no Create contraptions/trains
- no Aeronautics/Simulated
- no Create toolbox/Sophisticated deep nested adapters
- no arbitrary NBT scan

Источник:
- README.md (Known limitations)
- CURRENT_STATE_FOR_CHATGPT.md (phase status + open items)
- TESTING.md / BETA_TESTER_HANDOFF.md (retest и research-first notes)

## 10. Полный roadmap
- **Extra Stage 2**: Admin & Tester Guide (операции + инструкции retest + repeatable report format).
- **Extra Stage 3**: Spec-driven test hardening (coverage matrix against current behavior contract).
- **Beta 0.7**: planned topic placeholder (NEEDS_VERIFICATION; финальная тема не утверждена на этом этапе).
- **Future research/integration stages**:
  - modded nested adapters (Create toolbox, Sophisticated),
  - deeper integration tracks (Create contraptions/trains, Aeronautics/Simulated),
  - research по эволюции shielding/armor model.

Источник:
- User-approved stage context в текущем веточном процессе (список extra stages)
- CURRENT_STATE_FOR_CHATGPT.md / MIGRATION_STATUS.md (open-phase notes)

## 11. Список открытых будущих работ

### Carry-over / известные будущие задачи
- Финальный внешний ретест сценариев Beta 0.6 override в реальном modpack runtime.
- Консолидация admin/tester operational playbook (Stage 2).
- Расширение regression/spec hardening matrix (Stage 3).

### Research tracks
- Mapping nested-формата Create toolbox (adapter design на основе реальных данных).
- Mapping nested-формата Sophisticated Backpacks/Storage (optional-safe adapter design).
- Исследование multi-ray shielding (если future policy решит заменить line-sampling).

### Candidate inputs для Beta 0.7
- Любой утверждённый scope после результатов Stage 2/3.
- Integration topics только при наличии evidence и утверждённых ограничений.

Источник:
- BETA_TESTER_HANDOFF.md
- TESTING.md
- CURRENT_STATE_FOR_CHATGPT.md

## 12. Merge/artifact policy
- Workflow разработки: git branches -> commits -> docs/code review -> merge в `main`.
- Commit policy: small scoped commits по стадиям; behavior changes изолировать от closure/docs passes.
- Gates перед merge: `./gradlew test` + `./gradlew build`; optional `runServer` smoke, когда релевантно.
- Допустимы optional tags для milestones (пример: `beta-0.6`).
- Политика именования артефакта для beta-distribution:
  - `radworks-0.1.0-beta0.6-YYYYMMDD.jar`
- Remote policy:
  - Forgejo — primary remote/storage.
  - GitHub mirror может использоваться, если настроен.

Источник:
- README.md (build/install)
- TESTING.md (gate workflow)
- git history в текущих ветках (merge-oriented stage workflow)
