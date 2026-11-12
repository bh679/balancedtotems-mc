#!/usr/bin/env python3
"""Generate every Balanced Totems texture from the vanilla totem.

Usage: python3 art/make_textures.py <path to vanilla totem_of_undying.png>

The vanilla texture comes from the Minecraft client jar (NeoForge's moddev
"client-extra-aka-minecraft-resources" jar has it under
assets/minecraft/textures/item/). Each totem gradient-maps the gold body to its
curse colours by brightness and recolours the emerald eyes. Each curse effect
icon (18x18) is its totem, centred.
"""
import colorsys
import pathlib
import sys

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
ITEMS = ROOT / "src/main/resources/assets/balancedtotems/textures/item"
EFFECTS = ROOT / "src/main/resources/assets/balancedtotems/textures/mob_effect"

# name: (darkest body colour, lightest body colour, eye colour)
TOTEMS = {
    "weakness_totem": ((58, 58, 66), (214, 214, 222), (72, 77, 72)),
    "fatigue_totem": ((54, 46, 14), (190, 172, 98), (74, 66, 23)),
    "heartbreak_totem": ((96, 10, 28), (250, 150, 165), (255, 214, 224)),
    "blindness_totem": ((14, 14, 22), (98, 98, 120), (12, 12, 12)),
    "morphing_totem": ((60, 26, 112), (226, 196, 255), (110, 230, 160)),
    "zombified_totem": ((30, 66, 34), (150, 206, 120), (40, 60, 160)),
    "rampage_totem": ((80, 10, 0), (255, 170, 60), (255, 40, 20)),
    "void_totem": ((18, 6, 34), (130, 90, 190), (190, 120, 255)),
}

# Effect icons reuse the totem that causes them.
EFFECT_ICONS = {
    "heartbreak": "heartbreak_totem",
    "morphed": "morphing_totem",
    "zombified": "zombified_totem",
    "rampage": "rampage_totem",
}


def lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def is_eye(rgb):
    hue = colorsys.rgb_to_hsv(*[c / 255 for c in rgb])[0] * 360
    return 100 <= hue <= 180


def recolour(vanilla, dark, light, eye):
    out = vanilla.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if is_eye((r, g, b)):
                value = max(r, g, b) / 255
                px[x, y] = (*lerp((0, 0, 0), eye, 0.6 + 0.4 * value), a)
            else:
                value = max(r, g, b) / 255
                t = (value - 0.5) / 0.5  # vanilla body spans value 0.52 to 0.99
                px[x, y] = (*lerp(dark, light, max(0.0, min(1.0, t))), a)
    return out


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    vanilla = Image.open(sys.argv[1]).convert("RGBA")
    ITEMS.mkdir(parents=True, exist_ok=True)
    EFFECTS.mkdir(parents=True, exist_ok=True)
    made = {}
    for name, (dark, light, eye) in TOTEMS.items():
        made[name] = recolour(vanilla, dark, light, eye)
        made[name].save(ITEMS / f"{name}.png")
    for effect, totem in EFFECT_ICONS.items():
        icon = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
        icon.paste(made[totem], (1, 1))
        icon.save(EFFECTS / f"{effect}.png")
    print(f"wrote {len(made)} totems and {len(EFFECT_ICONS)} effect icons")


if __name__ == "__main__":
    main()
