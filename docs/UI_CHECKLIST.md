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

## 2. Touch Mod Menu (Screen A)

- [ ] The window is centred, ~66% of the width (capped at 720dp), ~78% of the height, with a
  hairline border and a shadow.
- [ ] The title bar carries the brand, the search field and the close button.
- [ ] The module list is **two columns** on a wide screen, **one** in compact mode.
- [ ] A module row: icon, name, toggle on the right; the whole row taps to toggle.
- [ ] Toggling a module updates the toggle and the row tint; a settings gear opens the side sheet.
- [ ] No violet/magenta anywhere; the accent is blue.

## 3. VIP Mod Menu (Screen B)

- [ ] Opens from the in-game bind (not the nav bar) and looks like the **same app** as Screen A
  (same window, same rows, same toggle).
- [ ] L1/R1 cycle the tabs; the controller and keyboard sub-modes render.

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
