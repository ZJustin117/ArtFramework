"""Parse ART_PROBE log lines into JSON objects."""

from __future__ import annotations

import json
import re
from typing import Any, Optional

PROBE_PREFIX = "ART_PROBE"
_LINE_RE = re.compile(r"ART_PROBE\s+(\{.*\})\s*$")


def parse_probe_line(line: str) -> Optional[Any]:
    if not line:
        return None
    s = line.strip()
    if not s.startswith(PROBE_PREFIX):
        # allow log prefix before marker
        idx = s.find(PROBE_PREFIX)
        if idx < 0:
            return None
        s = s[idx:]
    m = _LINE_RE.search(s)
    if not m:
        # fallback: after first space
        parts = s.split(None, 1)
        if len(parts) < 2:
            return None
        payload = parts[1].strip()
    else:
        payload = m.group(1)
    try:
        return json.loads(payload)
    except json.JSONDecodeError:
        return None


def last_probe_from_text(text: str) -> Optional[Any]:
    last = None
    for line in text.splitlines():
        p = parse_probe_line(line)
        if p is not None:
            last = p
    return last
