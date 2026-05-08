# New Codex Master Prompt

Ты работаешь в новом пустом репозитории.

Твоя задача — создать чистый, документированный, поддерживаемый NeoForge Minecraft mod с нуля. Старый проект был KubeJS-прототипом RadWorks. Он содержит полезные правила, поведение и edge cases, но также содержит баги, хрупкие эвристики и недоделки.

`migration_export/` является источником истины:
- `00_README.md`
- `01_PROJECT_OVERVIEW.md`
- `02_FEATURE_INVENTORY.md`
- `03_BUGS_AND_GAPS.md`
- `04_CONTENT_REGISTRY.md`
- `05_BEHAVIOR_SPEC.md`
- `06_NEOFORGE_ARCHITECTURE.md`
- `07_MIGRATION_PHASES.md`
- `08_DIAGNOSTICS_REQUIREMENTS.md`
- `09_TESTING_PLAN.md`
- `11_AGENTS_TEMPLATE.md`

Не копируй KubeJS-код как архитектуру. Реализуй эквивалентное поведение заново на Java/NeoForge.

## Hard rules

- Начни только с Phase 0 из `07_MIGRATION_PHASES.md`.
- Не реализуй радиацию в первом шаге, если Phase 0 ещё не завершён.
- Не требуй KubeJS как runtime dependency.
- Не копируй старую KubeJS-архитектуру blindly.
- Не трогай Create/Aeronautics до стабильного ядра.
- Если что-то UNKNOWN, создай TODO и не выдумывай.
- Если старая реализация содержит BUG, не портируй баг как нормальное поведение.
- Если неясно, сохранить ли поведение, пометь как `MIGRATION_DECISION_REQUIRED`.
- Каждый шаг должен обновлять `MIGRATION_STATUS.md`.
- Каждый шаг должен добавлять или обновлять диагностику.
- Каждый шаг должен иметь ручной тест в `TESTING.md`.
- Не делай крупные невидимые изменения без фиксации в status docs.

## First milestone

Сделай Phase 0:
- создать новый NeoForge project;
- настроить Gradle;
- добавить mod metadata;
- добавить `AGENTS.md` на основе `migration_export/11_AGENTS_TEMPLATE.md`;
- добавить `README.md`;
- добавить `MIGRATION_STATUS.md`;
- добавить `TESTING.md`;
- добавить `DIAGNOSTICS.md`;
- добавить `/radworks version`;
- добавить `/radworks dump`;
- запустить build/check, если команды доступны.

В Phase 0 не реализуй:
- radiation exposure;
- radioactive items;
- shielding;
- effects;
- Create integration;
- Aeronautics integration.

## Development style

Двигайся фазами из `07_MIGRATION_PHASES.md`.

После каждой фазы:
- запусти build/test, если команды доступны;
- обнови `MIGRATION_STATUS.md`;
- обнови `TESTING.md`;
- обнови `DIAGNOSTICS.md`;
- перечисли changed files;
- перечисли known limitations;
- перечисли manual test steps.

## Behavior preservation

Не теряй:
- ID предметов/блоков/жидкостей из `04_CONTENT_REGISTRY.md`;
- balance values из `05_BEHAVIOR_SPEC.md`;
- known bugs/gaps из `03_BUGS_AND_GAPS.md`;
- diagnostics requirements из `08_DIAGNOSTICS_REQUIREMENTS.md`;
- phased migration order из `07_MIGRATION_PHASES.md`.

## Unknown handling

Если данных недостаточно:
1. Не угадывай.
2. Добавь TODO с ссылкой на конкретный UNKNOWN из migration_export.
3. Если нужен пользовательский Minecraft test, добавь шаг в `TESTING.md`.
4. Если нужен diagnostic output, добавь команду или поле dump.

## Target architecture reminder

Новый мод должен иметь provider-based core:
- `RadiationSourceProvider`;
- `RadiationRules`;
- `ExposureEngine`;
- `ShieldingEngine`;
- `SourceCache`;
- `DiagnosticsService`.

Optional integrations must be isolated:
- `integration/create`;
- `integration/aeronautics`.

Core engine не должен напрямую зависеть от Create/Aeronautics classes.
