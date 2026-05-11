# Contributing to RadWorks

Thanks for contributing.

## Branching
- Create a feature/fix branch from `master`.
- Keep branch scope small and focused.

## Commit messages
- Prefer concise, imperative messages.
- Suggested style: `type: short summary` (for example `fix: clamp scan interval`).

## Before opening PR
Run:
```bash
./gradlew test
./gradlew build
```

## Code style
- Match existing project style and package structure.
- Keep changes minimal and reviewable.
- Avoid large refactors unless explicitly agreed.

## Files that should not be committed
- Runtime/output folders: `run/`, `build/`, `out/`, `target/`
- IDE/local files: `.idea/`, `.vscode/`, `*.iml`, `.env*`, temp files
- Logs/crash dumps

## Reporting issues and ideas
- Use GitHub issue templates (`bug_report`, `feature_request`).
- Include reproduction steps, expected behavior, actual behavior, and environment versions.
