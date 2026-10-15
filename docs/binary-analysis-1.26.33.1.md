# Binary analysis — Minecraft Bedrock 1.26.33.1 (arm64-v8a)

Static analysis of `lib/arm64-v8a/libminecraftpe.so` extracted from the 1.26.33.1 APK, produced
offline by `tools/verify-binary`. Nothing here is a claim about runtime behaviour; each fact is
marked with how it was derived and a confidence level. The library and the APK are **not** committed.

## Subject

| Property | Value |
|---|---|
| APK | `minecraft-26-33-clone-music.apk` from mcpedl.org (`file_id` 7301) |
| package | `com.mojang.mine26331`, `versionName=1.26.33.1` (`aapt2 dump badging`) |
| Library | `lib/arm64-v8a/libminecraftpe.so` |
| Size | 349,243,744 bytes |
| SHA-256 | `d659c56ebae1dd22a7014a70a293e3d68d26938f18dd88dfe4ee9e34eea9c8c1` |

Sections of interest (from `.shdr`): `.rodata` VA `0x213b5c0` size `0x12236b8`; `.text` VA
`0x6573e60` size `0xd8494b0`; `.data.rel.ro` VA `0x13e70680` size `0x8248d8`; `.rela.dyn` VA
`0xa95c60` size `0x159f0c0` (944,648 `R_AARCH64_RELATIVE` entries).

Because the ELF is a normal PIE with `p_offset == p_vaddr` for the PROGBITS sections read here, a
section's file offset equals its VA. That is what lets the tool treat `mm.find` results as VAs.

## 1. `LivePlayerRenderer` render entry — **verified (high confidence)**

Method: locate the typeinfo-name string `18LivePlayerRenderer` in `.rodata`, take its
`R_AARCH64_RELATIVE` reference (that addend is the typeinfo's `__name` field, so the typeinfo
begins 8 bytes earlier), find the relocation whose addend is the typeinfo (the vtable's typeinfo
pointer, `vtable-8`), take the address point at `vtable-8 + 16`, then walk the vtable's
`R_AARCH64_RELATIVE` slots.

| Fact | Value | Evidence |
|---|---|---|
| typeinfo name string | `0x2971019` | `.rodata` contains `…rer\0 18LivePlayerRenderer\0 15NameTagRender…` |
| typeinfo | `0x1404a7f0` | relocation addend at `0x1404a7f8` = name VA |
| vtable address point | `0x1404a318` | relocation addend `0x1404a7f0` at `0x1404a310` (= ap − 16) |
| render slot | **17** | the lowest vtable slot whose body references `variable.player_x_rotation` |
| render function VA | `0x9c4a78c` | slot 17's relocation addend |
| prologue | `stp d15,d14,[sp,#-0xa0]!; stp d13,d12,[sp,#0x10]; …; sub sp,sp,#0x300` | `llvm-objdump` at `0x9c4a78c` |

The five code references to `variable.player_x_rotation` (`0x9c417fc`, `0x9c4af10`, `0xaffdb14`,
`0xbdf79d0`, `0x10b991e4`) resolve against the vtable-slot functions as slots 225, **17**, 183, 183,
183. Slot 17 is the entry point the launcher's string resolver is built to find, and it is the same
index the shipped rules carry for 1.26.50 / 1.26.60. **`playerRenderVtableIndex = 17` is confirmed
for 1.26.33.1.**

> Confidence note: this is the *slot whose function references the diagnostic*. It is not a decoded
> C++ signature — the symbol table is stripped, so the argument layout (ActorRenderData /
> ScreenContext / model) remains **unverified**. The launcher already relies on this being the
> render entry (passthrough detour, no argument dereference), so an unknown ABI is safe there.

## 2. `ClientInstance` local-player accessor — **verified (high confidence)**

| Fact | Value | Evidence |
|---|---|---|
| typeinfo | `0x14055630` | as above |
| vtable address point | `0x140545a0` | |
| slot 31 | `0x9d72118` | the slot the preloader hooks for the local-player feed |

This matches the pinned `kGetLocalPlayerSlot = 31`, so the feed's design survives 1.26.33.1. The
`Actor` field offsets the feed reads (`+0x230` position, `+0x238` rotation) are **not** derivable
from the stripped binary; they are inherited from 1.26.50/1.26.60 and remain **unverified on
1.26.33.1** until a device read shows a plausible position.

## 3. `SerializedSkinRef` / `mce::Image` cape seam — **mostly absent (verified)**

Only two of the accessor-name strings survive:

| Accessor name string | Present |
|---|---|
| `getImageData` | yes |
| `SerializedSkinRef` | yes |
| `getCapeImageData`, `getCapeImageDataCereal`, `getAnimatedImageData`, `getGeometryData`, `getAnimationData`, `getCapeId`, `getSkinColor`, `getProfileHash` | **no** |

The `+0xa8` cape offset was recovered on 1.26.60.28 from those accessors; with the accessor table
stripped here, the offset is **unverified on 1.26.33.1** and `SwapCapeImage` must stay fail-closed
(it already does: a null/mismatched skin address leaves the vanilla cape alone).

## 4. `mce::ImageUtils::loadImageFromMemory` — **string present, pattern re-derivable**

* `loadImageFromMemory` appears once in `.rodata` (VA `0x23186a5` on this build).
* The configured `imageLoaderSig` (the 1.26.50 rule's pattern) matches **0** places in this build's
  `.text`, so the engine-image loader seam is off here (fail-closed, as designed).
* Re-deriving a pattern for this build is a disassembler pass (locate the string's `ADRP+ADD`
  xref, walk back to the enclosing frame prologue). Not done here; the cape path that matters on
  this build is the native render hook, and the cosmetics do not depend on this signature.

## 5. `ClientNetworkHandler` / `AddPlayerPacket` — **string present, slot unconfirmed**

`AddPlayerPacket` appears in `.rodata` at `0x24e8dc7` with one code reference at `0xd3ea27c`,
which sits inside `ClientNetworkHandler` vtable (address point `0x1419a580`) slot **258** by the
nearest-preceding-slot heuristic. The 1.26.60.28 analysis pinned slot 40 via the
`"AddPlayerPacket: NaN position sent by server"` string, which is **absent** on this build, so the
packet-read slot for 1.26.33.1 is **unverified**. Nothing in the shipped cosmetics path consumes it.

## 6. Byte-signature drift on 1.26.33.1 (verified)

Running the bundled 1.26.50 / 1.26.40 patterns against this `.text` (count of `? ?`-wildcard
matches):

| Rule | Pattern | Matches |
|---|---|---|
| 1.26.50.00 | `pauseMenuOpenSig` | 1 |
| 1.26.50.00 | `pauseMenuDtorSig`, `hudScreenDtorSig`, `hudScreenOpenSig` | **0** |
| 1.26.40.00 | `pauseMenuOpenSig` | 1 |
| 1.26.40.00 | `pauseMenuDtorSig`, `hudScreenDtorSig`, `hudScreenOpenSig` | **0** |
| 1.26.30.00 | all five menu/HUD sigs | **1 each** |
| 1.21.132..1.26.39.99 | all five menu/HUD sigs | **1 each** |

This is direct evidence for the Stage-1 finding: a version bump moves the byte patterns, so a rule
that matched 1.26.50 does not match 1.26.33.1. The 1.26.30-style signatures (which the shipped file
covers with the `min: 1.26.30.00` rule) still match this build — which is why the per-field merge
matters: the 1.26.30 rule carries the working signatures but (before the fix) no render slot.

## 7. GL backend, frame submission, matrix source — **not derived here**

* GLES-vs-Vulkan and the frame-submission function were not extracted: they need the game's own
  config and a live GL context, not just the binary. The runtime `GlBackend` probe and the
  `EGL context current` diagnostic cover this on a device.
* The real view-projection source is `CameraSource` (a matrix scan), not a named symbol; no
  accessor is exported. **Unverified**; the render hook reports whether its anchor came from the
  engine matrix or the first-person fallback so a device run can tell.

## Generated rules entry

`tools/verify-binary` emits, for this library:

```json
{
  "playerRenderVtableIndex": 17,
  "checksum_sha256": "d659c56ebae1dd22a7014a70a293e3d68d26938f18dd88dfe4ee9e34eea9c8c1",
  "min": "1.26.33.1"
}
```

The bundled rules now carry the render slot on the `1.26.30.00 .. 1.26.39.99` range with this
library's SHA-256 recorded beside it (`librarySha256`), so a future build can refuse a rule that
was not derived for it. The checksum is informational today (the native parser ignores unknown
keys); wiring a hard refusal is a follow-up.

## What remains unverified (needs a device)

1. That the render hook actually fires on 1.26.33.1 at runtime (the slot resolves statically; a
   runtime crash or a silently non-executable address is possible).
2. The `Actor` position/rotation offsets `+0x230`/`+0x238` on this build.
3. Whether the game uses GLES or Vulkan here, and whether a GL context is current on the render
   thread.
4. Whether cosmetics actually appear on the player in third person.
