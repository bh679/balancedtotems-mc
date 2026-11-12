"""Pixel helpers for the texture generator: palettes, grid rendering, vanilla sprite access."""
import io
import zipfile

from PIL import Image

TEXTURES = "assets/minecraft/textures/"


def hexc(code: str) -> tuple:
    return tuple(int(code[i:i + 2], 16) for i in (0, 2, 4))


def ramp(*codes: str) -> dict:
    """Seven body tones, darkest first, keyed '0'..'6' like the vanilla totem's tones."""
    return {str(i): hexc(code) for i, code in enumerate(codes)}


def cols(**codes: str) -> dict:
    return {key: hexc(code) for key, code in codes.items()}


def render(grid: list, colours: dict, zones=None) -> Image.Image:
    """Paint a grid of palette keys ('.' is transparent).

    zones is an optional function (x, y) -> palette override or None, for a part
    of the totem that takes other tones.
    """
    im = Image.new("RGBA", (len(grid[0]), len(grid)), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        if len(row) != len(grid[0]):
            raise ValueError(f"row {y} is {len(row)} wide, expected {len(grid[0])}: {row!r}")
        for x, key in enumerate(row):
            if key == ".":
                continue
            palette = colours
            if zones:
                override = zones(x, y)
                if override:
                    palette = {**colours, **override}
            im.putpixel((x, y), (*palette[key], 255))
    return im


def over(bottom: Image.Image, top: Image.Image, at: tuple = (0, 0)) -> Image.Image:
    out = bottom.copy()
    out.alpha_composite(top, at)
    return out


def scaled(colour: tuple, factor: float) -> tuple:
    return tuple(min(255, round(c * factor)) for c in colour)


def luminance_ramp(im: Image.Image, box: tuple) -> dict:
    """Seven tones sampled by brightness from a region of a vanilla skin; the darkest one deepened for outlines."""
    seen = {im.getpixel((x, y))[:3] for x in range(box[0], box[2]) for y in range(box[1], box[3])
            if im.getpixel((x, y))[3] > 0}
    tones = sorted(seen, key=lambda c: 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2])
    picked = [tones[round(i * (len(tones) - 1) / 6)] for i in range(7)]
    darkest = tuple(round(c * 0.55) for c in picked[0])
    return {str(i): c for i, c in enumerate([darkest] + picked[1:])}


class Vanilla:
    """Reads vanilla sprites straight out of the Minecraft client resources jar."""

    def __init__(self, jar_path: str) -> None:
        self.jar = zipfile.ZipFile(jar_path)

    def load(self, path: str) -> Image.Image:
        try:
            data = self.jar.read(TEXTURES + path)
        except KeyError:
            raise SystemExit(f"{TEXTURES + path} is not in {self.jar.filename}; is this the 1.21.1 client jar?")
        return Image.open(io.BytesIO(data)).convert("RGBA")
