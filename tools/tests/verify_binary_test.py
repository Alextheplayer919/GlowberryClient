#!/usr/bin/env python3
"""Host tests for ``tools/verify-binary`` using small synthetic fixtures.

The real ``libminecraftpe.so`` is hundreds of megabytes and must never be committed, so the
analyzer's rules are pinned here against a tiny, hand-built arm64 ELF: a `.rodata` holding a
typeinfo-name string and the render diagnostic, a `.data.rel.ro` holding the typeinfo and a
two-slot vtable, and a `.text` whose function loads the diagnostic with an ``ADRP``+``ADD`` pair.

Run:  python3 tools/tests/verify_binary_test.py
Exit code 0 on success, 1 on any failure.
"""

import importlib.machinery
import importlib.util
import os
import struct
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
TOOL = os.path.join(os.path.dirname(HERE), "verify-binary")


def load_tool():
    loader = importlib.machinery.SourceFileLoader("verify_binary", TOOL)
    spec = importlib.util.spec_from_loader("verify_binary", loader)
    module = importlib.util.module_from_spec(spec)
    loader.exec_module(module)
    return module


# --------------------------------------------------------------------------------------------
# Synthetic ELF builder
# --------------------------------------------------------------------------------------------

SHT_PROGBITS = 1
SHT_RELA = 4
SHT_STRTAB = 3
SHF_ALLOC = 0x2
SHF_EXECINSTR = 0x4
SHF_WRITE = 0x1
R_AARCH64_RELATIVE = 1027

TEXT_OFF, TEXT_SIZE = 0x40, 0x100
RODATA_OFF, RODATA_SIZE = 0x140, 0x100
DATAREL_OFF, DATAREL_SIZE = 0x240, 0x100
RELA_OFF = 0x340

# Addresses inside the sections (file offset == VA, the tool's assumption).
NAME_VA = RODATA_OFF + 0x10            # "18LivePlayerRenderer"
DIAG_VA = RODATA_OFF + 0x40            # "variable.player_x_rotation"
TYPEINFO_VA = DATAREL_OFF              # typeinfo struct
SLOT0_FN_VA = TEXT_OFF                 # the function that references the diagnostic


def _adrp(rd, imm):
    imm &= (1 << 21) - 1
    immlo = imm & 0x3
    immhi = (imm >> 2) & 0x7FFFF
    return 0x90000000 | (immlo << 29) | (immhi << 5) | rd


def _add_imm(rd, rn, imm12):
    return 0x91000000 | ((imm12 & 0xFFF) << 10) | (rn << 5) | rd


def build_synthetic_elf(path):
    """Write a minimal arm64 ELF with the sections the analyzer reads."""
    text = bytearray(TEXT_SIZE)
    page = DIAG_VA & ~0xFFF
    lo = DIAG_VA & 0xFFF
    struct.pack_into("<I", text, 0, _adrp(8, (page - (TEXT_OFF & ~0xFFF)) >> 12))
    struct.pack_into("<I", text, 4, _add_imm(8, 8, lo))

    rodata = bytearray(RODATA_SIZE)
    name = b"18LivePlayerRenderer\0"
    rodata[0x10:0x10 + len(name)] = name
    diag = b"variable.player_x_rotation\0"
    rodata[0x40:0x40 + len(diag)] = diag

    datarel = bytearray(DATAREL_SIZE)

    relocs = [
        (TYPEINFO_VA + 8, NAME_VA),          # typeinfo.__name
        (TYPEINFO_VA + 0x28, TYPEINFO_VA),   # vtable's typeinfo pointer (address point - 16)
        (TYPEINFO_VA + 0x30, SLOT0_FN_VA),   # slot 0: references the diagnostic
        (TYPEINFO_VA + 0x38, SLOT0_FN_VA + 0x80),  # slot 1: does not
    ]

    shstrtab = b"\0.text\0.rodata\0.data.rel.ro\0.rela.dyn\0.shstrtab\0"
    o_text = shstrtab.index(b".text")
    o_rodata = shstrtab.index(b".rodata")
    o_datarel = shstrtab.index(b".data.rel.ro")
    o_rela = shstrtab.index(b".rela.dyn")
    o_shstr = shstrtab.index(b".shstrtab")

    shstrtab_off = RELA_OFF + len(relocs) * 24

    def align(x, a=8):
        return (x + a - 1) & ~(a - 1)

    shoff = align(shstrtab_off + len(shstrtab))

    sections = [
        (0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        (o_text, SHT_PROGBITS, SHF_ALLOC | SHF_EXECINSTR, TEXT_OFF, TEXT_OFF, TEXT_SIZE, 0, 0, 16, 0),
        (o_rodata, SHT_PROGBITS, SHF_ALLOC, RODATA_OFF, RODATA_OFF, RODATA_SIZE, 0, 0, 8, 0),
        (o_datarel, SHT_PROGBITS, SHF_ALLOC | SHF_WRITE, DATAREL_OFF, DATAREL_OFF, DATAREL_SIZE, 0, 0, 8, 0),
        (o_rela, SHT_RELA, SHF_ALLOC, RELA_OFF, RELA_OFF, len(relocs) * 24, 2, 0, 8, 24),
        (o_shstr, SHT_STRTAB, 0, shstrtab_off, shstrtab_off, len(shstrtab), 0, 0, 1, 0),
    ]
    shnum = len(sections)

    blob = bytearray(shoff + shnum * 64)
    blob[0:4] = b"\x7fELF"
    blob[4] = 2
    blob[5] = 1
    blob[6] = 1
    struct.pack_into("<H", blob, 16, 3)
    struct.pack_into("<H", blob, 18, 0xB7)
    struct.pack_into("<I", blob, 20, 1)
    struct.pack_into("<Q", blob, 40, shoff)
    struct.pack_into("<H", blob, 52, 64)
    struct.pack_into("<H", blob, 58, 64)
    struct.pack_into("<H", blob, 60, shnum)
    struct.pack_into("<H", blob, 62, shnum - 1)

    blob[TEXT_OFF:TEXT_OFF + TEXT_SIZE] = text
    blob[RODATA_OFF:RODATA_OFF + RODATA_SIZE] = rodata
    blob[DATAREL_OFF:DATAREL_OFF + DATAREL_SIZE] = datarel
    for i, (r_offset, addend) in enumerate(relocs):
        struct.pack_into("<QQq", blob, RELA_OFF + i * 24, r_offset, R_AARCH64_RELATIVE, addend)
    blob[shstrtab_off:shstrtab_off + len(shstrtab)] = shstrtab

    for i, s in enumerate(sections):
        struct.pack_into("<IIQQQQIIQQ", blob, shoff + i * 64, *s)

    with open(path, "wb") as fh:
        fh.write(blob)
    return TYPEINFO_VA, SLOT0_FN_VA, DIAG_VA


# --------------------------------------------------------------------------------------------
# Tests
# --------------------------------------------------------------------------------------------

_checks = 0
_failures = []


def check(condition, what):
    global _checks
    _checks += 1
    if not condition:
        _failures.append(what)
        print("FAIL: %s" % what)


def main():
    vb = load_tool()

    text_va = 0x1000
    target = 0x2000 + 0x40
    words = bytearray(64)
    struct.pack_into("<I", words, 0, _adrp(8, (0x2000 - 0x1000) >> 12))
    struct.pack_into("<I", words, 4, _add_imm(8, 8, 0x40))
    hits = vb.scan_code_referencing(bytes(words), text_va, target)
    check(hits == [0x1000], "an ADRP+ADD pair resolves to the target (got %r)" % hits)

    words2 = bytearray(words)
    struct.pack_into("<I", words2, 4, _add_imm(8, 8, 0x50))
    check(vb.scan_code_referencing(bytes(words2), text_va, target) == [],
          "a mismatched ADD immediate is not a reference")

    # The pure-Python fallback (numpy absent) must agree with the vectorised path, or a machine
    # without numpy silently reports "no references" and disables every RTTI-derived seam.
    had_numpy = vb._np
    try:
        vb._np = None
        check(vb.scan_code_referencing(bytes(words), text_va, target) == [0x1000],
              "the pure-Python scan resolves the same reference")
        check(vb.scan_code_referencing(bytes(words2), text_va, target) == [],
              "the pure-Python scan rejects the same mismatch")
    finally:
        vb._np = had_numpy

    class FakeElf:
        def __init__(self, text):
            self._text = text

        def section_text(self, name):
            return self._text

    fake = FakeElf(bytes([0xAA, 0x11, 0x22, 0xBB, 0xAA, 0x33, 0x44, 0xCC]))
    check(vb._count_pattern(fake, "AA ? ? BB") == 1, "wildcard pattern matches once")
    check(vb._count_pattern(fake, "AA 11 22 BB") == 1, "exact pattern matches")
    check(vb._count_pattern(fake, "AA 99 22 BB") == 0, "a fixed-byte mismatch does not match")
    check(vb._count_pattern(fake, "? ? ? ? ? ? ? ? ? ?") == 0, "an all-wildcard pattern matches nothing")

    with tempfile.TemporaryDirectory() as tmp:
        path = os.path.join(tmp, "libminecraftpe.so")
        typeinfo_va, slot0_fn, diag_va = build_synthetic_elf(path)
        result = vb.analyze(path, version_override="9.9.9")
        lpr = result["findings"]["live_player_renderer"]
        check(lpr["typeinfo_va"] == "0x%x" % typeinfo_va,
              "typeinfo resolves (got %s, want %s)" % (lpr["typeinfo_va"], hex(typeinfo_va)))
        check(lpr["vtable_address_point"] == "0x%x" % (typeinfo_va + 0x30),
              "vtable address point is typeinfo_ptr + 16 (got %s)"
              % lpr["vtable_address_point"])
        check(lpr["render_vtable_index"] == 0,
              "slot 0 is the one referencing the diagnostic (got %r)" % lpr["render_vtable_index"])
        check(lpr["render_function_va"] == "0x%x" % slot0_fn,
              "the slot target is the documented function (got %s)" % lpr["render_function_va"])
        check(result["derived_rules"].get("playerRenderVtableIndex") == 0,
              "the derived rules entry carries the slot")
        check(len(result["sha256"]) == 64, "a SHA-256 is reported")

    print("verify_binary_test: %d checks, %d failure(s)" % (_checks, len(_failures)))
    return 1 if _failures else 0


if __name__ == "__main__":
    sys.exit(main())
