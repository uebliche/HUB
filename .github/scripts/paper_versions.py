#!/usr/bin/env python3
"""Resolve supported Paper game versions via the PaperMC downloads API."""
from __future__ import annotations

import json
import sys
import urllib.request
from typing import List, Tuple

USER_AGENT = "hub-ci-workflow"
API_BASE = "https://api.papermc.io/v2/projects/paper"

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

def fetch_json(url: str) -> dict:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=20) as response:
        return json.load(response)

def latest_build_info(version: str) -> Tuple[int | None, int | None]:
    version_data = fetch_json(f"{API_BASE}/versions/{version}")
    builds = version_data.get("builds", [])
    normalized: List[int] = []
    for entry in builds:
        try:
            normalized.append(int(entry))
        except Exception:
            continue
    normalized.sort()
    release_build: int | None = None
    snapshot_build: int | None = None
    for build in reversed(normalized):
        build_data = fetch_json(f"{API_BASE}/versions/{version}/builds/{build}")
        channel = str(build_data.get("channel", "default"))
        if channel == "default" and release_build is None:
            release_build = build
        elif channel != "default" and snapshot_build is None:
            snapshot_build = build
        if release_build is not None and snapshot_build is not None:
            break
    return release_build, snapshot_build

def resolve_versions(min_version: str) -> List[str]:
    project_data = fetch_json(API_BASE)
    versions = [v for v in project_data.get("versions", []) if isinstance(v, str)]
    versions.sort(key=parse_version)
    versions = [v for v in versions if compare_versions(v, min_version) >= 0]
    supported: List[str] = []
    snapshot_candidate: String | None = None
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
