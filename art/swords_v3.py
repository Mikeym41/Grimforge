"""Hand-placed sword sprites. Imported by art.py.

Coordinates are (x, y) on a 16x16 grid, tip at the top right. Each part is a list of
(x, y, tone) where tone indexes that part's colour ramp (0 = darkest).

Geometry: the blade runs along x + y = 15 (rows put light at 14-y, centre at 15-y, shadow at
16-y). The crossguard is a solid two-pixel bar on the diagonals x - y = -3 (lit, toward the tip)
and x - y = -4 (shadow, toward the hilt). The grip continues the blade line below the guard.
"""


def blade_rows(top, bottom, light=4, mid=3, dark=2):
    px = []
    for y in range(top, bottom + 1):
        for dx, tone in ((-1, light), (0, mid), (1, dark)):
            x = 15 - y + dx
            if 0 <= x < 16:
                px.append((x, y, tone))
    return px


def guard_bar(x0, x1, lit=(3, 2), shadow=(2, 1)):
    """Solid crossguard from x0 to x1. Tones fade from the left (lit) end to the right."""
    px = []
    for x in range(x0, x1 + 1):
        t = (x - x0) / max(1, x1 - x0)
        px.append((x, x + 3, lit[0] if t < 0.5 else lit[1]))
        px.append((x, x + 4, shadow[0] if t < 0.5 else shadow[1]))
    return px


def grip():
    # Leather wrap: alternating light/dark bands across the two-pixel grip.
    return [(5, 10, 1), (4, 11, 0), (5, 11, 1), (3, 12, 1), (4, 12, 0), (2, 13, 0), (3, 13, 1)]


def pommel(gem):
    px = [(1, 13, 2), (0, 14, 1), (2, 14, 1), (1, 15, 0)]
    gems = [(1, 14, 1)] if gem else []
    if not gem:
        px.append((1, 14, 3))
    return px, gems


def tempered():
    blade = [(15, 0, 3), (14, 0, 4)] + blade_rows(1, 8)
    blade += [(14, 3, 1), (12, 5, 1), (10, 7, 1)]          # serrated back edge
    glints = {(13, 1), (10, 4)}
    blade = [(x, y, 4 if (x, y) in glints else t) for x, y, t in blade]
    guard = guard_bar(4, 8)
    guard += [(3, 7, 2), (9, 12, 1)]                         # short quillon tips
    p, gems = pommel(gem=False)
    return dict(blade=blade, guard=guard + p, grip=grip(), gem=gems, glow=[], ember=[])


def runed():
    blade = [(15, 0, 3), (14, 0, 4)] + blade_rows(1, 2)
    for y in range(3, 9):                                     # broad section with a dark fuller
        cx = 15 - y
        blade += [(cx - 2, y, 4), (cx - 1, y, 3), (cx, y, 1), (cx + 1, y, 2)]
    glints = {(13, 1), (11, 3)}
    blade = [(x, y, 4 if (x, y) in glints else t) for x, y, t in blade]
    glow = [(12, 3, 1), (11, 4, 0), (10, 5, 1), (9, 6, 0), (8, 7, 1), (7, 8, 0)]   # runes in the fuller
    guard = guard_bar(3, 8)
    guard += [(2, 5, 3), (2, 4, 3),                           # left wing swept toward the tip
              (9, 11, 1), (10, 11, 2)]                        # right wing
    p, gems = pommel(gem=True)
    gems += [(6, 9, 1)]                                       # gem set in the guard
    ember = [(13, 5), (5, 3)]
    return dict(blade=blade, guard=guard + p, grip=grip(), gem=gems, glow=glow, ember=ember)


def infernal():
    blade = [(15, 0, 2), (15, 1, 1), (14, 1, 2)]
    for y in range(2, 9):
        cx = 15 - y
        blade += [(cx - 1, y, 3), (cx, y, 2), (cx + 1, y, 1)]
    blade += [(15, 2, 0), (13, 4, 0), (11, 6, 0), (9, 8, 0)]   # barbs on the back edge
    # Continuous molten cutting edge on the lit side, brighter toward the tip.
    glow = [(14, 0, 1), (13, 1, 1), (12, 2, 1), (11, 3, 1), (10, 4, 0), (9, 5, 1), (8, 6, 0),
            (7, 7, 0), (6, 8, 0)]
    glow += [(12, 4, 0), (10, 6, 0)]                           # cracks glowing through the core
    guard = guard_bar(3, 8, lit=(2, 2), shadow=(1, 0))
    guard += [(2, 5, 2), (2, 4, 2), (1, 3, 2),                 # left horn curling up and out
              (9, 11, 1), (10, 11, 1), (11, 10, 1)]            # right horn
    glow += [(1, 2, 1), (12, 9, 1)]                            # burning horn tips
    p, gems = pommel(gem=True)
    gems += [(6, 9, 1)]
    ember = [(12, 0), (10, 2), (7, 5)]
    return dict(blade=blade, guard=guard + p, grip=grip(), gem=gems, glow=glow, ember=ember)


STAGES = {"tempered": tempered, "runed": runed, "infernal": infernal}
