#!/usr/bin/env python3
"""Sync compatibility/ into docs/ and regenerate matrix + statistics pages."""

from __future__ import annotations

import csv
import json
import shutil
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "compatibility"
DST = ROOT / "docs" / "compatibility"
REPO = "AbadXDev/CoreJ2"


def emoji_for(status: str, legend: dict[str, str]) -> str:
    return legend.get(status, "⚪")


def pct(n: int, d: int) -> float:
    return 0.0 if d == 0 else round(100.0 * n / d, 1)


def mermaid_pie(title: str, counts: Counter) -> str:
    items = [(k, v) for k, v in counts.items() if v > 0]
    if not items:
        items = [("None", 1)]
    lines = ["```mermaid", "pie showData", f"    title {title}"]
    for key, value in sorted(items, key=lambda kv: (-kv[1], str(kv[0]))):
        safe = str(key).replace('"', "'")
        lines.append(f'    "{safe}" : {value}')
    lines.append("```")
    return "\n".join(lines)


def main() -> None:
    data = json.loads((SRC / "compatibility.json").read_text())
    games = data["games"]
    validation = data["validation"]
    status_emoji = data.get("status_emoji", {})

    DST.mkdir(parents=True, exist_ok=True)
    (DST / "games").mkdir(parents=True, exist_ok=True)
    shutil.copy2(SRC / "compatibility.json", DST / "compatibility.json")

    fields = [
        "title",
        "vendor",
        "midp",
        "genre",
        "resolution",
        "input",
        "status",
        "fps",
        "rms",
        "touch",
        "keypad",
        "audio",
        "notes",
        "jar",
        "sources",
        "launch_success",
    ]
    with (SRC / "compatibility.csv").open("w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        for g in games:
            writer.writerow(
                {
                    "title": g["title"],
                    "vendor": g.get("vendor", ""),
                    "midp": g.get("midp", ""),
                    "genre": g.get("genre", ""),
                    "resolution": g.get("resolution", ""),
                    "input": g.get("input", ""),
                    "status": g["status"],
                    "fps": "" if g.get("fps") is None else g["fps"],
                    "rms": g["rms"],
                    "touch": g["touch"],
                    "keypad": g["keypad"],
                    "audio": g["audio"],
                    "notes": g["notes"],
                    "jar": g["jar"],
                    "sources": ";".join(g.get("sources", [])),
                    "launch_success": g.get("launch_success", ""),
                }
            )
    shutil.copy2(SRC / "compatibility.csv", DST / "compatibility.csv")

    for game in (SRC / "games").glob("*.md"):
        shutil.copy2(game, DST / "games" / game.name)

    rows: list[str] = []
    for g in games:
        fps = "—" if g.get("fps") is None else f'{g["fps"]:.2f}'
        title = g["title"]
        status = g["status"]
        css = {
            "Perfect": "status-perfect",
            "Playable": "status-playable",
            "Partial": "status-partial",
            "Launch only": "status-launch",
            "Not working": "status-broken",
        }.get(status, "status-playable")
        badge = f'<span class="{css}">{emoji_for(status, status_emoji)} {status}</span>'
        page = SRC / "games" / f"{title}.md"
        label = f"[{title}](compatibility/games/{title}.md)" if page.exists() else title
        res = g.get("resolution", "")
        if res:
            suffix = res.replace("x", "×")
            if "Touch" in g.get("input", ""):
                suffix += " · touch"
            label = f"{label} ({suffix})"
        rows.append(
            f"| {label} | {g.get('vendor', '—')} | "
            f"{g.get('midp', '—')} | {badge} | {fps} | {g['touch']} | {g['keypad']} | "
            f"{g['rms']} | {g['audio']} | {g['notes']} |"
        )

    total = len(games)
    tested_games = [g for g in games if g["status"] != "Untested"]
    tested = len(tested_games)
    launch_ok = sum(1 for g in tested_games if g.get("launch_success"))
    playable = sum(1 for g in games if g["status"] in {"Perfect", "Playable"})
    partial = sum(1 for g in games if g["status"] == "Partial")
    launch_only = sum(1 for g in games if g["status"] == "Launch only")
    broken = sum(1 for g in games if g["status"] == "Not working")

    status_counts = Counter(g["status"] for g in games)
    vendor_counts = Counter(g.get("vendor", "Unknown") for g in games)
    genre_counts = Counter(g.get("genre", "Unknown") for g in games)
    midp_counts = Counter(g.get("midp", "Unknown") for g in games)
    launch_counts = Counter(
        {
            "Launch OK": launch_ok,
            "Launch fail": tested - launch_ok,
        }
    )
    outcome_counts = Counter(
        {
            "Playable+": playable,
            "Partial": partial,
            "Launch only": launch_only,
            "Not working": broken,
        }
    )

    per_game = sorted((SRC / "games").glob("*.md"))
    per_game_section = ""
    if per_game:
        links = "\n".join(f"- [{p.stem}](compatibility/games/{p.name})" for p in per_game)
        per_game_section = f"\n## Per-game reports\n\n{links}\n"

    compat_page = f"""# Compatibility

CoreJ2 tracks compatibility for the **50 most famous J2ME games** from **device validation reports**.
Titles marked Untested are on the target list and have not been measured yet.
This page is generated from [`compatibility/compatibility.json`](https://github.com/{REPO}/blob/main/compatibility/compatibility.json) — do not hand-edit the matrix.

**Downloads:** [JSON](compatibility/compatibility.json) · [CSV](compatibility/compatibility.csv)

**Validation baseline:** {validation['date']} · {validation['device']} · iOS {validation['ios']}  
**Runtime:** {validation['runtime']}  
**Primary reports:** Phase 3 device matrix · XS Max playtest

## Snapshot

| Metric | Value |
|--------|-------|
| Titles tracked (Top 50) | {total} |
| Tested on device | {tested}/{total} |
| Launch success (tested) | {launch_ok}/{tested} ({pct(launch_ok, tested)}%) |
| Playable or better (tested) | {playable}/{tested} ({pct(playable, tested)}%) |
| Compatibility (playable+ of tested) | **{pct(playable, tested)}%** |

See also: [Compatibility statistics](compatibility-stats.md) · [Performance](performance.md)

## Corpus matrix

| Game | Vendor | MIDP | Status | FPS | Touch | Keypad | RMS | Audio | Notes |
|------|--------|------|--------|-----|-------|--------|-----|-------|-------|
{chr(10).join(rows)}

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
{per_game_section}"""
    (ROOT / "docs" / "compatibility.md").write_text(compat_page)

    stats_page = f"""# Compatibility Statistics

Auto-generated from `compatibility/compatibility.json`. Re-run `scripts/docs/sync_compatibility.py` after editing the JSON.

## Headline rates

| Metric | Count | Rate |
|--------|------:|-----:|
| Titles tracked | {total} | 100% |
| Tested | {tested} | {pct(tested, total)}% |
| Launch success (tested) | {launch_ok} | {pct(launch_ok, tested)}% |
| Playable+ (tested) | {playable} | {pct(playable, tested)}% |
| Partial (tested) | {partial} | {pct(partial, tested)}% |
| Launch only (tested) | {launch_only} | {pct(launch_only, tested)}% |
| Not working (tested) | {broken} | {pct(broken, tested)}% |

{mermaid_pie("Compatibility outcome", outcome_counts)}

## Launch success

{mermaid_pie("Launch success", launch_counts)}

## Status distribution

{mermaid_pie("Status", status_counts)}

## Vendor comparison

{mermaid_pie("Vendor", vendor_counts)}

## Genre comparison

{mermaid_pie("Genre", genre_counts)}

## MIDP comparison

{mermaid_pie("MIDP profile", midp_counts)}

## Notes

Charts reflect the **Top 50 target list**. Rates update automatically as titles are tested and `compatibility.json` is updated.
"""
    (ROOT / "docs" / "compatibility-stats.md").write_text(stats_page)
    print(f"synced {total} games → docs/compatibility/ + stats")


if __name__ == "__main__":
    main()
