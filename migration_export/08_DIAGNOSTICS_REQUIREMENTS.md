# Diagnostics Requirements

Новый мод должен быть удобен для пользователя, который не программист, и для Codex, который не может сам проверить мод в Minecraft.

Диагностика должна появиться до сложной логики радиации.

## /radworks version

Показывает:
- mod version;
- Minecraft version;
- NeoForge version;
- Java version;
- loaded integrations;
- enabled/disabled optional providers;
- active rules checksum, если rules уже реализованы.

Пример формата:

```text
RadWorks 0.1.0
Minecraft: 1.xx.x
NeoForge: xx.x.x
Java: xx
Integrations: create=absent, aeronautics=absent
Rules: not loaded
```

## /radworks dump

Создаёт JSON-файл диагностики.

Должен включать:
- mod info;
- world info;
- player position;
- dimension;
- loaded mods relevant to RadWorks;
- config/rules checksum;
- recent warnings;
- performance stats;
- active providers;
- last exposure snapshot, если есть;
- known UNKNOWN/unsupported integration notices.

### Suggested JSON format

```json
{
  "schemaVersion": 1,
  "createdAt": "ISO-8601",
  "mod": {
    "id": "radworks",
    "version": "UNKNOWN",
    "minecraftVersion": "UNKNOWN",
    "neoforgeVersion": "UNKNOWN",
    "javaVersion": "UNKNOWN"
  },
  "world": {
    "dimension": "minecraft:overworld",
    "serverType": "integrated_or_dedicated",
    "gameTime": 0
  },
  "player": {
    "name": "UNKNOWN",
    "uuid": "UNKNOWN",
    "position": {
      "x": 0,
      "y": 0,
      "z": 0
    }
  },
  "rules": {
    "loaded": false,
    "checksum": "UNKNOWN",
    "itemRules": 0,
    "blockRules": 0,
    "fluidRules": 0,
    "warnings": []
  },
  "integrations": {
    "create": {
      "loaded": false,
      "enabled": false,
      "notes": []
    },
    "aeronautics": {
      "loaded": false,
      "enabled": false,
      "notes": []
    }
  },
  "performance": {
    "lastScanMillis": 0,
    "averageScanMillis": 0,
    "sourcesScanned": 0,
    "cacheHitRate": 0
  },
  "recentWarnings": []
}
```

## /radworks sources

Показывает найденные источники рядом с игроком:
- type;
- id;
- position;
- entity UUID, если source entity-based;
- container path;
- radius;
- strength;
- distance;
- shielding;
- final contribution.

Команда должна быть короткой в chat и подробной в dump/log при необходимости.

## /radworks exposure <player>

Показывает расчёт дозы для игрока:
- all candidate sources;
- filtered-out sources and reasons;
- armor protection;
- shielding result;
- distance multiplier;
- final exposure;
- effect/damage result, если Phase 6 реализована.

## /radworks debug on/off

Включает подробный debug.

Требования:
- должен быть permission-protected;
- не должен менять gameplay balance;
- должен быть выключаемым без перезапуска;
- должен логировать bounded output;
- должен показывать в `/radworks dump`, включён ли debug.

## /radworks validate

Проверяет JSON/config rules:
- unknown item IDs;
- unknown block IDs;
- unknown fluid IDs;
- invalid values;
- duplicate entries;
- missing tags;
- broken references;
- optional dependency missing for external IDs.

Команда должна отделять:
- ERROR: правило нельзя применить;
- WARNING: зависимость отсутствует или API неизвестен;
- INFO: правило загружено нормально.

## Required warning categories

- `UNKNOWN_REGISTRY_ID`
- `MISSING_OPTIONAL_MOD`
- `INVALID_RULE_VALUE`
- `DUPLICATE_RULE`
- `UNSUPPORTED_CONTAINER`
- `UNSUPPORTED_ENTITY`
- `UNSUPPORTED_CREATE_API`
- `UNSUPPORTED_AERONAUTICS_API`
- `NBT_FALLBACK_USED`
- `SOURCE_SCAN_TOO_EXPENSIVE`

## Diagnostic files

Recommended output directory:

```text
<minecraft-instance>/radworks_dumps/
```

File naming:

```text
radworks-dump-YYYYMMDD-HHMMSS-<player>.json
```

## Minimum Phase 0 diagnostics

Phase 0 must implement:
- `/radworks version`;
- `/radworks dump`;
- documentation for both commands.

Phase 0 must not implement radiation gameplay.
