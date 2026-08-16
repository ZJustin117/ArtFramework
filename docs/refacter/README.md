# Refacter projects

Persistent supervision records for long-running refactors live here:

```text
docs/refacter/<project-name>/
```

Each project contains `README.md`, `ledger.md`, and `reviews.md`. Continue a project by giving the
agent its directory, for example:

```text
@docs/refacter/traditional-ecs/
```

The directory is the durable continuation context. The agent reads all Markdown files directly in
it before changing code, reconciles the records with the worktree, and resumes the first active
ledger row unless another row is requested.

Project state belongs here, while product roadmap status remains in [`docs/task.md`](../task.md).
The OpenCode workflow contract is defined by the project skill at
`.opencode/skills/refacter/SKILL.md`.
