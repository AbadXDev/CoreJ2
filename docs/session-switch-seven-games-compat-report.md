# Session switch + 7 Spanish JARs — XS Max report

**Device:** iPhone XS Max `00008020-001645C20221002E` (America/Santo_Domingo)  
**Bundle:** `dev.abadx.corej2`  
**Vendor/FreeJ2ME:** intact (no Vendor edits)  
**GitHub push:** none

---

## A) BUG — «un inicio por iniciada»

### Root cause (real UI path)

Not HotSpot recreate. After Library → play → **back** → tap another game:

1. `dismissEmulator()` nils the `EmulatorViewModel` while `stopMidlet` is still in flight (fixed earlier via bridge-owned `enqueueStop` + `awaitPendingStop`).
2. Remaining failure: `NavigationStack` destination churn after back. A second push that arrived while the previous destination was still popping cancelled `EmulatorView.task` mid-`startSession`. Cancellation was silent → UI did nothing.

### Fix

| Change | Why |
|--------|-----|
| `DefaultEmulatorBridge` awaits pending stop / clears stale session | Survives VM teardown on back |
| `RuntimeEventPipe` multi-subscriber | New VM can observe frames |
| `selectGame` delays ~450 ms after a recent dismiss | Lets NavigationStack finish popping |
| `EmulatorView.onDisappear` only `endSession` if `activeSession != nil` | Avoids killing an in-flight launch |
| Cancellation / launch errors set `launchErrorMessage` | UI shows an alert instead of hanging |

### Verification (XS Max)

| Test | Result |
|------|--------|
| `ui-switch-test` Miami EN → ES (UI back simulation) | `startSession OK` ×2, both `lcdNonZeroPixels=76800` |
| Cross-title: Gangstar 2 → Bounce Tales (same process) | `startSession OK` ×2, both paint; `reusing live platform` |
| Spurious `onDisappear` during second push | Logged `ignored (no active session)` |

N games in one process work without force-quitting the app.

---

## B) PEDIDO — 7 JARs (`games/`)

| JAR | Profile | lcdNonZero | Audio | Verdict |
|-----|---------|------------|-------|---------|
| Gangstar-2-Kings-of-LA_ES_240x320.jar | 240×320 | 76800 | OK path | **Playable** |
| Asphalt-3-Street-Rules-3D_ES_240x320.jar | 240×320 | **0** | — | **Blocked — M3G** |
| Diamond-Rush_ES_240x400.jar | 240×400 | 92292 | — | **Playable** (ads `Connector.open` stubbed) |
| Bounce-Tales_ES_Multi.jar | 240×320 | 76800 | MIDI create/start OK | **Playable** |
| Real-Football-2011_ES_240x400.jar | 240×400 | 25000 | — | **Playable** (BMP assets) |
| Prince-of-Persia-Las-Arenas-Olvidadas_ES_640x360.jar | 640×360 | 230400 | MIDI OK (non-fatal bank warnings possible) | **Playable** (landscape touch) |
| Assassins-Creed_ES_640x360.jar | 640×360 | 230400 | — | **Playable** (landscape touch) |

**Starts that paint: 6 / 7.** Asphalt starts the MIDlet but the software LCD stays black.

### Fixes applied (overlay / app only)

1. **DeviceProfileDetector** — brand-only detection prefers 240×320 (no accidental Nokia 96×65 for Multi JARs).
2. **Legacy profile repair** — library entries stuck on `nokia-s40-96x65` upgraded to default 240×320 on `loadGames`.
3. **PngDecoder** — palette PNG bit depths 1/2/4 (Bounce Tales).
4. **Connector.open** overlay stubs — Diamond Rush no longer `NoSuchMethodError` on HTTP probe.
5. **BmpDecoder** + `PlatformImage` — Windows BMP (8/24-bit BI_RGB) for Real Football `/l1` etc.; failed loads use 1×1 placeholder (no NPE).
6. Catalog already includes **640×360** / **240×400** profiles.

### Documented blocker

**Asphalt 3 Street Rules 3D** — depends on `javax.microedition.m3g`. FreeJ2ME’s `Graphics3D` is a **stub** (`render` / `bindTarget` no-ops). The 2D LCD painter never receives M3G frames → `lcdNonZeroPixels=0`. Full M3G (or a software rasterizer wired into our LCD path) is required; out of scope for this pass. Vendor left untouched.

---

## Rebuild / install

- `./scripts/embedded-jvm/compile-freej2me-classpath.sh` → OK (overlay applied)
- `xcodebuild` Debug → **BUILD SUCCEEDED**
- Installed on XS Max; QA flags cleared after verification

---

## Files touched (non-Vendor)

- `LibraryViewModel.swift` — dismiss settle delay, cross-switch QA, tiny-profile repair  
- `EmulatorView.swift` / `EmulatorViewModel.swift` — safer disappear + visible launch errors  
- `DeviceProfileDetector.swift` — brand → 240×320  
- Overlay: `Connector.java`, `PngDecoder.java`, `BmpDecoder.java`, `PlatformImage.java`
