---
title: "RadWorks — руководство администратора и тестировщика"
lang: ru-RU
mainfont: "DejaVu Serif"
monofont: "DejaVu Sans Mono"
geometry: margin=2cm
---

# RadWorks: единый справочник (Beta 0.6)

## 1. Введение: что такое RadWorks

**RadWorks** — это мод с системой радиации для NeoForge, в котором поведение задаётся данными (правилами), а расчёты выполняются на стороне сервера.  
Документ покрывает фактическое состояние мода до **Beta 0.6**.

Что мод делает сейчас:
- ищет источники радиации в поддерживаемых местах (инвентарь, блоки, жидкости, сущности, вложенные контейнеры);
- считает вклад источников в итоговое воздействие;
- учитывает экранирование;
- применяет правила исключения/подавления/принудительных источников (`exclude`/`contain`/`force`);
- показывает состояние через команды и `dump`.

Что мод пока **не** делает:
- не накапливает долгосрочную дозу;
- не имеет отдельной системы урона, голода или истощения от радиации;
- не имеет GUI/HUD интерфейса.

---

## 2. Как пользоваться этим руководством

- **Игроку**: разделы 3, 4, 8, 12, 13, 16.
- **Администратору сервера**: разделы 3, 10, 11, 12, 14, 15, 16.
- **Тестировщику**: разделы 12, 14, 15, 16.
- **Модпак-мейкеру**: разделы 5, 6, 9, 10, 11, 17.
- **Если “не работает”**: сразу к разделу 16.

---

## 3. Быстрый старт

### Установка
1. Соберите jar:
```bash
./gradlew build
```
2. Возьмите файл:
`build/libs/radworks-0.1.0.jar`
3. Поместите jar в папку `mods/` сервера.
4. Для beta-проверок рекомендуется тот же jar и на клиентах.

### Первые команды после запуска
```text
/radworks validate
/radworks exposure
/radworks dump
```

Нормальный стартовый результат:
- `validate` показывает, что правила загружены;
- `exposure` выполняется без ошибки;
- `dump` создаёт JSON-файл в `radworks_dumps/`.

---

## 4. Краткая схема работы радиации

Простая цепочка:

**источники** -> **правила** -> **вклад (contribution)** -> **радиус** -> **исключения/подавление/принуждение** -> **экранирование** -> **итоговое воздействие (totalExposure)** -> **решение по эффекту**

Ключевые понятия:
- **Источник**: объект, который может излучать радиацию.
- **Сила (strength)**: базовая “мощность” источника по правилу.
- **Радиус (radius/effectiveRadius)**: расстояние, на котором источник учитывается.
- **Вклад (contribution)**: сколько источник добавляет в итоговое значение.
- **Итоговое воздействие (totalExposure)**: сумма вкладов всех источников.
- **Порог (threshold)**: уровень, при котором разрешено применение эффекта.

---

## 5. Источники радиации

| Источник | Поддержка | Пример | Где смотреть | Частые проблемы |
|---|---|---|---|---|
| Инвентарь игрока (`player_inventory`) | Да | `uranium_bucket` в инвентаре | `/radworks sources`, `lastExposureSnapshot` | Нет правила на item |
| Статические блоки (`block`) | Да | радиоактивный блок рядом | `/radworks sources`, `sourceScanSummary.block*` | Блок вне радиуса |
| Жидкости в мире (`world_fluid`) | Да | лужа/поток урана | `/radworks sources`, `worldFluidDiagnostics` | Обрезанный кластер, нет fluid rule |
| Ванильные контейнеры (`block_entity_inventory`) | Да | сундук с радиоактивными предметами | `/radworks sources`, `sourceScanSummary.container*` | Нет item rule |
| Item handler блоков (`block_item_handler`) | Да | модовый блок-хранилище | `/radworks sources`, `handlerDiagnostics` | Capability есть, но нет rule |
| Fluid handler блоков (`block_fluid_handler`) | Да | модовый бак | `/radworks sources`, `handlerDiagnostics` | Нет fluid rule |
| Create transient carriers | Да (optional-safe) | данные Create через known paths | `/radworks sources`, `createCarrierDiagnostics` | `path_missing`, неподдержанная структура |
| Выброшенные предметы (`entity_dropped_item`) | Да | `ItemEntity` с радиоактивным item | `/radworks sources`, `entityCarrierDiagnostics` | Источник вне scan radius |
| Рамки (`entity_item_frame`) | Да | item frame с радиоактивным item | `/radworks sources`, `entityCarrierDiagnostics` | Нет item rule |
| Аура другого игрока (`entity_player_inventory_aura`) | Да | рядом игрок с радиоактивным item | `/radworks sources`, `entityCarrierDiagnostics` | Сам игрок не считается в своей ауре |
| Инвентари сущностей (`entity_inventory`) | Да | chest boat, donkey/mule/llama | `/radworks sources`, `entityCarrierDiagnostics` | Путь доступа отключён флагом |
| Вложенные ванильные контейнеры | Да | shulker/bundle внутри инвентаря | `/radworks sources`, `nestedContainerDiagnostics` | Unsupported формат, depth/item cap |
| Принудительные источники (`force`) | Да (Beta 0.6.4) | forced source из observed candidate | `/radworks sources`, `sourceOverrideDiagnostics` | Кандидат не наблюдался, rule invalid |

---

## 6. Радиус и сила источника

### Как считается
- Базовый радиус берётся из правила.
- Для агрегированных источников используется динамический радиус (dynamic radius): чем больше суммарное количество, тем дальше “достаёт” источник.

### Почему “большой сундук/бак фонит дальше”
- Несколько одинаковых единиц объединяются в агрегат.
- Для агрегата растёт `effectiveRadius`.

### Почему “1 предмет/1 mB может быть слабым”
- Малое количество даёт маленький вклад.
- Даже при попадании в радиус итоговое значение может быть ниже `threshold`.

### Ключевые настройки
- `rules.dynamicRadiusEnabled`
- `rules.dynamicRadiusScale`
- `rules.dynamicRadiusMaxCap`
- `integrations.worldFluidClusterDiscoveryRadius`

---

## 7. Экранирование

### Что это
Экранирование (shielding) уменьшает вклад источника, если между источником и целью есть блоки из тега `#radworks:shielding_blocks`.

### Как работает
- Для источника с координатами строится линия к центру цели.
- По линии считаются защитные блоки.
- На выходе есть:
  - `rawContribution` (до экранирования),
  - `finalContribution` (после экранирования),
  - `shieldingMultiplier`, `shieldingBlocksHit`.

### Почему часть источников не экранируется
- `shielding=not_applicable` для источников без позиции.
- “Свой” переносимый источник (self-carried) не экранируется для той же сущности.

### Ограничения
- Используется line-sampling, без multi-ray модели.
- Отдельной логики брони для мобов нет.

---

## 8. Воздействие на игроков и мобов

### Что поддержано
- Игроки.
- Неигровые живые сущности (мобы и др.) в пределах ограниченного радиуса/лимита.
- Armor stand по умолчанию исключены (можно включить через конфиг).

### Когда эффект не применяется
- Режим эффекта отключён (`effectMode=disabled`).
- Не выбран или не зарегистрирован runtime-эффект.
- `totalExposure < threshold`.
- Сущность отфильтрована лимитом (`maxLivingTargetsPerScan`) или радиусом.

### Что важно помнить
- Моб с радиоактивным предметом может получить эффект от своего окружения/переносимого источника.
- Другой игрок рядом тоже может быть источником (aura path).
- Долгосрочная доза, отдельный урон и истощение сейчас не реализованы.

---

## 9. Вложенные контейнеры

### Что поддерживается
- `DataComponents.CONTAINER`
- `DataComponents.BUNDLE_CONTENTS`

### Где работает
- Инвентарь игрока;
- контейнеры блоков;
- инвентари сущностей;
- выброшенные контейнеры;
- контейнеры в рамках (item frame).

### Что пока не поддерживается полноценно
- Глубокие адаптеры для Create toolbox.
- Глубокие адаптеры для Sophisticated Backpacks/Storage.
- Произвольное глубокое NBT-сканирование.

### Что проверять
- В source rows: `nested=true`, `nestedDepth`, `containerItemId`, `containerPath`.
- В dump: `nestedContainerDiagnostics`.

---

## 10. Правила `exclude`, `contain`, `force`

### Простыми словами
- **exclude**: полностью исключить источник из расчёта.
- **contain**: ослабить (`scale`) или занулить (`suppress`) вклад.
- **force**: создать источник **только** из уже замеченного системой кандидата.

### Важно про `force`
- Не добавляет новые сканеры.
- Не расширяет радиус сканирования.
- Не делает дополнительный глубокий NBT/Components обход.
- `exclude` всегда сильнее `force`.

### Порядок обработки
1. найти источники;
2. агрегировать похожие;
3. применить `exclude`;
4. применить `contain`;
5. применить `force` только к уже замеченным кандидатам;
6. снова применить `contain` к принудительно добавленным строкам;
7. применить `shielding`;
8. посчитать итог и принять решение по эффекту.

### Пример: `exclude` по `itemId`
```json
{
  "id": "radworks:exclude_uranium_item",
  "enabled": true,
  "type": "exclude",
  "selectors": { "itemId": "createnuclear:raw_uranium" }
}
```

### Пример: `contain` по `carrierBlockId` с `scale`
```json
{
  "id": "radworks:contain_chest_scale_half",
  "enabled": true,
  "type": "contain",
  "selectors": {
    "carrierBlockId": "minecraft:chest",
    "sourceType": "block_entity_inventory"
  },
  "mode": "scale",
  "multiplier": 0.5
}
```

### Пример: `force` по `blockId`
```json
{
  "id": "radworks:force_stone_block",
  "enabled": true,
  "type": "force",
  "selectors": {
    "blockId": "minecraft:stone",
    "sourceType": "block"
  },
  "forceStrength": 3.0,
  "forceRadius": 4.0,
  "forceUnitMode": "block",
  "forceRespectsShielding": true
}
```

---

## 11. Конфигурация

Ниже только ключи, подтверждённые по `RadWorksConfig.java`.

### 11.1 `gameplay`

| Key | Default | Диапазон | Простыми словами | Когда менять | Риск неверной настройки |
|---|---:|---|---|---|---|
| `gameplay.enabled` | `true` | bool | Главный переключатель gameplay-части | Диагностика/изоляция | Полностью отключит auto-логику |
| `gameplay.autoApplyEffect` | `true` | bool | Автоматическое применение эффекта | Если нужен только ручной контроль | Будет казаться, что “не работает эффект” |
| `gameplay.exposureThreshold` | `1.0` | `0..1000000` | Порог для эффекта | Баланс | Слишком высокий порог почти всё выключит |
| `gameplay.effectDurationTicks` | `120` | `1..72000` | Длительность эффекта | Под баланс сервера | Очень малое значение даст “мигание” |
| `gameplay.scanIntervalTicks` | `40` | `1..12000` | Интервал проверок | Нагрузка/отклик | Малое значение увеличит нагрузку |
| `gameplay.applyEffectToPlayers` | `true` | bool | Применять к игрокам | Точечная изоляция | Игроки перестанут получать эффект |
| `gameplay.applyEffectToLivingEntities` | `true` | bool | Применять к живым сущностям | Тесты/изоляция | Мобы не будут обрабатываться |
| `gameplay.applyEffectToMobs` | `true` | bool | Допуск мобов | Тонкая настройка | Часть целей выпадет |
| `gameplay.applyEffectToArmorStands` | `false` | bool | Допуск бронестоек | Спец-тесты | Лишний шум в проверках |
| `gameplay.maxLivingTargetsPerScan` | `32` | `1..256` | Лимит живых целей за цикл | Производительность | Слишком низко -> пропуски целей |
| `gameplay.livingTargetScanRadius` | `8` | `1..32` | Радиус поиска живых целей | Большие зоны | Слишком большой радиус -> нагрузка |
| `gameplay.applyShieldingToLivingEntities` | `true` | bool | Экранирование для living targets | Сравнительные тесты | Изменит итог exposure |
| `gameplay.damageEnabled` | `false` | bool | Зарезервировано, урон не включён | Обычно не трогать | Может запутать ожидания |
| `gameplay.alwaysShowRadiusVisualization` | `false` | bool | Постоянный показ радиусов | Диагностика | Визуальный шум |
| `gameplay.effectMode` | `external_if_present` | enum | Режим выбора эффекта | Совместимость с внешним модом | `selected_effect_missing`/нет эффекта |

### 11.2 `rules`

| Key | Default | Диапазон | Простыми словами | Когда менять | Риск неверной настройки |
|---|---:|---|---|---|---|
| `rules.enableDevRules` | `false` | bool | Включает dev-правила | Локальный smoke test | Некорректный боевой баланс |
| `rules.dynamicRadiusEnabled` | `true` | bool | Включает динамический радиус | Отладка | Радиусы станут только базовыми |
| `rules.dynamicRadiusScale` | `0.5` | `0..10` | Насколько быстро растёт радиус | Баланс | Слишком большие “дальние” источники |
| `rules.dynamicRadiusMaxCap` | `8.0` | `0..128` | Максимальный радиус | Баланс/перф | Неверная дальность |
| `rules.dynamicRadiusFormula` | `log2_scaled` | string | Подпись формулы для диагностики | Почти не нужно | Почти только косметика |
| `rules.nestedContainersEnabled` | `true` | bool | Вложенные контейнеры | Изоляция nested-проблем | Вложенные источники исчезнут |
| `rules.nestedContainerMaxDepth` | `2` | `1..5` | Глубина вложенности | Перф/точность | Глубокий контент не учтётся |
| `rules.nestedContainerMaxItemsPerSource` | `128` | `1..1024` | Лимит дочерних item | Перф | Обрезание содержимого |
| `rules.nestedContainerDiagnosticSampleCap` | `20` | `1..200` | Лимит nested-samples | Глубокая диагностика | Потеря нужных примеров |
| `rules.sourceOverridesEnabled` | `true` | bool | Главный toggle override-слоя | Быстрый bypass | Выключит `exclude/contain/force` |
| `rules.sourceExclusionsEnabled` | `true` | bool | Применять `exclude` | Точечный контроль | Исключения не работают |
| `rules.sourceContainmentEnabled` | `true` | bool | Применять `contain` | Точечный контроль | Подавление/scale не работают |
| `rules.forcedSourcesEnabled` | `true` | bool | Применять `force` | Точечный контроль | Forced rows не появятся |
| `rules.sourceOverrideDiagnosticSampleCap` | `20` | `1..200` | Лимит override-samples | Расширенный debug | Потеря подробностей |

### 11.3 `integrations`

| Key | Default | Диапазон | Простыми словами | Когда менять | Риск неверной настройки |
|---|---:|---|---|---|---|
| `integrations.createTransientCarriersEnabled` | `true` | bool | Включить Create transient path | При отсутствии Create | Потеря части источников |
| `integrations.createTransientCarrierNbtScanEnabled` | `true` | bool | Чтение known-path полей Create | Локальная диагностика | Пропадут transient находки |
| `integrations.createTransientCarrierMaxScanRadius` | `8` | `1..64` | Радиус transient-скана | Большие сцены | Лишняя нагрузка |
| `integrations.createTransientCarrierDiagnosticSampleCap` | `20` | `1..200` | Лимит диагностик Create | Debug | Мало данных для анализа |
| `integrations.createTransientCarrierPathSampleCap` | `5` | `1..20` | Лимит path-записей | Debug | Скрытые path-проблемы |
| `integrations.entityCarriersEnabled` | `true` | bool | Включить entity sources | Изоляция | Потеря coverage по сущностям |
| `integrations.entityDroppedItemsEnabled` | `true` | bool | dropped item path | Частичный disable | Нет `entity_dropped_item` |
| `integrations.entityItemFramesEnabled` | `true` | bool | item frame path | Частичный disable | Нет `entity_item_frame` |
| `integrations.entityPlayerAuraEnabled` | `true` | bool | aura других игроков | Частичный disable | Нет aura-вкладов |
| `integrations.entityCarrierMaxScanRadius` | `8` | `1..64` | Радиус entity-скана | Большие зоны | Нагрузка/шум |
| `integrations.entityCarrierDiagnosticSampleCap` | `20` | `1..200` | Лимит entity-samples | Debug | Недостаток причин skip |
| `integrations.entityChestBoatsEnabled` | `true` | bool | chest boat инвентарь | Частичный disable | Потеря chest boat coverage |
| `integrations.entityPackAnimalsEnabled` | `true` | bool | pack animals | Частичный disable | Потеря покрытия животных |
| `integrations.entityGenericInventoryCapabilityEnabled` | `true` | bool | generic entity capability | Частичный disable | Потеря generic источников |
| `integrations.entityInventoryDiagnosticSampleCap` | `20` | `1..200` | Лимит inventory-samples | Debug | Сложнее понять причину |
| `integrations.worldFluidClusterDiscoveryRadius` | `10` | `1..32` | Радиус поиска кластеров жидкости | Waterfall/pool тесты | Обрезанные кластеры |

### 11.4 `diagnostics/debug`

Отдельной config-группы `diagnostics` в `RadWorksConfig` нет.  
Диагностика управляется командами и перечисленными лимитами (`*DiagnosticSampleCap`, `alwaysShowRadiusVisualization` и т.п.).

NEEDS_VERIFICATION: перед публикацией новой версии гайда повторно сверить ключи, если в ветке появились новые коммиты.

---

## 12. Команды

| Команда | Зачем нужна | Пример | Что означает результат | Если вывод странный |
|---|---|---|---|---|
| `/radworks validate` | Проверка правил, config и режимов | `/radworks validate` | Показывает состояние rules, overrides, effect mode, integration toggles | Смотрите `errors/warnings` и сразу делайте `/radworks dump` |
| `/radworks sources [player]` | Показать текущие источники | `/radworks sources` | Видно тип источника, вклад, override/shielding поля | Если строк мало — проверить радиусы/тогглы/правила |
| `/radworks exposure [player]` | Посчитать итоговое воздействие | `/radworks exposure` | `Total`, `threshold`, `reason`, источники | Если `Total=0`, ищите `exclude/contain` и `not_applicable` |
| `/radworks dump` | Снять полный диагностический снимок | `/radworks dump` | Создаёт JSON для анализа | Это главный артефакт для разработчика |
| `/radworks radius show [seconds]` | Визуально показать радиусы | `/radworks radius show 10` | `visualizedSources`, `skippedSources`, `maxRadiusSeen` | Если не видно — проверьте positioned sources и `radius status` |
| `/radworks radius clear` | Остановить визуализацию | `/radworks radius clear` | Сбрасывает активный показ | Если снова включается, проверьте `alwaysShowRadiusVisualization` |
| `/radworks radius status` | Статус визуализации | `/radworks radius status` | active/remaining/maxRadiusSeen | Если всегда inactive — нет подходящих источников |
| `/radworks effect apply-self` | Ручное применение `radworks:radiation` к себе | `/radworks effect apply-self` | Пробует выдать эффект, показывает `changed` | Нужен запуск от игрока и права |
| `/radworks effect clear-self` | Ручное снятие `radworks:radiation` с себя | `/radworks effect clear-self` | Показывает `removed` | Если `false`, эффекта могло не быть |
| `/radworks effect status` | Проверить ручной статус и strategy | `/radworks effect status` | Active/Duration + strategy mode | Если `selected_effect_missing`, проверяйте эффект в реестре |

Примечание: команда `/radworks version` также доступна, но для диагностики обычно важнее `validate/sources/exposure/dump`.

---

## 13. Визуализация и интерфейсы

- GUI/HUD/screen-интерфейсов на текущем baseline **нет**.
- Основной “интерфейс” мода: команды, конфиг и `dump`.
- Единственная встроенная визуальная диагностика: частицы радиуса через `/radworks radius ...`.

---

## 14. Как читать dump

| Раздел dump | Зачем нужен | Что смотреть | Тревожный признак |
|---|---|---|---|
| `rules` | Состояние rule-файлов | `loaded`, `errors`, `ruleCandidates` | Ошибки загрузки или отсутствие нужного rule |
| `sourceScanSummary` | Итог сканов и этапов | `*Checked`, `*Matches`, override/force counters | Нужный источник не появляется или всё “омитится” |
| `handlerDiagnostics` | Почему handler-источники не подошли | `itemHandlerNonMatchingSamples`, `fluidHandlerNonMatchingSamples` | Частые `no_active_*_rule` / distance issues |
| `worldFluidDiagnostics` | Поведение кластеров жидкости | `worldFluidDiscoveryRadius`, `clusterSamples` | Малый cluster size, `maybeClippedByDiscoveryRadius=true` |
| `createCarrierDiagnostics` | Диагностика Create known paths | `fluidPathSamples`, `unexpectedStructureSamples` | `path_missing`, нераспознанная структура |
| `entityCarrierDiagnostics` | Поиск источников у сущностей | `matched*`, `skipSamples` | Много skip по фильтрам/конфигу |
| `nestedContainerDiagnostics` | Вложенные контейнеры | `supported/unsupported`, `depth/item limit hits` | Unsupported format или лимит глубины/элементов |
| `sourceOverrideDiagnostics` | Этап `exclude/contain/force` | `sourcesExcluded`, `sourcesContained`, `forcedSourcesAdded`, `applicationSamples` | Force skip из-за invalid rule/no candidate |
| `radiusVisualization` | Статус визуализации | `active`, `lastVisualizedSources`, `maxRadiusSeen` | Всегда `active=false` при ожидаемом показе |
| `gameplay` | Решения по эффектам | `lastAutoApplyDecisions`, `livingEntityEffectDecisions` | `below_threshold`, `selected_effect_missing`, capping |
| `effectStrategy` | Выбор runtime-эффекта | `runtimeEffectMode`, selected/registered flags | Режим отключён или эффект не найден |
| `recentWarnings` | Быстрый журнал предупреждений | последние warning записи | Повторяющиеся предупреждения по одному узлу |

---

## 15. Практическое тестирование

### Сценарий A: базовая проверка
- Подготовка: чистый запуск с нужными правилами.
- Команды:
  - `/radworks validate`
  - `/radworks exposure`
  - `/radworks dump`
- Ожидание: rules loaded, команды отрабатывают, dump создаётся.

### Сценарий B: жидкость в мире
- Поставить: 1 блок, затем пул, затем “водопад”.
- Команды:
  - `/radworks sources`
  - `/radworks exposure`
  - `/radworks dump`
- Ожидание: есть `world_fluid`, cluster diagnostics меняются с размером массы.

### Сценарий C: контейнер
- Поставить сундук с радиоактивными предметами.
- Команды: `sources`, `exposure`, `dump`.
- Ожидание: `block_entity_inventory` строки и вклад в `totalExposure`.

### Сценарий D: вложенный контейнер
- Проверить shulker/bundle с радиоактивным содержимым.
- Команды: `sources`, `dump`.
- Ожидание: `nested=true`, `containerPath`; без crash.

### Сценарий E: живые сущности
- Моб рядом с источником, моб с радиоактивным предметом.
- Команды: `exposure`, `dump`.
- Ожидание: записи в `livingEntityEffectDecisions`.

### Сценарий F: экранирование
- Один и тот же источник: без блока и с блоком экранирования между source/target.
- Команды: `sources`, `exposure`, `dump`.
- Ожидание: изменение `rawContribution` -> `finalContribution`, `shielding=reduced`.

### Сценарий G: override rules
- Проверить `exclude`, `contain(scale/suppress)`, `force`.
- Команды: `validate`, `sources`, `exposure`, `dump`.
- Ожидание: этапы отражены в `sourceOverrideDiagnostics`.

### Что отправлять разработчику
- dump-файл (только для failing/confusing);
- использованные JSON из `source_override_rules`;
- версии модов;
- `latest.log` только при crash/неочевидном warning.

---

## 16. Если что-то не работает

### 1) Проблема: радиация не появляется
- Возможные причины: нет подходящего rule; отключён нужный integration toggle; источник вне радиуса.
- Команды проверки: `/radworks validate`, `/radworks sources`.
- Что смотреть в dump: `rules`, `sourceScanSummary`, профильный diagnostics section.
- Что прислать разработчику: dump + rules JSON + краткий сценарий.

### 2) Проблема: источник есть, но итог 0
- Возможные причины: `exclude` или `contain suppress`; вклад срезан до 0; ниже порога.
- Команды проверки: `/radworks sources`, `/radworks exposure`.
- Что смотреть в dump: source row `overrideMode/finalContribution`, `sourceOverrideDiagnostics`, `gameplay`.
- Что прислать: dump + override rules JSON.

### 3) Проблема: `force` не работает
- Возможные причины: кандидат не наблюдался; rule invalid; `exclude` перекрывает.
- Команды проверки: `/radworks validate`, `/radworks sources`.
- Что смотреть в dump: `sourceOverrideDiagnostics.force*`.
- Что прислать: dump + force rule + описание наблюдаемого объекта.

### 4) Проблема: `contain`/`exclude` сработали неожиданно
- Возможные причины: selector слишком общий; совпало больше строк, чем ожидалось.
- Команды проверки: `/radworks sources`, `/radworks validate`.
- Что смотреть в dump: `sourceOverrideDiagnostics.applicationSamples`.
- Что прислать: dump + конкретный rule файл.

### 5) Проблема: shield не снижает радиацию
- Возможные причины: источник `not_applicable`; блок не в `#radworks:shielding_blocks`; self-carried случай.
- Команды проверки: `/radworks exposure`, `/radworks sources`.
- Что смотреть в dump: source row shielding-поля, `sourceScanSummary.shielding*`.
- Что прислать: dump + координаты source/target/shield.

### 6) Проблема: моб не получает эффект
- Возможные причины: `applyEffectToLivingEntities/applyEffectToMobs` выключены; cap; ниже порога; режим эффекта.
- Команды проверки: `/radworks validate`, `/radworks exposure`.
- Что смотреть в dump: `gameplay.livingEntityEffectDecisions`, `livingTargetCounters`.
- Что прислать: dump + gameplay-конфиг.

### 7) Проблема: вложенный контейнер не фонит
- Возможные причины: unsupported формат; nested выключен; depth/item limit hit.
- Команды проверки: `/radworks sources`, `/radworks validate`.
- Что смотреть в dump: `nestedContainerDiagnostics`.
- Что прислать: dump + item id + mod list.

### 8) Проблема: жидкость ведёт себя странно
- Возможные причины: discovery radius мал; cluster clipping; не найден fluid rule.
- Команды проверки: `/radworks sources`, `/radworks exposure`.
- Что смотреть в dump: `worldFluidDiagnostics`, `sourceScanSummary.worldFluid*`.
- Что прислать: dump + описание формы жидкости (лужа/поток).

### 9) Проблема: визуализация радиуса не видна
- Возможные причины: нет positioned sources; визуализация истекла; `clear` уже выполнен.
- Команды проверки: `/radworks radius status`, `/radworks radius show 10`.
- Что смотреть в dump: `radiusVisualization`, source rows с `position`.
- Что прислать: dump + текст вывода `radius status`.

### 10) Проблема: команда эффекта не работает
- Возможные причины: команда выполнена не игроком; нет прав; эффект не зарегистрирован.
- Команды проверки: `/radworks effect status`, `/radworks effect apply-self`.
- Что смотреть в dump: `effectStrategy`, `gameplay`.
- Что прислать: dump + точный command output.

---

## 17. Ограничения текущей версии

- Нет долгосрочного накопления дозы радиации.
- Нет отдельной системы урона, голода или истощения от радиации.
- Нет отдельной логики брони для мобов.
- Нет глубокой поддержки Create contraptions/trains.
- Нет глубокой поддержки Aeronautics/Simulated.
- Нет готовых глубоких адаптеров для Create toolbox и Sophisticated.
- Нет произвольного глубокого NBT-сканирования “всего подряд”.

---

## 18. Словарь терминов

- **Источник (source)**: объект, который даёт вклад в радиацию.  
  Пример: `player_inventory`, `world_fluid`.
- **Кандидат (candidate)**: наблюдённый объект, из которого *можно* создать источник при `force`.
- **Уже увиденный кандидат (observed candidate)**: кандидат, который реально встретился существующему сканированию.
- **Вклад (contribution)**: числовой вклад одного источника в общий итог.
- **Итоговое воздействие (totalExposure)**: сумма вкладов всех источников.
- **Экранирование (shielding)**: ослабление вклада защитными блоками между источником и целью.
- **Подавление контейнером (containment)**: правило `contain`, которое уменьшает или зануляет вклад.
- **Принудительный источник (forced source)**: источник, добавленный по правилу `force` из observed candidate.
- **Диагностический дамп (dump)**: JSON-снимок состояния мода, создаётся `/radworks dump`.

---

## Экспорт в PDF

```bash
pandoc RADWORKS_ADMIN_TESTER_GUIDE.md \
  --pdf-engine=xelatex \
  -V mainfont="DejaVu Serif" \
  -V monofont="DejaVu Sans Mono" \
  -o RADWORKS_ADMIN_TESTER_GUIDE.pdf
```

Если `xelatex` недоступен:
```bash
pandoc RADWORKS_ADMIN_TESTER_GUIDE.md \
  --pdf-engine=lualatex \
  -V mainfont="DejaVu Serif" \
  -V monofont="DejaVu Sans Mono" \
  -o RADWORKS_ADMIN_TESTER_GUIDE.pdf
```

---

## Примечание о достоверности

Документ основан на текущих данных репозитория: `README.md`, `CHANGELOG.md`, `MIGRATION_STATUS.md`, `TESTING.md`, `BETA_TESTER_HANDOFF.md`, `CURRENT_STATE_FOR_CHATGPT.md`, `PROJECT_RECALL_AND_ROADMAP.md`, а также фактическом коде в `src/main/java/dev/radworks/**`.

Если в ветке после этого документа появятся новые коммиты по механикам/командам/конфигу, соответствующие пункты требуют повторной сверки (`NEEDS_VERIFICATION`).
