#!/usr/bin/env python3
"""Host tests for ``tools/gen-rules`` — the pure merge and guard logic.

The download and extraction steps need a real multi-hundred-megabyte APK, so they are exercised
by hand; what is pinned here is the arithmetic a wrong edit would silently break: the version
ordering, the idempotent replace, the revision bump and the "refuse an empty entry" guard.

Run:  python3 tools/tests/gen_rules_test.py
Exit code 0 on success, 1 on any failure.
"""

import importlib.machinery
import importlib.util
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
TOOL = os.path.join(os.path.dirname(HERE), "gen-rules")

FAILURES = []


def check(name, cond):
    if cond:
        print("  ok   %s" % name)
    else:
        print("  FAIL %s" % name)
        FAILURES.append(name)


def load_tool():
    loader = importlib.machinery.SourceFileLoader("gen_rules", TOOL)
    spec = importlib.util.spec_from_loader("gen_rules", loader)
    module = importlib.util.module_from_spec(spec)
    loader.exec_module(module)
    return module


def main():
    m = load_tool()

    print("== version_key ==")
    # Component-wise, not string, ordering: "1.26.10" must not sort before "1.26.9".
    check("four components", m.version_key("1.26.60.30") == (1, 26, 60, 30))
    check("three components pad with zero", m.version_key("1.26.51") == (1, 26, 51, 0))
    check("numeric order beats string order",
          m.version_key("1.26.9") < m.version_key("1.26.10"))
    check("string order would disagree", "1.26.10" < "1.26.9")

    print("== merge_rule ==")
    doc = {"schemaVersion": 1, "revision": 7,
           "rules": [{"min": "1.26.50.00", "max": "1.26.50.19"},
                     {"min": "1.26.40.20", "max": "1.26.40.20"}]}
    entry = {"min": "1.26.60.30", "max": "1.26.60.30", "playerRenderVtableIndex": 17}
    out = m.merge_rule(doc, entry)
    check("revision bumped", out["revision"] == 8)
    check("rule appended", len(out["rules"]) == 3)
    check("kept sorted by version",
          [r["min"] for r in out["rules"]] == ["1.26.40.20", "1.26.50.00", "1.26.60.30"])
    check("input document not mutated", len(doc["rules"]) == 2 and doc["revision"] == 7)

    # Re-generating the same build replaces rather than duplicates.
    out2 = m.merge_rule(out, dict(entry, playerRenderVtableIndex=18))
    check("same min/max replaces", len(out2["rules"]) == 3)
    check("replacement wins",
          [r for r in out2["rules"] if r["min"] == "1.26.60.30"][0]["playerRenderVtableIndex"] == 18)
    check("second bump", out2["revision"] == 9)

    # A missing revision reads as 0 and becomes 1 rather than throwing.
    out3 = m.merge_rule({"rules": []}, entry)
    check("missing revision becomes 1", out3["revision"] == 1)

    print("== has_derived_content ==")
    check("render index counts", m.has_derived_content({"playerRenderVtableIndex": 17}))
    check("a signature counts",
          m.has_derived_content({"sigs": {"pauseMenuOpenSig": "AB CD"}}))
    check("checksum alone does not", not m.has_derived_content({"checksum_sha256": "deadbeef"}))
    check("empty sigs do not", not m.has_derived_content({"sigs": {"x": ""}}))
    check("null entry does not", not m.has_derived_content(None))
    check("empty dict does not", not m.has_derived_content({}))

    print()
    if FAILURES:
        print("%d check(s) failed" % len(FAILURES))
        return 1
    print("all gen-rules checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
