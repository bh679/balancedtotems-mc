#!/usr/bin/env python3
"""Generate every Balanced Totems texture.

Usage: python3 art/make_textures.py <client resources jar>

The jar is NeoForge's moddev "client-extra-aka-minecraft-resources" jar for
1.21.1 (e.g. build/moddev/artifacts/neoforge-21.1.228-client-extra-aka-minecraft-resources.jar
in any NeoForge project). The totems are hand-authored grids over the vanilla
totem's shape (totems.py); the curse effect icons combine vanilla sprites:
health-bar hearts, effect icons and mob faces (icons.py). Some totems also
borrow a vanilla sprite (the Mining Fatigue spoon, the zombie villager skin).

Also writes art/alternates/, designs kept for later but not used by the mod.
"""
import pathlib
import sys

from icons import ICONS
from pixel import Vanilla
from totems import TOTEMS, pig_head_alternate

ROOT = pathlib.Path(__file__).resolve().parent.parent
ITEMS = ROOT / "src/main/resources/assets/balancedtotems/textures/item"
EFFECTS = ROOT / "src/main/resources/assets/balancedtotems/textures/mob_effect"
ALTERNATES = ROOT / "art/alternates"


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    vanilla = Vanilla(sys.argv[1])
    for folder in (ITEMS, EFFECTS, ALTERNATES):
        folder.mkdir(parents=True, exist_ok=True)
    for name, make in TOTEMS.items():
        make(vanilla).save(ITEMS / f"{name}.png")
    for name, make in ICONS.items():
        make(vanilla).save(EFFECTS / f"{name}.png")
    pig_head_alternate(vanilla).save(ALTERNATES / "morphing_pig_head_big_snout.png")
    print(f"wrote {len(TOTEMS)} totems, {len(ICONS)} effect icons and 1 alternate")


if __name__ == "__main__":
    main()
