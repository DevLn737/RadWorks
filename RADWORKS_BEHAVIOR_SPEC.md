# RADWORKS_BEHAVIOR_SPEC

## 1. Purpose

Этот документ фиксирует **ожидаемое поведение** RadWorks (до Beta 0.6 включительно) как продуктовый контракт.  
Документ:
- не является внутренним разбором классов;
- не заменяет код/тесты;
- используется как основа перед Stage 3B/3C (пересборка тестов по поведению).

Главный принцип: тесты должны проверять контракт поведения, а не случайные детали текущей реализации.

---

## 2. Terminology

- **source / источник**: объект, который даёт вклад в радиацию.
- **candidate / кандидат**: наблюдённый объект, из которого *может* быть создан источник (например для `force`).
- **observed candidate / уже увиденный кандидат**: кандидат, реально переданный существующим этапом обнаружения.
- **rule / правило**: запись из `radiation_rules` или `source_override_rules`.
- **contribution / вклад**: вклад конкретного источника в итоговый расчёт.
- **exposure / итоговое воздействие**: суммарный вклад всех активных источников.
- **dynamic radius / динамический радиус**: радиус, увеличивающийся от агрегированного количества/объёма.
- **shielding / экранирование**: снижение вклада за счёт защитных блоков между источником и целью.
- **containment / подавление контейнером**: правило `contain`, которое масштабирует или зануляет вклад.
- **forced source / принудительный источник**: источник, созданный правилом `force` из observed candidate.
- **target / цель воздействия**: игрок или живая сущность, для которой считается воздействие.

---

## 3. Global pipeline contract

Ожидаемый порядок этапов:
1. discover/extract
2. aggregate
3. exclude
4. contain
5. force only from observed candidates
6. contain forced rows
7. shielding
8. exposure/effect decision

### 3.1 discover/extract
- Вход: мир, target context, активные rules.
- Выход: начальные source rows + observed candidates.
- Запрещено: новые неограниченные сканеры, broad arbitrary NBT scan.
- Diagnostics: `sourceScanSummary` + профильные diagnostics sections.

### 3.2 aggregate
- Вход: найденные source rows.
- Выход: агрегированные rows (items/fluids/clustered world fluids).
- Запрещено: изменение формул под конкретный баг.
- Diagnostics: `aggregateCount`, `aggregateAmountMb`, `contributingStacks`, `dynamicRadiusBonus`.

### 3.3 exclude
- Вход: агрегированные rows + enabled `exclude`.
- Выход: rows с `overrideMode=excluded` и нулевым вкладом.
- Запрещено: удалять объяснимость (row/diagnostics должны показывать причину).
- Diagnostics: `sourceOverrideDiagnostics.sourcesExcluded`, `sourcesExcludedByOverride`.

### 3.4 contain
- Вход: post-exclude rows + enabled `contain`.
- Выход: scaled/suppressed rows (`overrideMode=contained`).
- Запрещено: менять глобальные формулы radii.
- Diagnostics: `containmentScaledSources`, `containmentSuppressedSources`.

### 3.5 force only from observed candidates
- Вход: observed candidates + enabled valid `force`.
- Выход: forced rows поверх уже наблюдённого контекста.
- Запрещено:
  - новые scanner loops,
  - расширение scan radius,
  - дополнительные capability lookups,
  - deep NBT/components traversal за пределами существующих provider loops.
- Diagnostics: `forceCandidatesObserved`, `forcedSourcesAdded`, skip reasons.

### 3.6 contain forced rows
- Вход: forced rows + contain rules.
- Выход: forced rows после contain.
- Запрещено: обходить contain для forced rows.
- Diagnostics: containment counters должны учитывать forced rows.

### 3.7 shielding
- Вход: post-override rows.
- Выход: rows с `rawContribution` -> `finalContribution`.
- Запрещено: применять shielding до override-процессинга.
- Diagnostics: `shielding*` поля в row + `sourceScanSummary.shielding*`.

### 3.8 exposure/effect decision
- Вход: final rows.
- Выход: `totalExposure`, decision по эффекту.
- Запрещено: скрытый side-effect, не отражённый в diagnostics.
- Diagnostics: `lastExposureSnapshot`, `gameplay.*Decisions`.

---

## 4. Radiation rules contract

- Источник правил: `data/radworks/radiation_rules/*.json`.
- Обязательные поля: `type`, `id`, `strength`, `radius`, `respectsShielding`, `enabled`.
- `profile` при отсутствии по умолчанию трактуется как `always`.
- `optionalModId` + `required=false` должны работать optional-safe (не фатальная ошибка при отсутствии мода).
- Семантика типов:
  - `item`: инвентари/стеки;
  - `block`: размещённые блоки;
  - `fluid`: жидкости.
- Ожидание валидации:
  - невалидная схема/поля -> validation issues;
  - отсутствие required id в реестре -> error;
  - optional missing target -> warning/info, но не crash.

NEEDS_VERIFICATION:
- Точная severity некоторых validation issues может меняться между версиями, важно проверять по актуальному `validate` и dump.

---

## 5. Source discovery contract

Для каждого path ниже:
- “должно создаваться” = when source must be created;
- “не должно создаваться” = when must not be created.

### 5.1 player inventory
- Должно: если item rule активен и stack > 0.
- Не должно: если rule отсутствует/disabled.
- Required fields: `type=player_inventory`, `itemId`, `count|aggregateCount`, radius/contribution поля.
- Diagnostics: `inventoryStacksChecked`, `inventoryMatches`.
- Ограничения: это server-side path, не GUI-скан клиента.

### 5.2 static blocks
- Должно: активный `block` rule + block в scan radius.
- Не должно: блок вне радиуса или без rule.
- Required fields: `type=block`, `blockId`, `position`.
- Diagnostics: `blockPositionsChecked`, `blockMatches`.

### 5.3 world fluids
- Должно: обнаруженный fluid state, активный fluid rule (exact/fallback), кластер в discovery window.
- Не должно: empty fluid state, нет rule, кластер вне effective radius.
- Required fields: `type=world_fluid`, `fluidId`, `position`, aggregate fluid fields.
- Diagnostics: `worldFluidDiagnostics`, `sourceScanSummary.worldFluid*`.
- Ограничения: кластер ограничен discovery radius.

### 5.4 block entity inventories
- Должно: block entity container + item rule.
- Не должно: пустые/нерелевантные слоты, нет rule.
- Required fields: `type=block_entity_inventory`, `blockId`, `containerPos`.
- Diagnostics: `container*` counters.

### 5.5 block item handlers
- Должно: handler найден + item rule.
- Не должно: нет handler/нет rule/вне радиуса.
- Required fields: `type=block_item_handler`, `blockId`, capability context.
- Diagnostics: `itemHandler*`, `handlerDiagnostics.itemHandlerNonMatchingSamples`.

### 5.6 block fluid handlers
- Должно: fluid handler + fluid rule.
- Не должно: нет handler/нет rule/вне радиуса.
- Required fields: `type=block_fluid_handler`, `fluidId`, `amountMb`.
- Diagnostics: `fluidHandler*`, `handlerDiagnostics.fluidHandlerNonMatchingSamples`.

### 5.7 Create transient carriers
- Должно: включён integration, known-path payload распознан.
- Не должно: path missing/malformed/no rule.
- Required fields: `type=create_transient_item|create_transient_fluid`, path/context поля.
- Diagnostics: `createCarrierDiagnostics`.
- Ограничения: только known paths, без hard dependency.

### 5.8 dropped items
- Должно: найден `ItemEntity` + item rule.
- Не должно: нет rule/вне радиуса.
- Required fields: `type=entity_dropped_item`, carrier entity fields.
- Diagnostics: `entityCarrierDiagnostics`.

### 5.9 item frames
- Должно: frame содержит item с rule.
- Не должно: empty/нет rule.
- Required fields: `type=entity_item_frame`.
- Diagnostics: `entityCarrierDiagnostics`.

### 5.10 entity inventories
- Должно: chest boats / pack animals / generic capability path доступен + rule.
- Не должно: path disabled/неподдержан/нет rule.
- Required fields: `type=entity_inventory`, `carrierEntityType`, `carrierEntityId`.
- Diagnostics: `entityCarrierDiagnostics` inventory counters.

### 5.11 player aura
- Должно: nearby other player carries relevant items.
- Не должно: self-аура для того же игрока.
- Required fields: `type=entity_player_inventory_aura`.
- Diagnostics: `entityCarrierDiagnostics`.

### 5.12 nested vanilla containers
- Должно: поддержанный формат (`DataComponents.CONTAINER`, `BUNDLE_CONTENTS`) + nested enabled.
- Не должно: unsupported format / empty / depth/item limits.
- Required fields: `nested=true`, `nestedDepth`, `containerItemId`, `containerPath`, `extractionMode`.
- Diagnostics: `nestedContainerDiagnostics`.

---

## 6. Dynamic radius contract

- Базовая формула: `effectiveRadius = min(maxCap, baseRadius + scale * log2(max(1, units)))`.
- Единицы:
  - items: `aggregateCount`;
  - fluids: `aggregateAmountMb / 1000`.
- `dynamicRadiusBonus = effectiveRadius - baseRadius` (неотрицательный).
- Поведение:
  - 1 unit -> радиус близок к базовому;
  - много units -> радиус растёт;
  - cap должен ограничивать рост.
- Не должно происходить:
  - отрицательные радиусы;
  - рост радиуса при уменьшении aggregate units.

SPEC_CODE_MISMATCH_CANDIDATE:
- В `RadWorksConfig.exposureThreshold()` есть clamp через `DEFAULT_EXPOSURE_THRESHOLD` (1.0), что может конфликтовать с ожидаемым “произвольным порогом до верхней границы spec range”.

---

## 7. Shielding contract

- Применяется **после** override-этапов.
- Модель: line-sampling между source и центром цели.
- Row contract:
  - `rawContribution` — до shielding;
  - `finalContribution` — после shielding;
  - `shielding`/`shieldingBlocksHit`/`shieldingMultiplier`/`shieldingReduction`.
- Для non-positioned или self-carried источников — `shielding=not_applicable`.
- Для living targets shielding допустим, если включён target-aware режим.
- Ограничения:
  - нет multi-ray;
  - нет отдельной mob armor policy.

---

## 8. Effect strategy contract

- Поддерживаемые режимы: `own`, `external_if_present`, `external_only`, `disabled`.
- Порог: эффект не применяется при `totalExposure < threshold`.
- `external_if_present`: использовать внешний эффект, иначе fallback.
- `external_only`: при отсутствии внешнего эффекта применять нельзя.
- Manual command path:
  - `/radworks effect apply-self`
  - `/radworks effect clear-self`
  - `/radworks effect status`
- Auto-apply:
  - player path;
  - bounded living-entity path.
- В текущем scope отсутствуют:
  - persistent dose,
  - отдельный damage/exhaustion loop.

NEEDS_VERIFICATION:
- Точная длительность manual apply и детали форматирования command output подтверждать runtime-проверкой после merge.

---

## 9. Living entity target contract

- Player target: должен сохранять baseline-поведение.
- Mob/other living target: должен обрабатываться в bounded scan (radius + cap + interval).
- Armor stand: по умолчанию skip (если `applyEffectToArmorStands=false`).
- Self inventory behavior:
  - non-player living target может получать вклад от собственного переносимого источника;
  - self-carried shielding not_applicable.
- Entity-carried sources должны участвовать в расчёте, если проходят фильтры.
- Shielding для living targets должен использовать post-override rows.

---

## 10. Nested container contract

- Supported formats:
  - `DataComponents.CONTAINER`
  - `DataComponents.BUNDLE_CONTENTS`
- Лимиты:
  - `nestedContainerMaxDepth`
  - `nestedContainerMaxItemsPerSource`
- Должны появляться nested metadata поля в source rows.
- Supported contexts:
  - player inventory,
  - block/container inventory,
  - entity inventory,
  - dropped item,
  - item frame.
- Unsupported modded formats должны:
  - не вызывать crash,
  - объясняться через diagnostics.
- Запрещено:
  - arbitrary deep NBT scan.

---

## 11. Source override contract

### 11.1 exclude
- Совпавший источник исключается из вклада (`finalContribution=0`).
- Источник должен оставаться объяснимым в diagnostics/output.

### 11.2 contain
- `mode=suppress` -> итоговый вклад 0.
- `mode=scale` -> вклад умножается на multiplier.
- Прецедент конфликтов:
  - suppress сильнее scale;
  - при scale+scale берётся самый “жёсткий” (минимальный multiplier).

### 11.3 force
- Применяется только к observed candidates.
- Rule должен иметь валидные runtime fields:
  - `forceStrength`,
  - `forceRadius`,
  - `forceUnitMode`,
  - concrete selector.
- Не создаёт новый discovery path.

### 11.4 precedence and safety
- `exclude` сильнее `force`.
- Forced rows могут затем пройти contain.
- Dedupe обязателен: force не должен дублировать уже существующий/исключённый identity.

### 11.5 diagnostics expectations
- `sourceOverrideDiagnostics` должен явно показывать:
  - applied counts,
  - skip reasons,
  - samples по exclude/contain/force.

SPEC_CODE_MISMATCH_CANDIDATE:
- Семантика `carrierBlockId` selector может не всегда совпадать с ожидаемым “carrier block context”, если row не хранит отдельный carrierBlockId field.

---

## 12. Commands and diagnostics contract

Минимальные команды и их назначение:
- `/radworks validate` — контракт загрузки/валидации правил и config status.
- `/radworks sources` — источник и его поля (включая overrides/shielding).
- `/radworks exposure` — итоговое воздействие и decision hints.
- `/radworks dump` — полный diagnostics snapshot.
- `/radworks radius` — visual diagnostics radius.
- `/radworks effect` — manual effect path (`apply-self|clear-self|status`).

Минимальные ожидания от diagnostics:
- всегда доступны `sourceScanSummary` и профильные sections (`worldFluidDiagnostics`, `entityCarrierDiagnostics`, `nestedContainerDiagnostics`, `sourceOverrideDiagnostics`);
- любой skip/suppress/force edge case должен быть объясним.

---

## 13. Config contract

- Конфиг должен быть server-safe и deterministic.
- Caps/clamps должны защищать от взрывного scan/load.
- Toggle должен реально отключать соответствующее поведение.
- Disable-пути должны быть тестируемы (unit/logic level).

NEEDS_VERIFICATION:
- Граница интерпретации `exposureThreshold` в runtime (см. mismatch-кандидат выше) должна быть подтверждена как намеренное поведение или исправлена в будущем этапе.

---

## 14. Non-goals / current limitations

- no persistent dose
- no damage/exhaustion
- no mob armor policy
- no Create contraptions/trains
- no Aeronautics/Simulated deep integration
- no Create toolbox/Sophisticated deep nested adapters
- no arbitrary NBT scan

---

## 15. Spec uncertainty log

| Behavior area | Uncertainty | Needed evidence | Blocks tests? |
|---|---|---|---|
| `exposureThreshold` clamp semantics | SPEC_CODE_MISMATCH_CANDIDATE: runtime clamp может ограничивать threshold сильнее, чем ожидает конфиг | unit test + runtime validate/exposure checks | Нет (но влияет на high-priority config tests) |
| `carrierBlockId` selector meaning | NEEDS_VERIFICATION: совпадение selector-семантики с intended carrier-context | targeted override selector tests + sample dump | Нет (но влияет на contain/force tests) |
| Command output formatting stability | NEEDS_VERIFICATION между ветками/релизами | runtime command snapshots | Нет |
| Optional integration edge behavior (modpack) | NEEDS_VERIFICATION без полного локального набора модов | external tester dumps | Нет (automation partial) |
| Force candidate completeness by provider path | NEEDS_VERIFICATION для всех редких carrier contexts | provider-level coverage + external runtime evidence | Частично (high priority for Stage 3B) |

