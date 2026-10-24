# Getting Started

This page walks through the launcher path: install GlowberryClient, prepare the official Minecraft app, select a version, and start the game.

## Requirements

- Android 9.0 or later.
- ARM64 device.
- A legitimate Minecraft Bedrock Edition APK (version 1.21.80 or newer) to import.
- Enough free storage for the launcher, Minecraft data, worlds, packs, and backups.

GlowberryClient is not a replacement for owning Minecraft. It works with your own licensed copy of the game and is not affiliated with Mojang or Microsoft.

## Install GlowberryClient

1. Open the [Releases page](https://github.com/ChimeraAnt-DEV/GlowberryClient/releases).
2. Download the latest APK.
3. Allow installation from the browser or file manager you used to download the APK.
4. Install the APK and open GlowberryClient.
5. Accept the launcher agreement and grant the permissions requested by the app.

## Prepare Minecraft

GlowberryClient runs the game from a Minecraft APK that you import, not from an installed copy. Import your own licensed Bedrock APK (1.21.80 or newer) from the Library screen; the launcher reads the game's native libraries out of that APK and runs them.

A Minecraft app installed from Google Play cannot be launched by GlowberryClient. The game keeps its native library inside the installed app, where this launcher cannot reach it, so an installed Play Store copy is reported as "This is either a Google Play version of Minecraft or a version missing a specific file needed to launch the game". You do not have to remove it — importing an APK is enough.

If the launcher reports that Minecraft is missing, import a supported APK from the Library rather than installing or updating the Play Store copy.

## Choose and Launch a Version

1. Open GlowberryClient.
2. Go to the version or instance screen.
3. Select the Minecraft version you want to run.
4. Enable version isolation when launching an imported version that should keep its data separate.
5. Return to the launch screen and press Launch.

When launch fails, open the launch log from the failure screen and check the reported reason before changing settings.

## Next Steps

- Read [Features](/guide/features) for version isolation, accounts, content management, and Quick Launch.
- Read [Compatibility](/guide/compatibility) before testing another device or Minecraft version.
- Developers can continue with [Native Mod Quick Start](/guide/developer).
