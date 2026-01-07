#!/usr/bin/env python3
import argparse
import os
import re
import sys


CHANGELOG_HEADER = "# Changelog"
UNRELEASED_HEADER = "## [Unreleased] - Unreleased"
UNRELEASED_RE = re.compile(r"^## \[Unreleased\](?: - .*)?$")
ENTRY_RE = re.compile(r"^## \[(.+?)\] - (\d{4}-\d{2}-\d{2})\s*$")
SECTION_RE = re.compile(r"^###\s+Changed\s*$")


def load_lines(path):
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as handle:
        return handle.read().splitlines()


def save_lines(path, lines):
    text = "\n".join(lines).rstrip("\n") + "\n"
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(text)


def ensure_header(lines):
    if not lines:
        return [CHANGELOG_HEADER, ""]
    if lines[0].strip() != CHANGELOG_HEADER:
        return [CHANGELOG_HEADER, ""] + lines
    return lines


def find_entry_indexes(lines):
    indexes = []
    for i, line in enumerate(lines):
        stripped = line.strip()
        if UNRELEASED_RE.match(stripped):
            indexes.append((i, "Unreleased"))
            continue
        match = ENTRY_RE.match(stripped)
        if match:
            indexes.append((i, match.group(2)))
    return indexes


def insert_new_entry(lines, subject):
    entry_lines = [
        UNRELEASED_HEADER,
        "### Changed",
        f"- {subject}",
        "",
    ]
    insert_at = 1
    while insert_at < len(lines) and lines[insert_at].strip() == "":
        insert_at += 1
    return lines[:insert_at] + entry_lines + lines[insert_at:]


def update_existing_entry(lines, entry_start, entry_end, subject):
    changed_idx = None
    for i in range(entry_start + 1, entry_end):
        if SECTION_RE.match(lines[i].strip()):
            changed_idx = i
            break

    if changed_idx is None:
        insert_at = entry_start + 1
        insert_block = ["### Changed", f"- {subject}"]
        return lines[:insert_at] + insert_block + lines[insert_at:]

    section_end = entry_end
    for i in range(changed_idx + 1, entry_end):
        if lines[i].startswith("## "):
            section_end = i
            break
        if lines[i].startswith("### "):
            section_end = i
            break

    last_bullet = None
    for i in range(changed_idx + 1, section_end):
        if lines[i].strip() == f"- {subject}":
            return lines
        if lines[i].lstrip().startswith("- "):
            last_bullet = i

    insert_at = changed_idx + 1 if last_bullet is None else last_bullet + 1
    return lines[:insert_at] + [f"- {subject}"] + lines[insert_at:]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--subject", required=True)
    parser.add_argument("--changelog", dest="changelog", default="CHANGELOG.md")
    args = parser.parse_args()

    subject = args.subject.strip()
    if not subject:
        print("Empty subject. Skipping.", file=sys.stderr)
        return 0

    lines = ensure_header(load_lines(args.changelog))
    entry_indexes = find_entry_indexes(lines)

    unreleased_idx = None
    for idx, label in entry_indexes:
        if label == "Unreleased":
            unreleased_idx = idx
            break

    if unreleased_idx is not None:
        entry_start = unreleased_idx
        entry_end = len(lines)
        for idx, _label in entry_indexes:
            if idx > entry_start:
                entry_end = idx
                break
        lines = update_existing_entry(lines, entry_start, entry_end, subject)
    else:
        lines = insert_new_entry(lines, subject)

    save_lines(args.changelog, lines)
    print(f"Updated {args.changelog} (Unreleased).")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        raise SystemExit(1)
