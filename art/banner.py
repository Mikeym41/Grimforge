"""Renders the README banner: docs/images/banner.png (1280x360).

Run from the repo root:  python3 art/banner.py
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import art  # noqa: E402

W, H = 1280, 360
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def pixel_text(text, cell, fill_top, fill_bottom, outline):
    """Blocky pixel-art lettering: render small, threshold, scale up with hard edges."""
    font = ImageFont.truetype(FONT_BOLD, 22)
    probe = ImageDraw.Draw(Image.new("L", (1, 1)))
    left, top, right, bottom = probe.textbbox((0, 0), text, font=font)
    w, h = right - left + 4, bottom - top + 4
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).text((2 - left, 2 - top), text, font=font, fill=255)
    mask = mask.point(lambda v: 255 if v > 110 else 0)
    px = mask.load()
    out = Image.new("RGBA", (w * cell, h * cell), (0, 0, 0, 0))
    d = ImageDraw.Draw(out)
    solid = {(x, y) for y in range(h) for x in range(w) if px[x, y]}
    for (x, y) in list(solid):  # one-pixel outline
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1)):
            n = (x + dx, y + dy)
            if n not in solid and 0 <= n[0] < w and 0 <= n[1] < h:
                d.rectangle([n[0] * cell, n[1] * cell, n[0] * cell + cell - 1, n[1] * cell + cell - 1], fill=outline)
    for (x, y) in solid:
        t = y / max(1, h - 1)
        col = tuple(round(fill_top[i] + (fill_bottom[i] - fill_top[i]) * t) for i in range(3))
        d.rectangle([x * cell, y * cell, x * cell + cell - 1, y * cell + cell - 1], fill=col + (255,))
    return out


def main(out_path):
    img = Image.new("RGBA", (W, H), (18, 14, 22, 255))

    # Ember glow behind the swords and a fainter one behind the title.
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g = ImageDraw.Draw(glow)
    g.ellipse([800, -40, 1300, 420], fill=(255, 90, 30, 120))
    g.ellipse([40, 40, 640, 330], fill=(160, 60, 200, 45))
    img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(90)))

    # Drifting ember specks.
    d = ImageDraw.Draw(img)
    for x, y, s, a in ((700, 60, 6, 180), (1220, 40, 6, 150), (760, 300, 6, 120), (1180, 310, 6, 170),
                       (660, 200, 6, 90), (1240, 180, 6, 140), (980, 26, 6, 160), (900, 334, 6, 110)):
        d.rectangle([x, y, x + s - 1, y + s - 1], fill=(255, 150, 60, a))

    # Title.
    title = pixel_text("GRIMFORGE", 5, (255, 214, 120), (232, 96, 28), (40, 14, 10, 255))
    img.alpha_composite(title, (56, 84))
    d.text((60, 84 + title.height + 22), "Leveling tools  ·  Evolving swords  ·  Diablo-style runes",
           font=ImageFont.truetype(FONT, 24), fill=(226, 220, 232))
    d.text((60, 84 + title.height + 64), "A Paper plugin for PvP and factions servers", font=ImageFont.truetype(FONT, 20),
           fill=(160, 152, 172))

    # The three sword stages, growing in size and glow.
    for i, (material, stage, scale) in enumerate((("iron", "tempered", 8), ("diamond", "runed", 9),
                                                  ("netherite", "infernal", 11))):
        s = art.draw_sword(material, stage).resize((16 * scale, 16 * scale), Image.NEAREST)
        x = 830 + i * 135 - (scale - 8) * 8
        y = H // 2 - s.height // 2 + (10 - i * 6)
        img.alpha_composite(s, (x, y))

    # A few runes around the swords.
    for rune, tier, x, y in (("ember", "legendary", 1196, 28), ("frost", "rare", 800, 268),
                             ("storm", "magic", 1196, 266)):
        r = art.draw_rune(rune, tier).resize((64, 64), Image.NEAREST)
        img.alpha_composite(r, (x, y))

    img.convert("RGB").save(out_path, optimize=True)
    print("ok", out_path)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "docs/images/banner.png")
