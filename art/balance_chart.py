"""Renders docs/images/balance.png: damage edge per upgrade from the 1v1 test duels.

Run from the repo root:  python3 art/balance_chart.py
"""
import sys

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402

# Extra damage per second over the same gear without the upgrade (%), from 30-40 duels per matchup.
ROWS = [
    ("Full Legendary set (3 runes)", "rune", 12),
    ("Sharp V modifier", "modifier", 10),
    ("Legendary Bleed", "rune", 6),
    ("Legendary Crushing Blow", "rune", 4),
    ("Legendary Venom", "rune", 3),
    ("Legendary Retribution", "rune", 3),
    ("Legendary Ember", "rune", 2),
    ("Lifesteal III modifier", "modifier", -1),
]
ONE_SHARPNESS_LEVEL = 4      # Sharpness V vs Sharpness IV
NOISE = (-1, 1)              # identical setups on both sides

BG, PANEL, TEXT, MUTED = "#141016", "#1c1720", "#ece6f0", "#a39aae"
RUNE, MODIFIER, REF = "#f08a3c", "#6b6474", "#d9d2e2"


def main(out):
    plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 12})
    fig, ax = plt.subplots(figsize=(10, 5.2), dpi=128)
    fig.patch.set_facecolor(BG)
    ax.set_facecolor(BG)

    labels = [r[0] for r in ROWS][::-1]
    values = [r[2] for r in ROWS][::-1]
    colors = [RUNE if r[1] == "rune" else MODIFIER for r in ROWS][::-1]
    y = range(len(ROWS))

    ax.axvspan(*NOISE, color="#ffffff", alpha=0.06, lw=0)
    ax.axvline(0, color=MUTED, lw=1)
    ax.axvline(ONE_SHARPNESS_LEVEL, color=REF, lw=1.3, ls=(0, (4, 3)))
    ax.text(ONE_SHARPNESS_LEVEL + 0.15, len(ROWS) - 0.35, f"One Sharpness level (+{ONE_SHARPNESS_LEVEL}%)",
            color=MUTED, fontsize=11, va="bottom")
    ax.barh(list(y), values, color=colors, height=0.62)

    for yi, v in zip(y, values):
        x = v + 0.25 if v >= 0 else v - 0.25
        # Keep labels off the dashed line.
        if 0 <= v < ONE_SHARPNESS_LEVEL and x + 1.0 > ONE_SHARPNESS_LEVEL:
            x = ONE_SHARPNESS_LEVEL + 0.25
        ax.text(x, yi, f"{v:+d}%".replace("-", "−"), va="center", ha="left" if v >= 0 else "right",
                color=TEXT, fontsize=12, fontweight="bold")

    ax.set_yticks(list(y))
    ax.set_yticklabels(labels, color=TEXT)
    ax.set_xlim(-3.5, 14.5)
    ax.set_ylim(-0.6, len(ROWS) - 0.1)
    ax.set_xticks([])
    for side in ("top", "right", "bottom", "left"):
        ax.spines[side].set_visible(False)
    ax.tick_params(axis="y", length=0, pad=10)

    fig.text(0.02, 0.95, "Each Legendary rune adds about one level of Sharpness", color=TEXT, fontsize=16,
             fontweight="bold", ha="left")
    fig.text(0.02, 0.895, "Extra damage per second vs. the same gear without the upgrade, in 1v1 duels  ·  "
             "orange = runes, grey = modifiers", color=MUTED, fontsize=11, ha="left")
    fig.text(0.02, 0.03, "Sharpness V netherite sword, Protection IV diamond armor  ·  30–40 duels per upgrade  "
             "·  shaded strip = test noise", color=MUTED, fontsize=9.5, ha="left")
    fig.subplots_adjust(left=0.27, right=0.97, top=0.84, bottom=0.1)
    fig.savefig(out, facecolor=BG)
    print("ok", out)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "docs/images/balance.png")
