<p align="center"><img src="docs/images/icon.png" width="128" alt="Grimforge icon"></p>

# Grimforge

A Paper plugin for PvP and factions servers: tools earn XP, level up and unlock modifiers, swords visually evolve, and Diablo-style runes drop from monsters and socket into your weapons.

Supports Paper 1.21.4 through 26.3 (26.x servers need Java 25). Optional resource pack for the sword and rune textures.

- **Pickaxes, axes, shovels and hoes** earn XP for mining blocks they're the right tool for (ores give more). Hoes earn XP from fully grown crops.
- **Any tool** earns XP from mob kills (swords included).
- Each level grants a **modifier point**. Spend points in the `/tool` menu.
- Progress shows in the tool's lore, with a level-up title and sound.
- **Anti-farming:** player-placed blocks and spawner mobs give no XP, and creative mode doesn't count.

## Modifiers

| Modifier | Tools | Effect |
|---|---|---|
| Haste | pickaxe, axe, shovel, hoe | +1 Efficiency per level |
| Lucky | pickaxe, axe, shovel, hoe | +1 Fortune per level (not on Silk Touch tools) |
| Autosmelt | pickaxe, shovel | Smelts drops (iron ore → iron ingot, sand → glass) |
| Magnetic | all | Drops go straight to your inventory |
| Reinforced | all | 10% chance per level to skip durability loss |
| Experienced | all | +50% XP orbs per level |
| Sharp | sword, axe | +1 melee damage per level (+0.2 vs players) |
| Lifesteal | sword, axe | Heal 5% of damage dealt per level (2% vs players) |

Max levels, strengths, XP values and the level curve are all in `config.yml`.

## Runes

![Runes](docs/images/runes.png)

Diablo-style runes drop from hostile mobs and socket into **swords and axes**. Weapons gain a socket at levels 5, 10 and 15. Open `/tool`, then click a rune in your inventory to socket it. Shift-click a socketed rune to shatter it (the rune is destroyed). A weapon can't hold two runes of the same type.

Runes come in three tiers: **Magic** (blue), **Rare** (yellow) and **Legendary** (orange). Default drop chances per hostile kill are 2% / 0.5% / 0.1%. Bosses (Wither, Ender Dragon, Warden, Elder Guardian) always drop a Rare or Legendary. Spawner mobs drop nothing.

| Rune | Effect (Magic / Rare / Legendary) | Cooldown |
|---|---|---|
| Ember | 10/15/20% chance to ignite for 3/4/5s | 5s |
| Frost | 10/14/18% chance of Slowness I for 1.5/2/2.5s | 8s |
| Venom | 10/14/18% chance of Poison I for 2/3/4s | 8s |
| Storm | 8/12/15% chance to arc to 2/3/3 nearby enemies for 2/3/4 damage | 10s |
| Bleed | 10/13/16% chance to bleed 1 damage/s for 3/4/5s | 10s |
| Crushing Blow | 6/9/12% chance to deal 10/15/20% of current health | 12s |
| Soul Harvest | Kills grant Regeneration I for 3/4/5s | 15s |
| Retribution | 15/20/25% chance to reflect 6/9/12% of incoming damage, measured before armor | 6s |

### PvP balance

- Runes only proc on charged swings (90%+), so spam-clicking can't fish for procs. Sweep hits don't proc.
- Every rune has its own cooldown per player.
- Rune damage against players is multiplied by 0.6. Crushing Blow is capped at 4 damage (2 hearts) against players, and Retribution reflects at most 4.
- Effects are level I only, poison can't kill, and bleeds don't stack.
- Proc damage doesn't grant invulnerability frames, so a proc never shields the target from your next hit.
- Storm hits as a normal attack from the player, so armor, world PvP settings and region plugins like WorldGuard still apply to bystanders it arcs to. Retribution and Bleed deal damage without knockback, since knocking someone into the air hands them falling critical hits.

All of these numbers are in the `runes:` section of `config.yml`. Set `runes.debug: true` to log every proc.

### Balance testing

The numbers above were tuned with about 540 1v1 test duels, 30 to 40 per matchup. Both sides used Sharpness V netherite swords and Protection IV diamond armor. "Damage edge" is how much more damage per second the side with the upgrade dealt; one vanilla Sharpness level is worth about +4% on the same scale.

| Matchup (vs. the same gear with nothing) | Damage edge |
|---|---|
| Sharpness V vs IV (vanilla yardstick) | +4% |
| Legendary Bleed | +6% |
| Legendary Crushing Blow | +4% |
| Legendary Venom | +3% |
| Legendary Retribution | +3% |
| Legendary Ember | +2% |
| Full Legendary set (Crushing Blow, Bleed, Ember) | +12% |
| Sharp V modifier | +10% (was +66% before the PvP split) |

Frost and Storm can't be measured this way: Frost's slow matters for chasing and escaping, which a straight 1v1 duel doesn't test, and Storm needs a crowd.

## Factions support

Grimforge works with **FactionsUUID** and forks that keep its API (such as **SaberFactions**). Nothing to set up: if one is installed, Grimforge hooks into it automatically and logs `Hooked into Factions` on startup.

- Runes never hit faction members, allies or truces. That includes Storm arcing into a crowd and Retribution reflecting damage.
- Runes are off in safezones.
- The Sharp modifier's bonus damage never applies to friendly players.

Settings live under `factions:` in `config.yml` (`truce-is-friendly` decides whether truces count as allies). Without a factions plugin, runes affect everyone except your own tamed pets.

## Evolving swords

![Evolving swords](docs/images/evolving-swords.png)

Swords change look as they level up: **Tempered** at level 10 (serrated steel blade), **Runed** at 15 (broad blade with glowing runes in the fuller, winged guard and gems) and **Infernal** at 20 (black blade with a molten edge, burning horns and drifting embers). Each sword material has its own glow color. Players get a message when their sword evolves. The levels are set by `visuals.sword-stage-levels` in `config.yml`.

This needs the resource pack below. Without it, swords keep their vanilla look.

## Resource pack (optional)

`Grimforge-Resource-Pack.zip` adds the evolving sword textures and gives each rune its own texture, with the frame color showing the tier. Without it, everything still works: swords look vanilla and runes appear as glowing echo shards.

To have players download it automatically, upload the zip somewhere with a direct download link and set these in `server.properties`:

```
resource-pack=https://your-host/Grimforge-Resource-Pack.zip
resource-pack-sha1=<the zip's SHA-1>
```

The pack only changes swords and echo shards that the plugin has tagged, so ordinary items look normal. Swords whose look was set by another plugin are left alone. It works on Minecraft 1.21.4 through 26.3.

## Commands

| Command | Permission | |
|---|---|---|
| `/tool` (or `/gf`) | `grimforge.use` (everyone) | Open the modifier menu for the held tool |
| `/tool info` | `grimforge.use` | Show level, XP and modifiers in chat |
| `/tool addxp <n>` | `grimforge.admin` (op) | Add XP to the held tool (handy for testing) |
| `/tool rune <type> [tier] [player]` | `grimforge.admin` | Give a rune, e.g. `/tool rune storm legendary` |
| `/tool socket <type> [tier]` | `grimforge.admin` | Socket a rune straight into the held weapon (for testing) |
| `/tool setmod <modifier> <level>` | `grimforge.admin` | Set a modifier level without spending points (for testing) |
| `/tool reload` | `grimforge.admin` | Reload `config.yml` |

## Building

Needs JDK 21.

```
./gradlew build          # Windows: gradlew.bat build
```

The jar lands in `build/libs/Grimforge-0.5.0.jar`. Drop it into your server's `plugins/` folder.

It compiles against the Paper 1.21.4 API (Java 21 bytecode, so it runs on both Java 21 and 25) and has been tested on Paper 1.21.4, 1.21.8, 1.21.11, 26.1.2 and 26.3.

The textures are drawn from code in `art/`. To regenerate the resource pack after tweaking them: `python3 art/art.py <pack-folder> <preview-folder>` (needs Python 3 and Pillow).

## How it stores data

- Tool level, XP, points and modifiers live in the item's persistent data, so they survive restarts, trading and chests.
- Player-placed blocks are tracked with one bit per block in each chunk's persistent data, so lookups stay instant even in huge factions bases. Data from versions before 0.5 is converted automatically.
- Runes are echo shards tagged with persistent data; they can't be used in crafting or anvils.
- Haste and Lucky add real enchantment levels; the rest are applied by event listeners.

## License

MIT. See `LICENSE`.

## Ideas for next versions

- Bows, crossbows, tridents and maces
- Modifier choices that get stronger tiers at certain levels (e.g. pick one of two at level 5, 10, 15)
- Area mining (3x3) as a late-game modifier
- Repair kits or a "broken, not destroyed" state
- Per-world toggles and a PlaceholderAPI hook
