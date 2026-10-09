# Project TDD context template

All paths must be repository-root-relative or explicitly labeled. Reference authoritative rules;
this context does not grant permissions or accept draft designs.

## Boundary and authority

- Repository owns: <scope>
- Applicable instructions and accepted design authority: <paths and acceptance policy>
- External project boundaries: <reference>

## Verification

| Module/path | Authority | Existing command and working directory |
|---|---|---|
| <module> | <document or script> | <verified command, or missing prerequisite> |

Distinguish behavioral tests, builds, static checks, and integration evidence. Name the independent
verification agents/gates and their triggers. New test infrastructure requires a separate task.

## Constraints and external stages

- Test/fixture conventions, dependencies and public-contract rules: <references>
- Temporary/generated files and environment-key policy: <references>
- External verification triggers, agents, prerequisites and commands: <references>
- Unresolved semantics, unavailable tooling and historical contracts: <limits and owner>
