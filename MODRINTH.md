# Grimforge

**Your weapons remember every fight.** Tools level up as you use them, swords evolve into demonic blades, and Diablo-style runes drop from monsters to socket into your gear. Built for PvP and factions servers, with balance tested in hundreds of 1v1 duels.

---

## ⚔ Tools that grow with you

Pickaxes, axes, shovels, hoes and swords earn XP as you mine, farm and fight. Every level grants a **modifier point** to spend in the `/tool` menu:

| Modifier | Effect |
|---|---|
| **Haste** | +1 Efficiency per level |
| **Lucky** | +1 Fortune per level |
| **Autosmelt** | Smelts what you mine |
| **Magnetic** | Drops go straight to your inventory |
| **Reinforced** | Chance to skip durability loss |
| **Experienced** | More XP orbs from blocks and kills |
| **Sharp** | Bonus melee damage |
| **Lifesteal** | Heal a share of the damage you deal |

Progress lives on the item itself, so leveled gear survives deaths, trades and chests. That makes a max-level sword a real prize when you kill its owner.

## 🔥 Swords that evolve

With the optional resource pack, swords change look as they level:

- **Level 10: Tempered.** A serrated, battle-worn blade.
- **Level 15: Runed.** Glowing runes etched into the fuller, a winged guard and set gems.
- **Level 20: Infernal.** A black blade with a molten edge, burning horns and drifting embers.

Every material glows its own color, so a glance tells you exactly what someone is carrying.

## 💎 Diablo-style runes

Hostile mobs drop runes in three tiers: **Magic**, **Rare** and **Legendary**. Bosses always drop Rare or better. Swords and axes gain rune sockets at levels 5, 10 and 15.

| Rune | Effect |
|---|---|
| **Ember** | Sets your target ablaze |
| **Frost** | Slows your target |
| **Venom** | Poisons your target |
| **Storm** | Lightning arcs to nearby enemies |
| **Bleed** | Damage over time |
| **Crushing Blow** | Bonus damage based on the target's current health |
| **Soul Harvest** | Regeneration after a kill |
| **Retribution** | Reflects part of the damage you take |

## 🛡 Built for PvP

Runes are fun, not mandatory. Every number was tuned with 1v1 test duels on a real server: each Legendary rune is worth about one Sharpness level, and a full set of three about three.

- Runes only trigger on **fully charged swings**. Spam-clicking gets you nothing.
- Every rune has its own **cooldown**.
- Rune damage against players is **reduced**, and percentage damage is **hard-capped**.
- Effects are level I only, poison can't kill, and bleeds don't stack.
- Rune hits never grant invulnerability frames, so they never shield a target from your next hit.

## 🏰 Factions ready

Works out of the box with **FactionsUUID** and **SaberFactions**:

- Runes **never hit faction members, allies or truces**, including Storm arcing into a crowd.
- Runes are **off in safezones**.
- **Anti-farming:** player-placed blocks, spawner mobs and creative mode give no XP. Placed-block tracking stays instant even in huge bases.

## 📦 Installation

1. Drop `Grimforge-0.5.0.jar` into your `plugins` folder and restart.
2. *(Optional)* Give players the resource pack for evolving swords and rune textures. Set `resource-pack` in `server.properties` to the pack's download link. Without it, everything still works with vanilla looks.

**Supports Paper 1.21.4 through 26.3.** 26.x servers need Java 25.

## Commands

| Command | Permission | |
|---|---|---|
| `/tool` (or `/gf`) | `grimforge.use` | Open the modifier and rune menu |
| `/tool info` | `grimforge.use` | Show your tool's level, modifiers and runes |
| `/tool addxp <n>` | `grimforge.admin` | Add XP to the held tool |
| `/tool rune <type> [tier] [player]` | `grimforge.admin` | Give a rune |
| `/tool reload` | `grimforge.admin` | Reload the config |

Everything is configurable in `config.yml`: XP rates, level curve, modifier strength, rune chances, cooldowns, PvP caps and drop rates.

---

*Grimforge is in beta. Found a bug or have an idea? Leave a comment or open an issue.*
