This mod is designed specifically for the [Dungeon Train modpack](https://modrinth.com/modpack/bEFyz3ji) / [mod](https://modrinth.com/mod/dungeon-train), but can be used independently.

## What it does

Dungeon Totems adds eight Totems of Undying that still save your life, but leave a curse behind. Each one pops just like the vanilla totem: you stay alive on half a heart with Regeneration, Absorption and Fire Resistance. Then the curse starts.

| Totem | After it saves you |
|---|---|
| Weakness Totem | Weakness for 20 minutes |
| Fatigue Totem | Mining Fatigue for 20 minutes |
| Heartbreak Totem | 2 fewer hearts for 20 minutes |
| Blindness Totem | Blindness for 20 seconds |
| Morphing Totem | You become a random animal for 20 seconds, with its size, speed, health and jump. Monsters leave you alone until you change back |
| Zombified Totem | You become a zombie. You burn in daylight and iron golems hunt you. Eat a golden apple or take a potion of weakness within 10 minutes, or die |
| Rampage Totem | Kill 3 players or Player Mobs within 15 minutes, or die |
| Void Totem | Saves you only from the void and deadly falls, and pulls you back to the last solid ground you stood on |

- **Evokers drop them.** An evoker drops one of the eight at random instead of the vanilla Totem of Undying. Its emeralds are unchanged.
- **Hold it in either hand.** A vanilla totem in hand is used first.
- **Milk won't save you.** It doesn't lift the Morphed, Zombified or Rampage curses.

## What it doesn't do

- It doesn't add crafting recipes. Evokers are the only way to get them outside creative mode.
- It doesn't change the vanilla Totem of Undying, apart from evokers no longer dropping it.

## Setup

- NeoForge 1.21.1.
- Install it on both the server and every client. The client draws the morph forms.
- No config. Datapacks can change which totems evokers drop (item tag `balancedtotems:evoker_totems`), which animals you can turn into (`balancedtotems:morph_pool`) and which kills count for Rampage (`balancedtotems:rampage_targets`).
- It only talks to your own game: the server tells nearby players when a totem pops or someone changes form. Nothing is sent anywhere else.

## For mod authors

`BalancedTotemsApi.setRescueTarget` picks where the Void Totem sends a player. The curses are plain mob effects: `balancedtotems:heartbreak`, `morphed`, `zombified` and `rampage`.

## Licence

PolyForm Shield 1.0.0. Source: [GitHub](https://github.com/bh679/balancedtotems-mc).
