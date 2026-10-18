# Changelog

## 0.5.0 (beta)

- **Renamed to Grimforge** (formerly ToolLevels). Permissions are now `grimforge.use` and `grimforge.admin`, and `/grimforge` and `/gf` work as aliases for `/tool`.
- **Factions support:** hooks into FactionsUUID and SaberFactions automatically. Runes never hit faction members, allies or truces, and are off in safezones.
- **Faster placed-block tracking:** one bit per block instead of a list, so lookups stay instant in large bases. Old data converts automatically.
- **New art:** redrawn sword and rune textures with proper shading, colored outlines, distinct sword silhouettes per stage and carved rune glyphs.
- **Supports Minecraft 1.21.4 through 26.3**, for both the plugin and the resource pack.
- Rune charge detection no longer uses a Paper event that is deprecated for removal in 26.x; it now tracks swings itself.

## 0.4.0

- Swords visually evolve at levels 10, 15 and 20 (Tempered, Runed, Infernal) with the resource pack.

## 0.3.0

- PvP rebalance based on 1v1 test duels: Sharp and Lifesteal have separate PvP values, Retribution no longer knocks attackers into the air, Ember buffed.
- Resource pack with a texture for every rune and tier.

## 0.2.0

- Diablo-style runes: 8 types in Magic, Rare and Legendary tiers, dropped by hostile mobs and bosses, socketed into swords and axes at levels 5, 10 and 15.

## 0.1.0

- Tool leveling, XP from mining and kills, 8 modifiers, anti-farming.
