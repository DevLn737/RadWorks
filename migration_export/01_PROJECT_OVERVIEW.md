# Project Overview

## One-sentence summary

RadWorks KubeJS is a radiation-safety gameplay prototype for a Create / Create Nuclear Minecraft modpack, currently implemented as KubeJS scripts and intended to be rebuilt as a clean NeoForge mod.

## Gameplay fantasy

Игрок строит индустриальные системы с радиоактивными материалами и должен думать как инженер по радиационной безопасности: изолировать уран, экранировать опасные зоны, не носить радиоактивные предметы без защиты, осторожно использовать Create contraptions, контейнеры, баки и автоматизацию.

## Current state

### Что уже работает

- IMPLEMENTED: конфиг `RADIATION_CONFIG` в `config.js`.
- IMPLEMENTED: статические радиоактивные блоки через `BLOCK_CONFIGS`.
- IMPLEMENTED: динамическая радиация от предметов из `RADIOACTIVE_ITEM_CONFIGS`.
- IMPLEMENTED: динамический радиус по формуле `RADIATION_LEVELS`.
- IMPLEMENTED: радиация от жидкости `createnuclear:uranium` по количеству mb.
- IMPLEMENTED: экранирование блоками из `RADIATION_SHIELDING_BLOCKS`.
- IMPLEMENTED: защита от нового наложения радиации полным сетом `ARMOR_SET`.
- IMPLEMENTED: обработка `PlayerEvents.loggedIn`, `PlayerEvents.tick`.
- IMPLEMENTED: урон и истощение голода игроку при активном `createnuclear:radiation`.
- IMPLEMENTED: optional debug Shift+RightClick по контейнеру.
- IMPLEMENTED/PARTIAL: чтение инвентарей и сущностей через KubeJS wrappers, Forge capabilities и NBT.
- PARTIAL: чтение `create:minecart_contraption`.
- PARTIAL: Sophisticated Storage / Sophisticated Backpacks.
- PARTIAL: placed contraption через NBT-узел `Contraption`.
- PARTIAL/FRAGILE: `create:cardboard_package`.

### Что частично работает

- PARTIAL: Create contraptions работают по NBT/компонентам, но точные позиции внутренних блоков не рассчитываются.
- PARTIAL: размещённые contraption детектятся от позиции entity, а не от отдельных блоков внутри.
- PARTIAL: поддержка Sophisticated зависит от `storageWrapper`, UUID и optional class names.
- PARTIAL: entity scanning покрывает часть vanilla случаев и некоторые обобщённые inventory API.

### Что сломано или подозрительно

- BUG/FRAGILE: старый промт пользователя указывал, что после рефакторинга запакованные Sophisticated Storage items переставали излучать в инвентаре. В коде есть попытка исправления, но без игровых тестов статус остаётся FRAGILE.
- BUG/FRAGILE: Create item drain / belt / basin / depot с contraption и NBT-компонентами требуют реальных тестов.
- FRAGILE: широкое NBT-сканирование может ловить не только реальные предметы/жидкости, но и фильтры/ghost items.
- FRAGILE: `radiation_modifiers.js` регистрирует `createnuclear:radiation`, что может конфликтовать с Create Nuclear, если эффект уже существует.
- PERFORMANCE: скан блоков вокруг каждого игрока и `world.getEntities()` может быть дорогим на 30+ игроках.

### Что ещё не сделано

- PLANNED: чистый NeoForge-мод.
- PLANNED: диагностические команды `/radworks`.
- PLANNED: Create trains.
- PLANNED: Create Aeronautics / Simulated sublevels.
- PLANNED: точные позиции источников внутри contraptions/trains.
- PLANNED: кастомная броня от радиации.
- PLANNED: нормальная performance/cache архитектура.

## Why KubeJS is no longer enough

Факты из проекта:

- `radiation_items.js` и `contraption.js` используют множество `try/catch`, прямой доступ к NBT, guessed keys и optional `Java.loadClass`.
- `contraption.js` пытается разбирать Create contraption через NBT (`Blocks`, `Palette`, `BlockList`, `Actors`, `Contraption`), но не имеет типизированного доступа к Create internals.
- `TODO.md` фиксирует, что поезда Create и Aeronautics sublevels не реализованы из-за отсутствия надёжного доступа к данным.
- Текущий scanner в `radiation_world_scan.js` делает пер-игроковый перебор блока в радиусе и перебор всех сущностей мира.
- Поддержка Sophisticated Storage/Backpacks зависит от классов `net.p3pp3rf1y...`, UUID и NBT-структур, что хрупко между версиями.
- Урон от эффекта находится в `events.js`, а не внутри полноценной реализации эффекта.

Итог: KubeJS хорош как прототип правил, но для стабильного, производительного и расширяемого проекта нужен Java-мод с типизированными интеграциями, кэшированием, диагностикой и контролируемой архитектурой.

## Target architecture

Цель — чистый NeoForge-мод:

- без обязательной зависимости от KubeJS;
- с data/config driven radiation rules;
- с диагностическими командами;
- с изолированными optional integrations;
- с core engine, который не зависит напрямую от Create/Aeronautics;
- с source provider model;
- с кэшем/профилированием;
- с ручными тестами и validation checklist.

## Non-goals

Новый Codex НЕ должен:

- сохранять KubeJS как обязательную зависимость;
- копировать старую KubeJS-архитектуру 1:1;
- реализовывать Create/Aeronautics до стабильного ядра радиации;
- начинать с огромного переписывания всех интеграций;
- портировать известные баги как нормальное поведение;
- скрыто менять баланс;
- выдумывать UNKNOWN-детали;
- делать крупные невидимые изменения без записи в `MIGRATION_STATUS.md`.
