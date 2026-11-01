#!/usr/bin/env python3
"""
scripts/compute-version.py
Enterprise-grade dynamic versioning for Android Calculator.
Computes SemVer (MAJOR.MINOR.PATCH) and monotonic versionCode from Git history.
"""

import os
import re
import subprocess
import sys

def run_git(cmd):
    try:
        return subprocess.check_output(["git"] + cmd, stderr=subprocess.DEVNULL).decode("utf-8").strip()
    except subprocess.CalledProcessError:
        return ""

def get_latest_tag():
    # Find latest tag reachable from HEAD matching vX.Y.Z
    tags_output = run_git(["tag", "--list", "v*", "--sort=-v:refname"])
    for tag in tags_output.splitlines():
        tag = tag.strip()
        if re.match(r"^v\d+\.\d+\.\d+$", tag):
            return tag
    return None

def compute_semver(base_tag):
    if base_tag:
        match = re.match(r"^v(\d+)\.(\d+)\.(\d+)$", base_tag)
        if match:
            major, minor, patch = map(int, match.groups())
        else:
            major, minor, patch = 19, 0, 0
        git_range = f"{base_tag}..HEAD"
    else:
        # Initial base version if no tags exist
        major, minor, patch = 19, 0, 0
        git_range = "HEAD"

    log_raw = run_git(["log", git_range, "--pretty=format:%s%n%b%n---COMMIT_END---"])
    commits = log_raw.split("---COMMIT_END---") if log_raw else []

    bump_type = "patch" # Default bump if commits exist
    has_commits = False

    for commit in commits:
        commit = commit.strip()
        if not commit:
            continue
        has_commits = True

        # Check for breaking changes
        if "BREAKING CHANGE" in commit or "BREAKING-CHANGE" in commit:
            bump_type = "major"
            break

        lines = commit.splitlines()
        first_line = lines[0] if lines else ""

        # Check conventional commit breaking indicator e.g. "feat!:" or "fix!:"
        if re.match(r"^[a-zA-Z0-9_-]+(\([^)]+\))?!:", first_line):
            bump_type = "major"
            break

        # Check for features
        if re.match(r"^feat(\([^)]+\))?:", first_line, re.IGNORECASE):
            bump_type = "minor"

    if not has_commits and base_tag:
        # No new commits since tag
        return f"{major}.{minor}.{patch}"

    if bump_type == "major":
        return f"{major + 1}.0.0"
    elif bump_type == "minor":
        return f"{major}.{minor + 1}.0"
    else:
        return f"{major}.{minor}.{patch + 1}"

def compute_version_code():
    count_str = run_git(["rev-list", "--count", "HEAD"])
    try:
        commit_count = int(count_str)
    except ValueError:
        commit_count = 1
    
    # Ensure versionCode exceeds previous Play Store versions (max seen was 19)
    # Using commit_count + 10 ensures strict monotonicity while staying > 19
    return max(20, commit_count + 10)

def main():
    # Check overrides from environment or workflow dispatch inputs
    override_name = os.environ.get("INPUT_VERSION_NAME") or os.environ.get("VERSION_NAME")
    override_code = os.environ.get("INPUT_VERSION_CODE") or os.environ.get("VERSION_CODE")

    latest_tag = get_latest_tag()
    
    if override_name and override_name.strip():
        version_name = override_name.strip().lstrip("v")
    else:
        version_name = compute_semver(latest_tag)

    if override_code and override_code.strip():
        try:
            version_code = int(override_code.strip())
        except ValueError:
            version_code = compute_version_code()
    else:
        version_code = compute_version_code()

    tag_name = f"v{version_name}"

    print(f"=== Dynamic Version Computation ===")
    print(f"Latest Git Tag: {latest_tag or 'None (Starting baseline v19.0.0)'}")
    print(f"Computed Version Name: {version_name}")
    print(f"Computed Tag Name:     {tag_name}")
    print(f"Computed Version Code: {version_code}")
    print(f"====================================")

    # Export to GitHub Actions if running inside GitHub Actions
    github_output = os.environ.get("GITHUB_OUTPUT")
    if github_output:
        with open(github_output, "a", encoding="utf-8") as f:
            f.write(f"version_name={version_name}\n")
            f.write(f"version_code={version_code}\n")
            f.write(f"tag_name={tag_name}\n")

    github_env = os.environ.get("GITHUB_ENV")
    if github_env:
        with open(github_env, "a", encoding="utf-8") as f:
            f.write(f"VERSION_NAME={version_name}\n")
            f.write(f"VERSION_CODE={version_code}\n")
            f.write(f"TAG_NAME={tag_name}\n")

if __name__ == "__main__":
    main()
