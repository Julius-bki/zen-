# zen-gui

A vanilla Minecraft `Screen` companion mod for the [zen skyblock mod](https://github.com/meowing-cat/zen) that replaces its config screen with a working GUI.

## Why?
Zen's built-in config screen renders invisible text when Night Vision Goggles (NVG) are active. This mod replaces it entirely with a custom screen that always renders correctly.

## Features
- Catppuccin Mocha color palette
- stella-style horizontal panel layout — one panel per config category
- Horizontal panning with scroll wheel
- Per-panel vertical module list scrolling
- Toggles write directly into zen's live in-memory config map via reflection
- Config saved to disk on every toggle and on ESC
- Opens automatically when zen's own config screen is triggered, or via `/zconf`

## Status
Work in progress. **Clicking to toggle features is currently broken** on MC 1.21.11 — likely due to zen's event system not being fully updated for 1.21.11's new input record types (`class_11908`, `class_11909`). The mod is pending a full rework of zen targeting a newer supported MC version.

## Build

Requirements:
- JDK 21
- Paths in `build.ps1` pointing to your local MC/Fabric jars (already configured for the dev environment)

```powershell
powershell -ExecutionPolicy Bypass -File build.ps1
```

This compiles the sources, packages `zen-gui-1.0.jar`, and deploys it to the mods folder.

## Architecture
The compiled classes (`com.zengui.*`) are embedded directly into `zen-1.21.11-1.3.0-FINAL.jar` alongside zen's own classes, with a second client entrypoint registered in `fabric.mod.json`. No separate jar is needed at runtime.

## MC version
1.21.11 + Fabric 0.18.6 (intermediary-mapped)
