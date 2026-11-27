# Compatibility

CoreJ2 tracks compatibility for the **50 most famous J2ME games** from **device validation reports**.
Titles marked Untested are on the target list and have not been measured yet.
This page is generated from [`compatibility/compatibility.json`](https://github.com/AbadXDev/CoreJ2/blob/main/compatibility/compatibility.json) — do not hand-edit the matrix.

**Downloads:** [JSON](compatibility/compatibility.json) · [CSV](compatibility/compatibility.csv)

**Validation baseline:** 2026-11-27 · iPhone XS Max · iOS 18.7.1  
**Runtime:** Production app + embedded OpenJDK Mobile + clean-room MIDP (Phase 3)  
**Primary reports:** Phase 3 device matrix · XS Max playtest

## Snapshot

| Metric | Value |
|--------|-------|
| Titles tracked (Top 50) | 50 |
| Tested on device | 8/50 |
| Launch success (tested) | 8/8 (100.0%) |
| Playable or better (tested) | 7/8 (87.5%) |
| Compatibility (playable+ of tested) | **87.5%** |

See also: [Compatibility statistics](compatibility-stats.md) · [Performance](performance.md)

## Corpus matrix

| Game | Vendor | MIDP | Status | FPS | Touch | Keypad | RMS | Audio | Notes |
|------|--------|------|--------|-----|-------|--------|-----|-------|-------|
| Miami Nights: Singles in the City | Gameloft | MIDP 2.0 | <span class="status-playable">🟢 Playable</span> | — | Pass | Pass | Pass | Pass | EN v1.0.8 + ES-ML v1.0.3 multilanguage builds; audio + RMS OK on XS Max. |
| Gangstar 2: Kings of L.A. | Gameloft | MIDP 2.0 | <span class="status-playable">🟢 Playable</span> | — | Pass | Pass | Pass | Pass | Full pass incl. RMS (Phase 3 matrix). |
| Asphalt 3: Street Rules 3D | Gameloft | MIDP 2.0 | <span class="status-partial">🟡 Partial</span> | — | Pass | Pass | Untested | Untested | M3G renders and the car is visible; textures still broken in real gameplay — fix in progress. |
| Diamond Rush | Gameloft | MIDP 2.0 | <span class="status-playable">🟢 Playable</span> | — | Pass | Pass | Pass | Pass | Freeze after first tap fixed (RMS enumeration); confirmed playable on XS Max. |
| Bounce Tales | Nokia | MIDP 2.0 | <span class="status-playable">🟢 Playable</span> | — | Pass | Pass | Untested | Pass | GameCanvas + MIDI audio OK. |
| Real Football 2011 | Gameloft | MIDP 2.0 | <span class="status-playable">🟢 Playable</span> | — | Pass | Pass | Pass | Untested | Full pass incl. RMS. |
| Prince of Persia: The Forgotten Sands | Gameloft | MIDP 2.0 | <span class="status-playable">🟢 Playable</span> | — | Pass | Pass | Pass | Partial | 640x360 touch build; one audio clip fails to prepare (AVAudio -10870). |
| Assassin's Creed | Gameloft | MIDP 2.0 | <span class="status-playable">🟢 Playable</span> | — | Pass | Pass | Pass | Pass | 640x360 touch build; full pass. |
| Snake III | Nokia | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Bounce | Nokia | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Tetris | EA Mobile | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Asphalt 4: Elite Racing | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Need for Speed: Most Wanted | EA Mobile | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Gangstar Rio: City of Saints | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Modern Combat 3: Fallen Nation | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| N.O.V.A. | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Hero of Sparta | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| God of War: Betrayal | Sony Pictures Mobile | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Splinter Cell: Conviction | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Assassin's Creed II | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Spider-Man: Toxic City | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| The Sims 3 | EA Mobile | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| New York Nights 2: Friends for Life | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Gravity Defied | Codebrew | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Worms 2007 | THQ | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Doom RPG | id Software / EA Mobile | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Wolfenstein RPG | id Software / EA Mobile | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Age of Empires II Mobile | In-Fusio | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Midnight Pool 3 | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Pro Evolution Soccer 2011 | Konami | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| FIFA 2011 | EA Mobile | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Ultimate Mortal Kombat 3 | EA Mobile | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Street Fighter II Turbo | Capcom | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Tekken Mobile | Namco | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Contra 4 | Konami | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Castlevania: Order of Shadows | Konami | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Sonic the Hedgehog | Sega | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Doodle Jump | Lima Sky | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Angry Birds | Rovio | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Zuma Deluxe | PopCap | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Bejeweled | PopCap | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Block Breaker Deluxe | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Brain Challenge | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Monopoly | EA Mobile | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Ferrari GT: Evolution | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Driver: L.A. Undercover | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Brothers in Arms | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Call of Duty: Black Ops | Glu Mobile | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Iron Man 2 | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |
| Avatar | Gameloft | MIDP 2.0 | <span class="status-playable">⚪ Untested</span> | — | Untested | Untested | Untested | Untested |  |

## Status legend

| Badge | Status | Meaning |
|-------|--------|---------|
| 🟢 | Perfect | Launch, continuous frames, input, and applicable subsystems clean |
| 🟢 | Playable | Launches and sustains interactive gameplay |
| 🟡 | Partial | Launches with significant limitations |
| 🟠 | Launch only | Starts but is not sustainably playable |
| 🔴 | Not working | Fails to launch or is unusable |
| ⚪ | Untested | Not measured in the referenced runs |

## How to read FPS

FPS is recorded per title when measured during device sessions. A dash (—) means it has not been measured yet.

## Audio

Audio / MMAPI is tested per title on device. Cells marked Untested have not been measured yet.
