#!/usr/bin/env python3
import argparse
import datetime as dt
import os
import re
import subprocess
import sys


CHANGELOG_HEADER = "# Changelog"
UNRELEASED_HEADER = "## [Unreleased] - Unreleased"
UNRELEASED_RE = re.compile(r"^## \[Unreleased\](?: - .*)?$")
ENTRY_RE = re.compile(r"^## \[(.+?)\] - (\d{4}-\d{2}-\d{2})\s*$")
DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")


def run_git(args):
    try:
        result = subprocess.run(
            ["git"] + args,
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
    except subprocess.CalledProcessError as exc:
        print(exc.stderr.strip(), file=sys.stderr)
        raise
    return result.stdout


def parse_latest_date(changelog_text):
    for line in changelog_text.splitlines():
        if UNRELEASED_RE.match(line.strip()):
            continue
        match = ENTRY_RE.match(line.strip())
        if match:
            return match.group(2)
    return None


def find_unreleased_block(lines):
    for i, line in enumerate(lines):
        if UNRELEASED_RE.match(line.strip()):
            end = len(lines)
            for j in range(i + 1, len(lines)):
                if lines[j].startswith("## "):
                    end = j
                    break
            return i, end
    return None


def insert_unreleased_placeholder(lines):
    if any(UNRELEASED_RE.match(line.strip()) for line in lines):
        return lines
    placeholder = [UNRELEASED_HEADER, "### Changed", ""]
    insert_at = 1
    while insert_at < len(lines) and lines[insert_at].strip() == "":
        insert_at += 1
    return lines[:insert_at] + placeholder + lines[insert_at:]


def resolve_release_date(value):
    if value:
        if not DATE_RE.match(value):
            raise ValueError("release_date must be in YYYY-MM-DD format")
        return value
    return dt.datetime.now(dt.timezone.utc).date().isoformat()


def compute_since_date(latest_date):
    if not latest_date:
        return None
    base = dt.date.fromisoformat(latest_date)
    return (base + dt.timedelta(days=1)).isoformat()


def collect_commits(since_ref, until_ref, since_date):
    args = ["log", "--no-merges", "--pretty=%s"]
    if since_ref:
        ref_range = f"{since_ref}..{until_ref}"
        args.append(ref_range)
    elif since_date:
        args.append(f"--since={since_date}")
        args.append(until_ref)
    else:
        args.append(until_ref)
    output = run_git(args)
    return [line.strip() for line in output.splitlines() if line.strip()]


def apply_excludes(subjects, exclude_prefixes):
    if not exclude_prefixes:
        return subjects
    prefixes = [
        prefix.strip().lower()
        for prefix in exclude_prefixes.split(",")
        if prefix.strip()
    ]
    if not prefixes:
        return subjects
    filtered = []
    for subject in subjects:
        lowered = subject.lower()
        if any(
            lowered.startswith(prefix + ":")
            or lowered.startswith(prefix + "(")
            or lowered == prefix
            for prefix in prefixes
        ):
            continue
        filtered.append(subject)
    return filtered


def build_entry(release_date, subjects):
    lines = [f"- {subject}" for subject in subjects] or ["- (no changes)"]
    body = "\n".join(lines)
    return f"## [{release_date}] - {release_date}\n### Changed\n{body}\n"


def insert_entry(changelog_text, entry_text):
    if not changelog_text.strip():
        return f"{CHANGELOG_HEADER}\n\n{entry_text}\n"

    if not changelog_text.startswith(CHANGELOG_HEADER):
        changelog_text = f"{CHANGELOG_HEADER}\n\n" + changelog_text.lstrip()

    lines = changelog_text.splitlines()
    header = lines[0]
    rest = "\n".join(lines[1:]).lstrip("\n")
    if rest:
        return f"{header}\n\n{entry_text}\n{rest}\n"
    return f"{header}\n\n{entry_text}\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--release-date", dest="release_date", default="")
    parser.add_argument("--since-ref", dest="since_ref", default="")
    parser.add_argument("--until-ref", dest="until_ref", default="HEAD")
    parser.add_argument("--exclude-prefixes", dest="exclude_prefixes", default="")
    parser.add_argument("--changelog", dest="changelog", default="CHANGELOG.md")
    args = parser.parse_args()

    release_date = resolve_release_date(args.release_date)

    changelog_path = args.changelog
    changelog_text = ""
    if os.path.exists(changelog_path):
        with open(changelog_path, "r", encoding="utf-8") as handle:
            changelog_text = handle.read()

    latest_date = parse_latest_date(changelog_text)
    lines = changelog_text.splitlines() if changelog_text else []
    if not lines:
        lines = [CHANGELOG_HEADER, ""]

    unreleased_block = find_unreleased_block(lines)
    if unreleased_block:
        release_date = resolve_release_date(args.release_date)
        if latest_date == release_date:
            print(
                f"CHANGELOG already contains an entry for {release_date}.",
                file=sys.stderr,
            )
            sys.exit(1)

        start, end = unreleased_block
        lines[start] = f"## [{release_date}] - {release_date}"
        lines = insert_unreleased_placeholder(lines)
        with open(changelog_path, "w", encoding="utf-8") as handle:
            handle.write("\n".join(lines).rstrip("\n") + "\n")
        print(f"Finalized Unreleased as {release_date}.")
        return

    if latest_date == release_date:
        print(
            f"CHANGELOG already contains an entry for {release_date}.",
            file=sys.stderr,
        )
        sys.exit(1)

    since_date = compute_since_date(latest_date)
    subjects = collect_commits(args.since_ref, args.until_ref, since_date)
    subjects = apply_excludes(subjects, args.exclude_prefixes)

    entry_text = build_entry(release_date, subjects)
    updated = insert_entry(changelog_text, entry_text)

    with open(changelog_path, "w", encoding="utf-8") as handle:
        handle.write(updated)

    print(f"Updated {changelog_path} for release {release_date}.")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)
