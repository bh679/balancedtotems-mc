# Dungeon Totems

Totems of Undying that still save your life, but curse you for carrying them. NeoForge 1.21.1.

| Totem | While held | After it saves you |
|---|---|---|
| Totem of Weakness | Weakness | Weakness, 4 min |
| Totem of Fatigue | Mining Fatigue | Mining Fatigue, 4 min |
| Totem of Heartbreak | 3 fewer max hearts | 3 fewer max hearts, 4 min |
| Totem of Hunger | Hunger | Hunger, 4 min |
| Totem of Slowness | Slowness | Slowness, 4 min |
| Totem of Rage | Rage: hit something every 60 s | Rage II (every 40 s), 5 min |
| Totem of Loneliness | Lonely: give a gift every 60 s | Lonely II (every 40 s), 5 min |
| Totem of Pacifism | Hurt anything and it pops | Vow of Peace, 4 min |
| Totem of Melon Diet | Eat anything but melon and it pops | Melon Diet, 10 min |
| Totem of Restraint | Open a chest, barrel or ender chest and it pops | Restrained, 4 min |
| Totem of Breathlessness | You lose air even on land, and drown | Breathless, 4 min |
| Totem of Insomnia | Phantoms come at night, and a quarter of the time by day (with Fire Resistance) | Insomnia, 4 min |
| Totem of Blindness | | Blindness, 30 s |
| Totem of Morphing | | You become a random animal for 30 seconds, seen in third person, with its size, speed, health, jump, animations and attack: goats ram, golems throw, polar bears rear up, bees sting once and may then die, llamas spit and snow golems throw snowballs (even at range: attack the air), frogs eat small slimes with their tongue, sheep, cows and horses graze grass. With an empty hand, right-click for the animal's own move: chickens lay eggs, armadillos roll into their shell (move to unroll), camels and cats sit, bats hang, frogs croak, sniffers dig up seeds, snow golems throw snowballs, turtles lay eggs on sand, parrots imitate, axolotls play dead, pufferfish puff up, squid ink and dolphins leap. Land forms walk at twice their mob's stroll and sprint at its run (a turtle crawls, a cow bolts, a horse gallops at its ridden speed and walks at half of it), and jump at their own pace too; rabbits hop wherever they go. Every attack shows, even on the air: golems swing, bees flip to sting, chickens flap, others lunge. Turtles, frogs, iron golems and undead forms never drown; water and rain hurt striders and snow golems. Horses always make their best jump, frogs leap, camels can't jump; cats and other fall-proof mobs take no fall damage. Creepers keep away from cats, zombified piglins treat you as one of the pack, a dolphin speeds up nearby swimmers, and a mooshroom can be milked for stew. Monsters leave you alone. Bees, parrots, bats and allays fly and can't land; chickens flutter down. Die underwater and you become a fish, squid, dolphin, axolotl or tadpole, which breathe as their mob does (fish gasp after 15 seconds on land, axolotls dry out after 5 minutes, dolphins hold their breath under water) and flop about on land; die in lava and you become a strider (stands and walks on lava at its own speed, and shivers slowly along on land) or a zombified piglin |
| Totem of Zombification | | You become a zombie: iron and snow golems hunt you. It lasts until you cure it: get Weakness, then eat a golden apple |
| Void Totem | | Only saves you from void death: a chorus-fruit hop onto ground near where you last stood. In Dungeon Train it puts you back on the train |
| Feather Totem | | Only saves you from falling |
| Boom Totem | | Only saves you from explosions |
| Fire Totem | | Only saves you from fire and lava |

Each one pops like a vanilla totem (Regeneration, Absorption, Fire Resistance; the Void, Feather and
Boom Totems skip Fire Resistance) when held in either hand. A vanilla totem in hand is used first.

A held curse comes off when you let go, except Rage and Lonely, which stay for another minute at level I.
Rage and Lonely are timers: miss one and you take true damage at Poison's rate until you hit something
or give a gift (a villager trade, food a villager picks up, or a PlayerMob gift). Their effect box's
border is the countdown: it drains from the curse's colour to grey, then flashes when time is up. Break Pacifism,
Melon Diet or Restraint while holding the totem and it pops, saving you, with a line in chat.

For its time after a save, a curse can't kill you: a broken rule, a missed timer or Breathless drowning
leaves you on half a heart, and starts that curse over at its full time. Food off the Melon Diet doesn't
fill you. Those after-save curses come back after you respawn; held ones don't.
Milk doesn't lift the Morphed, Zombified, Rage or other rule curses. Hover a curse in the inventory to see
the hearts taken, the cure, the rule or the seconds left.

Evokers drop one of these at random instead of the vanilla totem. The pool is the item tag
`dungeontotems:evoker_totems`, so a datapack can change it.

Built for [Dungeon Train](https://modrinth.com/mod/dungeon-train), where the Void Totem lands you
back on the train, but it works on its own.

## For mod authors

- `games.brennan.dungeontotems.api.DungeonTotemsApi.setRescueTarget(player -> Optional<Vec3>)`
  picks where the Void Totem sends a player.
- Curses are plain mob effects: `dungeontotems:heartbreak`, `morphed`, `zombified`, `rage`,
  `lonely`, `breathless`, `insomnia` and the rule curses.
- Datapacks can change `dungeontotems:flying_forms` (entity type tag), and the morph forms with the
  `dungeontotems:morph_forms` data map: per entity type a `pool` (`land`, `water`, `lava`) and a `weight`
  (0 leaves it out).

Licence: PolyForm Shield 1.0.0.
