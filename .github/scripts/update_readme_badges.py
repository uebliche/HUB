#!/usr/bin/env python3
"""Update README tested-version badges based on the provided matrix."""
from __future__ import annotations

import json
import os
from pathlib import Path

START_MARKER = "<!-- tested_versions:start -->"
END_MARKER = "<!-- tested_versions:end -->"
DEFAULT_README = Path("README.md")


def load_versions() -> list[str]:
    matrix = os.environ.get("TEST_MATRIX", "").strip()
    if not matrix:
        return []
    try:
        versions = json.loads(matrix)
    except json.JSONDecodeError:
        raise SystemExit(f"Invalid TEST_MATRIX JSON: {matrix!r}")
    return [str(version).strip() for version in versions if str(version).strip()]


def update_readme(readme: Path, versions: list[str]) -> None:
    text = readme.read_text(encoding="utf-8")
    if START_MARKER not in text or END_MARKER not in text:
        print("README markers not found; skipping update.")
        return

    opening, rest = text.split(START_MARKER, 1)
    middle, closing = rest.split(END_MARKER, 1)
    lines = [f"- `{version}`" for version in versions]
    replacement = f"\n\n{START_MARKER}\n" + "\n".join(lines) + f"\n{END_MARKER}\n"
    new_text = opening + replacement + closing
    if new_text != text:
        readme.write_text(new_text, encoding="utf-8")
        print("README updated with tested versions:", ", ".join(versions))
    else:
        print("README already up to date; no changes made.")


def main() -> int:
    versions = load_versions()
    if not versions:
        print("No versions supplied; skipping README update.")
        return 0
    update_readme(DEFAULT_README, versions)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
