# Visual Rust engine

Base: horizon-rust/horizon v0.2.97, commit a3acb5f0d2f59d2d5ff21a5aa000809ba1e35e2f.
The MIT copyright notice is retained in UPSTREAM-LICENSE.txt.

visual-client.patch introduces a separate Cargo feature `visual-client`: Visual
window title, custom title screen layout and main-menu surfaces. It does not
rename upstream crate APIs or alter gameplay/world formats.

Build from the launcher: `node scripts/build-visual-rust.mjs`.
Source checkout and Cargo output stay outside the synchronized project folder.
No Minecraft assets or prepared resources belong in this directory.

This is an initial Visual UI fork, not a full port of Java cosmetics/mods.

## 2026-11-04 Windows validation

Own `visual-client` build compiled and opened successfully. The real window showed
the Visual title screen and V icon. A bounded 6000-frame menu smoke test reported
no error. This does not validate all menus, online servers or long gameplay.

Two single 12-second forest benchmark runs at 1920x1080 on an RTX 5070 Laptop:

| LOD setting | Average FPS | 1% low FPS | Maximum frame time |
| --- | ---: | ---: | ---: |
| 0 | 893.527 | 279.934 | 9.784 ms |
| 256, reduced detail | 866.409 | 172.138 | 123.074 ms |

Both runs exited successfully. These are short, uncontrolled single samples, not
proof of a fully populated 256-chunk horizon or a cost-free view-distance increase.
The ordinary render/simulation distance in both reports was 12 chunks. Existing
profile settings are preserved; 256 LOD chunks is only the new-profile preset.

Visual builds now use an experimental maximum of 10,000 LOD chunks in the graphics menu,
settings parser and runtime clamp. This is distant, simplified terrain in local
worlds, not 10,000 chunks of full-detail rendering or simulation. Integrated GPUs
(other than the existing Apple exception) still cap the effective distance at 128.
The default for new launcher profiles stays 256; players can choose the maximum.
