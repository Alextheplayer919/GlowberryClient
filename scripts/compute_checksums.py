#!/usr/bin/env python3
"""Compute SHA-256 checksums for build artifacts.

Writes one ``<file>.sha256`` sidecar next to every APK under ``--dir`` so the
released bits can be verified by anyone downloading them (B16: signed release
builds + checksums, CI must not publish digest-less artifacts).

The sidecar format matches GNU coreutils ``sha256sum``::

    <64-hex-digest>  <exact file name>

Matching coreutils makes the files drop-in verifiable with ``sha256sum -c``.
"""

import argparse
import hashlib
import sys
from pathlib import Path


def _digest(path: Path) -> str:
    """Return the lowercase hex SHA-256 of *path* (streamed, no full load)."""
    digest = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _write_sidecar(path: Path) -> Path:
    """Write the sidecar and return its path."""
    sidecar = path.with_name(path.name + ".sha256")
    sidecar.write_text(f"{_digest(path)}  {path.name}\n", encoding="utf-8")
    return sidecar


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--dir",
        default=".",
        help="directory to scan for APK artifacts (recursive)",
    )
    parser.add_argument(
        "--pattern",
        default="*.apk",
        help="glob pattern for artifacts (default: *.apk)",
    )
    parser.add_argument(
        "--verify",
        action="store_true",
        help="verify existing sidecars instead of writing new ones",
    )
    args = parser.parse_args(argv)

    root = Path(args.dir)
    if not root.is_dir():
        print(f"no such directory: {root}", file=sys.stderr)
        return 2

    artifacts = sorted(p for p in root.rglob(args.pattern) if p.is_file())
    if not artifacts:
        print(f"no artifacts matching {args.pattern!r} under {root}", file=sys.stderr)
        return 2 if args.verify else 0

    failures = 0
    total = 0
    for apk in artifacts:
        total += 1
        if args.verify:
            sidecar = apk.with_name(apk.name + ".sha256")
            if not sidecar.exists():
                print(f"{apk}: missing checksum", file=sys.stderr)
                failures += 1
                continue
            expect = (sidecar.read_text(encoding="utf-8").split()[0]).strip().lower()
            actual = _digest(apk)
            if expect == actual:
                print(f"{apk.name}: OK")
            else:
                print(f"{apk.name}: FAILED", file=sys.stderr)
                failures += 1
        else:
            sidecar = _write_sidecar(apk)
            print(f"{sidecar.relative_to(root)}")

    print(f"{total - failures}/{total} checks {'passed' if args.verify else 'written'}")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())