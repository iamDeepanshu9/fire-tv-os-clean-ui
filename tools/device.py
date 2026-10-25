"""Select an explicitly configured ADB target; never guess a device."""
import os
import re

def target():
    serial = os.environ.get("TV_SERIAL", "")
    if not serial:
        host = os.environ.get("TV_HOST", "")
        if host:
            serial = host + ":" + os.environ.get("TV_PORT", "5555")
    if not serial or not re.fullmatch(r"[A-Za-z0-9_.:\[\]-]+", serial):
        raise SystemExit("Set TV_SERIAL or TV_HOST to your authorized ADB device first.")
    return serial
