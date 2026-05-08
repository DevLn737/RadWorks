# Behavior Specification

Этот файл описывает целевое поведение независимо от KubeJS. Старые скрипты являются источником фактов, но не архитектурой для нового мода.

## System: Radiation Rules

### Goal
Единообразно описывать, какие предметы, блоки и жидкости являются радиоактивными, с какими радиусами и силой воздействия.

### Current implemented behavior
Правила заданы в JavaScript-конфиге:
- блоки: `config.js`, `BLOCK_CONFIGS`;
- предметы: `config.js`, `radioactiveItems`;
- жидкости: `config.js`, `radioactiveFluidBlocks`;
- уровни воздействия: `config.js`, `RADIATION_LEVELS`.

### Intended final behavior
В новом моде правила должны быть data/config-driven: JSON/data pack/config, валидируемые командой `/radworks validate`.

### Inputs
- ID предметов.
- ID блоков.
- ID жидкостей.
- Количество предметов.
- Количество жидкости в mB.
- Таблица уровней радиации.

### Outputs
- Нормализованные правила источников.
- Ошибки валидации для неизвестных ID, дубликатов и некорректных значений.

### Rules
- Для статических блоков используется radius из `BLOCK_CONFIGS`.
- Для предметов радиус зависит от количества через `calculateDynamicRadius` в `radiation_math.js`.
- Для жидкости `createnuclear:uranium` используется `start`, `per1000`, `max` из `config.js`.
- Если правило не найдено, источник не должен давать радиацию.

### Balance values
- `CHECK_COOLDOWN = 20` ticks.
- `RADIATION_DURATION = 20` ticks.
- `PLAYER_INVENTORY_RADIATION.cooldown = 100` ticks.
- `RADIATION_LEVELS`: `1..4 => start 1, k 0.1`; `5..6 => start 2, k 0.2`; `7..99 => start 4, k 0.3`; floor `1`, ceil `10`.
- Radioactive fluid default: `start 2`, `per1000 1`, `max 10`.

### Edge cases
- UNKNOWN: точная версия Minecraft и loader старого проекта.
- FRAGILE: часть источников извлекается из NBT эвристиками, а не из стабильных API.

### Multiplayer behavior
Правила должны загружаться на сервере и одинаково применяться ко всем игрокам. Клиент может получать только диагностическую/визуальную информацию.

### Performance expectations
Правила должны быть закэшированы в registry-friendly структурах после загрузки. Запрещено парсить JSON/NBT-правила на каждый tick.

### Diagnostics
- `/radworks validate` показывает ошибки правил.
- `/radworks dump` включает checksum активных правил.

## System: Player Inventory Radiation

### Goal
Игрок получает радиационное воздействие от радиоактивных предметов в собственном инвентаре, если не защищён полной бронёй.

### Current implemented behavior
Реализовано в `events.js` через `PlayerEvents.tick` и `getPlayerRadioactivity` из `radiation_world_scan.js`. Предметы обрабатываются через `processRadioactiveItems` в `radiation_math.js`. Включается `PLAYER_INVENTORY_RADIATION.enabled` из `config.js`.

### Intended final behavior
Отдельный server-side provider `PlayerInventoryRadiationSourceProvider` должен выдавать источники из инвентаря игрока. Источник должен быть диагностируемым.

### Inputs
- Игрок.
- Inventory slots.
- Stack count.
- Nested items, если контейнерный предмет поддерживается модом.
- Armor state.

### Outputs
- Radiation exposure для игрока.
- Mob effect/урон/усталость через отдельную систему последствий.
- Diagnostic entries в `/radworks exposure <player>`.

### Rules
- Проверка инвентаря игрока происходит реже основного world scan: старое значение cooldown `100` ticks.
- Полный комплект брони из `RADIATION_ARMOR_ITEMS` полностью блокирует player inventory radiation в старом поведении.
- Вложенные предметы считаются через `extractItemsFromStack`.

### Balance values
- Cooldown: `100` ticks.
- Armor set currently: full vanilla diamond armor.

### Edge cases
- FRAGILE: `extractItemsFromStack` зависит от NBT/component key guesses.
- UNKNOWN: должны ли nested items в любых контейнерах игрока учитываться в финальном дизайне один-к-одному.

### Multiplayer behavior
Расчёт должен быть server-authoritative. Игроки не должны влиять на exposure друг друга через inventory radiation, кроме случаев dropped items/entity sources.

### Performance expectations
Сканировать только inventory конкретного игрока по cooldown; не пересчитывать nested stacks без cache/version checks при больших контейнерах.

### Diagnostics
- `/radworks exposure <player>` показывает вклад каждого stack.
- `/radworks sources` рядом с игроком отдельно помечает inventory source.

## System: Static Block Sources

### Goal
Радиоактивные блоки в мире создают область воздействия.

### Current implemented behavior
`findAllRadiationSources` в `radiation_world_scan.js` сканирует куб/сферу вокруг игрока до `MAX_RADIATION_RADIUS` и проверяет ID блока по `staticBlockConfigs`.

### Intended final behavior
`BlockRadiationSourceProvider` должен находить nearby block sources через оптимизированный поиск/кэш чанков, а не наивный полный перебор для каждого игрока.

### Inputs
- ServerLevel.
- Player position.
- BlockPos.
- BlockState.
- Radiation block rules.

### Outputs
- RadiationSource с type `block`, position, id, radius, strength.

### Rules
- Блоки из `BLOCK_CONFIGS` являются статическими источниками.
- Если игрок вне радиуса, источник не даёт вклад.
- Shielding может блокировать line-of-sight.

### Balance values
- Радиусы блоков заданы в `config.js`, `BLOCK_CONFIGS`.
- Максимальный скан радиуса рассчитывается из максимального радиуса известных источников.

### Edge cases
- `minecraft:air` встречается как техническая проверка/исключение в логике, но не как контент.

### Multiplayer behavior
Источник должен быть общим для всех игроков, расчёт exposure per-player.

### Performance expectations
Нужен cache по чанкам/секциям, invalidation на block update, ограничение scan cost для 30+ игроков.

### Diagnostics
- `/radworks sources` показывает block source ID, position, distance, radius, shielding.

## System: Block Entity and Container Sources

### Goal
Контейнеры и block entities должны излучать радиацию, если внутри лежат радиоактивные предметы или жидкости.

### Current implemented behavior
В `radiation_world_scan.js` block entity обрабатывается через `getItemsFromContainer` из `radiation_items.js` и `getFluidFromContainer` из `radiation_fluids.js`. Исключения контейнеров заданы в `config.js`, `containerExceptions`.

### Intended final behavior
Нужна provider-модель для block entity inventories/fluid handlers с нормальными NeoForge capabilities. NBT fallback использовать только как диагностический fallback, если стабильного API нет.

### Inputs
- BlockEntity.
- Item handlers.
- Fluid handlers.
- Radiation rules.
- Container exception list.

### Outputs
- RadiationSource с type `block_entity_inventory` или `block_entity_fluid`.

### Rules
- Исключённые контейнеры из `containerExceptions` не сканируются как обычные контейнеры.
- Предметы внутри контейнера суммируются в itemMap.
- Жидкости суммируются по ID жидкости.

### Balance values
- Dynamic radius для предметов.
- Fluid radius для `createnuclear:uranium`.

### Edge cases
- FRAGILE: Sophisticated Storage UUID/packed item logic.
- FRAGILE: Create block entity NBT keys `Item`, `HeldItem.Item`, `Inventory`, `InputItems`, `OutputItems`.
- BUG: broad recursive NBT scan может спутать вложенную жидкость с жидкостью самого контейнера.

### Multiplayer behavior
Сервер должен считать источники один раз на область/чанк, а не отдельно полностью для каждого игрока.

### Performance expectations
Capability scan + cache. Invalidate on inventory/fluid changes when possible.

### Diagnostics
- `/radworks sources` показывает container path, item/fluid contents, source contribution.
- `/radworks dump` включает warnings по unsupported container APIs.

## System: Entity Sources

### Goal
Dropped items, item frames, chest minecarts, armor stands и похожие сущности должны излучать радиацию при наличии радиоактивного содержимого.

### Current implemented behavior
`getItemsFromEntity` в `radiation_items.js` поддерживает ItemFrame, ArmorStand, ChestMinecart, Llama/Donkey/Mule/TraderLlama/AbstractChestedHorse/chested_horse, generic inventory/allSlots/armor/handSlots, NBT Items. `findAllRadiationSources` в `radiation_world_scan.js` сканирует entity list рядом с игроком.

### Intended final behavior
Отдельные `EntityRadiationSourceProvider` реализации по типам entity, плюс generic fallback.

### Inputs
- Entity.
- ItemStack contents.
- Equipment.
- Passenger/vehicle relation, если применимо.

### Outputs
- RadiationSource с entity UUID, entity type, stack contributions.

### Rules
- Dropped item source считается по stack entity.
- Inventory-like entities считаются по содержимому.
- Источник должен исчезать при удалении entity.

### Balance values
- Те же item radiation rules.

### Edge cases
- FRAGILE: generic access к `inventory`, `allSlots`, `armorSlots`, `handSlots` зависит от KubeJS/Java wrapper.

### Multiplayer behavior
Entity source может влиять на нескольких игроков рядом.

### Performance expectations
Использовать spatial query around player или server-managed source cache, а не полный scan всех entities мира на каждого игрока.

### Diagnostics
- `/radworks sources` показывает entity type, UUID, item summary.

## System: Shielding

### Goal
Блоки защиты должны блокировать радиацию между источником и целью.

### Current implemented behavior
`isShielded` в `radiation_shielding.js` строит 3 луча между source и target, шагает с шагом `0.3`, и считает защиту полной, если все лучи заблокированы блоками из `shieldingBlocks`.

### Intended final behavior
`ShieldingEngine` должен иметь формальную модель ray sampling и диагностируемый результат.

### Inputs
- Source position.
- Target position.
- Shielding block tags/rules.
- World block states.

### Outputs
- `shielded: true/false`.
- Optional attenuation, если будет принято в будущем.

### Rules
- В старом поведении защита бинарная.
- Все 3 sampled rays должны быть blocked.
- Список shielding blocks задан в `config.js`.

### Balance values
- Ray step: `0.3`.
- Ray offsets: из `radiation_shielding.js`.

### Edge cases
- FRAGILE: бинарная модель не учитывает толщину, материал, partial blocks, fluids.
- UNKNOWN: нужно ли переносить ровно 3-ray модель или заменить на более физически понятную после решения пользователя.

### Multiplayer behavior
Расчёт per-target, server-side.

### Performance expectations
Кэшировать ray results краткоживущим cache по source/target/block update, если источников много.

### Diagnostics
- `/radworks sources` показывает shielding result.
- Debug mode может рисовать/логировать blocked ray count.

## System: Armor Protection

### Goal
Полный комплект защитной брони предотвращает радиационное воздействие.

### Current implemented behavior
`isWearingFullSet` в `radiation_common.js` сравнивает armor slots с `RADIATION_ARMOR_ITEMS` из `config.js`. Сейчас это полный комплект vanilla diamond armor.

### Intended final behavior
В новом моде нужна отдельная `ArmorProtectionService`, желательно data-driven/tag-driven. Пользователь ранее указывал на идею кастомной брони, но она не реализована.

### Inputs
- Player equipment.
- Protection armor rules/tags.

### Outputs
- Protection state.
- Diagnostic explanation.

### Rules
- Старое поведение: только полный комплект; partial set не защищает.

### Balance values
- Armor item IDs из `config.js`, `RADIATION_ARMOR_ITEMS`.

### Edge cases
- TODO: custom radiation armor не реализована.
- UNKNOWN: должна ли diamond armor оставаться финальной защитой или это временный placeholder.

### Multiplayer behavior
Только server-authoritative armor state.

### Performance expectations
Проверять armor slots при exposure tick, можно кэшировать по equipment change.

### Diagnostics
- `/radworks exposure <player>` показывает armor protection reason.

## System: Radiation Effects and Consequences

### Goal
Радиация должна иметь игровые последствия: effect, damage, exhaustion, debuffs.

### Current implemented behavior
`radiation_modifiers.js` регистрирует/модифицирует `createnuclear:radiation`. `events.js` применяет эффект и при наличии эффекта добавляет damage/exhaustion.

### Intended final behavior
В новом моде нужно решить: использовать собственный `radworks:radiation` effect или совместимость с `createnuclear:radiation`. Баг старой реализации с возможным конфликтом registry переносить нельзя без решения.

### Inputs
- Exposure level.
- Player/mob entity.
- Armor/shielding result.

### Outputs
- Mob effect.
- Attribute modifiers.
- Damage.
- Exhaustion.

### Rules
- Старый effect harmful.
- Attribute modifiers: speed, attack damage, attack speed, block break speed.
- Damage/exhaustion apply in `events.js` while effect is active.

### Balance values
- Effect duration: `RADIATION_DURATION = 20`.
- Attribute multipliers from `radiation_modifiers.js`: `-0.5`, `-0.8`, `-0.8`, `-0.8`.

### Edge cases
- FRAGILE: регистрация чужого ID `createnuclear:radiation`.
- UNKNOWN: финальный mod ID для effect.

### Multiplayer behavior
Все gameplay consequences server-side.

### Performance expectations
Не пересоздавать modifiers; registry at startup only.

### Diagnostics
- `/radworks exposure` показывает final effect amplifier/duration.

## System: Create Minecart Contraptions

### Goal
Create minecart contraptions должны излучать радиацию от блоков, предметов и жидкостей внутри contraption.

### Current implemented behavior
`contraption.js` читает `create:minecart_contraption_data`, `Blocks.Palette`, `BlockList`, `Actors/actors`, `Data/UpdateTag`, nested items and fluids. Интеграция основана на NBT.

### Intended final behavior
Create integration должна быть изолирована в `integration/create` и использовать стабильные Create API, если доступны. NBT fallback разрешён только как диагностический fallback.

### Inputs
- Create contraption entity/block data.
- Contraption block list.
- Block entity data.
- Item/fluid contents.

### Outputs
- RadiationSource entries for contraption contents.

### Rules
- Радиус contraption сейчас выбирается как максимум из static block, item и fluid contribution.
- Точные positions внутри contraption сейчас не гарантированы.

### Balance values
- Используются существующие item/block/fluid rules.

### Edge cases
- FRAGILE: exact position inside moving contraption.
- BUG/TODO: processing block contents могут извлекаться неполно.
- UNKNOWN: актуальный API Create для версии модпака.

### Multiplayer behavior
Contraption source должен быть server-side и корректно двигаться.

### Performance expectations
Cache snapshot на contraption, invalidate при изменениях contraption/inventory.

### Diagnostics
- `/radworks sources` показывает contraption id/type, source count, source snapshot checksum.

## System: Create Trains

### Goal
Create trains должны поддерживаться как moving contraption-like radiation sources.

### Current implemented behavior
PLANNED/TODO. В `TODO.md` и обсуждениях проекта отмечено как будущая задача; полной реализации в текущих скриптах нет.

### Intended final behavior
Реализовывать только после стабильного core engine и minecart contraption integration.

### Inputs
- UNKNOWN: Create train API для целевой версии.

### Outputs
- Train-carriage radiation sources.

### Rules
- UNKNOWN.

### Balance values
- Те же radiation rules.

### Edge cases
- UNKNOWN: coordinate mapping, chunk loading, carriage inventories.

### Multiplayer behavior
Server-authoritative.

### Performance expectations
Train snapshots must be cached.

### Diagnostics
- Research spike должен добавить `/radworks sources` для train source.

## System: Aeronautics / Simulated Sublevels

### Goal
Поддержать radiation sources внутри физических объектов/субуровней Create Aeronautics/Simulated, если они используются в модпаке.

### Current implemented behavior
PLANNED/TODO. `config.js` содержит исключения для `simulated:*`, но полноценного сканирования sublevels нет.

### Intended final behavior
Отдельная optional integration после core engine. Не блокировать базовый мод.

### Inputs
- UNKNOWN: API Aeronautics/Simulated.
- Physical object/sublevel world mapping.

### Outputs
- RadiationSource entries inside moving/virtual worlds.

### Rules
- UNKNOWN.

### Balance values
- Те же source rules.

### Edge cases
- UNKNOWN: dimensions/sublevels, sync, ownership, chunk loading.

### Multiplayer behavior
Server-authoritative, integration optional.

### Performance expectations
Research first; no blind NBT scan of all sublevels every tick.

### Diagnostics
- Dump must list whether integration loaded and whether objects were found.

## System: Debug and Diagnostics

### Goal
Пользователь, который не является Java-программистом, должен уметь принести Codex данные из Minecraft.

### Current implemented behavior
`radiation_debug.js` содержит debug helpers; `config.js` имеет `DEBUG_CONTAINERS`, `DEBUG_NESTED_ITEMS`, `SHOW_PARTICLES`, `PARTICLE_TYPE`, `ANGLE_STEP`. Нет пользовательских команд.

### Intended final behavior
Диагностика должна быть первой частью нового мода: `/radworks version`, `/radworks dump`, `/radworks sources`, `/radworks exposure`, `/radworks debug on/off`, `/radworks validate`.

### Inputs
- Player.
- World.
- Active rules.
- Loaded mods.
- Recent warnings.
- Performance counters.

### Outputs
- Chat summaries.
- JSON dump files.
- Debug logs.

### Rules
- Диагностика не должна менять gameplay state.
- Debug on/off должен быть безопасным и обратимым.

### Balance values
- UNKNOWN: размер rolling warning buffer.

### Edge cases
- Dedicated server without client.
- Player lacks permissions.

### Multiplayer behavior
Commands should require permission level, especially dump/debug.

### Performance expectations
Diagnostics should be bounded and avoid huge dumps by default.

### Diagnostics
Этот раздел сам является требованием к диагностике; подробности в `08_DIAGNOSTICS_REQUIREMENTS.md`.
