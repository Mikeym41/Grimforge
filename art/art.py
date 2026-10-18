"""Grimforge pixel art v2: evolving swords and rune stones, 16x16.

Run:  python3 art.py <pack_dir> <preview_dir>

Everything is drawn from code so the art can be tweaked and regenerated. Light comes from the top
left. Outlines are a dark shade of whatever they surround (selective outlining) rather than black.
"""
import colorsys
import json
import os
import random
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image, ImageDraw, ImageFont

# --------------------------------------------------------------------------------------------
# Colour helpers
# --------------------------------------------------------------------------------------------


def hx(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def shade(rgb, light, hue_shift=0.0, sat=1.0):
    """Darken/lighten in HLS with a small hue shift (cool shadows, warm lights)."""
    r, g, b = (v / 255 for v in rgb)
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    h = (h + hue_shift) % 1.0
    l = max(0.0, min(1.0, l * light if light < 1 else l + (1 - l) * (light - 1)))
    s = max(0.0, min(1.0, s * sat))
    r, g, b = colorsys.hls_to_rgb(h, l, s)
    return (round(r * 255), round(g * 255), round(b * 255))


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


# --------------------------------------------------------------------------------------------
# Swords
# --------------------------------------------------------------------------------------------

# blade: 5 tones dark -> highlight. guard: 4 tones. grip: 2 tones. glow: core, bright.
SWORD_MATERIALS = {
    "wooden": dict(blade=["#4a2f1a", "#6e4826", "#946535", "#b98a4e", "#dcb274"],
                   guard=["#3b2a1e", "#5e4532", "#86674b", "#a8896a"],
                   grip=["#3a2617", "#6b4a2c"], glow=["#5fd04a", "#d4ffb0"], gem=["#2f7a2a", "#9cf27e"]),
    "stone": dict(blade=["#2e2d33", "#4c4b52", "#6f6e75", "#94939a", "#bdbcc2"],
                  guard=["#2c2733", "#4a4254", "#6b6277", "#8f869c"],
                  grip=["#3a2a1e", "#6a5038"], glow=["#a46bff", "#e7d6ff"], gem=["#5a2f9e", "#c9a6ff"]),
    "iron": dict(blade=["#4b5160", "#7a8291", "#a9b0bd", "#d2d7df", "#f6f8fb"],
                 guard=["#3a3f4c", "#5d6474", "#8a92a3", "#b6bdca"],
                 grip=["#3d241f", "#6e3f33"], glow=["#5cb8ff", "#dff2ff"], gem=["#1f5fa8", "#8fd0ff"]),
    "golden": dict(blade=["#7a4a12", "#b0761e", "#dba73a", "#f3d266", "#fff4b8"],
                   guard=["#5c2c1a", "#8a4a26", "#b9773c", "#e2a85e"],
                   grip=["#4a1620", "#7d2a35"], glow=["#ff8a2a", "#ffe9a8"], gem=["#a3142a", "#ff6a7a"]),
    "diamond": dict(blade=["#13525a", "#1f8a8a", "#3fc6be", "#8cf0e4", "#e2fffb"],
                    guard=["#25304a", "#3c4d70", "#5f7598", "#8aa0c0"],
                    grip=["#241d38", "#463a66"], glow=["#56e6ff", "#e8ffff"], gem=["#1a6fb0", "#9fe8ff"]),
    "netherite": dict(blade=["#1c171c", "#2f282f", "#463d46", "#625762", "#8a7d89"],
                      guard=["#1f1a1f", "#362d36", "#504450", "#6e606e"],
                      grip=["#3b1414", "#6e2222"], glow=["#ff4a1c", "#ffd25a"], gem=["#a3140c", "#ff7a3a"]),
}
SWORD_STAGES = ["tempered", "runed", "infernal"]


def diag(x, y):
    """(along, across): along runs from tip (-15) toward pommel; across 0 = blade centre line."""
    return y - x, x + y - 15


def sword_pixels(stage):
    """Returns {(x, y): (part, tone)} for a stage. Parts: blade, guard, grip, glow, gem, ember."""
    px = {}

    def put(x, y, part, tone):
        if 0 <= x < 16 and 0 <= y < 16:
            px[(x, y)] = (part, tone)

    for y in range(16):
        for x in range(16):
            a, c = diag(x, y)

            # ---- Blade -------------------------------------------------------------------
            if stage == "tempered":
                if -15 <= a <= 1:
                    w = 0 if a == -15 else 1
                    if abs(c) <= w:
                        tone = {-1: 3, 0: 2, 1: 1}[c] if w else 4
                        put(x, y, "blade", tone)
                    # serrated back edge: small teeth on the shadow side
                    if c == 2 and a in (-11, -7, -3):
                        put(x, y, "blade", 1)
            elif stage == "runed":
                if -15 <= a <= 1:
                    if a == -15:
                        w = 0
                    elif a <= -11:
                        w = 1
                    else:
                        w = 2  # broad blade
                    if abs(c) <= w:
                        tone = {-2: 4, -1: 3, 0: 1, 1: 2, 2: 1}[c] if w == 2 else ({-1: 3, 0: 2, 1: 1}[c] if w else 4)
                        put(x, y, "blade", tone)
                        # fuller with glowing runes along the centre
                        if w == 2 and c == 0 and -9 <= a <= -1:
                            put(x, y, "glow", 1 if a in (-7, -3) else 0)
            else:  # infernal: wavy flamberge with a molten edge
                if -15 <= a <= 1:
                    if a == -15:
                        w_lo, w_hi = 0, 0
                    elif a <= -12:
                        w_lo, w_hi = 1, 1
                    else:
                        wave = 1 if (a % 4 in (0, 1)) else 0
                        w_lo, w_hi = 2, 2 + wave
                    if -w_lo <= c <= w_hi:
                        if c <= -w_lo and w_lo >= 1:
                            put(x, y, "glow", 1 if a % 3 == 0 else 0)   # molten cutting edge
                        elif c < 0:
                            put(x, y, "blade", 3)
                        elif c == 0:
                            put(x, y, "glow", 0) if (-11 <= a <= -1 and a % 4 == 1) else put(x, y, "blade", 2)
                        elif c >= 3:
                            put(x, y, "blade", 0)
                        else:
                            put(x, y, "blade", 1)

            # ---- Guard -------------------------------------------------------------------
            if stage == "tempered":
                if a in (2, 3) and abs(c) <= 3:
                    put(x, y, "guard", 3 if (a == 2 and c < 0) else (2 if a == 2 else 1))
            elif stage == "runed":
                if a in (2, 3) and abs(c) <= 4:
                    put(x, y, "guard", 3 if (a == 2 and c < 0) else (2 if a == 2 else 1))
            else:
                if a in (2, 3) and abs(c) <= 4:
                    put(x, y, "guard", 2 if a == 2 else 1)

            # ---- Grip with leather wrap --------------------------------------------------
            if 4 <= a <= 8 and abs(c) <= 1:
                put(x, y, "grip", 1 if (a + c) % 4 in (0, 1) else 0)

            # ---- Pommel ------------------------------------------------------------------
            if a == 9 and abs(c) <= 2:
                put(x, y, "guard", 2 if c < 0 else 1)
            if a == 10 and abs(c) <= 1:
                put(x, y, "guard", 1)

    # Quillon tips: Tempered droops back, Runed sweeps forward, Infernal grows horns.
    def at(a, c):
        if (c - a) % 2 == 0:
            return None
        return (c + 15 - a) // 2, (c + 15 + a) // 2

    if stage == "tempered":
        for a, c, t in ((4, -4, 1), (4, 4, 0)):
            p = at(a, c)
            if p:
                put(*p, "guard", t)
    elif stage == "runed":
        for a, c, t in ((1, -5, 3), (0, -6, 2), (1, 5, 1), (0, 6, 1)):
            p = at(a, c)
            if p:
                put(*p, "guard", t)
    else:
        for a, c, t in ((1, -5, 2), (0, -6, 2), (-1, -6, 2), (1, 5, 1), (0, 6, 1), (-1, 6, 1)):
            p = at(a, c)
            if p:
                put(*p, "guard", t)
        for a, c in ((-2, -6), (-2, 6)):
            p = at(a, c) or at(a + 1, c)
            if p:
                put(*p, "glow", 1)

    # Gems: guard centre on Runed/Infernal, pommel on all.
    if stage in ("runed", "infernal"):
        p = at(3, 0) or at(2, 0)
        if p:
            put(*p, "gem", 1)
        p2 = at(2, -1)
        if p2:
            put(*p2, "gem", 0)
    p = at(9, 0)
    if p:
        put(*p, "gem", 1 if stage != "tempered" else 0)

    # Specular glints near the tip on the lit side.
    if stage != "infernal":
        for a in (-13, -9):
            p = at(a, -1)
            if p and px.get(p, ("", 0))[0] == "blade":
                put(*p, "blade", 4)

    # Infernal: embers drifting off the molten edge (semi-transparent, outside the outline).
    if stage == "infernal":
        for a, c in ((-12, -4), (-8, -5), (-4, -5), (-10, -6), (-14, -2)):
            p = at(a, c) or at(a + 1, c)
            if p and p not in px:
                px[p] = ("ember", 0)
    if stage == "runed":
        for a, c in ((-12, -3), (-5, -5)):
            p = at(a, c) or at(a + 1, c)
            if p and p not in px:
                px[p] = ("ember", 0)
    return px


def draw_sword(material, stage):
    import swords_v3
    m = SWORD_MATERIALS[material]
    ramps = {k: [hx(c) for c in m[k]] for k in ("blade", "guard", "grip", "glow", "gem")}
    parts = swords_v3.STAGES[stage]()
    pix = {}
    for part in ("blade", "guard", "grip", "glow", "gem"):  # later parts draw over earlier ones
        for x, y, tone in parts[part]:
            if 0 <= x < 16 and 0 <= y < 16:
                pix[(x, y)] = (part, tone)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    out = img.load()

    # Selective outline: a dark shade of the neighbouring part's colour.
    for (x, y), (part, _) in pix.items():
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, y + dy)
            if n in pix or not (0 <= n[0] < 16 and 0 <= n[1] < 16):
                continue
            src = {"glow": "blade", "gem": "guard"}.get(part, part)
            col = shade(ramps[src][0], 0.5, hue_shift=0.02)
            if part == "glow":
                col = mix(col, ramps["glow"][0], 0.3)
            if out[n][3] == 0 or sum(col) < sum(out[n][:3]):
                out[n] = col + (255,)
    for (x, y), (part, tone) in pix.items():
        out[x, y] = ramps[part][min(tone, len(ramps[part]) - 1)] + (255,)
    for x, y in parts["ember"]:
        if out[x, y][3] == 0:
            out[x, y] = ramps["glow"][0] + (170,)
    return img


# --------------------------------------------------------------------------------------------
# Runes
# --------------------------------------------------------------------------------------------

RUNE_ORDER = ["ember", "frost", "venom", "storm", "bleed", "crushing_blow", "soul_harvest", "retribution"]
TIER_ORDER = ["magic", "rare", "legendary"]

# 8x8 glyphs. '#' glow, '+' bright core.
GLYPHS = {
    "ember": ["...#....", "...##...", "..#+#...", "..#+##..", ".##++#..", ".#+++##.", ".##+++#.", "..####.."],
    "frost": ["...#....", ".#.#.#..", "..###...", "###+###.", "..###...", ".#.#.#..", "...#....", "........"],
    "venom": ["...#....", "...#....", "..###...", "..#+#...", ".##+##..", ".#+++#..", ".#+++#..", "..###..."],
    "storm": ["....##..", "...##...", "..##....", ".#####..", "...+#...", "..##....", "..#.....", ".#......"],
    "bleed": [".....##.", "....##..", "...##...", "..##....", ".##.....", "#..#..#.", "...#..+.", "...+...."],
    "crushing_blow": ["######..", "#++++#..", "######..", "..##....", "..##....", "..##....", "..##....", "..##...."],
    "soul_harvest": [".#####..", "#######.", "#.+#.+#.", "#.+#.+#.", "#######.", ".##.##..", ".#####..", ".#.#.#.."],
    "retribution": ["#.#.#.#.", "#######.", "##+++##.", "##+++##.", ".##+##..", ".#####..", "..###...", "...#...."],
}
RUNE_GLOW = {
    "ember": ("#ff6a1a", "#ffd34d"), "frost": ("#6fdcff", "#ffffff"), "venom": ("#5fd13a", "#d2ff8a"),
    "storm": ("#ffe95a", "#ffffff"), "bleed": ("#e01e2c", "#ff8a8a"), "crushing_blow": ("#c9ced8", "#ffffff"),
    "soul_harvest": ("#6ff0dc", "#e2fffa"), "retribution": ("#d85cff", "#f6c4ff"),
}
RUNE_NAMES = {
    "ember": "Ember", "frost": "Frost", "venom": "Venom", "storm": "Storm", "bleed": "Bleed",
    "crushing_blow": "Crushing Blow", "soul_harvest": "Soul Harvest", "retribution": "Retribution",
}
TIER_RIM = {  # 4-tone metal ramp: shadow, mid, light, glint
    "magic": ["#1f2a8a", "#3b52d6", "#6f86ff", "#c4ceff"],
    "rare": ["#7a4e0e", "#c08a1e", "#f0c64a", "#fff1b0"],
    "legendary": ["#7a2a06", "#d15a10", "#ff9a2a", "#ffe08a"],
}
TIER_LABEL = {"magic": ("Magic", "#6969ff"), "rare": ("Rare", "#ffff55"), "legendary": ("Legendary", "#ff8000")}
STONE = ["#1d1b24", "#2b2934", "#3a3845", "#4b4957", "#5f5d6c"]


def tablet_mask():
    """A 14x14 rune tablet with chamfered corners, centred in 16x16."""
    mask = set()
    for y in range(1, 15):
        for x in range(1, 15):
            dx = min(x - 1, 14 - x)
            dy = min(y - 1, 14 - y)
            if dx + dy >= 2:
                mask.add((x, y))
    return mask


def draw_rune(rune, tier):
    rng = random.Random(f"{rune}:{tier}")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    out = img.load()
    mask = tablet_mask()
    stone = [hx(c) for c in STONE]
    rim = [hx(c) for c in TIER_RIM[tier]]
    glow, core = hx(RUNE_GLOW[rune][0]), hx(RUNE_GLOW[rune][1])

    def inside(p):
        return p in mask

    edge = {p for p in mask if any((p[0] + dx, p[1] + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}

    # Stone face: lit top-left, shaded bottom-right, a little grain.
    for (x, y) in mask:
        t = (x + y - 2) / 26  # 0 top-left .. 1 bottom-right
        idx = 3 - t * 2.2 + rng.uniform(-0.35, 0.35)
        out[x, y] = stone[max(1, min(4, round(idx)))] + (255,)
    # Inner bevel ring just inside the rim.
    for (x, y) in mask:
        if (x, y) in edge:
            continue
        near = [(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
        if any(n in edge for n in near):
            lit = any(n in edge for n in ((x - 1, y), (x, y - 1)))
            out[x, y] = (stone[4] if lit else stone[1]) + (255,)

    # Rim in the tier metal: lit on the top-left, dark bottom-right.
    for (x, y) in edge:
        tl = (x - 1, y) not in mask or (x, y - 1) not in mask
        br = (x + 1, y) not in mask or (x, y + 1) not in mask
        if tl and not br:
            col = rim[2]
        elif br and not tl:
            col = rim[0]
        else:
            col = rim[1]
        out[x, y] = col + (255,)
    for p in ((3, 1), (1, 3)):
        if p in edge:
            out[p] = rim[3] + (255,)

    # Carved glyph: inner shadow above-left of each stroke, lip highlight below-right, soft glow.
    gx, gy = 4, 4
    cells = {}
    for r, row in enumerate(GLYPHS[rune]):
        for c, ch in enumerate(row):
            if ch != ".":
                cells[(gx + c, gy + r)] = ch
    for (x, y) in cells:
        for (dx, dy), col in (((-1, 0), stone[0]), ((0, -1), stone[0]), ((1, 0), None), ((0, 1), None)):
            n = (x + dx, y + dy)
            if n in cells or n not in mask or n in edge:
                continue
            if col is not None:
                out[n] = col + (255,)
            else:
                cur = out[n][:3]
                out[n] = mix(cur, glow, 0.35) + (255,)
    for (x, y), ch in cells.items():
        out[x, y] = (core if ch == "+" else glow) + (255,)

    # Tier extras.
    if tier in ("rare", "legendary"):
        for p in ((4, 2), (11, 2), (2, 4), (13, 4), (2, 11), (13, 11), (4, 13), (11, 13)):
            if p in edge or p in mask:
                pass
        for p in ((3, 2), (12, 2), (3, 13), (12, 13)):  # rivets on the chamfers
            out[p] = rim[3] + (255,)
    if tier == "legendary":
        # Filled, gilded corners on the chamfers and a small spike on each side, all attached to the rim.
        for corner, glint in (((1, 1), True), ((14, 1), False), ((1, 14), False), ((14, 14), False)):
            cx, cy = corner
            sx = 1 if cx == 1 else -1
            sy = 1 if cy == 1 else -1
            for p, tone in (((cx, cy), 3 if glint else 2), ((cx + sx, cy), 1), ((cx, cy + sy), 1)):
                if out[p][3] == 0 or p not in mask:
                    out[p] = rim[tone] + (255,)
                else:
                    out[p] = rim[tone] + (255,)
        for p, tone in (((7, 0), 2), ((8, 0), 1), ((0, 7), 2), ((0, 8), 1),
                        ((15, 7), 1), ((15, 8), 0), ((7, 15), 1), ((8, 15), 0)):
            out[p] = rim[tone] + (255,)
        # Brighter glow around the glyph.
        for (x, y) in cells:
            for dx, dy in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
                n = (x + dx, y + dy)
                if n in mask and n not in cells and n not in edge:
                    out[n] = mix(out[n][:3], glow, 0.22) + (255,)
    return img


# --------------------------------------------------------------------------------------------
# Pack + previews
# --------------------------------------------------------------------------------------------


def rune_model_id(rune, tier):
    return 1001 + RUNE_ORDER.index(rune) * 3 + TIER_ORDER.index(tier)


def write_pack(out):
    if os.path.exists(out):
        shutil.rmtree(out)
    tex = os.path.join(out, "assets/grimforge/textures/item")
    models = os.path.join(out, "assets/grimforge/models/item")
    items = os.path.join(out, "assets/minecraft/items")
    for d in (tex, models, items):
        os.makedirs(d)

    def model(name, parent):
        with open(os.path.join(models, name + ".json"), "w") as f:
            json.dump({"parent": parent, "textures": {"layer0": f"grimforge:item/{name}"}}, f, indent=2)

    def dispatch(item, entries):
        with open(os.path.join(items, item + ".json"), "w") as f:
            json.dump({"model": {
                "type": "minecraft:range_dispatch", "property": "minecraft:custom_model_data", "index": 0,
                "fallback": {"type": "minecraft:model", "model": f"minecraft:item/{item}"},
                "entries": entries}}, f, indent=2)

    entries = []
    for rune in RUNE_ORDER:
        for tier in TIER_ORDER:
            name = f"rune_{rune}_{tier}"
            draw_rune(rune, tier).save(os.path.join(tex, name + ".png"))
            model(name, "minecraft:item/generated")
            entries.append({"threshold": rune_model_id(rune, tier),
                            "model": {"type": "minecraft:model", "model": f"grimforge:item/{name}"}})
    dispatch("echo_shard", entries)

    for material in SWORD_MATERIALS:
        entries = []
        for i, stage in enumerate(SWORD_STAGES, start=1):
            name = f"sword_{material}_{stage}"
            draw_sword(material, stage).save(os.path.join(tex, name + ".png"))
            model(name, "minecraft:item/handheld")
            entries.append({"threshold": i, "model": {"type": "minecraft:model", "model": f"grimforge:item/{name}"}})
        dispatch(f"{material}_sword", entries)

    with open(os.path.join(out, "pack.mcmeta"), "w") as f:
        # pack_format + supported_formats are read by 1.21.4-1.21.8; min_format/max_format by 1.21.9+.
        # 46 = 1.21.4, 97 = 26.3 (an integer max_format covers every minor version of 97).
        json.dump({"pack": {"pack_format": 46, "supported_formats": {"min_inclusive": 46, "max_inclusive": 97},
                            "min_format": 46, "max_format": 97,
                            "description": "Grimforge runes and evolving swords"}}, f, indent=2)
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    icon.paste(draw_sword("netherite", "infernal"), (0, 0))
    icon.resize((128, 128), Image.NEAREST).save(os.path.join(out, "pack.png"))


def font(size, bold=False):
    return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf" % ("-Bold" if bold else ""), size)


def big(img, scale):
    return img.resize((16 * scale, 16 * scale), Image.NEAREST)


def preview_swords(path):
    scale, cell = 9, 190
    left, top = 170, 150
    w = left + cell * 3 + 330
    h = top + cell * len(SWORD_MATERIALS) + 20
    img = Image.new("RGB", (w, h), (24, 23, 30))
    d = ImageDraw.Draw(img)
    d.text((30, 24), "Evolving swords", font=font(30, True), fill=(240, 240, 245))
    d.text((30, 66), "Swords stay vanilla until level 10, then change look at each stage. 16×16, shown at 9×.",
           font=font(16), fill=(160, 160, 175))
    info = [("Tempered", "Level 10", (196, 198, 210)), ("Runed", "Level 15", (120, 190, 255)),
            ("Infernal", "Level 20", (255, 120, 50))]
    for c, (name, lvl, col) in enumerate(info):
        cx = left + c * cell + cell // 2
        tw = d.textlength(name, font=font(19, True))
        d.text((cx - tw / 2, top - 40), name, font=font(19, True), fill=col)
        tw = d.textlength(lvl, font=font(14))
        d.text((cx - tw / 2, top - 16), lvl, font=font(14), fill=(150, 150, 165))
    for r, material in enumerate(SWORD_MATERIALS):
        y = top + 20 + r * cell
        d.text((30, y + 62), material.capitalize(), font=font(18, True), fill=(220, 220, 228))
        for c, stage in enumerate(SWORD_STAGES):
            t = big(draw_sword(material, stage), scale)
            img.paste(t, (left + c * cell + (cell - 16 * scale) // 2, y), t)
    sx = left + cell * 3 + 30
    d.text((sx, top + 10), "Netherite, Infernal", font=font(18, True), fill=(255, 120, 50))
    d.rounded_rectangle([sx, top + 40, sx + 276, top + 316], radius=10, fill=(78, 74, 90))
    t = draw_sword("netherite", "infernal").resize((256, 256), Image.NEAREST)
    img.paste(t, (sx + 10, top + 50), t)
    d.text((sx, top + 330), "Each material has its own glow:", font=font(15), fill=(170, 170, 185))
    y = top + 360
    for material, m in SWORD_MATERIALS.items():
        d.rectangle([sx, y + 3, sx + 14, y + 17], fill=hx(m["glow"][0]))
        d.text((sx + 24, y), material.capitalize(), font=font(15), fill=(200, 200, 210))
        y += 26
    img.save(path)


def preview_runes(path):
    scale, cell_w, cell_h = 8, 170, 180
    left, top = 150, 100
    width = left + cell_w * len(RUNE_ORDER) + 30
    img = Image.new("RGB", (width, top + cell_h * 3 + 20), (24, 23, 30))
    d = ImageDraw.Draw(img)
    d.text((30, 26), "Rune textures", font=font(30, True), fill=(240, 240, 245))
    d.text((290, 38), "Glyphs carved into stone. Rim metal and trim show the tier. 16×16, shown at 8×.",
           font=font(16), fill=(160, 160, 175))
    for col, rune in enumerate(RUNE_ORDER):
        cx = left + col * cell_w + cell_w // 2
        tw = d.textlength(RUNE_NAMES[rune], font=font(16, True))
        d.text((cx - tw / 2, top - 14), RUNE_NAMES[rune], font=font(16, True), fill=hx(RUNE_GLOW[rune][0]))
    for row, tier in enumerate(TIER_ORDER):
        y = top + 16 + row * cell_h
        label, colr = TIER_LABEL[tier]
        d.text((30, y + 54), label, font=font(19, True), fill=hx(colr))
        for col, rune in enumerate(RUNE_ORDER):
            t = big(draw_rune(rune, tier), scale)
            img.paste(t, (left + col * cell_w + (cell_w - 16 * scale) // 2, y), t)
    img.save(path)


def preview_inventory(path):
    """Mock Minecraft inventory at the real in-game scale (16px items, 3x GUI scale)."""
    slot, cols = 54, 9
    img = Image.new("RGB", (slot * cols + 60, slot * 4 + 120), (24, 23, 30))
    d = ImageDraw.Draw(img)
    d.text((30, 20), "In an inventory (real size at GUI scale 3)", font=font(18, True), fill=(240, 240, 245))
    px, py = 30, 60
    d.rectangle([px - 8, py - 8, px + slot * cols + 4, py + slot * 4 + 4], fill=(198, 198, 198))
    items = [draw_sword(m, s) for m, s in (("netherite", "infernal"), ("diamond", "runed"), ("iron", "tempered"),
                                           ("golden", "infernal"), ("stone", "runed"), ("wooden", "tempered"))]
    items += [draw_rune(r, t) for r, t in (("ember", "legendary"), ("frost", "rare"), ("storm", "magic"),
                                           ("bleed", "legendary"), ("venom", "rare"), ("soul_harvest", "magic"),
                                           ("crushing_blow", "legendary"), ("retribution", "rare"))]
    for i in range(cols * 4):
        r, c = divmod(i, cols)
        sx, sy = px + c * slot, py + r * slot
        d.rectangle([sx, sy, sx + slot - 6, sy + slot - 6], fill=(139, 139, 139))
        d.line([sx, sy, sx + slot - 6, sy], fill=(55, 55, 55), width=3)
        d.line([sx, sy, sx, sy + slot - 6], fill=(55, 55, 55), width=3)
        d.line([sx, sy + slot - 6, sx + slot - 6, sy + slot - 6], fill=(255, 255, 255), width=3)
        d.line([sx + slot - 6, sy, sx + slot - 6, sy + slot - 6], fill=(255, 255, 255), width=3)
        if i < len(items):
            t = items[i].resize((48, 48), Image.NEAREST)
            img.paste(t, (sx, sy), t)
    img.save(path)


if __name__ == "__main__":
    pack_dir, preview_dir = sys.argv[1], sys.argv[2]
    write_pack(pack_dir)
    os.makedirs(preview_dir, exist_ok=True)
    preview_swords(os.path.join(preview_dir, "sword-evolution-preview.png"))
    preview_runes(os.path.join(preview_dir, "rune-preview.png"))
    preview_inventory(os.path.join(preview_dir, "inventory-preview.png"))
    print("ok")
