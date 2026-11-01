#!/usr/bin/env python3
"""
scripts/generate-changelog.py
Automated changelog and release notes generation for Android Calculator.
Generates:
1. RELEASE_NOTES.md (Full markdown changelog for GitHub Releases)
2. distribution/whatsnew/whatsnew-en-US (Strictly <= 500 characters for Google Play Console)
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
    tags_output = run_git(["tag", "--list", "v*", "--sort=-v:refname"])
    for tag in tags_output.splitlines():
        tag = tag.strip()
        if re.match(r"^v\d+\.\d+\.\d+$", tag):
            return tag
    return None

def clean_subject(subject):
    # Remove conventional commit prefix e.g. "feat(ui): " -> ""
    cleaned = re.sub(r"^[a-zA-Z0-9_-]+(\([^)]+\))?!:\s*", "", subject)
    cleaned = re.sub(r"^[a-zA-Z0-9_-]+(\([^)]+\))?:\s*", "", cleaned)
    # Capitalize first letter
    if cleaned:
        cleaned = cleaned[0].upper() + cleaned[1:]
    return cleaned.strip()

def main():
    latest_tag = get_latest_tag()
    git_range = f"{latest_tag}..HEAD" if latest_tag else "HEAD"

    log_raw = run_git(["log", git_range, "--pretty=format:%h%x09%s"])
    lines = [line.strip() for line in log_raw.splitlines() if line.strip()]

    categories = {
        "features": [],
        "fixes": [],
        "refactor": [],
        "docs": [],
        "other": []
    }

    whatsnew_bullets = []

    for line in lines:
        parts = line.split("\t", 1)
        if len(parts) == 2:
            commit_hash, subject = parts
        else:
            commit_hash, subject = "", parts[0]

        subject_lower = subject.lower()
        cleaned = clean_subject(subject)

        if subject_lower.startswith("feat"):
            categories["features"].append(f"- {cleaned} (`{commit_hash}`)")
            whatsnew_bullets.append(f"• {cleaned}")
        elif subject_lower.startswith("fix"):
            categories["fixes"].append(f"- {cleaned} (`{commit_hash}`)")
            whatsnew_bullets.append(f"• {cleaned}")
        elif subject_lower.startswith("refactor") or subject_lower.startswith("perf"):
            categories["refactor"].append(f"- {cleaned} (`{commit_hash}`)")
        elif subject_lower.startswith("docs"):
            categories["docs"].append(f"- {cleaned} (`{commit_hash}`)")
        else:
            categories["other"].append(f"- {cleaned} (`{commit_hash}`)")

    version_name = os.environ.get("VERSION_NAME", "19.1.0")

    # 1. Build Markdown Release Notes
    md_lines = [
        f"# Release v{version_name}",
        "",
        "Pixelabs Android Calculator Release with upgraded enterprise CI/CD pipeline, offline privacy enhancements, and UI polish.",
        ""
    ]

    if categories["features"]:
        md_lines.append("### 🚀 New Features")
        md_lines.extend(categories["features"])
        md_lines.append("")

    if categories["fixes"]:
        md_lines.append("### 🐛 Bug Fixes & Improvements")
        md_lines.extend(categories["fixes"])
        md_lines.append("")

    if categories["refactor"]:
        md_lines.append("### ⚡ Performance & Refactoring")
        md_lines.extend(categories["refactor"])
        md_lines.append("")

    if categories["docs"]:
        md_lines.append("### 📝 Documentation & Legal")
        md_lines.extend(categories["docs"])
        md_lines.append("")

    if categories["other"]:
        md_lines.append("### 🔧 Maintenance & Chores")
        md_lines.extend(categories["other"])
        md_lines.append("")

    release_notes_content = "\n".join(md_lines)
    with open("RELEASE_NOTES.md", "w", encoding="utf-8") as f:
        f.write(release_notes_content)

    print("Generated RELEASE_NOTES.md successfully.")

    # 2. Build Play Store localized whatsnew-en-US (Strictly <= 500 characters)
    whatsnew_dir = os.path.join("distribution", "whatsnew")
    os.makedirs(whatsnew_dir, exist_ok=True)
    whatsnew_path = os.path.join(whatsnew_dir, "whatsnew-en-US")

    # Default concise bullets if not enough conventional commit bullets
    if not whatsnew_bullets:
        whatsnew_bullets = [
            "• Direct feedback submission with privacy opt-in",
            "• Enhanced expression display & auto-scaling",
            "• Smooth two-way gesture navigation for history",
            "• Performance optimizations and bug fixes"
        ]

    header = f"What's New in v{version_name}:\n"
    current_content = header
    
    for bullet in whatsnew_bullets:
        candidate = current_content + bullet + "\n"
        if len(candidate) <= 490: # Buffer to guarantee <= 500
            current_content = candidate
        else:
            break

    # If still empty or just header, fall back to safe curated text
    if current_content.strip() == header.strip():
        current_content = (
            f"What's New in v{version_name}:\n"
            "• Direct feedback submission with explicit privacy opt-in\n"
            "• Auto-scaling display for large scientific numbers\n"
            "• Two-way swipe gestures for calculation history\n"
            "• 100% offline utility with zero background tracking"
        )

    # Hard truncate if somehow > 500
    if len(current_content) > 500:
        current_content = current_content[:497] + "..."

    with open(whatsnew_path, "w", encoding="utf-8") as f:
        f.write(current_content.strip())

    print(f"Generated {whatsnew_path} ({len(current_content.strip())} chars):")
    print("--------------------------------------------------")
    print(current_content.strip())
    print("--------------------------------------------------")

if __name__ == "__main__":
    main()
