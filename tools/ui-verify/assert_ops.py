"""Programmatic assertions over nested probe JSON (dot + [index] paths)."""

from __future__ import annotations

from typing import Any, Mapping, Optional, Tuple


class AssertError(Exception):
    def __init__(
        self, path: str, op: str, expected: Any, actual: Any, message: str = ""
    ) -> None:
        self.path = path
        self.op = op
        self.expected = expected
        self.actual = actual
        super().__init__(
            message
            or f"assert failed path={path} op={op} expected={expected!r} actual={actual!r}"
        )


def resolve_path(data: Any, path: str) -> Tuple[bool, Any]:
    """Return (found, value). Empty path returns (True, data)."""
    if path is None or str(path).strip() == "":
        return True, data
    cur: Any = data
    token = ""
    i = 0
    s = str(path)
    while i < len(s):
        c = s[i]
        if c == ".":
            if token:
                ok, cur = _step_key(cur, token)
                if not ok:
                    return False, None
                token = ""
            i += 1
            continue
        if c == "[":
            if token:
                ok, cur = _step_key(cur, token)
                if not ok:
                    return False, None
                token = ""
            j = s.find("]", i)
            if j < 0:
                return False, None
            idx_s = s[i + 1 : j].strip()
            try:
                idx = int(idx_s)
            except ValueError:
                return False, None
            ok, cur = _step_index(cur, idx)
            if not ok:
                return False, None
            i = j + 1
            continue
        token += c
        i += 1
    if token:
        return _step_key(cur, token)
    return True, cur


def _step_key(cur: Any, key: str) -> Tuple[bool, Any]:
    if isinstance(cur, Mapping) and key in cur:
        return True, cur[key]
    return False, None


def _step_index(cur: Any, idx: int) -> Tuple[bool, Any]:
    if isinstance(cur, (list, tuple)) and 0 <= idx < len(cur):
        return True, cur[idx]
    return False, None


def run_assert(
    data: Any,
    spec: Mapping[str, Any],
    *,
    vars: Optional[Mapping[str, Any]] = None,
) -> None:
    """
    Supported keys (exactly one operator besides path / optional message):
      path, eq, neq, exists, gte, lte, contains, truthy, falsey, eq_var
    """
    if not isinstance(spec, Mapping):
        raise AssertError("", "spec", "mapping", type(spec).__name__)
    path = str(spec.get("path", "") or "")
    found, actual = resolve_path(data, path)
    var_map = vars or {}

    if "exists" in spec:
        want = bool(spec["exists"])
        if found != want:
            raise AssertError(path, "exists", want, found)
        return

    if not found:
        raise AssertError(path, "resolve", "present", "missing")

    if "eq" in spec:
        if actual != spec["eq"]:
            raise AssertError(path, "eq", spec["eq"], actual)
        return
    if "neq" in spec:
        if actual == spec["neq"]:
            raise AssertError(path, "neq", f"not {spec['neq']!r}", actual)
        return
    if "eq_var" in spec:
        key = str(spec["eq_var"])
        if key not in var_map:
            raise AssertError(path, "eq_var", f"var {key}", "missing var")
        if actual != var_map[key]:
            raise AssertError(path, "eq_var", var_map[key], actual)
        return
    if "gte" in spec:
        if not (actual >= spec["gte"]):
            raise AssertError(path, "gte", spec["gte"], actual)
        return
    if "lte" in spec:
        if not (actual <= spec["lte"]):
            raise AssertError(path, "lte", spec["lte"], actual)
        return
    if "contains" in spec:
        needle = spec["contains"]
        if isinstance(actual, str):
            if needle not in actual:
                raise AssertError(path, "contains", needle, actual)
            return
        if isinstance(actual, (list, tuple)):
            if needle not in actual:
                raise AssertError(path, "contains", needle, actual)
            return
        raise AssertError(path, "contains", needle, f"uncontainable {type(actual)}")
    if "truthy" in spec:
        want = bool(spec["truthy"])
        if bool(actual) != want:
            raise AssertError(path, "truthy", want, bool(actual))
        return
    if "falsey" in spec:
        want = bool(spec["falsey"])
        if (not bool(actual)) != want:
            raise AssertError(path, "falsey", want, not bool(actual))
        return

    raise AssertError(path, "operator", "one of eq/neq/…", "none")
