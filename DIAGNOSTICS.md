# Diagnostics

Phase 0 diagnostics exist so users and future Codex sessions can inspect the mod before radiation gameplay is implemented.

## `/radworks version`
Prints:

```text
RadWorks 0.1.0
Minecraft: 1.21.1
NeoForge: 21.1.228
Java: <runtime version>
Integrations: create=absent, aeronautics=absent
Rules: loaded, checksum=<short>, mode=lenient/dev
```

If an optional mod is present in the instance, its state may appear as `loaded_disabled`. Phase 0 never enables optional integrations.

Before a world has loaded rules, the final line may still be:

```text
Rules: not loaded
```

## `/radworks validate`
Validates datapack radiation rules from:

```text
data/radworks/radiation_rules/*.json
```

Phase 1 uses validation mode:

```text
lenient/dev
```

Clean output should look like:

```text
RadWorks rules validation: loaded=1 enabled=1 disabled=0 errors=0 warnings=0 mode=lenient/dev checksum=<short>
```

Validation categories:
- `UNKNOWN_REGISTRY_ID`: warning in `lenient/dev`.
- `INVALID_RULE_VALUE`: error for invalid fields like `radius <= 0` or `strength <= 0`.
- `DUPLICATE_RULE`: error for duplicate enabled `type:id`; warning for duplicate disabled rules.
- `MALFORMED_JSON`: error for malformed or unreadable JSON.
- `DISABLED_RULE`: info; disabled rules are counted but not active.

## `/radworks dump`
Creates a JSON diagnostics file in:

```text
<minecraft-instance>/radworks_dumps/
```

Filename format:

```text
radworks-dump-YYYYMMDD-HHMMSS-<player>.json
```

If the command is run from a server console, the filename uses `server` and the JSON field `player` is `null`.

## JSON schema
```json
{
  "schemaVersion": 1,
  "createdAt": "ISO-8601",
  "mod": {
    "id": "radworks",
    "version": "0.1.0",
    "minecraftVersion": "1.21.1",
    "neoforgeVersion": "21.1.228",
    "javaVersion": "runtime"
  },
  "world": {
    "dimension": "minecraft:overworld",
    "serverType": "integrated_or_dedicated",
    "gameTime": 0
  },
  "player": {
    "name": "player",
    "uuid": "uuid",
    "position": {
      "x": 0,
      "y": 0,
      "z": 0
    }
  },
  "rules": {
    "loaded": true,
    "checksum": "full SHA-256",
    "validationMode": "lenient/dev",
    "itemRules": 0,
    "blockRules": 0,
    "fluidRules": 0,
    "disabledRules": 0,
    "errors": [],
    "warnings": [],
    "infos": []
  },
  "integrations": {
    "create": {
      "loaded": false,
      "enabled": false,
      "notes": ["Phase 0: not implemented"]
    },
    "aeronautics": {
      "loaded": false,
      "enabled": false,
      "notes": ["Phase 0: not implemented"]
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

## Not available in Phase 0
- `/radworks sources`
- `/radworks exposure`
- `/radworks debug`
- Gameplay use of radiation rules
- Exposure snapshots

## Phase 1 rule format
```json
{
  "type": "item",
  "id": "minecraft:rotten_flesh",
  "strength": 1.0,
  "radius": 2.0,
  "respectsShielding": true,
  "enabled": true,
  "comment": "Temporary Phase 1 dev-only smoke rule"
}
```

Allowed `type` values:
- `item`
- `block`
- `fluid`

Phase 1 does not scan the world or apply these rules to gameplay.
