# RadWorks (NeoForge)

RadWorks is a data-driven radiation mod for NeoForge, migrated from an earlier KubeJS prototype into standalone Java mod architecture.  
Current project status is **beta**: core source detection, diagnostics, and controlled gameplay effect application are implemented, while deep cross-mod integrations remain post-beta work.

## Supported Environment
- Minecraft: `1.21.1`
- Loader: `NeoForge 21.1.228`
- Java: `21`
- Mod ID: `radworks`

## What It Does (Beta)
- Loads radiation rules from datapack JSON files (`data/radworks/radiation_rules`).
- Detects radiation sources from:
  - player inventory,
  - static blocks,
  - vanilla containers/block entities,
  - NeoForge block item/fluid capabilities,
  - Create transient/internal carrier data paths (optional, guarded).
- Applies shielding attenuation using `#radworks:shielding_blocks`.
- Computes armor/effect diagnostics and runtime effect strategy.
- Can auto-apply a runtime radiation effect (config-gated, no damage loop in beta).
- Provides command diagnostics:
  - `/radworks version`
  - `/radworks validate`
  - `/radworks sources`
  - `/radworks exposure`
  - `/radworks dump`
  - `/radworks effect ...`
  - `/radworks radius ...`

## Build
```bash
./gradlew test
./gradlew build
```

## Development Run
```bash
./gradlew runClient
./gradlew runServer
```

## Install (User)
1. Build the mod (`./gradlew build`).
2. Take the jar from `build/libs/` (typically `radworks-0.1.0.jar`).
3. Place it into your NeoForge instance `mods/` folder.
4. Start the game and run `/radworks version`.

## Known Limitations (Beta)
- No hard dependencies on Create/Create Nuclear/TFMG/Aeronautics/Sophisticated: optional content is handled safely via diagnostics when absent.
- No deep Create contraption/train internals yet.
- No Aeronautics/Simulated deep integration yet.
- No persistent accumulated dose system yet.
- Damage/exhaustion balancing is not enabled for beta gameplay loop.

## Developer Quick Map
- Entry point: `src/main/java/dev/radworks/RadWorks.java`
- Commands: `src/main/java/dev/radworks/command/`
- Source/rules/exposure logic: `src/main/java/dev/radworks/radiation/`
- Gameplay loop/config: `src/main/java/dev/radworks/gameplay/`, `src/main/java/dev/radworks/config/`
- Diagnostics and dumps: `src/main/java/dev/radworks/diagnostics/`

## License Status
License is **not finalized yet**. See `LICENSE_PLACEHOLDER.md` before publishing.
