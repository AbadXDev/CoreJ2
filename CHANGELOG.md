# Changelog

All notable changes to CoreJ2, organized by epic.

## [Unreleased]

## [1.0] — 2026-11-27

### Runtime

- Embedded OpenJDK Mobile (Zero) statically linked; `JNI_CreateJavaVM` on device (linker dead-strip fix + reproducible OpenJDK patches in `patches/openjdk-mobile/`)
- **Clean-room MIDP 2.0 / CLDC / MMAPI / M3G** written from the public specifications — the shipped classpath is pure (280 classes; **0 FreeJ2ME classes in the app**; `Vendor/FreeJ2ME` kept as reference only)
- Pure-Java graphics (no AWT on iOS): framebuffer images, PNG/BMP decoding, bitmap fonts, software M3G renderer
- In-process game switching (single VM, session swap) and frame pacing (skip unchanged frames)

### Compatibility

- 10 titles device-validated on iPhone XS Max (iOS 18.7.1): Miami Nights (EN/ES), Gangstar 2, Bounce Tales, Real Football 2011, Prince of Persia, Assassin's Creed, Diamond Rush, Asphalt 3D (M3G), plus the vibration test MIDlet
- Device profiles 96×65 – 480×800 with auto-detection (manifest, filename, brand hints)

### App

- On-screen controls (D-pad, soft keys, numeric keypad, touch pointer) with key repeat
- Native audio (MIDI/WAV via AVAudio bridge), vibration via haptics
- Settings (theme, keypad, vibration), per-game RMS savedata management, library dedupe + delete

### Branding & license

- New app icon (pixel candybar phone with glowing "C2" LCD); full rename to CoreJ2 (`dev.abadx.corej2`)
- License: free for personal, non-commercial use; attribution required (see `LICENSE`)

## [0.9.0-alpha] — 2026-08-06

### Documentation

- Public project refresh (DOCS-US004): README, GitHub Pages, screenshots, GIFs, and gameplay videos
- Position CoreJ2 as a device-validated commercial J2ME runner (Miami Nights reference title)
- Status table, architecture stack, and roadmap split (completed / current / future)

### Media

- Device screenshots under `docs/assets/screenshots/`
- Optimized gameplay GIFs under `docs/assets/gifs/`
- Full MP4 recordings under `docs/assets/videos/`

## Epic 12 — Compatibility Program (in progress)

- Corpus expansion beyond canary titles
- Public compatibility.json / CSV as source of truth
- Statistics charts generated from validation data

## Epic 11 — Device Gameplay, Lifecycle & RMS

- E11-US001 continuous gameplay validation on physical iPhone
- E11-US002 PlatformBootstrap relaunch recovery
- E11-US003 sandboxed RMS `dataPath` (Gryzzles / Ubertris)

## Epic 10 — Font, ImageIO & RunJar Hardening

- FontManager / fontconfig bring-up
- ImageIO sandbox paths
- JPEG decode path
- RunJar failure diagnostics

## Epic 9 — Embedded AWT Runtime

- Enough `java.awt.image` / graphics natives for FreeJ2ME LCD buffers
- First LCD frame path on iOS

## Epic 8 — JNI Gateway Throwable Path

- Safe Java throwable propagation through JNIGateway

## Epic 7 — OpenJDK Java Home / JNI Bring-up

- Correct `java.home` / boot classpath
- Successful `JNI_CreateJavaVM` on device

## Epic 6 — Embedded OpenJDK Mobile + FreeJ2ME

- Replace native JVM stubs
- Embedded FreeJ2ME classpath
- Real MIDlet smoke, stability, exit containment, painter safety

## Epic 5 — Persistent Runtime

- Persistent JVM / session design and migration notes

## Epic 4 — Renderer / Emulator Surface

- LCD surface metadata and SwiftUI frame presentation

## Epic 3 — Emulator Bridge & FreeJ2ME Host

- Bridge protocols, RuntimeHost, vendor FreeJ2ME submodule
- Bootstrap through MIDlet startup (`v0.6-midlet-startup`)

## Epic 2 — Import Engine

- Manifest → Hash → DuplicateDetection → Artwork (`v0.2-import-engine`)

## Epic 1 — Game Library

- SwiftData library and empty-state UX

## Documentation site (DOCS-US001)

- Initial MkDocs Material site and GitHub Pages workflow
