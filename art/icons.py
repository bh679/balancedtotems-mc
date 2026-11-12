"""The curse effect icons (18x18), each built from vanilla sprites."""
from PIL import Image

from pixel import Vanilla, cols, over, render

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


def lonely(vanilla: Vanilla) -> Image.Image:
    """The vanilla empty health-bar heart at 2x."""
    return vanilla.load(HEART + "container.png").resize((18, 18), Image.NEAREST)


def vow_of_peace(vanilla: Vanilla) -> Image.Image:
    """The vanilla poppy."""
    return over(Image.new("RGBA", (18, 18)), vanilla.load("block/poppy.png"), (1, 1))


def melon_diet(vanilla: Vanilla) -> Image.Image:
    """The vanilla melon slice."""
    return over(Image.new("RGBA", (18, 18)), vanilla.load("item/melon_slice.png"), (1, 1))


def restrained(vanilla: Vanilla) -> Image.Image:
    """The vanilla barrel top, struck through in red."""
    icon = over(Image.new("RGBA", (18, 18)), vanilla.load("block/barrel_top.png"), (1, 1))
    for t in range(18):
        for x in (t, t + 1):
            if x < 18:
                icon.putpixel((x, t), (220, 30, 30, 255))
    return icon


def insomnia(vanilla: Vanilla) -> Image.Image:
    """A Zzz rising to the top right, three sizes of Z with a pale shade on the bottom stroke (vanilla has no Zzz)."""
    shades = cols(W="ffffff", S="b8c6e0")
    icon = Image.new("RGBA", (18, 18))
    icon.alpha_composite(render(["WWW", ".W.", "W..", "SSS"], shades), (1, 14))
    icon.alpha_composite(render(["WWWW", "..W.", ".W..", "SSSS"], shades), (5, 9))
    icon.alpha_composite(render(["WWWWWW", "....W.", "...W..", "..W...", ".W....", "SSSSSS"], shades), (11, 2))
    return icon


# Hand-drawn air bubbles in the vanilla air sprite's four colours (a shrunk copy blurs).
BUBBLE_COLOURS = cols(a="000000", b="0094ff", c="d1ebff", d="56b8ff")
BUBBLE_7 = ["..aaa..", ".abbba.", "abc..da", "ab...da", "ab...da", ".addda.", "..aaa.."]
BUBBLE_6 = ["..aa..", ".abba.", "abc.da", "ab..da", ".adda.", "..aa.."]


def breathless(vanilla: Vanilla) -> Image.Image:
    """The vanilla air bubble rising, with two smaller ones below it, like the Totem of Breathlessness."""
    icon = Image.new("RGBA", (18, 18))
    icon.alpha_composite(vanilla.load("gui/sprites/hud/air.png"), (8, 0))
    icon.alpha_composite(render(BUBBLE_6, BUBBLE_COLOURS), (1, 10))
    icon.alpha_composite(render(BUBBLE_7, BUBBLE_COLOURS), (9, 11))
    return icon


ICONS = {
    "heartbreak": heartbreak,
    "morphed": morphed,
    "zombified": zombified,
    "rampage": rampage,
    "lonely": lonely,
    "vow_of_peace": vow_of_peace,
    "melon_diet": melon_diet,
    "restrained": restrained,
    "insomnia": insomnia,
    "breathless": breathless,
}
