This mod is designed specifically for the [Dungeon Train modpack](https://modrinth.com/modpack/bEFyz3ji) / [mod](https://modrinth.com/mod/dungeon-train), but can be used independently.

## What it does

Dungeon Totems adds Totems of Undying that still save your life, but leave a curse behind. Each one pops just like the vanilla totem: you stay alive on half a heart with Regeneration, Absorption and Fire Resistance (the Void, Feather and Boom Totems skip Fire Resistance). Then the curse starts.

| | Totem | After it saves you |
|---|---|---|
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/weakness_totem.png" width="32" alt="Totem of Weakness"> | Totem of Weakness | Weakness for 20 minutes |
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/fatigue_totem.png" width="32" alt="Totem of Fatigue"> | Totem of Fatigue | Mining Fatigue II for 20 minutes |
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/heartbreak_totem.png" width="32" alt="Totem of Heartbreak"> | Totem of Heartbreak | 2 fewer hearts for 20 minutes |
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/blindness_totem.png" width="32" alt="Totem of Blindness"> | Totem of Blindness | Blindness for 30 seconds |
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/morphing_totem.png" width="32" alt="Totem of Morphing"> | Totem of Morphing | You become a random animal for 30 seconds, seen in third person, with its size, speed, health and jump. Monsters leave you alone |
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/zombified_totem.png" width="32" alt="Totem of Zombification"> | Totem of Zombification | You become a zombie. Get Weakness, then eat a golden apple within 10 minutes, like curing a zombie villager, or die |
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/rampage_totem.png" width="32" alt="Totem of Rage"> | Totem of Rage | Kill 3 players or Player Mobs within 15 minutes, or die |
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/pacifism_totem.png" width="32" alt="Totem of Pacifism"> | Totem of Pacifism | Hurt anything within 20 minutes and you die |
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/melon_totem.png" width="32" alt="Totem of Melon Diet"> | Totem of Melon Diet | Eat anything but melon within 20 minutes and you die |
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/restraint_totem.png" width="32" alt="Totem of Restraint"> | Totem of Restraint | Open a chest, barrel or ender chest within 20 minutes and you die |
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/loneliness_totem.png" width="32" alt="Totem of Loneliness"> | Totem of Loneliness | Make 3 friends within 10 minutes, or die. A friend is a Player Mob you swap gifts with |
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/void_totem.png" width="32" alt="Void Totem"> | Void Totem | Only saves you from the void. It teleports you like a chorus fruit, onto ground near where you fell. In Dungeon Train it puts you back on the train |
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/feather_totem.png" width="32" alt="Feather Totem"> | Feather Totem | Only saves you from falling |
| <img src="https://raw.githubusercontent.com/bh679/balancedtotems-mc/main/listing/totems/boom_totem.png" width="32" alt="Boom Totem"> | Boom Totem | Only saves you from explosions |

- **Evokers drop them.** An evoker drops one at random instead of the vanilla Totem of Undying. Its emeralds are unchanged.
- **Hold it in either hand.** A vanilla totem in hand is used first.
- **Curses stack.** A later pop never clears an earlier curse. The same curse again goes up a level with a fresh timer, and curses come back after you respawn.
- **Milk won't save you.** It doesn't lift the Morphed, Zombified or Rage curses.
- **No Player Mobs?** Villagers stand in: Rage counts villager kills, and Loneliness counts a villager you trade with, feed or cure.

## Setup

- NeoForge 1.21.1.
- Install it on both the server and every client. The client draws the morph forms.
- No config. Datapacks can change which totems evokers drop (item tag `balancedtotems:evoker_totems`), which animals you can turn into (`balancedtotems:morph_pool`) and which kills count for Rage (`balancedtotems:rampage_targets`).
- It only talks to your own game: the server tells nearby players when a totem pops or someone changes form. Nothing is sent anywhere else.

## Licence

PolyForm Shield 1.0.0. Source: [GitHub](https://github.com/bh679/balancedtotems-mc).
