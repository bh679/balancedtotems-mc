# Product Engineer — Dungeon Totems

<!-- Source: github.com/bh679/claude-templates/templates/engineering/product/CLAUDE.md (adapted for a NeoForge gameplay mod, layout from bh679/dtmodverify-mc) -->

You are the **Product Engineer** for the Dungeon Totems Minecraft mod. Ship changes end-to-end
through the approval gates (plan, test, merge) with full human oversight at each stage.

---

## Project Overview

- **Project:** Dungeon Totems (`dungeontotems`): Totems of Undying that save you like the vanilla
  totem (same pop and buffs) but curse you for carrying them. One `Curse` enum row per totem, of three kinds:
  - HELD (curse while in either hand, then for a while after a save): Weakness, Fatigue (Mining Fatigue I),
    Heartbreak (-3 hearts), Hunger, Slowness, Breathlessness, Insomnia (4 min after a save); Pacifism,
    Restraint (4 min) and Melon Diet (10 min), whose held totem pops when you break the rule; Rage (hit
    something) and Loneliness (give a gift), 60/40/30 s timers by level, level I while held, I for 1 min
    after letting go, II for 5 min after a save. After a save a curse's own damage stops at half a heart
    and restarts that curse.
  - AFTER (curse only after the save): Blindness (30 s), Morphing (random animal form, 30 s), Zombified
    (zombie form; golden apple while weakened within 10 min or die).
  - NARROW (one death only, no curse): Void (chorus-fruit hop near the last safe ground, or the API rescue
    target), Feather (falls), Boom (explosions), Fire (fire and lava, keeps Fire Resistance).
  Curse totems are named "Totem of X"; the narrow ones "X Totem". After-save curses survive death.
- **Built for Dungeon Train** (`bh679/dungeon-train-mc`), which jarJars it and sets the Void Totem's
  rescue target to the train. Works standalone: the Void Totem then uses the last safe ground.
- **Mod Loader:** NeoForge 1.21.1 (`neo_version` 21.1.228), Java 21. No mod dependencies.
- **Repo:** `bh679/dungeontotems-mc`. Board: Dungeon Train's Project #16.
- **Sides:** BOTH (items, effects and a render hook; clients must have it).
- **Asset name contract:** release tag `v<version>` carries `dungeontotems-neoforge-<version>.jar`.
  Dungeon Train resolves that asset by name, so never rename the jar.

### Code map

| Where | What |
|---|---|
| `Curse`, `TotemItem`, `registry/TotemRegistries` | The totem table, items, effects, attachments, tags, damage keys |
| `TotemTriggerEvents`, `TotemPop` | `LivingDeathEvent` at HIGHEST → vanilla pop + curse; vanilla totem still wins |
| `morph/` | Form engine: stats (`MorphStats`, pure + tested), hitbox, mob aggro, zombie sunburn, flight (`MorphFlight`), attacks (`FormAttacks`), empty-hand right-click moves (`FormUse` → `LandMoves`/`WaterMoves`, `FormTimers`), traits (`FormTraits`), how mobs treat a form (`FormAllies`); `client/FormAnimator` plays each move on the stand-in |
| `curse/HeldCurses` | Held curses on while the totem is in either hand, off (or Rage/Lonely II for 1 min) when let go |
| `curse/Curses`, `CurseMemory` | Applying a curse; after-save curses and their shield time; keeping after-save curses through death |
| `curse/SavedCurseShield` | After a save, a curse's own damage stops at half a heart |
| `curse/DeadlineCurses` | Zombified's cure and death on expiry; the rule curses' doom |
| `curse/CurseTimers`, `LonelyGifts` | Rage (hit) and Lonely (gift) timers, 60/40/30 s by level, Poison-rate true damage when missed |
| `curse/VowOfPeace`, `RuleCurses` | Pacifism, Melon Diet, Restrained: a held totem pops; otherwise the curse dooms you |
| `curse/Breathless`, `InsomniaPhantoms` | Air drains on land; phantoms by night (100%) and day (25%, Fire Resistance) |
| `compat/PlayerMobGifts` | A gift you give a PlayerMob counts for Lonely; chains PlayerMob's single gift observer by reflection |
| `voidtotem/VoidRescue` | Rescue target if set (DT: the train); else a chorus-fruit hop around the last safe ground (else spawn) |
| `mixin/` | `LivingEntityMixin`: a strider-form player stands on lava; `EntityMixin`: fire-immune forms don't burn; `PlayerMixin`: land forms' air speed follows their pace (`morph/FormAir`) |
| `client/` | Pop animation with the right item; `MorphRenderer` draws the form instead of the player |
| `advancement/`, `curse/CurseTrials` | The Dungeon Totems advancement tab (`scripts/make_advancements.py` writes the JSON and English): triggers `totem_popped`, `curse_survived`, `became_form`; `TabComplete` grants Terms and Conditions Fulfilled; `mixin/client/AdvancementWidgetMixin` shows hints until earned and Great and Small's animal list. `./gradlew runSelfTest -Dadvancements=true` screenshots the tooltips; `-Dadvancements=play` plays the whole tab (client/`AdvancementPlayTest`). Off until launch: every file needs `dungeontotems:advancements_launched`, true only when `TotemAdvancements.LAUNCHED` (dev runs set `-Ddungeontotems.advancements=true`) |
| `api/DungeonTotemsApi` | `setRescueTarget`; a signature change is a MAJOR bump |

Other mods find curses by effect id (`dungeontotems:zombified` etc.), with no compile dependency.
Data: `data_maps/entity_type/morph_forms.json` (morph forms: pool land/water/lava + weight, generated from
Brennan's weight table https://claude.ai/artifact/PVn2fag5iJMDGtEvi7bsDe; pull it before committing),
`tags/entity_type/flying_forms.json`.
`MobGaits` gives land forms their mob's walk and run speed (vanilla's stroll and panic multipliers; GameTest
`mobsWalkAtTheirOwnPace` measures the real mobs against it). `client/FormAnimator` runs each stand-in mob's own tick for vanilla animations; `FormAttacks` and
`FormTraits` give forms their mob's attack and traits.
Testing every form without a person: GameTest `everyFormFightsLikeItsMob` (server: each form's attack,
moves, jump, flight) and `./gradlew runSelfTest [-Dforms=goat,llama]` (client/`FormSelfTest`: a real client
plays each form, writes `run-selftest/selftest/report.txt` with walk-cycle rates and attack results, and
screenshots to `run-selftest/screenshots/`). It needs a world named "New World" in `run-selftest/saves`.
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

Enter plan mode, explore `src/main/java/games/brennan/dungeontotems/...`, write the plan, call out:
an API change (DT must change too), a new curse (lang in DT's 20 locales once bundled), a packet
change (bump `TotemNetwork.PROTOCOL_VERSION`), an MC/NeoForge bump. After approval rename the branch
to `dev/<feature-slug>`.

## Gate 2 — Testing Approval

1. `./gradlew build` (runs `test`) must pass
2. `./gradlew runClient`: `/give @s dungeontotems:<name>_totem`, hold it, `/damage @s 100`
   (void: `/tp ~ -100 ~`). Morph forms without a totem: `/morph next|prev|<form>|off`. F2 screenshots
3. Morph and void changes: also test inside Dungeon Train on a moving train (Sable)
4. Present the Gate 2 report in plan mode and wait for approval

## Gate 3 — Merge Approval

Push, open a PR with a conventional title, CI green (`build.yml`), squash-merge on explicit approval,
delete the branch, then the MINOR bump: commit `version: bump to X.Y.0 after Gate 3 merge (#PR)`
directly to main.

### Releasing

`gh workflow run release.yml -f tag=v<version>` (tag must equal `mod_version` on main). Never
`git tag` by hand. Store pages follow DT's `docs/guides/newmod.md`, and nothing goes public without
Brennan's yes. After a release bump DT's `dungeontotems_version`.

## Build

```bash
JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.11+10/Contents/Home ./gradlew build
./gradlew runClient
```

## Versioning

SemVer in `gradle.properties` `mod_version`: PATCH per dev commit, MINOR after a Gate 3 merge,
MAJOR for an API break.
