"""The eight totems (16x16). Each is a grid over the vanilla totem's shape.

Grid keys: '0'..'6' are body tones darkest to lightest (the vanilla totem's own
seven), 'g'/'h' its light/dark eye pixels, '.' transparent, and any other letter
a colour named in that totem's palette.
"""
from PIL import Image

from pixel import Vanilla, cols, hexc, luminance_ramp, over, ramp, render, scaled

GOLD = ramp("85400f", "a05b23", "c58742", "d1a75d", "eadb84", "f8eea5", "fdfbee")
GOLD_EYES = cols(g="41e67f", h="00bb46")
PINK = ramp("7a3a48", "b45a6c", "d8788a", "e68c9c", "f2a6b2", "f8bec6", "fcdce0")
PIG_FACE = cols(q="9a4456", e="1a0e10", E="fcfcfc", n="fcc4cc", m="a04458")

# The vanilla totem body below the head, shared by most designs.
BODY = [
    ".11110244201111.",
    ".14304322320230.",
    "..001565443100..",
    "....14333320....",
    "....01111110....",
    ".....032220.....",
    ".....032230.....",
    "......0000......",
]
VANILLA = [
    "................",
    ".....111111.....",
    "....14444441....",
    "....04665540....",
    "....03354330....",
    "....06g336g0....",
    "....0gh65gh0....",
    "....02354320....",
] + BODY


def weakness(vanilla: Vanilla) -> Image.Image:
    """Ash grey, droopy arms, solid head; the bottom breaks away in three chunks."""
    grid = [
        "................",
        ".....111111.....",
        "....14444441....",
        "....04665540....",
        "....03354330....",
        "....0ll33ll0....",
        "....06h65h60....",
        "....02h54320....",
        "...1102442011...",
        "..140432k32041..",
        ".1401565k431041.",
        ".430143k3320034.",
        ".31.01k1111k.13.",
        ".00.032..22.000.",
        "....020.....30..",
        ".......230..00..",
    ]
    palette = {**ramp("3c3c46", "5c5c66", "80808a", "9c9ca6", "bcbcc4", "d4d4da", "ececf0"),
               **cols(h="4a524a", l="5c5c66", k="26262e")}
    return render(grid, palette)


def fatigue(vanilla: Vanilla) -> Image.Image:
    """Dark slate, cracked, eyes shut, with the vanilla Mining Fatigue spoon behind it."""
    grid = [
        "................",
        ".....111111.....",
        "....1444k441....",
        "....0466k540....",
        "....0335k330....",
        "....06333360....",
        "....0hh35hh0....",
        "....02354320....",
        ".11110244201111.",
        ".143k432232k230.",
        "..001k65443100..",
        "....1433k320....",
        "....01111k10....",
        ".....032220.....",
        ".....032230.....",
        "......0000......",
    ]
    palette = {**ramp("1c1e30", "30344e", "444a6a", "565c80", "6c7296", "8288aa", "a0a6c4"),
               **cols(h="10121e", k="0c0e18")}
    spoon = vanilla.load("mob_effect/mining_fatigue.png").crop((1, 1, 17, 17))
    return over(spoon, render(grid, palette))


def heartbreak(vanilla: Vanilla) -> Image.Image:
    """Gold totem with half a vanilla health-bar heart on its chest; the other half is an empty container."""
    heart = over(vanilla.load("gui/sprites/hud/heart/container.png"), vanilla.load("gui/sprites/hud/heart/half.png"))
    return over(render(VANILLA, {**GOLD, **GOLD_EYES}), heart, (3, 7))


def blindness(vanilla: Vanilla) -> Image.Image:
    """Gold totem, 3px black blindfold with cut corners and tails off the right."""
    grid = [
        "................",
        ".....111111.....",
        "....14444441....",
        "....04665540....",
        "....WWWWWWWW....",
        "...VVVVVVVVVVV..",
        "....VVVVVVVV.U..",
        "....02354320.WV.",
        ".1111024420111U.",
    ] + BODY[1:]
    return render(grid, {**GOLD, **cols(W="3a3a44", V="24242c", U="16161c")})


def morphing(vanilla: Vanilla) -> Image.Image:
    """Full pig: pink, ears, snout."""
    grid = [
        "....11....11....",
        "....1q1111q1....",
        "....14444441....",
        "....04665540....",
        "....03354330....",
        "....0Ee33eE0....",
        "....04nnnn40....",
        "....02mnnm20....",
    ] + BODY
    return render(grid, {**PINK, **PIG_FACE})


def zombified(vanilla: Vanilla) -> Image.Image:
    """Zombie villager (plains): face and robe colours from the vanilla skin, drawn nose."""
    skin = vanilla.load("entity/zombie_villager/zombie_villager.png")
    skin.alpha_composite(vanilla.load("entity/zombie_villager/type/plains.png"))
    face = skin.crop((8, 9, 16, 17))
    robe = luminance_ramp(skin, (16, 20, 40, 38))
    flesh = luminance_ramp(skin, (8, 8, 16, 18))
    im = Image.new("RGBA", (16, 16))
    for y, row in enumerate(BODY):
        for x, key in enumerate(row):
            if key == ".":
                continue
            hand = (x <= 2 or x >= 13) and y <= 1
            im.putpixel((x, y + 8), (*(flesh if hand else robe)[key], 255))
    for y in range(8):
        for x in range(8):
            colour = face.getpixel((x, y))[:3]
            if x in (0, 7) or y == 0:
                colour = tuple(round(c * 0.6) for c in colour)
            im.putpixel((x + 4, y), (*colour, 255))
    for y in range(5, 9):
        im.putpixel((7, y), (*flesh["3"], 255))
        im.putpixel((8, y), (*flesh["1"], 255))
    im.putpixel((7, 5), (*flesh["4"], 255))
    return im


def rampage(vanilla: Vanilla) -> Image.Image:
    """Blood red, angry brows, glowing eyes."""
    grid = [
        "................",
        ".....111111.....",
        "....14444441....",
        "....04665540....",
        "....0bb44bb0....",
        "....06gb3bg0....",
        "....0gh65hg0....",
        "....02355320....",
    ] + BODY
    palette = {**ramp("3a0606", "6a0e0e", "9a1a14", "b42a1c", "cc4426", "e06a3a", "f0a070"),
               **cols(g="ffe23a", h="ff3a10", b="3a0606")}
    return render(grid, palette)


def void(vanilla: Vanilla) -> Image.Image:
    """Plain dark, dim purple eyes."""
    palette = {**ramp("06060a", "0e0e14", "16161e", "1c1c26", "242430", "2c2c3a", "363648"),
               "g": scaled(hexc("c070ff"), 0.72), "h": scaled(hexc("6a20b0"), 0.72)}
    return render(VANILLA, palette)


def pig_head_alternate(vanilla: Vanilla) -> Image.Image:
    """Unused alternate Morphing design: big-snout pig head on the gold body."""
    head = [
        "...q1......1q...",
        "...qq111111qq...",
        "....14444441....",
        "....04665540....",
        "....0Ee44eE0....",
        "....03nnnn30....",
        "....0nmnnmn0....",
        "....02nnnn20....",
    ]
    return render(head + BODY, {**GOLD, **PIG_FACE, **GOLD_EYES}, lambda x, y: PINK if y < 8 else None)


TOTEMS = {
    "weakness_totem": weakness,
    "fatigue_totem": fatigue,
    "heartbreak_totem": heartbreak,
    "blindness_totem": blindness,
    "morphing_totem": morphing,
    "zombified_totem": zombified,
    "rampage_totem": rampage,
    "void_totem": void,
}
