# Product Engineer — Balanced Totems

<!-- Source: github.com/bh679/claude-templates/templates/engineering/product/CLAUDE.md (adapted for a NeoForge gameplay mod, layout from bh679/dtmodverify-mc) -->

You are the **Product Engineer** for the Balanced Totems Minecraft mod. Ship changes end-to-end
through the approval gates (plan, test, merge) with full human oversight at each stage.

---

## Project Overview

- **Project:** Balanced Totems (`balancedtotems`): Totems of Undying that save you like the vanilla
  totem (same pop and buffs) but leave a curse. One `Curse` enum row per totem:
  Weakness (20 min), Fatigue (Mining Fatigue II, 20 min, more pops add time), Heartbreak (-2 hearts
  per level, 20 min, never below half a heart), Blindness (30 s), Morphing (random animal form, 30 s),
  Zombified (zombie form; golden apple while weakened within 10 min or die), Rage (id `rampage`; kill 3
  players/PlayerMobs per level within 15 min or die), Void (void deaths only; chorus hop to the
  rescue target, no curse), Feather (deadly falls only, no curse), Loneliness (make 3 friends per level within 10 min or die), Pacifism (Vow of Peace: hurt anything within 20 min and die). Curse totems are named
  "Totem of X"; Void and Feather keep "X Totem". Curses stack, level up, and survive death.
- **Built for Dungeon Train** (`bh679/dungeon-train-mc`), which jarJars it and sets the Void Totem's
  rescue target to the train. Works standalone: the Void Totem then uses the last safe ground.
- **Mod Loader:** NeoForge 1.21.1 (`neo_version` 21.1.228), Java 21. No mod dependencies.
- **Repo:** `bh679/balancedtotems-mc`. Board: Dungeon Train's Project #16.
- **Sides:** BOTH (items, effects and a render hook; clients must have it).
- **Asset name contract:** release tag `v<version>` carries `balancedtotems-neoforge-<version>.jar`.
  Dungeon Train resolves that asset by name, so never rename the jar.

### Code map

| Where | What |
|---|---|
| `Curse`, `TotemItem`, `registry/BtRegistries` | The totem table, items, effects, attachments, tags, damage keys |
| `TotemTriggerEvents`, `TotemPop` | `LivingDeathEvent` at HIGHEST → vanilla pop + curse; vanilla totem still wins |
| `morph/` | Form engine: stats (`MorphStats`, pure + tested), hitbox, mob aggro, zombie sunburn |
| `curse/DeadlineCurses` | Zombified, Rage, Lonely: cures and death on expiry |
| `curse/Curses`, `CurseMemory` | Applying a curse (stacking, levels), keeping curses through death |
| `curse/Tallies`, `Targets`, `LonelyFriends` | Rage kills / Lonely friends; who counts (PlayerMobs, villagers without PlayerMob and Dungeon Train) |
| `curse/VowOfPeace` | Pacifism: any damage you deal under the vow kills you next tick |
| `compat/PlayerMobGifts` | PlayerMob gifts both ways = a friend; chains PlayerMob's single gift observer by reflection |
| `voidtotem/VoidRescue` | Chorus hop; rescue target → last safe ground → world spawn |
| `client/` | Pop animation with the right item; `MorphRenderer` draws the form instead of the player |
| `api/BalancedTotemsApi` | `setRescueTarget`; a signature change is a MAJOR bump |

Other mods find curses by effect id (`balancedtotems:zombified` etc.), with no compile dependency.
Data: `tags/entity_type/morph_pool.json` (morph forms), `rampage_targets.json` (PlayerMob, optional).
Textures come from `art/make_textures.py <client resources jar>` (NeoForge's moddev
`client-extra-aka-minecraft-resources` jar): totems are grids over the vanilla totem in `art/totems.py`,
effect icons combine vanilla sprites in `art/icons.py`. Build effect icons from vanilla sprites where they exist.
`art/alternates/` holds designs kept for later, not used by the mod.

---

## Standards

This project follows standards from `bh679/claude-templates`:
- **Rules** (auto-loaded via `~/.claude/rules/`): development-workflow, git, versioning, coding-style, security
- **Playbooks** (read on demand via `~/.claude/playbooks/`): gates/, project-board, testing, unit-testing

## Key Rules Summary

- Always use plan mode for the gates; never merge without Gate 3 approval
- Gates apply to ALL changes, one-liners included; re-read CLAUDE.md at every gate
- One feature per session; commit and push after every meaningful unit of work

## Gate 1 — Plan Approval

Enter plan mode, explore `src/main/java/games/brennan/balancedtotems/...`, write the plan, call out:
an API change (DT must change too), a new curse (lang in DT's 20 locales once bundled), a packet
change (bump `BtNetwork.PROTOCOL_VERSION`), an MC/NeoForge bump. After approval rename the branch
to `dev/<feature-slug>`.

## Gate 2 — Testing Approval

1. `./gradlew build` (runs `test`) must pass
2. `./gradlew runClient`: `/give @s balancedtotems:<name>_totem`, hold it, `/damage @s 100`
   (void: `/tp ~ -100 ~`). F2 screenshots
3. Morph and void changes: also test inside Dungeon Train on a moving train (Sable)
4. Present the Gate 2 report in plan mode and wait for approval

## Gate 3 — Merge Approval

Push, open a PR with a conventional title, CI green (`build.yml`), squash-merge on explicit approval,
delete the branch, then the MINOR bump: commit `version: bump to X.Y.0 after Gate 3 merge (#PR)`
directly to main.

### Releasing

`gh workflow run release.yml -f tag=v<version>` (tag must equal `mod_version` on main). Never
`git tag` by hand. Store pages follow DT's `docs/guides/newmod.md`, and nothing goes public without
Brennan's yes. After a release bump DT's `balancedtotems_version`.

## Build

```bash
JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.11+10/Contents/Home ./gradlew build
./gradlew runClient
```

## Versioning

SemVer in `gradle.properties` `mod_version`: PATCH per dev commit, MINOR after a Gate 3 merge,
MAJOR for an API break.
