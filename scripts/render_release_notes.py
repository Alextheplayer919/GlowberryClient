#!/usr/bin/env python3
import json
import sys
from pathlib import Path


def render(source: Path, destination: Path) -> None:
    with source.open(encoding="utf-8") as file:
        changelog = json.load(file)

    lines = [f"## {changelog['headline']}", "", changelog["summary"], ""]

    # C2: date the release and publish its support window, so the release page answers "how long is
    # this build supported?" instead of leaving it implied.
    released = str(changelog.get("releasedAt", "")).strip()
    support_until = str(changelog.get("supportUntil", "")).strip()
    if released or support_until:
        if released and support_until:
            lines.extend([f"**Released {released[:10]}** \u00b7 supported until {support_until[:10]}", ""])
        elif released:
            lines.extend([f"**Released {released[:10]}**", ""])
        else:
            lines.extend([f"**Supported until {support_until[:10]}**", ""])

    for section in changelog["sections"]:
        lines.extend([f"### {section['title']}", ""])
        for item in section["items"]:
            label = item["text"]
            url = item.get("url", "")
            lines.append(f"- [{label}]({url})" if url else f"- {label}")
        lines.append("")

    destination.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")


def main(argv) -> int:
    render(Path(argv[1]), Path(argv[2]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
