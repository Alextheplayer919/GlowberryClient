# Finding: where skins are lost on a big server (Phase 6, measurement first)

Phase 6 asks for a written finding that names the failing step **before** any fix is written. It
also asks for a device run comparing a big server with a small one. That run cannot be done from
this repository: it needs an arm64 device, a live world and two servers, and there is no emulator
in the build environment. What follows is what the code can and cannot answer without it, with the
step each answer rules in or out. **No fix is written here, by design.**

## The pipeline, in order, and what each step can drop

The skin a player sees on their own character and the skin the preview shows are two different
paths. The relevant one for "skins are wrong on big servers" is the engine's own path, which the
launcher can only observe, not drive.

| # | Step | Who owns it | Can it drop a remote skin? |
|---|---|---|---|
| 1 | Player-list / join packet carries each entry's `SerializedSkinRef` | game (packet handler) | **Yes** - a dropped or unparsed entry never yields a skin |
| 2 | The engine builds an `mce::Image` for a skin it is about to use | game (image loader) | **Yes** - an upload that never happens leaves the default |
| 3 | The engine binds that image to the actor | game (renderer) | **Yes** - a bound-but-wrong texture reads as a default |
| 4 | The launcher captures the last base-skin-shaped upload | launcher (`mce_image_hook` -> `GameSkinFeed`) | It only observes; it cannot cause a loss, but it can *misattribute* |

Steps 1-3 are entirely inside the game. This build exports no symbol for a game class and no static
vtable address yields a live `Actor*` (see the "Native entity/camera feed" section of `AGENTS.md`),
so the launcher cannot read an arbitrary player's skin or force a rebind. The one seam it owns is
step 4, and that seam is **content, not attribution**: the loader carries no player id, so the
capture is the most recent base-skin-shaped upload, which is usually the local player's skin.

## What the existing instrumentation already answers

`IsPacketHookLive()` (`GameCosmetics.cpp`) reports whether the join-packet hook has fired at least
once. `ReadMceImageHookStats()` reports loader calls, substitutions, registered overrides and bytes
seen. `ReadCapeCaptureDiagnostics()` and `nativeReadCapturedSkin()`/`nativeReadCapturedCape()` report
what the loader actually captured. Together these can answer, on a device:

- **Step 1 is live** if the packet-hook count is non-zero after a join. A zero count on a big server
  while a small server is non-zero points at step 1.
- **Step 2 is happening** if the loader-call count rises as players join. Loader calls that rise but
  whose captured payloads are not base-skin-shaped point at step 2 (the skin is being built by a
  path the capture does not recognise).
- **Steps 1 and 2 both live, skin still default** leaves step 3 (the bind), which this build cannot
  observe at all.

That is a genuine triage order, and it needs the two-server comparison Phase 6 describes.

## Why a client-side skin cache is not obviously the fix

Phase 6 notes a cache "is plausible only if skins are being dropped". The instrumentation above can
show *that* something is dropped; it cannot show that re-applying a skin client-side would help,
because steps 2 and 3 are engine-owned. If the loss is at step 1, a cache cannot recover an entry
the client never received. If it is at step 3, the client has the image and still cannot rebind it
without a hook this build does not have. A cache is therefore only indicated when the drop is
specifically at step 2 with a capturable payload - which the two-server run has to establish first.

## What is needed to finish Phase 6

1. An arm64 device, Minecraft 1.26.33.1, in a world on a large server (many players joining at once)
   and a small one, with the cosmetics diagnostics open across each join.
2. Record, per join: packet-hook count delta, loader-call delta, captured-skin stamp, and whether the
   preview head shows a real skin or Steve.
3. The first step whose count stays at zero names the failing stage.

Until that run exists, this file is the honest state: the pipeline, the observable seams, the
triage order and the reason no fix has been written. Guessing a fix here would be exactly the
"promise a result the build cannot produce" failure the rest of this work avoids.
