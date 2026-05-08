# Testing Plan

Новый мод должен иметь ручные тесты, которые пользователь может выполнить в Minecraft и принести результат Codex. Автотесты полезны, но не заменяют диагностику в реальном модпаке.

## Test: Mod starts

### Purpose
Проверить, что новый NeoForge-мод загружается.

### Setup
Чистый тестовый клиент или dedicated server с новым модом.

### Steps
1. Запустить Minecraft/сервер.
2. Дождаться входа в мир.
3. Проверить лог на ошибки RadWorks.

### Expected result
Игра запускается, RadWorks присутствует в списке модов, crash отсутствует.

### Diagnostic command to run
`/radworks version`

### What to paste back to Codex
Вывод команды и ошибки из latest.log, если они есть.

## Test: Command exists

### Purpose
Проверить регистрацию команд.

### Setup
Мир с cheats/operator permissions.

### Steps
1. Ввести `/radworks`.
2. Проверить autocomplete.
3. Выполнить `/radworks version`.

### Expected result
Команда существует, подкоманды доступны согласно phase.

### Diagnostic command to run
`/radworks version`

### What to paste back to Codex
Список доступных подкоманд или screenshot/autocomplete text.

## Test: Dump file created

### Purpose
Проверить, что пользователь может создать diagnostic dump.

### Setup
Мир с RadWorks Phase 0+.

### Steps
1. Выполнить `/radworks dump`.
2. Найти путь к созданному JSON в chat/log.
3. Открыть JSON и проверить, что он не пустой.

### Expected result
Создан readable JSON с mod/world/player info.

### Diagnostic command to run
`/radworks dump`

### What to paste back to Codex
Содержимое JSON или ключевые поля: mod, world, player, integrations, warnings.

## Test: Rules validate

### Purpose
Проверить загрузку data-driven radiation rules.

### Setup
Phase 1 build with default rules.

### Steps
1. Запустить мир.
2. Выполнить `/radworks validate`.
3. Проверить warnings/errors.

### Expected result
Валидные правила загружены; отсутствующие optional mods помечаются как warnings, а не crash.

### Diagnostic command to run
`/radworks validate`

### What to paste back to Codex
Полный вывод validate.

## Test: Radioactive item in inventory affects player

### Purpose
Проверить Phase 2 inventory radiation.

### Setup
Игрок в мире, доступен радиоактивный предмет из rules, например `createnuclear:raw_uranium`, если мод установлен.

### Steps
1. Убедиться, что inventory пуст от radioactive items.
2. Выполнить `/radworks exposure <player>`.
3. Положить radioactive item в inventory.
4. Повторить `/radworks exposure <player>`.

### Expected result
До предмета exposure отсутствует или 0. После предмета появляется contribution с ID предмета.

### Diagnostic command to run
`/radworks exposure <player>`

### What to paste back to Codex
Оба вывода exposure: before/after.

## Test: Non-radioactive item does not affect player

### Purpose
Проверить отсутствие false positives.

### Setup
Обычный предмет, например `minecraft:dirt`.

### Steps
1. Положить обычный предмет в inventory.
2. Выполнить `/radworks exposure <player>`.

### Expected result
Обычный предмет не появляется как radiation source.

### Diagnostic command to run
`/radworks exposure <player>`

### What to paste back to Codex
Exposure output.

## Test: Radius works

### Purpose
Проверить distance/radius behavior.

### Setup
Radioactive block source from rules.

### Steps
1. Поставить source block.
2. Встать рядом.
3. Выполнить `/radworks sources`.
4. Отойти дальше радиуса.
5. Повторить `/radworks sources`.

### Expected result
Внутри радиуса source даёт contribution; вне радиуса не влияет.

### Diagnostic command to run
`/radworks sources`

### What to paste back to Codex
Вывод на двух дистанциях.

## Test: Shielding works

### Purpose
Проверить shielding blocks.

### Setup
Phase 5 build, radioactive source, shielding block from rules/tag.

### Steps
1. Встать в зоне источника без shielding.
2. Выполнить `/radworks exposure <player>`.
3. Поставить shielding block между игроком и source.
4. Повторить команду.

### Expected result
Diagnostics показывает shielding result и изменение final contribution согласно выбранной модели.

### Diagnostic command to run
`/radworks exposure <player>`

### What to paste back to Codex
Before/after exposure output with shielding section.

## Test: Chest source works

### Purpose
Проверить block entity inventory source.

### Setup
Phase 4 build, chest, radioactive item.

### Steps
1. Поставить chest.
2. Положить radioactive item.
3. Встать рядом.
4. Выполнить `/radworks sources`.
5. Убрать item.
6. Повторить command.

### Expected result
Chest source появляется только когда внутри radioactive item.

### Diagnostic command to run
`/radworks sources`

### What to paste back to Codex
Output before/after removing item.

## Test: Fluid source works

### Purpose
Проверить block entity fluid provider.

### Setup
Phase 4 build, tank/fluid handler, radioactive fluid rule.

### Steps
1. Наполнить tank radioactive fluid.
2. Выполнить `/radworks sources`.
3. Опустошить tank.
4. Повторить command.

### Expected result
Fluid source отображается с fluid ID и amount.

### Diagnostic command to run
`/radworks sources`

### What to paste back to Codex
Sources output and installed tank mod name.

## Test: Armor protection works

### Purpose
Проверить full-set protection.

### Setup
Phase 6 build, radioactive source, configured protection armor.

### Steps
1. Получить exposure без брони.
2. Надеть полный комплект protection armor.
3. Повторить exposure.
4. Снять одну часть комплекта.
5. Повторить exposure.

### Expected result
Full set protects according to rules; incomplete set не считается полным комплектом.

### Diagnostic command to run
`/radworks exposure <player>`

### What to paste back to Codex
Три вывода: no armor/full set/partial set.

## Test: Effect application works

### Purpose
Проверить gameplay consequences.

### Setup
Phase 6 build.

### Steps
1. Встать в зоне exposure.
2. Подождать несколько ticks/seconds.
3. Проверить active effects.
4. Выйти из зоны.

### Expected result
Effect/damage/exhaustion соответствуют documented values.

### Diagnostic command to run
`/radworks exposure <player>`

### What to paste back to Codex
Exposure output and active effect list.

## Test: Performance smoke test

### Purpose
Проверить, что сканирование источников не ломает TPS.

### Setup
Phase 7 build, debug/performance diagnostics enabled.

### Steps
1. Разместить много static sources и containers.
2. Подождать 1-2 минуты.
3. Выполнить `/radworks dump`.

### Expected result
Нет заметных лагов, dump содержит scan timings.

### Diagnostic command to run
`/radworks dump`

### What to paste back to Codex
Performance section from dump.

## Test: Multiplayer smoke test

### Purpose
Проверить server-authoritative behavior для нескольких игроков.

### Setup
Dedicated server или LAN с двумя игроками.

### Steps
1. Игрок A стоит рядом с source.
2. Игрок B стоит далеко.
3. Выполнить exposure для обоих.
4. Поменять позиции.

### Expected result
Exposure считается отдельно для каждого игрока.

### Diagnostic command to run
`/radworks exposure <player>`

### What to paste back to Codex
Выводы для обоих игроков.

## Test: Dedicated server smoke test

### Purpose
Проверить отсутствие client-only crashes.

### Setup
Dedicated server с RadWorks.

### Steps
1. Запустить сервер.
2. Подключиться клиентом.
3. Выполнить `/radworks version` и `/radworks dump`.

### Expected result
Сервер не падает, команды работают.

### Diagnostic command to run
`/radworks dump`

### What to paste back to Codex
Server log snippet and dump.

## Test: Create integration test, later

### Purpose
Проверить Create contraption provider после Phase 8.

### Setup
Create installed, Create integration enabled, radioactive source inside minecart contraption.

### Steps
1. Создать minecart contraption.
2. Поместить radioactive block/item.
3. Выполнить `/radworks sources`.
4. Переместить contraption.
5. Повторить command.

### Expected result
Contraption source обнаруживается и меняет позицию.

### Diagnostic command to run
`/radworks sources`

### What to paste back to Codex
Sources output with contraption section.

## Test: Aeronautics integration test, later

### Purpose
Проверить Aeronautics/Simulated integration после отдельного research spike.

### Setup
Actual target Aeronautics/Simulated mod installed.

### Steps
1. Создать объект/субуровень с radioactive source.
2. Выполнить `/radworks dump`.
3. Выполнить `/radworks sources`.

### Expected result
Integration reports loaded status and detected/unsupported objects.

### Diagnostic command to run
`/radworks dump`

### What to paste back to Codex
Integration section from dump and source output.
