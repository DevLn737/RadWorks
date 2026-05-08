# AGENTS.md

## Project
RadWorks is a clean NeoForge Minecraft mod rebuilt from an old KubeJS prototype.

The old KubeJS project is a source of behavior, requirements, bugs and migration notes. It is not the architecture for the new mod.

## User context
The project owner is not a Java programmer.

Codex must keep the project understandable, documented, testable and diagnosable. Prefer explicit status updates, small phases and manual Minecraft tests.

## Core rules
- Work in small phases.
- Never implement large hidden rewrites.
- Always update `MIGRATION_STATUS.md`.
- Always update `TESTING.md` when behavior changes.
- Always update `DIAGNOSTICS.md` when debug output changes.
- Prefer diagnostic-first development.
- Do not require KubeJS as a runtime dependency.
- Do not copy KubeJS architecture blindly.
- Do not implement Create/Aeronautics before core radiation works.
- Isolate optional integrations.
- If something is unknown, write UNKNOWN/TODO instead of guessing.
- If something is buggy in the old project, do not preserve the bug unless explicitly required.
- If behavior is unclear, mark `MIGRATION_DECISION_REQUIRED`.

## Phase 0 status
Phase 0 only includes:
- a minimal NeoForge mod scaffold;
- `/radworks version`;
- `/radworks dump`;
- repository documentation.

Phase 0 must not include radiation gameplay, radioactive items, shielding, effects, Create integration, Aeronautics integration, capabilities, attachments, radiation rules or KubeJS dependencies.

## Accepted technical baseline
- Minecraft: `1.21.1`
- NeoForge: `21.1.228`
- Java: `21`
- Mod ID: `radworks`
- Java package: `dev.radworks`
- Mod version: `0.1.0`

These choices are accepted for Phase 0 and can be revisited if the target modpack uses another Minecraft/NeoForge version.

## Build commands
Expected commands:

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
```

If `runServer` stops for Minecraft EULA acceptance, document it as an expected manual setup step, not a Phase 0 failure.

## Required after every coding task
- Summarize changed files.
- Explain how to test.
- List known limitations.
- Update status docs.
- Run available build/check command if possible.

## Migration source of truth
Use `migration_export/` as the specification, especially:
- `00_README.md`
- `01_PROJECT_OVERVIEW.md`
- `06_NEOFORGE_ARCHITECTURE.md`
- `07_MIGRATION_PHASES.md`
- `08_DIAGNOSTICS_REQUIREMENTS.md`
- `11_AGENTS_TEMPLATE.md`
- `12_FIRST_TASK_PROMPT.md`

## Coding style
- Keep Java code conventional and boring.
- Prefer explicit names over clever abstractions.
- Keep optional integrations isolated from core code.
- Add comments only where they explain non-obvious migration or compatibility decisions.
