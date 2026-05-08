# First Task Prompt

Ты работаешь в новом пустом репозитории.

Создай Phase 0 нового RadWorks NeoForge mod.

Используй `migration_export/` как спецификацию, особенно:
- `06_NEOFORGE_ARCHITECTURE.md`;
- `07_MIGRATION_PHASES.md`;
- `08_DIAGNOSTICS_REQUIREMENTS.md`;
- `11_AGENTS_TEMPLATE.md`.

Задачи:
- создать новый NeoForge mod project;
- настроить Gradle и mod metadata;
- добавить `AGENTS.md`;
- добавить `README.md`;
- добавить `MIGRATION_STATUS.md`;
- добавить `TESTING.md`;
- добавить `DIAGNOSTICS.md`;
- добавить команду `/radworks version`;
- добавить команду `/radworks dump`;
- запустить build/check, если возможно.

Важно:
- не реализовывай радиацию;
- не реализовывай radioactive items;
- не реализовывай shielding;
- не реализовывай effects;
- не реализовывай Create integration;
- не реализовывай Aeronautics integration;
- не требуй KubeJS как dependency.

После работы:
- обнови `MIGRATION_STATUS.md`;
- опиши, как запустить build;
- опиши ручной тест для `/radworks version`;
- опиши ручной тест для `/radworks dump`;
- перечисли known limitations и UNKNOWN.
