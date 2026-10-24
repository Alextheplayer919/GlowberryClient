#!/usr/bin/env python3
"""Tests for scripts/compute_checksums.py (B16 checksum sidecars)."""

import hashlib
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import compute_checksums


def run_tests() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        a = root / "app-debug.apk"
        b = root / "app-release.apk"
        a.write_bytes(b"apk-one")
        b.write_bytes(b"apk-two")
        (root / "ignore.txt").write_bytes(b"not an apk")

        # sidecars are written with the coreutils sha256sum format
        assert compute_checksums.main(["--dir", str(root)]) == 0
        sidecar = root / "app-debug.apk.sha256"
        assert sidecar.exists(), "debug sidecar missing"
        expected = f"{hashlib.sha256(b'apk-one').hexdigest()}  app-debug.apk\n"
        assert sidecar.read_text(encoding="utf-8") == expected, "sidecar format mismatch"
        assert (root / "app-release.apk.sha256").exists()
        assert not (root / "ignore.txt.sha256").exists()

        # verify passes when digests agree
        assert compute_checksums.main(["--dir", str(root), "--verify"]) == 0

        # corruption is detected
        a.write_bytes(b"apk-one-tampered")
        assert compute_checksums.main(["--dir", str(root), "--verify"]) == 1

        # a missing sidecar fails verification too
        (root / "app-release.apk.sha256").unlink()
        assert compute_checksums.main(["--dir", str(root), "--verify"]) == 1

    print("compute_checksums_test: OK")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(run_tests())
    except AssertionError as error:
        print(f"compute_checksums_test: FAILED: {error}", file=sys.stderr)
        sys.exit(1)