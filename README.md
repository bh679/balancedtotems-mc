# Balanced Totems

Totems of Undying that still save your life, but leave a curse behind. NeoForge 1.21.1.

| Totem | After it saves you |
|---|---|
| Totem of Weakness | Weakness for 20 minutes |
| Totem of Fatigue | Mining Fatigue II for 20 minutes |
| Totem of Heartbreak | 2 fewer hearts for 20 minutes |
| Totem of Blindness | Blindness for 30 seconds |
| Totem of Morphing | You become a random animal for 30 seconds, seen in third person, with its size, speed, health, jump, animations and attack: goats ram, golems throw, polar bears rear up, bees sting once and may then die, llamas spit and snow golems throw snowballs (even at range: attack the air), frogs eat small slimes with their tongue, sheep, cows and horses graze grass. With an empty hand, right-click for the animal's own move: chickens lay eggs, armadillos roll into their shell, camels dash, cats sit, bats hang, frogs croak, sniffers dig up seeds, snow golems throw snowballs, turtles lay eggs on sand, parrots imitate, axolotls play dead, pufferfish puff up, squid ink and dolphins leap. Land forms walk at their mob's own pace and sprint at its run (a turtle crawls, a cow bolts, a horse gallops at its ridden speed). Horses always make their best jump, frogs leap, camels can't jump; cats and other fall-proof mobs take no fall damage. Creepers keep away from cats, zombified piglins treat you as one of the pack, a dolphin speeds up nearby swimmers, and a mooshroom can be milked for stew. Monsters leave you alone. Bees, parrots, bats and allays fly and can't land; chickens flutter down. Die underwater and you become a fish, squid, dolphin, axolotl or tadpole, which dries out on land; die in lava and you become a strider (walks on lava) or a zombified piglin |
| Totem of Zombification | You become a zombie: iron and snow golems hunt you. Get Weakness, then eat a golden apple within 10 minutes (like curing a zombie villager), or die |
| Totem of Rage | Rage: kill 3 players (or PlayerMobs) within 15 minutes, or die |
| Totem of Pacifism | Vow of Peace: hurt anything within 20 minutes and you die |
| Totem of Melon Diet | Melon Diet: eat anything but melon within 20 minutes and you die |
| Totem of Restraint | Restrained: open a chest, barrel or ender chest within 20 minutes and you die |
| Totem of Loneliness | Lonely: make 3 friends within 10 minutes, or die. A friend is a PlayerMob you swap gifts with |
| Void Totem | Only saves you from void death: it teleports you like a chorus fruit, onto ground near where you last stood. In Dungeon Train it puts you back on the train |
| Feather Totem | Only saves you from falling |
| Boom Totem | Only saves you from explosions |

Each one pops like a vanilla totem (Regeneration, Absorption, Fire Resistance; the Void, Feather and
Boom Totems skip Fire Resistance) when held in either hand. A vanilla totem in hand is used first. Milk doesn't lift the Morphed, Zombified or Rage curses.

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
- Datapacks can change `balancedtotems:rampage_targets` and `balancedtotems:flying_forms` (entity type tags), and
  the morph forms with the `balancedtotems:morph_forms` data map: per entity type a `pool` (`land`, `water`,
  `lava`) and a `weight` (0 leaves it out).

Without PlayerMob, villagers stand in: Rage counts villager kills, and Lonely counts a villager you
trade with, feed, or cure. Under Dungeon Train villagers never count.

Licence: PolyForm Shield 1.0.0.
