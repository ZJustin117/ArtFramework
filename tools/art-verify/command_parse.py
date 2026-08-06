"""Parse structured ART_COMMAND log lines."""

from __future__ import annotations

import json
from typing import Any, Dict, Optional

COMMAND_PREFIX = "ART_COMMAND"


def parse_command_line(line: str) -> Optional[Dict[str, Any]]:
    if not line:
        return None
    idx = line.find(COMMAND_PREFIX)
    if idx < 0:
        return None
    payload = line[idx + len(COMMAND_PREFIX):].strip()
    if not payload:
        return None
    try:
        value = json.loads(payload)
    except json.JSONDecodeError:
        return None
    return value if isinstance(value, dict) else None


def last_command_for_text(text: str, command: str) -> Optional[Dict[str, Any]]:
    last = None
    for line in text.splitlines():
        value = parse_command_line(line)
        if value is not None and value.get("command") == command:
            last = value
    return last
