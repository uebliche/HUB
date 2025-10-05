#!/usr/bin/env python3
"""Resolve supported Paper game versions via the PaperMC downloads API."""
from __future__ import annotations

import json
import re
import sys
import urllib.error
import urllib.request
from typing import List, Optional, Tuple

USER_AGENT = "hub-ci-workflow/1.0 (+https://github.com/net-uebliche/HUB)"
API_BASE = "https://fill.papermc.io/v3/projects/paper"
NUMERIC_VERSION = re.compile(r"^\d+(\.\d+)*$")

def parse_version(value: str) -> List[int]:
    parts: List[int] = []
    for token in value.replace('-', '.').split('.'):
        if token.isdigit():
            parts.append(int(token))
        else:
            break
    return parts

def compare_versions(a: str, b: str) -> int:
    left = parse_version(a)
    right = parse_version(b)
    size = max(len(left), len(right))
    for i in range(size):
        l = left[i] if i < len(left) else 0
        r = right[i] if i < len(right) else 0
        if l != r:
            return -1 if l < r else 1
    return 0

def fetch_json(url: str, *, allow_missing: bool = False) -> Optional[dict]:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        if allow_missing and error.code == 404:
            return None
        raise


def latest_build_info(version: str) -> Tuple[Optional[int], Optional[int]]:
    latest = fetch_json(f"{API_BASE}/versions/{version}/builds/latest", allow_missing=True)
    release_build: Optional[int] = None
    snapshot_build: Optional[int] = None
    if latest:
        channel = str(latest.get("channel", "")).upper()
        build_id = latest.get("id")
        if isinstance(build_id, int):
            if channel in {"DEFAULT", "STABLE", "RELEASE"}:
                release_build = build_id
            else:
                snapshot_build = build_id
    if release_build is None:
        stable = fetch_json(
            f"{API_BASE}/versions/{version}/builds/latest?channel=STABLE",
            allow_missing=True,
        )
        if stable and isinstance(stable.get("id"), int):
            release_build = int(stable["id"])
    return release_build, snapshot_build

def ordered_versions(project_data: dict) -> List[str]:
    ordered: List[str] = []
    versions = project_data.get("versions", {})
    if isinstance(versions, dict):
        for values in versions.values():
            if isinstance(values, list):
                ordered.extend(
                    entry
                    for entry in values
                    if isinstance(entry, str) and NUMERIC_VERSION.fullmatch(entry)
                )
    return ordered


def resolve_versions(min_version: str) -> List[str]:
    project_data = fetch_json(API_BASE)
    if not project_data:
        return [min_version]
    versions = ordered_versions(project_data)
    versions = [v for v in versions if compare_versions(v, min_version) >= 0]
    supported: List[str] = []
    snapshot_candidate: Optional[str] = None
    for version in versions:
        release, snapshot = latest_build_info(version)
        if release is not None:
            supported.append(version)
        if snapshot_candidate is None and snapshot is not None:
            snapshot_candidate = version
    if snapshot_candidate and snapshot_candidate not in supported:
        supported.append(snapshot_candidate)
    return supported or [min_version]

def main() -> int:
    min_version = sys.argv[1].strip() if len(sys.argv) > 1 and sys.argv[1].strip() else "1.16"
    try:
        versions = resolve_versions(min_version)
    except Exception as exc:  # pragma: no cover
        print(f"Warning: {exc}", file=sys.stderr)
        versions = [min_version]
    print(json.dumps(versions))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
