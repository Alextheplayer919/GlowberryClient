#!/usr/bin/env python3
"""Tests for the release-dating half of scripts/validate_release_content.py and
scripts/render_release_notes.py (item C2: date + support window on every release)."""

import json
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import render_release_notes
import validate_release_content


def write(root: Path, released, support_until) -> Path:
    doc = {
        "schemaVersion": 1,
        "releasedAt": released,
        "supportUntil": support_until,
        "headline": "Headline",
        "summary": "Summary",
        "sections": [{"title": "Section", "items": [{"text": "Item", "url": ""}]}],
    }
    path = root / "changelog.json"
    path.write_text(json.dumps(doc), encoding="utf-8")
    return path


def run_tests() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)

        # A dated release validates and renders both halves.
        good = write(root, "2026-10-23T00:00:00Z", "2027-04-23T00:00:00Z")
        validate_release_content.validate_changelog(good)
        notes = root / "notes.md"
        render_release_notes.render(good, notes)
        body = notes.read_text(encoding="utf-8")
        assert "**Released 2026-10-23**" in body, body
        assert "supported until 2027-04-23" in body, body

        # A missing date is refused rather than silently accepted.
        for missing in ({"releasedAt": None}, {"supportUntil": None}, {"releasedAt": None, "supportUntil": None}):
            doc = json.loads(good.read_text(encoding="utf-8"))
            doc.update(missing)
            bad = root / "bad.json"
            bad.write_text(json.dumps(doc), encoding="utf-8")
            try:
                validate_release_content.validate_changelog(bad)
            except ValueError:
                pass
            else:
                raise AssertionError(f"a release missing {missing} must be refused")

        # A non-UTC / malformed timestamp is refused.
        for bad_value in ("2026-10-23", "2026-10-23T00:00:00", "not a date"):
            bad = write(root, bad_value, "2027-04-23T00:00:00Z")
            try:
                validate_release_content.validate_changelog(bad)
            except ValueError:
                pass
            else:
                raise AssertionError(f"timestamp {bad_value!r} must be refused")

        # A support window that ends before it starts is refused.
        bad = write(root, "2027-04-23T00:00:00Z", "2026-10-23T00:00:00Z")
        try:
            validate_release_content.validate_changelog(bad)
        except ValueError:
            pass
        else:
            raise AssertionError("supportUntil before releasedAt must be refused")

        # The renderer omits the line entirely when neither date is present.
        undated = {
            "headline": "H",
            "summary": "S",
            "sections": [{"title": "T", "items": [{"text": "I", "url": ""}]}],
        }
        src = root / "undated.json"
        src.write_text(json.dumps(undated), encoding="utf-8")
        render_release_notes.render(src, notes)
        assert "Released" not in notes.read_text(encoding="utf-8")

    print("release_notes_test: OK")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(run_tests())
    except AssertionError as error:
        print(f"FAIL: {error}", file=sys.stderr)
        sys.exit(1)
