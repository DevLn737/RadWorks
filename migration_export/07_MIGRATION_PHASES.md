# Migration Phases

Каждая фаза должна быть маленькой, проверяемой и фиксироваться в `MIGRATION_STATUS.md`. Новый Codex должен начинать только с Phase 0.

## Phase 0 — Repository foundation

Цель: создать пустой NeoForge-мод, документацию и базовые команды.

### Source references from old project
- `README.md`
- `CHANGELOG.md`
- `VERSION`
- `config.js`
- `radiation_debug.js`

### Target files
- `settings.gradle`
- `build.gradle`
- `src/main/resources/META-INF/neoforge.mods.toml`
- `src/main/java/<package>/RadWorks.java`
- `src/main/java/<package>/command/*`
- `AGENTS.md`
- `README.md`
- `MIGRATION_STATUS.md`
- `TESTING.md`
- `DIAGNOSTICS.md`

### Implementation tasks
- [ ] создать NeoForge project;
- [ ] создать AGENTS.md;
- [ ] создать README.md;
- [ ] создать MIGRATION_STATUS.md;
- [ ] создать TESTING.md;
- [ ] создать DIAGNOSTICS.md;
- [ ] добавить `/radworks version`;
- [ ] добавить `/radworks dump`;
- [ ] убедиться, что проект собирается.

### Diagnostics
- `/radworks version` выводит mod version, Minecraft version, NeoForge version, loaded integrations.
- `/radworks dump` создаёт JSON с базовой информацией.

### Manual test steps
1. Запустить client или dedicated server.
2. Ввести `/radworks version`.
3. Ввести `/radworks dump`.
4. Проверить, что dump file создан и читается.

### Acceptance criteria
- Gradle build проходит.
- `runClient` доступен или явно описан как UNKNOWN.
- `/radworks version` работает.
- `/radworks dump` создаёт JSON с базовой информацией.
- Радиационная логика ещё не реализована.

### Risks
- UNKNOWN: точные Minecraft/NeoForge versions.

### Rollback notes
Удалить только каркас нового репозитория; старый KubeJS проект не трогать.

## Phase 1 — Data-driven radiation rules

Цель: описать radioactive sources через JSON/config.

### Source references from old project
- `config.js`
- `radiation_common.js`
- `radiation_math.js`
- `radiation_fluids.js`

### Target files
- `src/main/java/<package>/radiation/RadiationRules.java`
- `src/main/java/<package>/radiation/RadiationRulesLoader.java`
- `src/main/resources/data/radworks/radiation_rules/*.json`
- `src/main/java/<package>/command/ValidateCommand.java`

### Implementation tasks
- [ ] перенести default item/block/fluid rules из `config.js`;
- [ ] добавить schema/validation;
- [ ] добавить checksum активных правил;
- [ ] добавить `/radworks validate`;
- [ ] обновить `DIAGNOSTICS.md` и `TESTING.md`.

### Diagnostics
- `/radworks validate` показывает unknown IDs, invalid values, duplicates.
- `/radworks dump` включает rules checksum и количество загруженных правил.

### Manual test steps
1. Запустить мир.
2. Выполнить `/radworks validate`.
3. Проверить, что известные ID загружаются.

### Acceptance criteria
- Правила загружаются без gameplay effects.
- Ошибочные ID диагностируются.

### Risks
- В новом окружении может не быть модов `createnuclear`, `tfmg`, `create`.

### Rollback notes
Отключить rules loader и оставить только команды Phase 0.

## Phase 2 — Player inventory radiation

Цель: радиация от предметов в инвентаре игрока.

### Source references from old project
- `events.js`
- `radiation_world_scan.js`
- `radiation_math.js`
- `radiation_items.js`
- `config.js`

### Target files
- `radiation/source/PlayerInventorySourceProvider.java`
- `radiation/ExposureEngine.java`
- `command/ExposureCommand.java`

### Implementation tasks
- [ ] добавить provider для inventory;
- [ ] реализовать расчёт item count -> radius/strength;
- [ ] добавить cooldown;
- [ ] добавить diagnostics для каждого stack;
- [ ] пока не добавлять damage/effect.

### Diagnostics
- `/radworks exposure <player>` показывает inventory contributions.

### Manual test steps
1. Положить radioactive item в inventory.
2. Выполнить `/radworks exposure <player>`.
3. Убрать предмет.
4. Повторить команду.

### Acceptance criteria
- Radioactive item обнаруживается.
- Non-radioactive item не даёт вклад.
- Изменение количества меняет расчёт.

### Risks
- Nested container behavior из `radiation_items.js` хрупкое; не переносить blindly.

### Rollback notes
Отключить provider через config.

## Phase 3 — Exposure diagnostics

Цель: полная диагностика расчёта радиации.

### Source references from old project
- `radiation_debug.js`
- `radiation_shielding.js`
- `radiation_world_scan.js`

### Target files
- `diagnostics/DiagnosticsService.java`
- `diagnostics/DiagnosticsDump.java`
- `command/SourcesCommand.java`
- `command/ExposureCommand.java`

### Implementation tasks
- [ ] добавить structured exposure breakdown;
- [ ] добавить recent warnings;
- [ ] добавить performance counters;
- [ ] добавить `/radworks sources`;
- [ ] добавить `/radworks debug on/off`.

### Diagnostics
- JSON dump должен содержать последние warnings и snapshot расчёта.

### Manual test steps
1. Включить debug.
2. Выполнить `/radworks sources`.
3. Создать dump.
4. Проверить, что output можно отправить Codex.

### Acceptance criteria
- Диагностика объясняет, почему игрок получает или не получает exposure.

### Risks
- Слишком большой dump; нужен bounded output.

### Rollback notes
Отключить debug verbosity, оставить базовый dump.

## Phase 4 — Blocks and block entities

Цель: источники в блоках и контейнерах.

### Source references from old project
- `radiation_world_scan.js`
- `radiation_items.js`
- `radiation_fluids.js`
- `config.js`

### Target files
- `radiation/source/BlockSourceProvider.java`
- `radiation/source/BlockEntityInventorySourceProvider.java`
- `radiation/source/BlockEntityFluidSourceProvider.java`
- `radiation/cache/ChunkSourceIndex.java`

### Implementation tasks
- [ ] реализовать static block sources;
- [ ] реализовать block entity item handler sources;
- [ ] реализовать fluid handler sources;
- [ ] добавить container exclusions;
- [ ] добавить cache/invalidation.

### Diagnostics
- `/radworks sources` показывает block/block entity path.

### Manual test steps
1. Поставить radioactive block.
2. Проверить sources.
3. Положить radioactive item в chest.
4. Проверить sources.

### Acceptance criteria
- Static block работает.
- Chest/container source работает.
- Исключения контейнеров применяются.

### Risks
- Старый NBT scan из `radiation_fluids.js` может давать false positives.

### Rollback notes
Отключить block entity providers, оставить static blocks.

## Phase 5 — Shielding

Цель: экранирование между источником и игроком.

### Source references from old project
- `radiation_shielding.js`
- `config.js`

### Target files
- `radiation/shielding/ShieldingEngine.java`
- `radiation/shielding/ShieldingResult.java`
- `data/radworks/tags/blocks/radiation_shielding.json`

### Implementation tasks
- [ ] перенести shielding block list в tag/default config;
- [ ] реализовать line-of-sight model;
- [ ] добавить debug explanation;
- [ ] решить, сохранять ли old 3-ray binary model.

### Diagnostics
- `/radworks sources` показывает blocked rays/final shielding.

### Manual test steps
1. Поставить источник.
2. Встать рядом без защиты.
3. Поставить lead/reinforced glass block между игроком и источником.
4. Проверить exposure.

### Acceptance criteria
- Shielding уменьшает или блокирует вклад согласно выбранной модели.

### Risks
- Старое поведение бинарное и может быть неинтуитивным.

### Rollback notes
Feature flag выключает shielding.

## Phase 6 — Effects and gameplay consequences

Цель: эффекты радиации, урон, статусы, прогрессия.

### Source references from old project
- `radiation_modifiers.js`
- `events.js`
- `config.js`

### Target files
- `registry/RadEffects.java`
- `radiation/RadiationEffectApplier.java`
- `config/RadWorksConfig.java`

### Implementation tasks
- [ ] создать или выбрать radiation effect strategy;
- [ ] реализовать application from exposure;
- [ ] добавить damage/exhaustion options;
- [ ] добавить armor protection;
- [ ] добавить manual tests.

### Diagnostics
- `/radworks exposure` показывает final effect duration/amplifier.

### Manual test steps
1. Получить exposure.
2. Проверить effect.
3. Надеть full protection armor.
4. Проверить отсутствие effect.

### Acceptance criteria
- Gameplay consequences соответствуют documented rules.
- Баг с чужим effect ID не переносится без явного решения.

### Risks
- Совместимость с `createnuclear:radiation`.

### Rollback notes
Отключить effect applier, оставить exposure diagnostics.

## Phase 7 — Performance and caching

Цель: серверная производительность, 30+ игроков.

### Source references from old project
- `radiation_world_scan.js`
- `events.js`
- `config.js`

### Target files
- `radiation/cache/SourceCache.java`
- `radiation/cache/ChunkSourceIndex.java`
- `diagnostics/PerformanceStats.java`

### Implementation tasks
- [ ] добавить metrics scan time/source count/cache hit rate;
- [ ] добавить chunk/source cache;
- [ ] добавить configurable intervals;
- [ ] протестировать много источников.

### Diagnostics
- `/radworks dump` содержит performance stats.

### Manual test steps
1. Разместить много источников.
2. Подключить несколько игроков или имитировать нагрузку.
3. Снять dump.

### Acceptance criteria
- Нет заметного server tick lag в smoke test.
- Diagnostics показывают scan cost.

### Risks
- Без реального модпака нагрузка UNKNOWN.

### Rollback notes
Вернуться к более простой provider model, но оставить metrics.

## Phase 8 — Create integration

Цель: Create contraptions/trains, только после стабильного ядра.

### Source references from old project
- `contraption.js`
- `radiation_world_scan.js`
- `radiation_items.js`
- `radiation_fluids.js`
- `config.js`

### Target files
- `integration/create/CreateIntegration.java`
- `integration/create/CreateContraptionSourceProvider.java`
- `integration/create/CreateTrainSourceProvider.java`

### Implementation tasks
- [ ] research Create API for target version;
- [ ] implement minecart contraption provider;
- [ ] add NBT fallback only as diagnostic fallback;
- [ ] document unsupported cases;
- [ ] postpone trains if API remains UNKNOWN.

### Diagnostics
- Sources output includes Create contraption snapshot.

### Manual test steps
1. Создать Create minecart contraption with radioactive block/item.
2. Проверить sources.
3. Переместить contraption.
4. Проверить updated position.

### Acceptance criteria
- Minecart contraption source работает.
- Create отсутствует => core mod still loads.

### Risks
- API mismatch.
- Old NBT position mapping is FRAGILE.
- Train support PLANNED, not implemented in old project.

### Rollback notes
Disable Create integration module without affecting core.

## Phase 9 — Aeronautics/Simulated integration

Цель: Aeronautics/physics objects, только после Create или отдельного research spike.

### Source references from old project
- `config.js`
- `TODO.md`

### Target files
- `integration/aeronautics/AeronauticsIntegration.java`
- `integration/aeronautics/AeronauticsSourceProvider.java`

### Implementation tasks
- [ ] identify actual mod/API;
- [ ] determine sublevel/world mapping;
- [ ] add diagnostics only first;
- [ ] implement provider after research.

### Diagnostics
- Dump lists integration status and discovered objects.

### Manual test steps
1. Запустить с Aeronautics/Simulated.
2. Создать объект с radioactive source.
3. Выполнить dump/sources.

### Acceptance criteria
- UNKNOWNs resolved before gameplay implementation.

### Risks
- Real data/API needed; cannot be solved from current files.

### Rollback notes
Keep integration absent; core remains stable.

## Phase 10 — Compatibility and packaging

Цель: публичная сборка, changelog, версии, config migration.

### Source references from old project
- `CHANGELOG.md`
- `VERSION`
- `versions/`
- `.gitignore`

### Target files
- `CHANGELOG.md`
- `VERSION`
- `README.md`
- `gradle.properties`
- release scripts, if needed

### Implementation tasks
- [ ] define versioning scheme;
- [ ] document compatibility;
- [ ] package release artifacts;
- [ ] update changelog in Russian or bilingual if user prefers;
- [ ] document config migration.

### Diagnostics
- `/radworks version` matches packaged version.

### Manual test steps
1. Build jar.
2. Install in clean modpack.
3. Run client/server.

### Acceptance criteria
- Release jar builds.
- Docs explain install and known limitations.

### Risks
- Unknown final dependency set.

### Rollback notes
Do not publish; keep snapshot build internal.
