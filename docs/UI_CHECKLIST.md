# UI check list — Blueberry restyle

The dev environment has no device, so the pure JVM tests cover the geometry, tokens and rules, and
this list is what only a phone can confirm. Work top to bottom, screenshot each step, and send the
shots back.

Everything below is **unverified (needs device)** until you run it. No claim in the release notes
should say a screen "works in game" until this list passes.

## 1. Radial wheel

1. Open the wheel from its in-game bind.
   - [ ] Opens in under ~200ms; the ring scales up with the scrim fading in.
   - [ ] The four sectors (top MODS, right CLOSET, bottom SETTINGS, left HUD) are rounded wedges
     with a constant-width gap between them (the gap should look parallel, not a widening wedge).
   - [ ] Each sector shows an outline icon above a caps label.
   - [ ] The hub shows your **real skin head**, turning slowly. If you have no skin set, it shows a
     neutral disc, never an emoji.
2. Drag across sectors.
   - [ ] The sector under your finger lightens and grows slightly.
   - [ ] A short haptic tick fires on each change.
3. Quick chips.
   - [ ] Chips float outside the ring (two top-left, three top-right, one right-middle), none
     overlapping the ring or each other.
   - [ ] Tapping a chip toggles the module and the dot changes colour.
4. Release on a sector opens that screen; releasing outside the ring or pressing Back closes.

## 2. Mod Menu (one screen, touch and bind)

- [ ] The window is centred, ~66% of the width (capped at 720dp), ~78% of the height, with a
  hairline border and a shadow.
- [ ] The title bar carries the brand, the search field and the close button.
- [ ] The module list is **two columns** on a wide screen, **one** in compact mode.
- [ ] A module row: icon, name, toggle on the right; the whole row taps to toggle.
- [ ] Toggling a module updates the toggle and the row tint; a settings gear opens the side sheet.
- [ ] No violet/magenta anywhere; the accent is blue.
- [ ] The in-game bind opens the radial wheel; its sectors open the same Mod Menu the launcher's
  Mods screen opens, on the page the sector names.
- [ ] The rail lists every page (General, Voice, Packs, Cosmetics, HUD editor, Replay, Settings,
  Controller, Keyboard). The Controller and Keyboard tiles carry a small accent dot — the
  Supporter badge — and are otherwise ordinary pages.
- [ ] L1/R1 cycle every page in both directions and wrap.
- [ ] The Controller page draws the connected pad and lights the pressed button; a modifier
  capture in the full editor takes the next press.
- [ ] The Keyboard page draws the board, flashes a captured key and rebinds the action.

## 3. Adaptive navigation (U5) and the type/shape scales (U6)

- [ ] On a phone-width window the destinations are a horizontal bar at the top, icon-only, with
  the selected entry's label shown.
- [ ] On a 600-839dp window the destinations are a vertical icon rail on the leading edge.
- [ ] On an 840dp+ window the rail also shows every label.
- [ ] Rotating does not change the navigation shape; unfolding / resizing across a breakpoint does.
- [ ] Every label uses a type role (display/title/body/label/caption); no screen sets a raw sp.
- [ ] The active rail row's corners animate between the chip and card stops as the selection moves,
  and its label is heavier than the inactive rows'.

## 3b. First run, failure states, Play dock and mod rows (U13-U17)

- [ ] Fresh install with no instance: the splash opens the **first-run wizard**, not the dashboard.
- [ ] The wizard shows Import game -> Pick version -> Sign in -> Ready, and the progress rail has
  as many dots as the active steps (three when launcher-managed sign-in is off, four when on).
- [ ] Backing out and returning resumes on the first incomplete step, not at the start.
- [ ] Skip marks the wizard done and opens the launcher; it never appears again.
- [ ] With an instance already installed, an upgrade into this build does **not** show the wizard.
- [ ] Launch with no version selected: the dialog says "No Version Selected".
- [ ] Import a version whose APK ships 32-bit libs only: the dialog says "Unsupported build" and
  names the ABI, not a generic launch failure.
- [ ] Settings -> Diagnostics -> Launch readiness shows the state key and whether it blocks.
- [ ] The same state is what the Play button reports, so the two cannot disagree.
- [ ] Stale native libs (bump the extractor version) launch anyway after a silent re-extract.
- [ ] The dock is one row: profile on the left, version as a chip on the right, then the full-width
  Play button. The chip opens the instance picker.
- [ ] A mod row shows its runtime kind (native/script) and, when it failed, a red badge whose
  content description is the recorded `kind: reason`.
- [ ] A mod that failed to load can still be switched off from its row.

## 4. Closet (cosmetics)

- [ ] Left rail: Capes, Hats, Pets, Back, Offhand, Effects.
- [ ] Centre: a 3-column grid of cards with a name and an Equipped/Owned pill.
- [ ] Right: the live 3D preview of your character with your real skin; drag to rotate; it turns
  slowly when idle.
- [ ] Tapping a card equips it (the pill moves to it) and the preview updates.
- [ ] The Equip button below the preview is the accent gradient.

## 5. Launch tab

- [ ] The hero shows your character (real skin) standing on a soft shadow, with the instances/mods/
  playtime stats beside it.
- [ ] The LAUNCH button is the accent gradient, ~56dp, with a soft glow.
- [ ] The dock is opaque: no text shows through it.
- [ ] Scrolling to the bottom, no row runs underneath the dock.
- [ ] No empty grey banner at the top of the tab.

## 6. Skin behaviour (the special update)

- [ ] The radial hub head and the Closet/hero preview show the **same** skin.
- [ ] Change your skin (import one in the launcher, or apply a skin pack) and reopen the wheel /
  Closet: the head updates **without restarting** the app.
- [ ] With a game session running, the preview shows your **real Minecraft skin** read from the
  game (the engine image-loader seam). The Cosmetics diagnostics screen should read
  "Game-skin feed installed: yes" and "Base skin captured from game: yes" once you are in a world.

## 7. UI gallery (debug)

- [ ] Settings > Updates, tap the version text 7 times: the gallery opens.
- [ ] Every component (toggle off/on/disabled, rows, colours, type) is visible and legible.

## Known limitations (not bugs)

- **The in-game skin read is content capture, not attribution.** The engine image-loader seam carries
  no player id, so the held texture is the most recent base-skin-shaped upload — in practice the local
  player's skin, but not proven to be. Whether the game routes the player's skin through this seam at
  all is **unverified (needs device)**; if it does not, the preview falls back to the imported /
  applied / Steve skin.
- **Cosmetics in game** depend on the preloader's player-render hook, which resolves to "no hook" on
  the shipped builds (see `AGENTS.md`); the preview is the verified surface.
