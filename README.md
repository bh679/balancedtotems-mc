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
| Totem of Morphing | | You become a random animal for 30 s, seen in third person, with its size, speed, health and moves. Monsters leave you alone |
| Totem of Zombification | | You become a zombie: iron and snow golems hunt you. Get Weakness, then eat a golden apple within 10 minutes, or die |
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
- Curses are plain mob effects: `dungeontotems:heartbreak`, `morphed`, `zombified`, `rampage` (shown as Rage),
  `lonely`, `breathless`, `insomnia` and the rule curses.
- Datapacks can change `dungeontotems:morph_pool` (entity type tag).

Licence: PolyForm Shield 1.0.0.
