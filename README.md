# RadWorks

RadWorks is a clean NeoForge Minecraft mod rebuilt from an old KubeJS radiation prototype.

Phase 0 is intentionally small: it creates the repository foundation, documentation and diagnostics commands. It does not implement radiation gameplay.

## Phase 0 Features
- Minimal NeoForge mod metadata.
- `/radworks version`.
- `/radworks dump`.
- Basic documentation for migration, diagnostics and manual testing.

## Phase 0 Non-goals
- No radiation mechanics.
- No radioactive items, blocks, fluids or effects.
- No shielding.
- No capabilities or attachments.
- No radiation rules or configs.
- No KubeJS dependency.
- No Create, Create Nuclear or Aeronautics dependencies.

## Accepted Baseline
- Minecraft: `1.21.1`
- NeoForge: `21.1.228`
- Java: `21`
- Mod ID: `radworks`
- Java package: `dev.radworks`
- Mod version: `0.1.0`

These versions are accepted for Phase 0 and may be revisited if the target modpack uses a different Minecraft/NeoForge version.

## Build
```bash
./gradlew build
```

## Run
```bash
./gradlew runClient
./gradlew runServer
```

If `runServer` stops because the Minecraft EULA is not accepted, follow the generated `eula.txt` instructions in the run directory and rerun the command.

## Diagnostics
In Minecraft:

```text
/radworks version
/radworks dump
```

Dump files are written under the active Minecraft instance working directory:

```text
radworks_dumps/radworks-dump-YYYYMMDD-HHMMSS-<player>.json
```
