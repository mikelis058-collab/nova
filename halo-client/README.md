# Halo Client (Fabric 1.21.11)

Black-and-white glass HUD and click GUI. **Open the menu with Right Shift.**

## Modules
- Combat: TriggerBot, AimHelper, ShieldStatus
- Visual: Xray, Fog, Fullbright, Zoom (hold C), Knives, Hats, Atmosphere
- Movement: ToggleSprint

Every module has its own settings and keybind (click the Keybind button, press a key; Backspace or right-click clears it).
Settings are saved to `config/halo.json`.


## Not included
Glossy Rain and real shaders: those need custom shader/render-pipeline work that I could not do safely for 1.21.11.
Atmosphere is a sky + horizon tint with presets, not a shader. For wet/glossy rain use Iris with a shader pack.

## Build
Needs JDK 21. Run `gradle build` (Gradle 8.14+) or push to GitHub (the workflow builds the jar).
Put the jar and Fabric API 0.141.2+1.21.11 in your mods folder.

## If something does not build or work
Written against the 1.21.11 Yarn docs, not compiled in my sandbox.
- Build error inside `HatFeature.java`: set `enable_hats=false` in `gradle.properties`.
- Zoom / Xray / Fog / Atmosphere / ShieldStatus do nothing: they are mixins; check `logs/latest.log` for "Mixin" lines.
- Knives toggling reloads resource packs (short freeze is normal).
