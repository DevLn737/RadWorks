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

## Required after every coding task
- Summarize changed files.
- Explain how to test.
- List known limitations.
- Update status docs.
- Run available build/check command if possible.

## Migration source of truth
Use `migration_export/` as the specification:
- `00_README.md`
- `01_PROJECT_OVERVIEW.md`
- `02_FEATURE_INVENTORY.md`
- `03_BUGS_AND_GAPS.md`
- `04_CONTENT_REGISTRY.md`
- `05_BEHAVIOR_SPEC.md`
- `06_NEOFORGE_ARCHITECTURE.md`
- `07_MIGRATION_PHASES.md`
- `08_DIAGNOSTICS_REQUIREMENTS.md`
- `09_TESTING_PLAN.md`
- `10_NEW_CODEX_MASTER_PROMPT.md`
- `12_FIRST_TASK_PROMPT.md`

## Build commands
UNKNOWN until the new NeoForge project is created.

Expected commands after setup may include:

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
```

If these commands differ, update this file.

## Manual testing
Every feature must include a manual Minecraft test in `TESTING.md`.

Minimum tests:
- mod starts;
- command exists;
- dump file created;
- rules validate;
- radioactive item in inventory affects player;
- non-radioactive item does not affect player;
- radius works;
- shielding works;
- chest source works;
- multiplayer smoke test;
- dedicated server smoke test.

## Diagnostics
Every complex feature must include debug output or dump data.

Required commands:
- `/radworks version`;
- `/radworks dump`;
- `/radworks sources`;
- `/radworks exposure <player>`;
- `/radworks debug on/off`;
- `/radworks validate`.

Phase 0 only requires `/radworks version` and `/radworks dump`.

## Coding style
- Keep Java code conventional and boring.
- Prefer explicit names over clever abstractions.
- Keep optional integrations isolated from core code.
- Use immutable data objects for radiation source snapshots where practical.
- Prefer data-driven rules over hardcoded IDs.
- Add comments only where they explain non-obvious migration or compatibility decisions.

## Compatibility hooks
Do not delete compatibility hooks simply because an optional mod is absent in the local test environment.

Optional integrations must fail closed:
- core mod loads without the optional mod;
- diagnostics says integration is absent/disabled/unsupported;
- no classloading crash when optional dependency is missing.

## Commit-sized changes
Make changes that could reasonably be reviewed as one commit:
- one phase foundation;
- one provider;
- one diagnostic command;
- one rule loader change;
- one integration spike.

Avoid mixing architecture, gameplay balance and UI in a single hidden change.
