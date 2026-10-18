# Device checklist — proving the native cosmetic draw

This is the one thing the development environment cannot do. There is no emulator or KVM in the
build container, so nothing here has been observed in the running game. **Every in-game claim in this
repository is unverified on device until you run this list and report back.**

The checklist needs only your phone and the app. No adb, no PC.

Target build: **Minecraft 1.26.33.1** (the version the native render slot is being verified against).

---

## Before you start

1. Install the debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
2. Launch the app once so the native library loads and cosmetics initialise.

---

## Step 1 — Read the diagnostics report

1. Launch a version in the launcher and enter a world.
2. Open the Mod Menu (the in-game menu), go to the **Cosmetics** section.
3. Tap **Diagnostics**.
4. Tap **Copy report** (or **Share report** and send it to yourself).

Record the whole report. The lines that matter most:

| Line | What it means | What we need |
|---|---|---|
| `Native library loaded` | Is `libpreloader.so` present and loadable? | If `no`, nothing else can work. |
| `Player-render slot` | Which vtable slot the hook resolved, and whether by the render-diagnostic string scan or by the configured index. | This is the **slot 17 verification**. If it says a different number, or `unknown`, tell us. |
| `Hook installed` | Did the detour install? | If `no`, the slot did not resolve to executable code. |
| `Frames seen` | How many render ticks the hook has seen. | Should climb while you play. |
| `Cape engine texture route` | Did the cape reach the game through the engine's own image loader? | This is the Vulkan-safe cape path; `yes` means a cape appears with no GL renderer. |
| `Engine image pipeline live` | Did the `mce::Image` loader seam resolve? | The cape swap needs this; `no` on a build whose loader signature did not match. |
| `GL backend` | `GLES`, `Vulkan (GLES unavailable)`, or `unknown`. | Informational now — the cape route does not need GLES. |
| `EGL context current` | Is an EGL context current on the render thread? | Informational; the engine texture route does not depend on it. |
| `Last GL error` | `none` or a hex code. | A code here narrows down a failed engine-image load. |
| `Published geometry bytes` | Did the launcher publish the equipped geometry? | A size > 0 confirms the launcher half. |
| `Piece atlases` | Which of cape/hat/pet textures were published. | Confirms per-piece textures. |
| `Local cosmetics published under key` | Should read `"local"`. | Confirms the key scheme. |
| `Rules source` | `bundled` or `remote`, and the revision (should be ≥ 6). | A remote file with an older or field-less rule would disable the hook. |
| `Rules delivered to native` | Did the native library receive the rules path at all? | If `no`, the hooks refuse to install (fail-closed vanilla). |
| `Effective rule` | The merged fields the native hook used; must contain `playerRenderVtableIndex=17`. | If the slot is missing here, the hook installed without one. |
| `Rules matching this version (extra)` | `> 0` means several rules covered this build — exactly where first-match selection used to drop fields. | The merge now inherits the missing field. |
| `Rule has render slot` | `yes`. | The single most important post-fix line. |
| `Hook install state` | `resolved after N frame tick(s)` vs `armed, waiting`. | Distinguishes a slow runtime from a failure. |
| `libpreloader.so copies mapped` | Must be exactly `1`. | `> 1` = **SPLIT**: JNI and the hook are in different copies; nothing can ever draw. |
| `Local player feed` | `live x=… y=… z=… yaw=… pitch=…` with plausible values. | Cosmetics are scoped to a live local player. |
| `Camera anchor source` | `engine view-projection` or `first-person fallback`. | A pass/fail test must not accept the fallback as the engine camera. |
| `Status` | The route the client reports as live this session. | The honest summary line: which pieces reach the game and which are preview-only. |

---

## Step 2 — The cape engine texture route (the Vulkan-safe path)

The cape no longer needs a GLES draw surface: it reaches the game as a texture swap at the engine's
own image loader, so it works on a Vulkan-only RenderDragon build. This step confirms that route.

1. Still in the world, open **Cosmetics → Diagnostics**.
2. Confirm the report reads `Cape engine texture route: yes` and `Engine image pipeline live: yes`.
3. Equip a cape, close the menu and look at your character (third-person view is easiest).
4. Take a screenshot.

Report back:
- Does the equipped cape appear on your character's back? **yes / no**.
- If no, copy the diagnostics report again — the `Cape engine texture route`, `Engine image pipeline
  live` and `Last GL error` lines tell us where the route stopped.

---

## Step 3 — An equipped accessory and pet

1. Equip a hat and a pet in the Cosmetics section.
2. Return to the world; watch your character.
3. Take a screenshot with each piece if possible.

Accessories and pets reach the game through the native player-render hook, so on a build where that
hook is not live they show in the preview only (the panel says so). Report back which, if any, of
cape / hat / pet appear, and whether they are attached in a sensible place (cape on the back, hat on
the head, pet beside you) or floating/off-screen.

---

## What to send back

- The full text of the diagnostics report (Step 1).
- The Step 2 answer, with a screenshot.
- The Step 3 answer, with screenshots.

That is enough to decide the next step: if the cube shows, the native path is real and the remaining
work is placement and art; if it does not, the report says which link failed (slot, hook, GLES, or
EGL context).

---

## Honesty note

Nothing in this document claims the cape, accessory or pet appear. It is a procedure to find out.
The app's own wording (`Cape engine texture route`, `Engine image pipeline live`, the status line)
never claims a route that is not live, and neither should any result you report.
