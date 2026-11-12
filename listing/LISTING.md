This mod is designed specifically for the [Dungeon Train modpack](https://modrinth.com/modpack/bEFyz3ji) / [mod](https://modrinth.com/mod/dungeon-train), but can be used independently.

## What it does

Dungeon Totems adds Totems of Undying that still save your life, but curse you for carrying them. Each one pops just like the vanilla totem: you stay alive on half a heart with Regeneration, Absorption and Fire Resistance (the Void, Feather and Boom Totems skip Fire Resistance).

| Totem | While held | After it saves you |
|---|---|---|
| Totem of Weakness | Weakness | Weakness for 4 minutes |
| Totem of Fatigue | Mining Fatigue | Mining Fatigue for 4 minutes |
| Totem of Heartbreak | 3 fewer hearts | 3 fewer hearts for 4 minutes |
| Totem of Hunger | Hunger | Hunger for 4 minutes |
| Totem of Slowness | Slowness | Slowness for 4 minutes |
| Totem of Rage | Hit something every 60 seconds, or take damage until you do | Rage II for 5 minutes: every 40 seconds |
| Totem of Loneliness | Give a gift every 60 seconds, or take damage until you do | Lonely II for 5 minutes: every 40 seconds |
| Totem of Pacifism | Hurt anything and it breaks, taking your life and giving it back | Vow of Peace for 4 minutes |
| Totem of Melon Diet | Eat anything but melon and it breaks, taking your life and giving it back | Melon Diet for 10 minutes |
| Totem of Restraint | Open a chest, barrel or ender chest and it breaks, taking your life and giving it back | Restrained for 4 minutes |
| Totem of Breathlessness | You lose air even on land, and start to drown | Breathless for 4 minutes |
| Totem of Insomnia | Phantoms hunt you at night, and sometimes by day | Insomnia for 4 minutes |
| Totem of Blindness | | Blindness for 30 seconds |
| Totem of Morphing | | You become a random animal for 30 seconds, with its size, speed, health and jump. Monsters leave you alone |
| Totem of Zombification | | You become a zombie. Get Weakness, then eat a golden apple within 10 minutes, like curing a zombie villager, or die |
| Void Totem | | Only saves you from the void. It teleports you like a chorus fruit, onto ground near where you fell. In Dungeon Train it puts you back on the train |
| Feather Totem | | Only saves you from falling |
| Boom Totem | | Only saves you from explosions |
| Fire Totem | | Only saves you from fire and lava |

- **Evokers drop them.** An evoker drops one at random instead of the vanilla Totem of Undying. Its emeralds are unchanged.
- **Hold it in either hand.** A vanilla totem in hand is used first.
- **Let go and it ends.** A held curse comes off when you put the totem away. Rage and Loneliness stay at level II for a minute.
- **After a save, a curse can't kill you.** A broken rule, a missed timer or drowning on land leaves you on half a heart, and the curse comes back after you respawn.
- **Milk won't save you.** It doesn't lift the Morphed, Zombified or Rage curses.
- **Gifts.** Trading with a villager, food a villager picks up, or a gift to a Player Mob all count for Loneliness.

## Setup

- NeoForge 1.21.1.
- Install it on both the server and every client. The client draws the morph forms.
- No config. Datapacks can change which totems evokers drop (item tag `dungeontotems:evoker_totems`) and which animals you can turn into (`dungeontotems:morph_pool`).
- It only talks to your own game: the server tells nearby players when a totem pops or someone changes form. Nothing is sent anywhere else.

## Licence

PolyForm Shield 1.0.0. Source: [GitHub](https://github.com/bh679/dungeontotems-mc).
