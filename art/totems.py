"""The fourteen totems (16x16). Each is a grid over the vanilla totem's shape.

Grid keys: '0'..'6' are body tones darkest to lightest (the vanilla totem's own
seven), 'g'/'h' its light/dark eye pixels, '.' transparent, and any other letter
a colour named in that totem's palette.
"""
from PIL import Image, ImageOps

from pixel import Vanilla, cols, hexc, luminance_ramp, outlined, over, ramp, render, scaled

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


def loneliness(vanilla: Vanilla) -> Image.Image:
    """Pink, downcast, a tear, holding an empty vanilla health-bar heart."""
    grid = list(VANILLA)
    grid[5] = "....06333360...."
    grid[7] = "....0t354320...."
    grid[8] = ".1111t244201111."
    palette = {**PINK, **cols(g="fcdce0", h="9a4456", t="6ab4f0")}
    return over(render(grid, palette), vanilla.load("gui/sprites/hud/heart/container.png"), (3, 7))


def pacifism(vanilla: Vanilla) -> Image.Image:
    """White, eyes closed, a big vanilla poppy over the chest."""
    grid = list(VANILLA)
    grid[5] = "....06333360...."
    grid[6] = "....0hh65hh0...."
    palette = {**ramp("6a6a78", "9a9aa8", "c4c4d0", "d4d4de", "e8e8f0", "f2f2f8", "ffffff"), **cols(h="6a6a78")}
    poppy = vanilla.load("block/poppy.png").resize((14, 14), Image.NEAREST)
    return over(render(grid, palette), poppy, (4, 2))


def melon(vanilla: Vanilla) -> Image.Image:
    """Rind green with pink eyes, holding a vanilla melon slice."""
    palette = {**ramp("1e3a10", "2e5a18", "3e7a22", "4e8c2a", "62a236", "78b646", "96cc66"), **cols(g="fcb8a0", h="cc2c30")}
    melon = vanilla.load("item/melon_slice.png").resize((10, 10), Image.NEAREST)
    return over(render(VANILLA, palette), melon, (7, 6))


def restraint(vanilla: Vanilla) -> Image.Image:
    """Made of the vanilla barrel side, lit by the totem's own shading."""
    return block_textured(vanilla.load("block/barrel_side.png"), {"g": (30, 20, 10), "h": (10, 5, 0)})


# How bright each body tone makes a block texture mapped onto the totem.
TONE_LIGHT = {"0": 0.45, "1": 0.62, "2": 0.78, "3": 0.88, "4": 1.0, "5": 1.12, "6": 1.25}


def block_textured(texture: Image.Image, eyes: dict) -> Image.Image:
    """The vanilla totem's shape filled with a 16x16 texture, shaded by its tones; eyes in plain colours."""
    im = Image.new("RGBA", (16, 16))
    for y, row in enumerate(VANILLA):
        for x, key in enumerate(row):
            if key == ".":
                continue
            if key in eyes:
                im.putpixel((x, y), (*eyes[key], 255))
                continue
            r, g, b, _ = texture.getpixel((x, y))
            light = TONE_LIGHT[key]
            im.putpixel((x, y), (min(255, int(r * light)), min(255, int(g * light)), min(255, int(b * light)), 255))
    return im


def void(vanilla: Vanilla) -> Image.Image:
    """Plain dark, dim purple eyes."""
    palette = {**ramp("06060a", "0e0e14", "16161e", "1c1c26", "242430", "2c2c3a", "363648"),
               "g": scaled(hexc("c070ff"), 0.72), "h": scaled(hexc("6a20b0"), 0.72)}
    return render(VANILLA, palette)


def feather(vanilla: Vanilla) -> Image.Image:
    """White totem with the vanilla Allay's wings behind it."""
    wing = ImageOps.mirror(vanilla.load("entity/allay/allay.png").crop((16, 22, 24, 27)))
    wings = Image.new("RGBA", (16, 16))
    wings.alpha_composite(wing, (0, 3))
    wings.alpha_composite(ImageOps.mirror(wings))
    palette = {**ramp("6a6a78", "9a9aa8", "c4c4d0", "d4d4de", "e8e8f0", "f2f2f8", "ffffff"),
               **cols(g="7ab8e8", h="3a6a9a")}
    return over(wings, render(VANILLA, palette))


def boom(vanilla: Vanilla) -> Image.Image:
    """Gold totem hugging a vanilla TNT block, its hands showing at the sides."""
    body = render(VANILLA, {**GOLD, **GOLD_EYES})
    tnt = vanilla.load("block/tnt_side.png").resize((8, 8), Image.NEAREST)
    im = over(body, tnt, (4, 7))
    for y in (8, 9):
        for x in [1, 2, 3, 12, 13, 14]:
            im.putpixel((x, y), body.getpixel((x, y)))
    return im


def hunger(vanilla: Vanilla) -> Image.Image:
    """Deep rust, hollow cheeks, clutching the vanilla hunger-bar shank, outlined so it reads in the hotbar."""
    grid = list(VANILLA)
    grid[5] = "....0hh33hh0...."
    grid[6] = "....0gh65hg0...."
    grid[7] = "....0c3543c0...."
    palette = {**ramp("260a06", "42120a", "641e12", "7c2a18", "963c22", "b05630", "cc7a48"),
               **cols(g="cc7a48", h="260a06", c="42120a")}
    shank = outlined(vanilla.load("gui/sprites/hud/food_full.png"), hexc("140402"))
    return over(render(grid, palette), shank, (2, 6))

def slowness(vanilla: Vanilla) -> Image.Image:
    """Slate blue, eyes shut, its feet caught in a 10px vanilla cobweb (every other pixel of the real one)."""
    grid = list(VANILLA)
    grid[5] = "....06333360...."
    grid[6] = "....0hh65hh0...."
    palette = {**ramp("1a2232", "2a3650", "3e4e70", "4c5e84", "62769c", "7e92b4", "a4b4cc"), **cols(h="1a2232")}
    web = vanilla.load("block/cobweb.png")
    small = Image.new("RGBA", (10, 10))
    for y in range(10):
        for x in range(10):
            small.putpixel((x, y), web.getpixel((round(x * 15 / 9), round(y * 15 / 9))))
    return over(render(grid, palette), small, (3, 6))


# A smaller air bubble drawn in the vanilla air sprite's four colours (a shrunk copy blurs).
SMALL_BUBBLE = ["..aa..", ".abba.", "abc.da", "ab..da", ".adda.", "..aa.."]
BUBBLE_COLOURS = cols(a="000000", b="0094ff", c="d1ebff", d="56b8ff")


def breathlessness(vanilla: Vanilla) -> Image.Image:
    """Gold with a 20% blue tint, gasping, a vanilla air bubble rising and a small one below on the left."""
    grid = list(VANILLA)
    grid[5] = "....0gg33gg0...."
    grid[6] = "....0hh65hh0...."
    grid[7] = "....023kk320...."
    blue = ramp("101a3a", "1a2a5a", "28407e", "345096", "4c6cb0", "6c8cc8", "a0b8e4")
    tinted = {key: tuple(round(g * 0.8 + b * 0.2) for g, b in zip(GOLD[key], blue[key])) for key in GOLD}
    palette = {**tinted, **cols(g="fdfbee", h="85400f", k="3a1a04")}
    im = over(render(grid, palette), vanilla.load("gui/sprites/hud/air.png"), (7, 0))
    return over(im, render(SMALL_BUBBLE, BUBBLE_COLOURS), (2, 9))


def insomnia(vanilla: Vanilla) -> Image.Image:
    """Night navy, wide awake: pinpoint red pupils, softened lower lids and dark bags under the eyes."""
    grid = [
        "................",
        ".....111111.....",
        "....14444441....",
        "....www44www....",
        "....wrw33wrw....",
        "....cwc33cwc....",
        "....Kk3653kK....",
        "....2K3543K2....",
    ] + BODY
    palette = {**ramp("0a0c1e", "141834", "222a50", "2c3664", "404c80", "5a68a0", "8492c8"),
               **cols(w="ffffff", c="b8bcd0", r="c01818", k="3a2a6a", K="241848")}
    return render(grid, palette)


def fire(vanilla: Vanilla) -> Image.Image:
    """Ember red with fire eyes; the tips of the vanilla fire, made yellow-hot, rise behind both shoulders."""
    palette = {**ramp("3a0a02", "6a1804", "a02a06", "c2400a", "e0641a", "f49434", "fcd070"), **cols(g="fff060", h="ff8000")}
    flames = Image.new("RGBA", (16, 16))
    flames.alpha_composite(vanilla.load("block/fire_0.png").crop((0, 0, 16, 16)).resize((14, 14), Image.NEAREST), (1, 0))
    for y in range(8, 16):  # only above the arms
        for x in range(16):
            flames.putpixel((x, y), (0, 0, 0, 0))
    flames.alpha_composite(ImageOps.mirror(flames))
    for y in range(16):
        for x in range(16):
            r, g, b, a = flames.getpixel((x, y))
            if a:
                flames.putpixel((x, y), (255, min(255, g + 90), min(255, b + 30), a))
    return over(flames, render(VANILLA, palette))


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
    "loneliness_totem": loneliness,
    "pacifism_totem": pacifism,
    "melon_totem": melon,
    "restraint_totem": restraint,
    "void_totem": void,
    "feather_totem": feather,
    "boom_totem": boom,
    "hunger_totem": hunger,
    "slowness_totem": slowness,
    "breathlessness_totem": breathlessness,
    "insomnia_totem": insomnia,
    "fire_totem": fire,
}
