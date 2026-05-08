# Migration Export

## Purpose

Этот пакет предназначен для переноса старого KubeJS-проекта RadWorks KubeJS в новый чистый NeoForge-мод. Он должен быть самодостаточным контекстом для нового Codex, который будет работать в новом пустом репозитории без доступа к истории этого диалога.

## Source project

Старый проект — набор KubeJS-скриптов радиации для Minecraft-сборки с Create / Create Nuclear. В текущем репозитории нет стандартной структуры `kubejs/`; найден набор отдельных файлов, которые по документации должны быть помещены в `kubejs/server_scripts/`, и один startup-script `radiation_modifiers.js`, предназначенный для `kubejs/startup_scripts/`.

Основная механика: радиоактивные блоки, предметы, жидкости, контейнеры, сущности, инвентарь игрока и `create:minecart_contraption` могут накладывать эффект `createnuclear:radiation`. Есть экранирование блоками, защита полным комплектом брони, debug-режим, частицы и частичная поддержка Create/Sophisticated Storage/Sophisticated Backpacks.

## Target project

Целевой проект — чистый, документированный, поддерживаемый и версионированный NeoForge-мод. KubeJS не должен быть обязательной runtime-зависимостью нового мода. Старый KubeJS-код является источником знаний, прототипом поведения, списком багов и требований, но не архитектурой, которую нужно копировать.

## Important warning

Текущая KubeJS-реализация содержит баги, недоделки и хрупкие NBT-эвристики. Новый Codex должен явно различать:

- фактически реализованное поведение;
- желаемое поведение;
- BUG: сломанное поведение;
- TODO: запланированное, но не реализованное;
- FRAGILE: работающее, но хрупкое/эвристическое;
- UNKNOWN: данные, которых нет в репозитории.

Если старая реализация содержит баг, новый мод не должен автоматически портировать этот баг как нормальное поведение.

## File order for new Codex

Рекомендуемый порядок чтения:

1. `00_README.md`
2. `01_PROJECT_OVERVIEW.md`
3. `03_BUGS_AND_GAPS.md`
4. `02_FEATURE_INVENTORY.md`
5. `04_CONTENT_REGISTRY.md`
6. `05_BEHAVIOR_SPEC.md`
7. `06_NEOFORGE_ARCHITECTURE.md`
8. `07_MIGRATION_PHASES.md`
9. `08_DIAGNOSTICS_REQUIREMENTS.md`
10. `09_TESTING_PLAN.md`
11. `10_NEW_CODEX_MASTER_PROMPT.md`
12. `11_AGENTS_TEMPLATE.md`
13. `12_FIRST_TASK_PROMPT.md`

Новый Codex должен начинать только с Phase 0 из `07_MIGRATION_PHASES.md`.
