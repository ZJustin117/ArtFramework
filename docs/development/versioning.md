# Versioning

Single source of truth: `gradle.properties` → `artframework.version`.

Build filters `ModTheSpire.json` (`@ART_VERSION@`) and jar
`Implementation-Version` from the same property.

## Scheme

| Tag | Meaning |
|-----|---------|
| `1.0.0-alpha.N` | Public API still tightening; consumers pin exact alpha |
| `1.0.0-rc.N` | Freeze candidate; only bugfixes + docs |
| `1.0.0` | First stable consumer contract |

## Bump rules

- **Patch / alpha increment**: bugfix, additive probe fields, new optional surfaces
- **Minor** (post-1.0): additive stable API
- **Major**: breaking facade, probe schema migration, removed aliases

## Release gate

```bash
./scripts/release-gate.sh
```

See [`api-stability.md`](./api-stability.md) and [`CHANGELOG.md`](../../CHANGELOG.md).
