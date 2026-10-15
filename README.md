<p align="center">
  <img src="docs/assets/banner.png" alt="GlowberryClient — Android launcher for Minecraft: Bedrock Edition" width="100%">
</p>

<p align="center"><b>An Android launcher for Minecraft: Bedrock Edition with multi-version management and an extensible native modding system.</b></p>

<p align="center">
  <a href="https://github.com/ChimeraAnt-DEV/GlowberryClient/releases/latest">
    <img src="https://img.shields.io/github/v/release/ChimeraAnt-DEV/GlowberryClient?style=for-the-badge&logo=github&logoColor=white&label=RELEASE&color=7E32AA" alt="Latest release"></a>
  <a href="https://github.com/ChimeraAnt-DEV/GlowberryClient/releases">
    <img src="https://img.shields.io/github/downloads/ChimeraAnt-DEV/GlowberryClient/total?style=for-the-badge&logo=githubactions&logoColor=white&label=DOWNLOADS&color=0F5BE4" alt="Downloads"></a>
  <a href="https://github.com/ChimeraAnt-DEV/GlowberryClient/stargazers">
    <img src="https://img.shields.io/github/stars/ChimeraAnt-DEV/GlowberryClient?style=for-the-badge&logo=github&logoColor=white&label=STARS&color=FFC94D" alt="Stars"></a>
</p>
<p align="center">
  <a href="https://github.com/ChimeraAnt-DEV/GlowberryClient/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/LICENSE-Apache--2.0-2FA85A?style=for-the-badge&logo=apache&logoColor=white" alt="License: Apache 2.0"></a>
  <img src="https://img.shields.io/badge/ANDROID-9.0%2B%20(API%2028)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 9.0+">
  <img src="https://img.shields.io/badge/ABI-arm64--v8a-6D4AFF?style=for-the-badge&logo=arm&logoColor=white" alt="arm64-v8a">
  <img src="https://img.shields.io/badge/BUILT%20WITH-Kotlin%20%2B%20C%2B%2B-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin + C++">
</p>
<p align="center">
  <a href="https://github.com/ChimeraAnt-DEV/GlowberryClient/actions/workflows/build.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/ChimeraAnt-DEV/GlowberryClient/build.yml?style=for-the-badge&logo=githubactions&logoColor=white&label=BUILD&color=0F5BE4" alt="Build status"></a>
  <a href="https://github.com/ChimeraAnt-DEV/GlowberryClient/issues">
    <img src="https://img.shields.io/github/issues/ChimeraAnt-DEV/GlowberryClient?style=for-the-badge&logo=github&logoColor=white&label=ISSUES&color=FF5C5C" alt="Issues"></a>
  <a href="https://github.com/ChimeraAnt-DEV/GlowberryClient/commits/main">
    <img src="https://img.shields.io/github/last-commit/ChimeraAnt-DEV/GlowberryClient?style=for-the-badge&logo=git&logoColor=white&label=LAST%20COMMIT&color=9AA0A6" alt="Last commit"></a>
  <a href="docs/guide/getting-started.md">
    <img src="https://img.shields.io/badge/DOCS-English%20%2B%20%E7%AE%80%E4%BD%93%E4%B8%AD%E6%96%87-71ACEF?style=for-the-badge&logo=readthedocs&logoColor=white" alt="Docs"></a>
</p>

---

## What is GlowberryClient?

GlowberryClient is an open-source Android launcher for **Minecraft: Bedrock Edition**. It lets you **import your official Minecraft APK and launch it without a system install**, keep **multiple isolated game versions** side by side, and **extend the game with native modules** through a Preloader runtime.

It is built for legitimate players: it runs the copy of Minecraft you already own, it does not ship the game, and it does not bypass licensing.

> **Independent project:** GlowberryClient has its own architecture, branding, feature set, and maintenance. It is **not a fork** of any other launcher. Its native-modding layer builds on conventions from the wider open-source Bedrock modding ecosystem, but every component here is developed and maintained as part of GlowberryClient.

**Jump to:** [Screenshots](#screenshots) · [Quick start](#quick-start-players) · [Features](#key-features) · [Status](#project-status--compatibility) · [Developers](#for-developers) · [Docs](#documentation)

---

## Screenshots

> These are **schematic mockups** of the launcher's own UI (drawn from the real `UiTokens` palette), not device photos — they show layout and mood. Device screenshots are captured via [`docs/UI_CHECKLIST.md`](docs/UI_CHECKLIST.md).

<table>
  <tr>
    <td width="50%" align="center"><b>Mod Menu</b><br><sub>Two columns, category rail, search, live stats</sub><br>
      <img src="docs/assets/mockups/mod_menu.png" alt="GlowberryClient Mod Menu"></td>
    <td width="50%" align="center"><b>Radial wheel</b><br><sub>Opened by an in-game bind, head hub + quick chips</sub><br>
      <img src="docs/assets/mockups/radial_wheel.png" alt="GlowberryClient radial wheel"></td>
  </tr>
  <tr>
    <td width="50%" align="center"><b>Closet (cosmetics)</b><br><sub>Category rail, card grid, live 3D preview</sub><br>
      <img src="docs/assets/mockups/closet.png" alt="GlowberryClient Closet"></td>
    <td width="50%" align="center"><b>Launch tab</b><br><sub>Hero preview, info cards, docked launch bar</sub><br>
      <img src="docs/assets/mockups/launch_tab.png" alt="GlowberryClient Launch tab"></td>
  </tr>
  <tr>
    <td colspan="2" align="center"><b>.AntEgg mod workflow</b><br><sub>Drop a package, validated + sandboxed + atomic install</sub><br>
      <img src="docs/assets/mockups/antegg_import.png" alt=".AntEgg import workflow" width="70%"></td>
  </tr>
</table>

---

## Quick start (players)

### Requirements

| | |
|---|---|
| **Android** | 9.0 (API 28) or higher |
| **Architecture** | 64-bit (`arm64-v8a`). Most versions ship both 64- and 32-bit libs and run fine; a version whose APK ships 32-bit **only** is refused |
| **RAM** | 1 GB minimum, **3 GB+ recommended** |
| **Storage** | 2 GB minimum, **5 GB+ recommended** |
| **Minecraft** | You must **install**| A unofficial Minecraft Apk to be able to launch Minecraft bedrock edition without any missing .so file errors.|

### Install in 4 steps

1. **Get the latest build** → [**Releases**](https://github.com/ChimeraAnt-DEV/GlowberryClient/releases/latest) (download the APK/ZIP and extract it).
2. On your device, allow installs from unknown sources (**Settings → Security / Applications**).
3. Open the downloaded APK and install it.
4. Launch **GlowberryClient**, then point it at your Minecraft APK when asked.

> **Licensing:** GlowberryClient requires a legitimate, licensed copy of Minecraft Bedrock Edition. Do not use it with pirated or unauthorized versions, and keep your license linked to your Microsoft account.

<details>
<summary><b>Upgrading from a previous name</b></summary>

- **From “Chimera Client”:** GlowberryClient is a display-name rebrand; the package id (`org.chimeramc.client`) is unchanged, so it installs as an in-place update and your settings carry over.
- **From the older “Chimera Launcher”:** that rename changed the package id, so Android will not offer an in-place update and the old app's private settings are not reachable. Export them from the old app (**Settings → Backup & Restore → Export**) and import them here. Account credentials and API keys are never included in the export.

</details>

---

## Key Features

**Launching & instances**
- **APK Import & Installation-Free Launching** – Import your official Minecraft APK (XAPK or composed APK) and run it directly, without a system install.
- **Installations Tab** – Browse the Bedrock versions a package mirror publishes and install one as its own isolated instance. Downloads survive a failed import so a retry never re-downloads hundreds of megabytes; listing pages page cleanly; a browser fallback handles a mirror that challenges plain requests. The source is pluggable.
- **Multi-Version & Instance Management** – Run multiple versions independently with complete isolation between configurations, game data, and worlds.
- **Options Editing & Quick Launch** – Edit per-version `options.txt`-style settings, and use Minecraft URI quick-launch actions (open screens, connect to servers, add servers, join Realms, load worlds, run commands).
- **Custom Flat Worlds** – Craft preset or fully customized superflat worlds before the game opens.
- **Playtime Tracking** – Per-instance playtime, shown on the home hero card and the instance list.

**Modding**
- **Native Mod System (Preloader)** – Load external native `.so` modules packaged as `.chimerapack` (or a plain `.so`), with a Preloader input/output pipeline, hook installation, patches, and keyboard/gamepad input callbacks. See [`docs/api`](docs/api) and [`examples/full-cpp-mod`](examples/full-cpp-mod).
- **`.AntEgg` Mod Packaging** – A ZIP-based format for shipping **native or Lua-script** mods, with a validated manifest, sandboxed extraction, and atomic install. Script mods run on an embedded Lua VM and report failures in the Mods tab instead of taking the launcher down. See [`examples/antegg-template`](examples/antegg-template).
- **Mod Sourcing** – Import from **CurseForge** (user API key) and install `.mcpack`/`.mcaddon` files, or pick local files. A **Mod Hub** browses the **Modrinth** index and scans local Downloads; Modrinth hosts Java Edition content this launcher cannot install, so it is browse-only and says so.
- **Mod Load Diagnostics & Safe Mode** – Failed mods are recorded with a user-readable reason and badged inline instead of crashing the process; a crash-loop safe mode offers to disable the mods loaded before a crash.
- **JavaBridge (Java Edition Mod Auto-Porter)** – Point it at a Java Edition `.jar`; it assesses portability, then (with explicit consent and a configured LLM) decompiles and generates a Bedrock `.AntEgg` mod. It **rewrites** Java mods rather than running them, and says so — there is no Java Edition runtime.

**In-game menu & overlays**
- **Mod Menu (two screens)** – A touch Mod Menu with **General**, **HUD Editor**, **Cosmetics**, **Packs**, **Voice**, **Replay**, and **Settings** sections, plus a premium **VIP Mod Menu** opened by an in-game bind, with controller and keyboard surfaces and a draggable HUD overlay system. A **radial wheel** bound in-game offers the same destinations around the player's skin head.
- **PvP Modules** – A dedicated **PvP** section groups combat tools: Aim Settings, CPS Display, Snaplook, Armor HUD, Crystal Optimizer, Hit Registration, Select Hit, and Hitboxes. Modules that need live game data say so on screen instead of showing a fabricated reading.
- **PvP Suite (V1.1)** – Four overlay-only visual modules in the General tab: Reach Indicator, Trajectory Prediction, Hit Prediction, and Custom Kill Effects. Each fails closed (draws nothing) when it has no data rather than guessing.
- **Replay System** – Record, trim, favorite, rename, and export clips from a shared Replay panel available in both Mod Menu screens, backed by a single capture/library backend.
- **In-Game Pack Changer** – Optionally change an instance's resource packs from inside the running game, written to every candidate game-data root so the change is never silently ignored.

**Social & identity**
- **Proximity Voice Chat** – Talk to players in the same world over LAN multicast, with multiple channels, shareable private join codes, a live in-game mic indicator, and a dedicated Voice tab. An optional **internet relay** extends voice to players who are not on the same Wi-Fi (Opus + a lightweight Go relay server).
- **Multiple Xbox Account Management** – Add and switch multiple Microsoft/Xbox accounts, with in-app MSAL login and device-code flows, so the game launches with the expected identity.
- **Cosmetics Studio** – Preview your character with an applied skin, cape, and accessories, and apply cosmetics in-game. A **Cosmetics status** diagnostic turns the silent failure modes into named checks.

**Input, content & feel**
- **Controller Support & Input Mapping** – Up to 5 profiles per controller type (Xbox, DualShock 4, DualSense), with button remapping (including remap layers), stick dead-zone tuning, sensitivity, rumble strength, anti-stick-drift with hysteresis, explicit calibration, and a Low Input Delay mode — applied live to gameplay. Controller screens use **3D illustrations**.
- **Content Management & Skin Packs** – Import, export, and back up worlds, resource packs, skin packs, and launcher data from one place.
- **Performance & Personalization** – One-tap presets (Battery/Balanced/Performance), an **FPS Optimization** coordinator that quiets the launcher and sheds speculative work under thermal/battery/memory pressure while a session runs, a **Reduce Network Latency** mode that tunes launcher-owned sockets and avoids polling during a session, user-configurable accent color, glass-card/compact theming, animations, empty states, a "last played" hero card, and an optional blue firework touch layer.
- **News, Updates & Crash Reporting** – In-app news feed with notifications, a crash-reporting screen surfacing logs, automated release-content tooling ([`docs/RELEASE_CONTENT.md`](docs/RELEASE_CONTENT.md)), and a Discord notification action for beta releases.

---

## Project status & compatibility

GlowberryClient is **actively developed**. The table below describes what is reachable on the shipped build; anything that needs a per-build native hook fails closed and says so in-app rather than guessing.

| Area | Status |
|---|---|
| **App build** | Debug and release APKs from [`main`](https://github.com/ChimeraAnt-DEV/GlowberryClient/commits/main); CI builds a debug APK on every push |
| **Android** | 9.0 (API 28) → 15; `arm64-v8a` only |
| **Bedrock versions** | Multi-version instances; a version is refused only when its APK ships no `arm64-v8a` libraries |
| **Native modding** | Preloader pipeline + `.AntEgg` (native + Lua) |
| **In-game overlays** | Mod Menu, VIP menu, radial wheel, HUD editor |
| **Cosmetics** | Preview + native render route; in-game appearance depends on the per-build render hook |
| **Voice** | LAN multicast by default; optional internet relay |
| **Java Edition** | **Not supported** — no Java Edition runtime; JavaBridge ports mods instead |

**Where to get updates:** [Releases](https://github.com/ChimeraAnt-DEV/GlowberryClient/releases) for builds, [Actions](https://github.com/ChimeraAnt-DEV/GlowberryClient/actions) for CI artifacts, and the in-app news feed for announcements. A more detailed compatibility matrix lives in [`docs/guide/compatibility.md`](docs/guide/compatibility.md).

---

## For developers

> Everything below is for building GlowberryClient from source or writing mods against it. If you only want to play, see [Quick start](#quick-start-players).

### Build from source

**Prerequisites:** Git, Android Studio (latest), JDK 21+, Android SDK (API 28+), Android NDK r28.

```bash
# The native Preloader and LibHttpClient code are git submodules the native build needs.
git clone --recurse-submodules https://github.com/ChimeraAnt-DEV/GlowberryClient.git
# Already cloned without them?
git submodule update --init --recursive
```

Then open the project in Android Studio, let Gradle sync, connect a device (API 28+), and Run.

```bash
./gradlew :app:compileDebugJavaWithJavac   # fast Java-only check
./gradlew :app:testDebugUnitTest           # JVM unit tests
./gradlew :app:assembleDebug               # full APK (native build included)
```

### Write a mod

- **Preloader API reference** → [`docs/api`](docs/api) (mod, hook, patch, input, memory-hook macros, signature, config, mod-menu)
- **Full C++ mod example** → [`examples/full-cpp-mod`](examples/full-cpp-mod)
- **`.AntEgg` template** → [`examples/antegg-template`](examples/antegg-template)
- **Developer guide** → [`docs/guide/developer.md`](docs/guide/developer.md)

### Continuous integration

Every push to `main` and every pull request triggers [`.github/workflows/build.yml`](.github/workflows/build.yml), which checks out **with submodules**, builds a debug APK (JDK 21, Gradle 8.13 via the wrapper), **fails the run if the build breaks**, and uploads the APK as a workflow artifact. Grab it from the latest green **Build Debug APK** run under the **Actions** tab.

### Documentation

Full user guide (English and 简体中文), developer docs, and the Preloader API reference live under [`docs/`](docs/) — start at [`docs/index.md`](docs/index.md).

---

## Contribution Guidelines

We welcome contributions from the community. To keep the codebase high-quality and collaboration smooth:

### Code Quality
Write clean, modular code with descriptive names and consistent formatting, following the Kotlin and Java style guidelines established in the project.

### Commit Structure
Use small, focused commits with clear messages; each should address a single feature or fix. Example: `Fixed memory leak in version manager`.

### Documentation
Comment complex logic and update relevant docs. If you add a feature, update the README and any related documentation.

### Performance
Keep additions low-latency. The controller input path and in-game overlays run on the input-to-photon critical path, so keep them allocation-free.

### Testing
Test on multiple devices and Android versions. Where logic can be isolated from Android and game APIs, add a pure unit test — many subsystems (controller response math, voice protocol, cosmetics, hit projection, mod packaging) are covered this way.

### Pull Requests
Submit PRs with a detailed description, reference related issues, and include screenshots or video for UI changes.

**Before submitting:** run a full build and test on at least one device. We review contributions promptly.

---

## Usage Guidelines

### Permitted Uses
- Modify GlowberryClient for personal gameplay and to test new features.
- Create educational content (videos, tutorials, blog posts).
- Create derivative projects and share them, provided you comply with the Apache License 2.0.

### Prohibited Uses
- Do not claim GlowberryClient as your own without crediting the Chimera Team and its contributors.
- Do not use GlowberryClient to violate Mojang or Microsoft's user agreements.

> **Disclaimer:** The authors and contributors are not responsible for bans, damages, or issues arising from the use of this software. Use it at your own risk and in accordance with Minecraft's terms of service.

Full legal details: see the [LICENSE](LICENSE).

---

## Acknowledgements

GlowberryClient is an independent project that stands on the shoulders of the open-source Android and Minecraft modding ecosystems.

- **Chimera Team** – For designing, building, and maintaining GlowberryClient and providing infrastructure support.
- **Open-Source Bedrock Modding Community** – For the shared knowledge, tooling conventions, and ecosystem research that inform how native modules are packaged, loaded, and kept safe.
- **Android Community** – For the documentation, libraries, and tools that made this launcher possible.
- **Open Source Community** – For the libraries, frameworks, and tools that power this project.
- **Contributors** – Thank you to everyone on the [contributors graph](https://github.com/ChimeraAnt-DEV/GlowberryClient/graphs/contributors).

---

## Contact & Support

| | |
|---|---|
| **Author / Team** | Chimera Team |
| **Repository** | [github.com/ChimeraAnt-DEV/GlowberryClient](https://github.com/ChimeraAnt-DEV/GlowberryClient) |
| **Report an issue** | [GitHub Issues](https://github.com/ChimeraAnt-DEV/GlowberryClient/issues) |
| **Support & questions** | Open an issue on the repository |

---

<div align="center">

<a href="https://github.com/ChimeraAnt-DEV/GlowberryClient/releases/latest"><img src="https://img.shields.io/badge/⬇%20DOWNLOAD-LATEST%20RELEASE-7E32AA?style=for-the-badge&logo=github&logoColor=white" alt="Download"></a>
<a href="docs/guide/getting-started.md"><img src="https://img.shields.io/badge/📖%20READ-THE%20DOCS-0F5BE4?style=for-the-badge&logo=readthedocs&logoColor=white" alt="Docs"></a>
<a href="https://github.com/ChimeraAnt-DEV/GlowberryClient/issues/new"><img src="https://img.shields.io/badge/🐛%20REPORT-A%20BUG-FF5C5C?style=for-the-badge&logo=github&logoColor=white" alt="Report a bug"></a>

<img src="docs/assets/glowberry_mark.png" alt="GlowberryClient" width="72">

**Made with ❤️ by the Chimera Team Community**

<sub>All rights reserved.</sub>
<sub>This account is shared among the 20 members of the Official ChimeraTeam.</sub>

</div>

