# Balanced Totems

Totems of Undying that still save your life, but leave a curse behind. NeoForge 1.21.1.

| Totem | After it saves you |
|---|---|
| Totem of Weakness | Weakness for 20 minutes |
| Totem of Fatigue | Mining Fatigue II for 20 minutes |
| Totem of Heartbreak | 2 fewer hearts for 20 minutes |
| Totem of Blindness | Blindness for 30 seconds |
| Totem of Morphing | You become a random animal for 30 seconds, seen in third person: its size, speed, health and jump. Monsters leave you alone |
| Totem of Zombification | You become a zombie. Get Weakness, then eat a golden apple within 10 minutes (like curing a zombie villager), or die |
| Totem of Rage | Rage: kill 3 players (or PlayerMobs) within 15 minutes, or die |
| Totem of Pacifism | Vow of Peace: hurt anything within 20 minutes and you die |
| Totem of Melon Diet | Melon Diet: eat anything but melon within 20 minutes and you die |
| Totem of Restraint | Restrained: open a chest, barrel or ender chest within 20 minutes and you die |
| Totem of Loneliness | Lonely: make 3 friends within 10 minutes, or die. A friend is a PlayerMob you swap gifts with |
| Void Totem | Only saves you from void death, and pulls you back to safety |
| Feather Totem | Only saves you from falling |

Each one pops like a vanilla totem (Regeneration, Absorption, Fire Resistance) when held in either
hand. A vanilla totem in hand is used first. Milk doesn't lift the Morphed, Zombified or Rage curses.

Curses stack: a later pop never clears an earlier curse. The same curse again goes up a level with a
fresh timer (Rage needs 3 more kills per level; Morphing picks a new animal; Heartbreak stops at
half a heart). Fatigue, Pacifism, Melon Diet and Restraint never level up: another pop adds 20 minutes instead. Curses survive
death and come back on respawn with a full timer. Hover Heartbreak, Zombified or Rage in the inventory to see
the hearts taken, the cure, or the kills and time left.

Evokers drop one of these at random instead of the vanilla totem. The pool is the item tag
`balancedtotems:evoker_totems`, so a datapack can change it.

Built for [Dungeon Train](https://modrinth.com/mod/dungeon-train), where the Void Totem lands you
back on the train, but it works on its own.

## For mod authors

- `games.brennan.balancedtotems.api.BalancedTotemsApi.setRescueTarget(player -> Optional<Vec3>)`
  picks where the Void Totem sends a player.
- Curses are plain mob effects: `balancedtotems:heartbreak`, `morphed`, `zombified`, `rampage` (shown as Rage).
- Datapacks can change `balancedtotems:morph_pool` and `balancedtotems:rampage_targets` (entity type tags).

Without PlayerMob, villagers stand in: Rage counts villager kills, and Lonely counts a villager you
trade with, feed, or cure. Under Dungeon Train villagers never count.

Licence: PolyForm Shield 1.0.0.
