# Compatibility

One-page statement of what GlowberryClient runs on and needs.

## At-a-glance

| | Supported |
| --- | --- |
| **Android** | 9.0 (API 28) or newer, 64-bit (`arm64-v8a`) |
| **Minecraft** | Bedrock Edition **1.21.80 or newer** (older versions are refused at launch) |
| **RAM** | 1 GB minimum, **3 GB recommended** for multi-version use |
| **Storage** | 2 GB minimum, **5 GB recommended** |

## How the game is obtained and used

GlowberryClient is a launcher, not a game. It **requires an existing Minecraft:
Bedrock Edition APK** — either the official Google Play app or an otherwise
authentic, licensed copy of a supported version — and it runs the copy you
already own. It never ships, redistributes, or bypasses licensing for the game.
The launcher extracts native libraries from that APK on the device and does not
provide a Minecraft license.

## What is supported

- **APK import and installation-free launching** — import your Minecraft APK
  (or composed XAPK) and run it without a system-level install.
- **Multi-version instances** — keep multiple isolated game versions and their
  data side by side. Most Bedrock versions ship both 64- and 32-bit libraries
  and run on this build; a version whose APK ships **only** 32-bit libraries is
  refused with a clear message.
- **Version isolation** — enable it for imported versions so their data stays
  separate from an installed copy.

## Failure states and what they mean

| Launcher says | Meaning | Fix |
| --- | --- | --- |
| "Minecraft is missing" | No compatible Minecraft APK is available to the launcher | Provide an official/authentic Bedrock APK and retry |
| "Version refused" | The selected version ships no `arm64-v8a` libraries, or is older than 1.21.80 | Pick a supported version |
| "Import won't launch" | The import did not complete (ABI or extract mismatch) | Re-import the APK, or use Settings → Diagnostics |

When a launch fails, the launch log from the failure screen names the reported
reason before changing settings.

## Backups and migration

- The launcher may migrate older data directories before continuing. **Do not
  delete the old directory** until the migration succeeds and you have confirmed
  worlds, packs, and settings are available.
- Keep an extra copy of important worlds outside app storage.

## Native modules

Native modules run code in the game process. Install only modules from sources
you trust and test them with an isolated version first. Modules that need game
data this build cannot read fail closed and say so in-app.

## Common questions

**Why does the launcher say Minecraft is missing?** — Provide an official or
authentic Bedrock Edition APK and open GlowberryClient again.

**Why can an imported version fail to launch?** — Enable version isolation in the
instance settings, or re-import a supported APK via Diagnostics.

**Where do developer API questions go?** — The [Developer](/guide/developer)
section. The Preloader API reference is intentionally grouped there instead of
the main user guide.
