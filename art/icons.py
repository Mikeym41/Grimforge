"""Renders the small README icons into docs/images/icons/ (32x32, pixel-perfect 2x scale).

Run from the repo root:  python3 art/icons.py
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import art  # noqa: E402

OUT = "docs/images/icons"
SIZE = 32


def save(img, name):
    img.resize((SIZE, SIZE), Image.NEAREST).save(os.path.join(OUT, name + ".png"), optimize=True)


def main():
    os.makedirs(OUT, exist_ok=True)
    for rune in art.RUNE_ORDER:
        save(art.draw_rune(rune, "legendary"), "rune-" + rune.replace("_", "-"))
    for tier in art.TIER_ORDER:
        save(art.draw_rune("ember", tier), "tier-" + tier)
    for material, stage in (("iron", "tempered"), ("diamond", "runed"), ("netherite", "infernal")):
        save(art.draw_sword(material, stage), "sword-" + stage)
    print("ok", len(os.listdir(OUT)), "icons")


if __name__ == "__main__":
    main()
