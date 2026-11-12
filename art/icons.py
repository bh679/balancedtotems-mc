"""The four curse effect icons (18x18), each built from vanilla sprites."""
from PIL import Image

from pixel import Vanilla, over

HEART = "gui/sprites/hud/heart/"
# Zigzag crack through the 2x heart: row -> column.
CRACK = {2: 9, 3: 9, 4: 8, 5: 8, 6: 9, 7: 9, 8: 8, 9: 8, 10: 9, 11: 9, 12: 8, 13: 8, 14: 9, 15: 9}


def outlined_face(face: Image.Image, outline: tuple) -> Image.Image:
    """An 8x8 mob face at 2x inside a 1px outline with open corners."""
    icon = Image.new("RGBA", (18, 18))
    for x in range(18):
        for y in range(18):
            if (x in (0, 17)) != (y in (0, 17)):
                icon.putpixel((x, y), (*outline, 255))
    icon.alpha_composite(face.resize((16, 16), Image.NEAREST), (1, 1))
    return icon


def heartbreak(vanilla: Vanilla) -> Image.Image:
    """The vanilla health-bar heart at 2x with a black zigzag crack."""
    heart = over(vanilla.load(HEART + "container.png"), vanilla.load(HEART + "full.png"))
    icon = heart.resize((18, 18), Image.NEAREST)
    for y, x in CRACK.items():
        icon.putpixel((x, y), (0, 0, 0, 255))
    return icon


def morphed(vanilla: Vanilla) -> Image.Image:
    """The vanilla pig's face with its snout, at 2x."""
    skin = vanilla.load("entity/pig/pig.png")
    face = over(skin.crop((8, 8, 16, 16)), skin.crop((17, 17, 21, 20)), (2, 4))
    return outlined_face(face, (58, 22, 30))


def zombified(vanilla: Vanilla) -> Image.Image:
    """The vanilla zombie villager's face (plains), at 2x."""
    skin = vanilla.load("entity/zombie_villager/zombie_villager.png")
    skin.alpha_composite(vanilla.load("entity/zombie_villager/type/plains.png"))
    return outlined_face(skin.crop((8, 9, 16, 17)), (14, 30, 10))


def rampage(vanilla: Vanilla) -> Image.Image:
    """Vanilla Weakness sword between a half heart and an empty heart: kill, or lose them."""
    container = vanilla.load(HEART + "container.png")
    half = over(container, vanilla.load(HEART + "half.png"))
    icon = vanilla.load("mob_effect/weakness.png")
    icon.alpha_composite(half, (0, 0))
    icon.alpha_composite(container, (9, 9))
    return icon


ICONS = {
    "heartbreak": heartbreak,
    "morphed": morphed,
    "zombified": zombified,
    "rampage": rampage,
}
