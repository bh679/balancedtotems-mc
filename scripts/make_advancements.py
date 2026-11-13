#!/usr/bin/env python3
"""Writes the Dungeon Totems advancement tab and its English text.

    python3 scripts/make_advancements.py

Great and Small gets one criterion per morph form, read from the morph_forms data map, so run this again
whenever a form is added (GameTest ``greatAndSmallHasEveryForm`` fails until you do). Every file carries
the ``dungeontotems:advancements_launched`` condition, so the tab stays off until a release sets
``TotemAdvancements.LAUNCHED``. Progress bars come
free: each curse is its own criterion and requirement, and vanilla counts them.
"""

import json
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "src/main/resources"
OUT = RES / "data/dungeontotems/advancement"
LANG = RES / "assets/dungeontotems/lang/en_us.json"
FORMS = RES / "data/dungeontotems/data_maps/entity_type/morph_forms.json"
ROOT = "terms_and_conditions"

# Curse keys (Curse.key()): live through each one's after-save time, or cure Zombified.
TIMED = ["weakness", "fatigue", "heartbreak", "hunger", "slowness", "breathlessness", "insomnia", "blindness", "morphing"]
DEADLINES_AND_VOWS = ["zombified", "rage", "loneliness", "pacifism", "melon_diet", "restraint"]
LIFESAVERS = ["void", "feather", "boom", "fire"]


def per_key(trigger, field, keys):
    """One criterion and one requirement per key."""
    return {k: {"trigger": "dungeontotems:" + trigger, "conditions": {field: k}} for k in keys}, [[k] for k in keys]


def single(trigger, conditions=None):
    criterion = {"trigger": trigger}
    if conditions:
        criterion["conditions"] = conditions
    return {"done": criterion}, [["done"]]


# id: (parent, icon, frame, title, description, hint, (criteria, requirements))
ADVANCEMENTS = {
    ROOT: (None, "dungeontotems:heartbreak_totem", "task", "Terms and Conditions",
           "Be saved by a dungeon totem", "Its just a totem, right?", single("dungeontotems:totem_popped")),
    "back_on_two_legs": (ROOT, "minecraft:leather", "task", "Back on Two Legs",
                         "Become an animal and change back alive", None,
                         single("dungeontotems:curse_survived", {"curse": "morphing"})),
    "great_and_small": ("back_on_two_legs", "minecraft:lead", "challenge", "Great and Small",
                        "Be every animal a Totem of Morphing can make you", "Be them all", None),
    "brains_optional": (ROOT, "minecraft:golden_apple", "goal", "Brains Optional",
                        "Become a zombie and be cured", "Die. Un-die. Un-un-die.",
                        single("dungeontotems:curse_survived", {"curse": "zombified"})),
    "waiting_it_out": (ROOT, "minecraft:clock", "goal", "Waiting It Out",
                       "Live through Weakness, Fatigue, Heartbreak, Hunger, Slowness, Breathlessness, Insomnia, Blindness and Morphing after a save", None,
                       per_key("curse_survived", "curse", TIMED)),
    "promises_kept": (ROOT, "minecraft:writable_book", "challenge", "Promises Kept",
                      "Cure Zombification, and live through Rage, Loneliness, Pacifism, Melon Diet and Restraint after a save", None,
                      per_key("curse_survived", "curse", DEADLINES_AND_VOWS)),
    "close_calls": (ROOT, "minecraft:tnt", "goal", "Close Calls",
                    "Be saved by the Void, Feather, Boom and Fire totems", "Even with limits, theyre useful",
                    per_key("curse_survived", "curse", LIFESAVERS)),
    # Granted by advancement/TabComplete once the rest of the tab is earned.
    "terms_and_conditions_fulfilled": (ROOT, "minecraft:totem_of_undying", "challenge", "Terms and Conditions Fulfilled",
                                       "Earn every Dungeon Totems advancement", None,
                                       ({"every_advancement": {"trigger": "minecraft:impossible"}}, [["every_advancement"]])),
}


def write_lang(keys):
    """Sets ``keys`` in en_us.json, keeping the rest of the file as it is (blank lines included)."""
    text = LANG.read_text()
    existing = json.loads(text)
    lines = [l for l in text.rstrip().rstrip("}").rstrip().splitlines()
             if not any(l.strip().startswith(json.dumps(k) + ":") for k in keys if k in existing)]
    body = "\n".join(lines).rstrip().rstrip(",")
    added = ",\n".join(f"  {json.dumps(k)}: {json.dumps(v, ensure_ascii=False)}" for k, v in keys.items())
    LANG.write_text(body + ",\n\n" + added + "\n}\n")
    json.loads(LANG.read_text())  # still valid


def main():
    forms = sorted(json.loads(FORMS.read_text())["values"])
    lang = {}
    OUT.mkdir(parents=True, exist_ok=True)
    for name, (parent, icon, frame, title, description, hint, rules) in ADVANCEMENTS.items():
        if rules is None:
            rules = per_key("became_form", "form", forms)
        criteria, requirements = rules
        key = f"advancements.dungeontotems.{name}"
        display = {"icon": {"id": icon}, "title": {"translate": key + ".title"},
                   "description": {"translate": key + ".description"}, "frame": frame,
                   "show_toast": True, "announce_to_chat": True, "hidden": False}
        if parent is None:
            display["background"] = "minecraft:textures/block/crying_obsidian.png"
        # Off until launch: advancement/TotemAdvancements.LAUNCHED.
        advancement = {"neoforge:conditions": [{"type": "dungeontotems:advancements_launched"}]}
        if parent:
            advancement["parent"] = f"dungeontotems:{parent}"
        advancement.update({"display": display, "criteria": criteria, "requirements": requirements})
        (OUT / f"{name}.json").write_text(json.dumps(advancement, indent=2) + "\n")
        lang[key + ".title"] = title
        lang[key + ".description"] = description
        if hint:
            lang[key + ".hint"] = hint
    lang["advancements.dungeontotems.great_and_small.unknown"] = "????"
    write_lang(lang)
    print(f"{len(ADVANCEMENTS)} advancements, {len(forms)} forms")


if __name__ == "__main__":
    main()
