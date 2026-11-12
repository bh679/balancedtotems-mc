# Balanced Totems

Totems of Undying that still save your life, but leave a curse behind. NeoForge 1.21.1.

| Totem | After it saves you |
|---|---|
| Weakness Totem | Weakness for 20 minutes |
| Fatigue Totem | Mining Fatigue II for 20 minutes |
| Heartbreak Totem | 2 fewer hearts for 20 minutes |
| Blindness Totem | Blindness for 20 seconds |
| Morphing Totem | You become a random animal for 20 seconds, seen in third person: its size, speed, health and jump. Monsters leave you alone |
| Zombified Totem | You become a zombie. Get Weakness, then eat a golden apple within 10 minutes (like curing a zombie villager), or die |
| Rampage Totem | Kill 3 players (or PlayerMobs) within 15 minutes, or die |
| Void Totem | Saves you only from the void and deadly falls, and pulls you back to safety |

Each one pops like a vanilla totem (Regeneration, Absorption, Fire Resistance) when held in either
hand. A vanilla totem in hand is used first. Milk doesn't lift the Morphed, Zombified or Rampage curses.

Curses stack: a later pop never clears an earlier curse. The same curse again goes up a level with a
fresh timer (Rampage needs 3 more kills per level; Morphing picks a new animal; Heartbreak stops at
half a heart). Fatigue never levels up: another Fatigue pop adds 20 minutes instead. Curses survive
death and come back on respawn with a full timer. Hover Zombified or Rampage in the inventory to see
the time left and what lifts it.

Evokers drop one of these at random instead of the vanilla totem. The pool is the item tag
`balancedtotems:evoker_totems`, so a datapack can change it.

Built for [Dungeon Train](https://modrinth.com/mod/dungeon-train), where the Void Totem lands you
back on the train, but it works on its own.

## For mod authors

- `games.brennan.balancedtotems.api.BalancedTotemsApi.setRescueTarget(player -> Optional<Vec3>)`
  picks where the Void Totem sends a player.
- Curses are plain mob effects: `balancedtotems:heartbreak`, `morphed`, `zombified`, `rampage`.
- Datapacks can change `balancedtotems:morph_pool` and `balancedtotems:rampage_targets` (entity type tags).

Licence: PolyForm Shield 1.0.0.
