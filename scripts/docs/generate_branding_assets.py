#!/usr/bin/env python3
"""Regenerate CoreJ2 branding SVG/PNG assets.

The mark is the current app icon (pixel candybar phone with a glowing
"C2" LCD) kept at assets/branding/corej2-appicon-1024.png. SVGs embed a
downscaled copy of it; rasters composite it over the brand navy.

Requires Pillow. Fonts: Inter + Space Grotesk under
assets/branding/fonts/ (gitignored) when present; otherwise falls back
to Noto Sans.
"""

from __future__ import annotations

import base64
import io
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "assets" / "branding"
ICON_MASTER = OUT / "corej2-appicon-1024.png"

PRIMARY = "#2563EB"
SECONDARY = "#60A5FA"
BG = "#0B1220"
WHITE = "#FFFFFF"
GRAY = "#94A3B8"

INTER = ROOT / "assets/branding/fonts/Inter/extras/ttf/Inter-SemiBold.ttf"
INTER_REG = ROOT / "assets/branding/fonts/Inter/extras/ttf/Inter-Regular.ttf"
INTER_BOLD = ROOT / "assets/branding/fonts/Inter/extras/ttf/Inter-Bold.ttf"
SG = ROOT / "assets/branding/fonts/SpaceGrotesk/SpaceGrotesk-2.0.0/ttf/static/SpaceGrotesk-Bold.ttf"

NOTO = Path("/usr/share/fonts/truetype/noto")
FALLBACK = {
    INTER: NOTO / "NotoSans-SemiBold.ttf",
    INTER_REG: NOTO / "NotoSans-Regular.ttf",
    INTER_BOLD: NOTO / "NotoSans-Bold.ttf",
    SG: NOTO / "NotoSans-Bold.ttf",
}


def font(path: Path, size: int) -> ImageFont.FreeTypeFont:
    if not path.is_file():
        path = FALLBACK[path]
    return ImageFont.truetype(str(path), size)


def icon_image(size: int) -> Image.Image:
    """Master icon resized, with rounded-corner alpha."""
    img = Image.open(ICON_MASTER).convert("RGBA").resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, size, size], radius=int(size * 0.22), fill=255
    )
    img.putalpha(mask)
    return img


def icon_b64(size: int = 256) -> str:
    buf = io.BytesIO()
    icon_image(size).save(buf, "PNG", optimize=True)
    return base64.b64encode(buf.getvalue()).decode()


def isotype_svg(label: str = "CoreJ2 isotype") -> str:
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 128" role="img" aria-label="{label}">
  <image href="data:image/png;base64,{icon_b64(256)}" x="0" y="0" width="128" height="128"/>
</svg>
"""


def logo_svg(bg: str | None, word_fill: str, tag_fill: str) -> str:
    bg_rect = f'<rect width="512" height="160" rx="24" fill="{bg}"/>' if bg else ""
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 160" role="img" aria-label="CoreJ2">
  {bg_rect}
  <image href="data:image/png;base64,{icon_b64(256)}" x="20" y="16" width="128" height="128"/>
  <text x="168" y="92" font-family="Space Grotesk, Inter, Helvetica, Arial, sans-serif" font-size="64" font-weight="700" fill="{word_fill}">CoreJ2</text>
  <text x="172" y="124" font-family="Inter, Helvetica, Arial, sans-serif" font-size="16" font-weight="500" fill="{tag_fill}" letter-spacing="0.08em">JAVA ME ON IPHONE & IPAD</text>
</svg>
"""


def paste_icon(img: Image.Image, size: int, xy: tuple[int, int]) -> None:
    img.alpha_composite(icon_image(size), xy)


def glow_bg(w: int, h: int, ellipses: list[tuple[tuple, tuple]]) -> Image.Image:
    img = Image.new("RGBA", (w, h), BG)
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    for box, fill in ellipses:
        gd.ellipse(box, fill=fill)
    return Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(70)))


def render_app_icon(path: Path) -> None:
    Image.open(ICON_MASTER).convert("RGBA").save(path, "PNG")


def render_splash(path: Path, w: int = 1284, h: int = 2778) -> None:
    img = glow_bg(w, h, [((w * 0.1, h * 0.18, w * 0.9, h * 0.55), (37, 99, 235, 55))])
    size = int(w * 0.44)
    ox = (w - size) // 2
    oy = int(h * 0.30)
    paste_icon(img, size, (ox, oy))
    draw = ImageDraw.Draw(img)
    title = font(SG, int(w * 0.07))
    tag = font(INTER_REG, int(w * 0.028))
    tw = draw.textbbox((0, 0), "CoreJ2", font=title)
    draw.text(((w - (tw[2] - tw[0])) / 2, oy + size + h * 0.04), "CoreJ2", font=title, fill=WHITE)
    tagline = "Java ME on iPhone & iPad"
    tb = draw.textbbox((0, 0), tagline, font=tag)
    draw.text(((w - (tb[2] - tb[0])) / 2, oy + size + h * 0.09), tagline, font=tag, fill=GRAY)
    img.convert("RGB").save(path, "PNG")


def render_banner(path: Path, w: int = 1280, h: int = 640) -> None:
    img = glow_bg(
        w,
        h,
        [((40, 60, 560, 580), (37, 99, 235, 70)), ((w - 480, h - 360, w + 80, h + 80), (96, 165, 250, 40))],
    )
    paste_icon(img, 430, (105, (h - 430) // 2))
    draw = ImageDraw.Draw(img)
    x = 600
    draw.text((x, 140), "CoreJ2", font=font(SG, 96), fill=WHITE)
    draw.text(
        (x, 262),
        "Run classic Java ME games on iPhone & iPad",
        font=font(INTER_BOLD, 29),
        fill=SECONDARY,
    )
    y = 335
    for line in ("Run classic MIDlets using", "Embedded OpenJDK Mobile", "and a clean-room MIDP."):
        draw.text((x, y), line, font=font(INTER_REG, 27), fill=GRAY)
        y += 42
    draw.rounded_rectangle([x, 478, x + 180, 485], radius=3, fill=PRIMARY)
    img.convert("RGB").save(path, "PNG")


def render_social(path: Path, w: int = 1200, h: int = 630) -> None:
    img = glow_bg(w, h, [((w * 0.4, -100, w * 1.1, h * 0.8), (37, 99, 235, 50))])
    paste_icon(img, 400, (90, (h - 400) // 2))
    draw = ImageDraw.Draw(img)
    x = 550
    draw.text((x, 170), "CoreJ2", font=font(SG, 84), fill=WHITE)
    draw.text(
        (x, 278),
        "Run classic Java ME games on iPhone & iPad",
        font=font(INTER_BOLD, 26),
        fill=SECONDARY,
    )
    draw.text((x, 340), "Run classic MIDlets using Embedded OpenJDK Mobile", font=font(INTER_REG, 22), fill=GRAY)
    draw.text((x, 378), "and a clean-room MIDP.", font=font(INTER_REG, 22), fill=GRAY)
    draw.rounded_rectangle([x, 450, x + 160, 458], radius=4, fill=PRIMARY)
    img.convert("RGB").save(path, "PNG")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "isotype.svg").write_text(isotype_svg())
    (OUT / "favicon.svg").write_text(isotype_svg())
    (OUT / "logo-square.svg").write_text(isotype_svg("CoreJ2 square logo"))
    (OUT / "logo.svg").write_text(logo_svg(None, PRIMARY, GRAY))
    (OUT / "logo-dark.svg").write_text(logo_svg(BG, WHITE, GRAY))
    (OUT / "logo-light.svg").write_text(logo_svg(WHITE, BG, "#64748B"))

    render_app_icon(OUT / "app-icon-concept.png")
    render_splash(OUT / "splash.png")
    render_banner(OUT / "banner-github.png")
    render_social(OUT / "social-preview.png")

    docs = ROOT / "docs" / "assets" / "branding"
    docs.mkdir(parents=True, exist_ok=True)
    for p in OUT.glob("*"):
        if p.suffix.lower() in {".svg", ".png"}:
            (docs / p.name).write_bytes(p.read_bytes())
    # docs/assets/logo.svg is a standalone copy of the isotype
    (ROOT / "docs" / "assets" / "logo.svg").write_text(isotype_svg())
    print(f"branding assets written → {OUT} and mirrored to {docs}")


if __name__ == "__main__":
    main()
