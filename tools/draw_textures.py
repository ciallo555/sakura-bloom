"""Draw the original 16px texture set with Pillow using fixed palettes and pixel glyphs."""

from pathlib import Path
from random import Random

from PIL import Image, ImageColor, ImageDraw
from chest_textures import draw_chests
from charm_sprites import draw_charms

PROJECT = Path(__file__).resolve().parents[1]
RESOURCES = PROJECT / "src/main/resources"
TEXTURES = RESOURCES / "assets/sakura_bloom/textures"
CUISINE_TEXTURES = PROJECT / "cuisine/src/main/resources/assets/sakura_bloom/textures"
PALETTES = {
    "shidare": {
        "bark": ("#39323c", "#51404a", "#62505a", "#80676a", "#a4847c"),
        "leaves": ("#576962", "#7e8d7d", "#9ba697", "#b6bfb1"),
        "flower": ("#a5879a", "#c4a7b9", "#e7d7dc", "#fff3e0", "#ffedd0"),
        "paper": ("#cbaeb1", "#d9c1be", "#ead6c8"),
        "tea": ("#8d8745", "#b6b15e", "#e4cf87"),
    },
    "yae": {
        "bark": ("#3c2432", "#51313b", "#653c44", "#845256", "#a3695c"),
        "leaves": ("#684750", "#82565c", "#976064", "#ae786d"),
        "flower": ("#873d59", "#ad526f", "#d3728a", "#ed9cac", "#f7c5ae"),
        "paper": ("#c78c74", "#daa185", "#ecc4a0"),
        "tea": ("#88394f", "#b65367", "#ec9386"),
    },
    "yamazakura": {
        "bark": ("#282934", "#403544", "#55414f", "#6f5360", "#917073"),
        "leaves": ("#344b3e", "#495e48", "#617658", "#788566"),
        "flower": ("#77546c", "#a0718b", "#c791a6", "#e4afc0", "#f4d3bd"),
        "paper": ("#9fadb3", "#b9bbbb", "#d0ccbe"),
        "tea": ("#744969", "#9c627f", "#d89cae"),
    },
}
TIMBER_TONES = {
    "shidare": {
        "planks": ("#ad8468", "#bb9476", "#caa487", "#d2ad8f", "#dab79b", "#e1c0a5", "#e8cbb1"),
        "cut": ("#ad8368", "#bc9376", "#c8a082", "#d3af91", "#dbb99b", "#e5c5a9", "#edd1b8"),
        "bark": ("#352d35", "#42363d", "#4d4045", "#5a4a4e", "#69565a"),
    },
    "yae": {
        "planks": ("#a16760", "#b2756b", "#bf8478", "#c88d7f", "#d09687", "#d9a092", "#e1ac9e"),
        "cut": ("#ab6a65", "#b77870", "#c5877d", "#cf9186", "#d69f92", "#e1aea0", "#e9bbae"),
        "bark": ("#33212a", "#422931", "#502f38", "#5c3a42", "#704850"),
    },
    "yamazakura": {
        "planks": ("#785568", "#886376", "#986f82", "#a3798e", "#ac8398", "#b58ea2", "#bf9aae"),
        "cut": ("#785469", "#886479", "#956f85", "#a37c91", "#af889d", "#bc96aa", "#c6a3b6"),
        "bark": ("#292830", "#34313b", "#423944", "#4c414e", "#605160"),
    },
}

PLANK_PIXELS = (
    "5655566555554566",
    "4545655444554345",
    "5544445554465444",
    "2112321022212022",
    "6655555665554556",
    "4545554455545445",
    "5554544445455544",
    "0232201122102320",
    "5556665554555665",
    "4545544455454544",
    "5445545444565445",
    "1202223022321101",
    "6565554565566555",
    "4544445455545554",
    "4555544544564445",
    "2210123201122230",
)
BARK_PIXELS = (
    "1221310122321122",
    "2232112233210102",
    "3210133322122321",
    "1001124310112210",
    "1212233221211143",
    "2310122100123332",
    "3220111013443221",
    "2103222112322110",
    "0123331011001210",
    "2221211022012122",
    "3432211343322332",
    "2310112221332102",
    "1012212112211210",
    "3233320100123332",
    "2211222112243211",
    "1220102233321021",
)
STRIPPED_PIXELS = (
    "3323433334443323",
    "3444332444322233",
    "4333313443333323",
    "3422233332344333",
    "3322344322333433",
    "3433444344332223",
    "4333243334323334",
    "4422333343333444",
    "3333244433334333",
    "2312323434434233",
    "3333334434332223",
    "4234433333323444",
    "3344322333433334",
    "3333233344443333",
    "2344333433332233",
    "3334334443322334",
)
CANOPY_COLORS = {
    "shidare": (("#738c7d", "#95ac96", "#b2c1aa", "#c9d4bc"),
                ("#b6a2b6", "#d9c5d2", "#f0e4e4", "#fff8eb", "#ffebbe")),
    "yae": (("#8f586a", "#ad6b80", "#c98591", "#dda2a0"),
            ("#a84d68", "#cc6580", "#e28fa4", "#f6b5c5", "#ffdac1")),
    "yamazakura": (("#52775d", "#71976b", "#91ae7c", "#b0c392"),
                   ("#966987", "#bb84a1", "#dfa6be", "#f5c5d7", "#ffe0c6")),
}
FOLIAGE_GAPS = (
    # Forty pixels in small groups, matching the coverage of vanilla cherry leaves.
    (0, 0, (" #", "##", "# ")), (6, 1, (" ##", "## ")), (12, 0, ("##", " #")),
    (14, 4, (" #", "##", " #")), (4, 4, ("##", " #")), (10, 5, ("#",)),
    (1, 6, ("##", "# ")), (14, 8, ("#",)), (8, 7, ("###", "  #")),
    (4, 9, ("##",)), (12, 10, ("# ", "##")), (1, 11, ("#",)),
    (8, 12, ("##", "# ")), (4, 14, ("#",)), (14, 14, ("##",)), (0, 15, ("#",)),
)
FLOWER = (
    "  12  ",
    " 1221 ",
    "122222",
    " 23421",
    " 2121 ",
    " 1 1  ",
)
FLECK = (" 11 ", "1232", " 232", "  1 ")


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True)


def texture(folder, name, image, root=TEXTURES):
    save(image, root / folder / f"{name}.png")


def glyph(draw, x, y, pattern, colors):
    for row, line in enumerate(pattern):
        for column, char in enumerate(line):
            if char != " ":
                draw.point((x + column, y + row), fill=colors[int(char)])


def muted_palette(colors, strength):
    base = ImageColor.getrgb(colors[2])
    return tuple(tuple(round(mid + (value - mid) * strength) for mid, value in zip(base, ImageColor.getrgb(color)))
                 for color in colors)


def grain(palette, random):
    palette = muted_palette(palette, 0.55)
    image = Image.new("RGBA", (16, 16), palette[2])
    draw = ImageDraw.Draw(image)
    bands = (1, 2, 2, 3, 3, 2, 1, 0, 1, 2, 3, 3, 2, 1, 2, 2)
    for x in range(16):
        for y in range(16):
            phase = (y + (x // 4) * 3) % 16
            ridge = (x + int(7 <= phase <= 12)) % 16
            value = (bands[ridge] + 2) // 2
            if random.random() < 0.28:
                value += random.choice((-1, 1))
            draw.point((x, y), palette[max(0, min(4, value))])
    for left, y, height in ((2, 3, 5), (6, 10, 3), (11, 3, 4), (13, 12, 3)):
        draw.line((left, y, left, y + height), fill=palette[1])
        draw.line((left + 1, y + 1, left + 1, y + height - 1), fill=palette[3])
    return image


def timber_side(pattern, colors, random):
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    for x in range(16):
        for y in range(16):
            tone = int(pattern[y][x])
            if random.random() < 0.28:
                fleck = random.choice((-1, 1))
                if (x + 2 * y) % 5 == 1:
                    tone += fleck
            draw.point((x, y), fill=colors[max(0, min(len(colors) - 1, tone))])
    return image


def rings(wood, bark, random, *, stripped=False):
    image = Image.new("RGBA", (16, 16), wood[4])
    draw = ImageDraw.Draw(image)
    tones = (0, 5, 2, 5, 4, 1, 4, 3)
    for y in range(16):
        for x in range(16):
            inset = min(x, 15 - x, y, 15 - y)
            if not stripped and inset == 0:
                color = random.choice(bark[1:4])
            else:
                tone = tones[inset]
                if inset in (1, 2) and min(x, 15 - x) == min(y, 15 - y):
                    tone = tones[inset - 1]
                elif (3 * x + y * y) % 13 < 3:
                    tone = min(tone + 1, len(wood) - 1)
                color = wood[tone]
            draw.point((x, y), fill=color)
    draw.point((7, 8), fill=wood[2])
    return image


def board(wood, random):
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    glyph(draw, 0, 0, PLANK_PIXELS, wood)
    seams = (4, 11, 7, 13)
    for row, seam in enumerate(seams):
        y = 4 * row
        draw.point((seam, y + 2), fill=wood[3])
        for start, end in ((0, seam - 1), (seam + 2, 15)):
            for _ in range(2):
                x = random.randint(start, end)
                height = y + random.choice((1, 2))
                length = random.randint(1, 3)
                draw.line((x, height, min(end, x + length - 1), height), fill=random.choice(wood[3:6]))
    return image


def leaves(random, variety):
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    foliage, bloom = CANOPY_COLORS[variety]
    for y in range(16):
        for x in range(16):
            draw.point((x, y), fill=random.choices(foliage, weights=(2, 6, 5, 2))[0])
    blossoms = {
        "shidare": ((-2, 0), (5, -2), (11, 2), (1, 6), (7, 5), (10, 10), (3, 11), (-2, 12)),
        "yae": ((-1, -1), (4, 0), (10, -1), (1, 4), (8, 3), (12, 6), (5, 8), (-1, 10), (3, 12), (10, 11)),
        "yamazakura": ((0, 0), (8, 1), (4, 5), (12, 5), (0, 11), (9, 10)),
    }
    for x, y in blossoms[variety]:
        glyph(draw, x, y, FLECK if variety == "yamazakura" else FLOWER, bloom)
    shift_x, shift_y = {"shidare": (0, 0), "yae": (2, 1), "yamazakura": (1, 3)}[variety]
    for x, y, rows in FOLIAGE_GAPS:
        for dy, row in enumerate(rows):
            for dx, pixel in enumerate(row):
                if pixel == "#":
                    draw.point(((x + shift_x + dx) % 16, (y + shift_y + dy) % 16), fill=(0, 0, 0, 0))
    return image


def sapling(palette, variety):
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    bark, bloom, greens = palette["bark"], palette["flower"], palette["leaves"]
    draw.line([(7, 15), (8, 14), (8, 9), (9, 6), (9, 3)], fill=bark[1], width=2)
    draw.line([(8, 12), (6, 9), (3, 8)], fill=bark[2])
    draw.line([(8, 9), (11, 7), (13, 7)], fill=bark[2])
    draw.line([(8, 13), (8, 10), (9, 8)], fill=bark[4])
    glyph(draw, 2, 8, (" 12", "132", " 1 "), greens)
    glyph(draw, 10, 8, (" 22", "231", " 1 "), greens)
    for offset in ((1, 4), (7, 1), (10, 5)):
        glyph(draw, *offset, FLOWER if variety == "yae" else FLECK, bloom)
    if variety == "shidare":
        draw.point((3, 10), fill=bloom[2])
        draw.point((12, 10), fill=bloom[3])
    return image


def door_halves(palette, planks, random):
    upper = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(upper)
    paper, bloom = palette["paper"], palette["flower"]
    for x in range(16):
        for y in range(16):
            draw.point((x, y), fill=random.choices(paper, weights=(2, 9, 4))[0])
    glyph(draw, 5, 5, FLOWER, bloom)
    for x in (0, 1, 7, 8, 14, 15):
        draw.line((x, 0, x, 15), fill=planks[1 if x % 2 == 0 else 4])
    for y in (0, 1, 5, 10, 14, 15):
        draw.line((0, y, 15, y), fill=planks[1 if y % 2 == 0 else 4])
    lower = board(planks, random)
    draw = ImageDraw.Draw(lower)
    draw.rectangle((0, 0, 15, 15), outline=planks[1])
    draw.rectangle((3, 3, 12, 12), outline=planks[5])
    draw.line((1, 4, 3, 4), fill="#e7c180")
    draw.point((3, 5), fill="#674536")
    return upper, lower


def petals(palette):
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    for x, y, pattern in ((1, 1, FLOWER), (9, 1, FLOWER), (1, 9, FLOWER), (9, 9, FLOWER),
                          (5, 5, FLECK), (12, 6, FLECK), (4, 12, FLECK)):
        glyph(draw, x, y, pattern, palette["flower"])
    return image


def bottled_tea(tea, flower):
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    for y, (x0, x1) in enumerate(((6, 9), (6, 9), (6, 9), (5, 10), (4, 11), (4, 11), (3, 12),
                                 (3, 12), (3, 12), (3, 12), (4, 11), (5, 10)), start=2):
        draw.line((x0, y, x1, y), fill="#495059")
        if y < 6:
            draw.line((x0 + 1, y, x1 - 1, y), fill="#b0baba")
        elif y <= 12:
            color = tea[0] if y > 10 else tea[1]
            draw.line((x0 + 1, y, x1 - 1, y), fill=color)
            draw.point((x0 + 1, y), fill=tea[2])
    draw.rectangle((5, 1, 10, 3), fill="#81604e")
    draw.line((5, 1, 10, 1), fill="#c29b73")
    draw.point((5, 6), fill="#f9eed6")
    glyph(draw, 6, 7, (" 2 ", "242", " 2 "), flower)
    return image


def tree_art():
    for index, (name, palette) in enumerate(PALETTES.items()):
        random = Random(3314 + index)
        timber = TIMBER_TONES[name]
        bark, cut, planks = timber["bark"], timber["cut"], timber["planks"]
        texture("block", f"{name}_log", timber_side(BARK_PIXELS, bark, random))
        texture("block", f"{name}_log_top", rings(cut, bark, random))
        texture("block", f"stripped_{name}_log", timber_side(STRIPPED_PIXELS, cut, random))
        texture("block", f"stripped_{name}_log_top", rings(cut, bark, random, stripped=True))
        texture("block", f"{name}_planks", board(planks, random))
        texture("block", f"{name}_leaves", leaves(random, name))
        texture("block", f"{name}_sapling", sapling(palette, name))

        upper, lower = door_halves(palette, planks, random)
        texture("block", f"{name}_door_top", upper)
        texture("block", f"{name}_door_bottom", lower)
        texture("block", f"{name}_trapdoor", upper)
        door = Image.new("RGBA", (16, 16))
        door.paste(upper.resize((8, 7), Image.Resampling.NEAREST), (4, 1))
        door.paste(lower.resize((8, 7), Image.Resampling.NEAREST), (4, 8))
        texture("item", f"{name}_door", door)

        texture("block", f"{name}_petals", petals(palette))
        stem = Image.new("RGBA", (16, 16))
        draw = ImageDraw.Draw(stem)
        for x in (2, 6, 10, 14):
            draw.line((x, 0, x, 15), fill=palette["leaves"][1], width=2)
            draw.point((x + 1, 3), fill=palette["leaves"][3])
        texture("block", f"{name}_petals_stem", stem)
        icon = Image.new("RGBA", (16, 16))
        draw = ImageDraw.Draw(icon)
        glyph(draw, 3, 2, FLOWER, palette["flower"])
        glyph(draw, 8, 7, FLOWER, palette["flower"])
        glyph(draw, 2, 9, FLECK, palette["flower"])
        texture("item", f"{name}_petals", icon)
        texture("item", f"{name}_tea", bottled_tea(palette["tea"], palette["flower"]), CUISINE_TEXTURES)

        for frame in range(4):
            mote = Image.new("RGBA", (8, 8))
            glyph(ImageDraw.Draw(mote), 2, 2, (" 12", "123", " 2 "), palette["flower"])
            texture("particle", f"{name}_petal_{frame}", mote.rotate(frame * 90))


def surface(colors, seed):
    random = Random(seed)
    image = Image.new("RGBA", (16, 16))
    image.putdata([ImageColor.getcolor(color, "RGBA") for color in random.choices(colors, weights=(2, 8, 3), k=256)])
    return image


def cuboid_faces(u, v, width, height, depth):
    """Texture rectangles for Minecraft's cuboid UV layout."""
    return {
        "down": (u + depth, v, width, depth),
        "up": (u + depth + width, v, width, depth),
        "west": (u, v + depth, depth, height),
        "north": (u + depth, v + depth, width, height),
        "east": (u + depth + width, v + depth, depth, height),
        "south": (u + 2 * depth + width, v + depth, width, height),
    }


FACE_AXES = {"down": "xz", "up": "xz", "west": "zy", "north": "xy", "east": "zy", "south": "xy"}
IRON = ("#303745", "#434d60", "#586479", "#77859a")
CHEST_WOOD = ("#614330", "#835b3a", "#a27549", "#ba925e", "#d4aa72")


def wood_face(width, height, wood, random, course=None):
    face = Image.new("RGBA", (width, height), wood[2])
    draw = ImageDraw.Draw(face)
    for y in range(height):
        x = 0
        while x < width:
            end = min(width, x + random.randint(2, 5))
            color = random.choices(wood[1:4], weights=(2, 6, 2))[0]
            draw.line((x, y, end - 1, y), fill=color)
            x = end

    if course:
        for y in range(course, height - 1, course):
            draw.line((0, y, width - 1, y), fill=wood[0])
            start = random.randrange(max(1, width // 3))
            draw.line((start, y + 1, width - 1 - start, y + 1), fill=wood[3])
        if width >= 10:
            for y in range(0, height, course):
                joint = random.randrange(width // 4, 3 * width // 4)
                draw.line((joint, y, joint, min(y + course - 1, height - 1)), fill=wood[0])
                draw.point((joint + 1, min(y + 1, height - 1)), fill=wood[3])

    if width >= 8 and height >= 5:
        knot_x = random.randrange(3, width - 2)
        knot_y = random.randrange(2, height - 1)
        if course and knot_y % course in (0, course - 1):
            knot_y = (knot_y // course) * course + 1
        draw.line((knot_x - 1, knot_y, knot_x + 1, knot_y), fill=wood[1])
        draw.point((knot_x, knot_y), fill=wood[0])
        draw.point((knot_x + 1, knot_y - 1), fill=wood[3])
    return face


def paint_wood_box(image, uv, wood, random, *, grain="x", course=None):
    faces = cuboid_faces(*uv)
    for name, (x, y, width, height) in faces.items():
        if not width or not height:
            continue
        axes = FACE_AXES[name]
        seams = course if grain in axes else None
        if grain == axes[1]:
            surface = wood_face(height, width, wood, random, seams).transpose(Image.Transpose.TRANSPOSE)
        else:
            surface = wood_face(width, height, wood, random, seams)
        image.paste(surface, (x, y))
    return faces


def bevel_face(draw, face, light, shade):
    x, y, width, height = face
    draw.line((x, y, x + width - 1, y), fill=light)
    draw.line((x, y + height - 1, x + width - 1, y + height - 1), fill=shade)


def paint_hull(image, wood, fastener, random):
    floor = paint_wood_box(image, (0, 0, 28, 16, 3), wood, random, course=4)
    draw = ImageDraw.Draw(image)
    for face in ("north", "south"):
        x, y, width, height = floor[face]
        draw.rectangle((x, y, x + width - 1, y + height - 1), outline=wood[1])
    for v, width in ((19, 18), (27, 16), (35, 28), (43, 28)):
        walls = paint_wood_box(image, (0, v, width, 6, 2), wood, random)
        for face in ("north", "south"):
            x, y, width, height = walls[face]
            bevel_face(draw, walls[face], wood[3], wood[0])
            for offset in (2, width - 3):
                draw.point((x + offset, y + 2), fill=fastener)
                draw.point((x + offset, y + 3), fill=wood[1])
            draw.line((x + 1, y + 4, x + width - 2, y + 4), fill=wood[1])
        x, y, width, height = walls["up"]
        for offset in (2, width - 3):
            draw.line((x + offset, y, x + offset, y + height - 1), fill=wood[1])


def paint_paddles(image, wood, random):
    draw = ImageDraw.Draw(image)
    for v in (0, 20):
        paint_wood_box(image, (62, v, 2, 2, 18), wood, random, grain="z")
        # The blade and shaft share part of the UV sheet, so both use the same wood palette.
        blade = paint_wood_box(image, (62, v, 1, 6, 7), wood, random, grain="z")
        for face in ("west", "east"):
            x, y, width, height = blade[face]
            bevel_face(draw, blade[face], wood[3], wood[1])
            draw.line((x + 1, y + 2, x + width - 2, y + 2), fill=wood[3])
            draw.point((x + width - 2, y + height - 2), fill=wood[0])


def paint_cargo_chest(image, random):
    draw = ImageDraw.Draw(image)
    for uv in ((0, 59, 12, 4, 12), (0, 76, 12, 8, 12)):
        faces = paint_wood_box(image, uv, CHEST_WOOD, random, course=4)
        for face in ("west", "north", "east", "south"):
            x, y, width, height = faces[face]
            bevel_face(draw, faces[face], CHEST_WOOD[3], CHEST_WOOD[0])
            for offset in (1, width - 2):
                draw.line((x + offset, y + 1, x + offset, y + height - 2), fill=CHEST_WOOD[1])
                draw.point((x + offset, y + 1), fill=CHEST_WOOD[0])
    latch = paint_wood_box(image, (0, 59, 2, 4, 1),
                           ("#654e37", "#987244", "#bf985c", "#d9bb79", "#efd69a"), random, grain="y")
    for face in ("north", "south"):
        x, y, width, height = latch[face]
        draw.point((x, y + 1), fill="#e4c484")
        draw.point((x + width - 1, y + height - 2), fill="#6a4d36")


def boat_texture(palette, random, chest):
    image = Image.new("RGBA", (128, 128 if chest else 64))
    wood = palette["wood"]
    paint_hull(image, wood, palette["bark"][2], random)
    paint_paddles(image, wood, random)
    if chest:
        paint_cargo_chest(image, random)
    return image


def paint_hanging_hardware(image, wood, random):
    beam = paint_wood_box(image, (0, 0, 16, 2, 4), wood, random)
    draw = ImageDraw.Draw(image)
    for face in ("down", "up", "north", "south"):
        x, y, width, height = beam[face]
        for offset in (2, width - 4):
            draw.rectangle((x + offset, y, x + offset + 1, y + height - 1), fill=IRON[1])
            draw.line((x + offset, y, x + offset, y + height - 1), fill=IRON[2])
            draw.point((x + offset, y), fill=IRON[3])
    chain = ("232", "21 ", "1 2", " 21", "12 ", " 12")
    for x in (0, 3, 6, 9):
        glyph(draw, x, 6, chain, IRON)
    diagonal = ("     32     ", "    21 23   ", "   21   21  ",
                "  21     21 ", " 21       21", "21        12")
    for x in (14, 26):
        glyph(draw, x, 6, diagonal, IRON)


def sign_texture(palette, random, hanging):
    image = Image.new("RGBA", (64, 32))
    wood = palette["wood"]
    if hanging:
        board = paint_wood_box(image, (0, 12, 14, 10, 2), wood, random, grain="y")
        paint_hanging_hardware(image, wood, random)
    else:
        board = paint_wood_box(image, (0, 0, 24, 12, 2), wood, random)
        paint_wood_box(image, (0, 14, 2, 14, 2), wood, random, grain="y")

    draw = ImageDraw.Draw(image)
    for face in ("north", "south"):
        x, y, width, height = board[face]
        bevel_face(draw, board[face], wood[3], wood[1])
        draw.line((x + width - 1, y + 1, x + width - 1, y + height - 1), fill=wood[1])
        for offset in (2, width - 3):
            draw.point((x + offset, y + 1), fill=wood[0])
    return image


def hanging_sign_preview(sign):
    image = Image.new("RGBA", (16, 16))
    for layout, location in (((0, 0, 16, 2, 4), (0, 0)), ((0, 12, 14, 10, 2), (1, 6))):
        x, y, width, height = cuboid_faces(*layout)["north"]
        image.paste(sign.crop((x, y, x + width, y + height)), location)
    draw = ImageDraw.Draw(image)
    for x in (2, 12):
        glyph(draw, x, 2, ("32", "21", "12", "21"), IRON)
    return image


def sign_sprite(palette, hanging):
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    wood = palette["wood"]
    edge = palette["bark"][1]
    if hanging:
        draw.rectangle((1, 1, 14, 2), fill=wood[0])
        draw.line((2, 1, 13, 1), fill=wood[3])
        chain = ("#47454c", "#726f72", "#a7a2a0")
        for x in (3, 11):
            glyph(draw, x, 1, ("212", "122", "01 ", " 10", " 2 "), chain)
        top = 6
    else:
        glyph(draw, 6, 10, ("212", "210", "230", "210", "120", " 0 "), wood)
        top = 2

    draw.rectangle((1, top, 14, top + 8), fill=wood[2])
    draw.line((1, top, 13, top), fill=wood[4])
    draw.line((1, top + 1, 1, top + 7), fill=wood[3])
    draw.line((14, top, 14, top + 8), fill=wood[1])
    draw.line((1, top + 8, 14, top + 8), fill=wood[0])
    draw.line((2, top + 6, 13, top + 6), fill=wood[1])
    draw.line((2, top + 4, 4, top + 4), fill=wood[3])
    draw.line((9, top + 1, 12, top + 1), fill=wood[3])
    glyph(draw, 3, top + 2, ("2 11  2 1", "1121  112", "         ", "11  12 11", " 21  1 21"),
          (wood[2], wood[0], edge))
    return image


def boat_sprite(palette, chest):
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    wood, bark = palette["wood"], palette["bark"]

    # The far oar sits behind the rim, while the near one overlaps the hull.
    draw.line((8, 6, 11, 1), fill=bark[1], width=2)
    draw.polygon([(10, 2), (11, 0), (13, 0), (13, 1), (11, 3)], fill=wood[1], outline=bark[2])
    draw.point((12, 1), fill=wood[3])

    hull = [(3, 3), (6, 2), (9, 3), (12, 6), (14, 9), (13, 12),
            (10, 14), (7, 14), (4, 12), (2, 9), (1, 6), (2, 4)]
    draw.polygon(hull, fill=wood[1], outline=bark[1])
    draw.polygon([(2, 6), (4, 8), (7, 11), (11, 11), (12, 10),
                  (12, 12), (10, 13), (7, 13), (4, 11), (2, 8)], fill=wood[2])
    draw.line([(3, 9), (6, 12), (8, 13)], fill=wood[3])
    draw.line([(2, 5), (4, 3), (7, 3), (9, 4), (12, 7)], fill=wood[3])
    draw.polygon([(4, 4), (7, 4), (10, 6), (12, 9), (10, 11),
                  (7, 11), (4, 8), (3, 6)], fill=bark[1])
    draw.polygon([(4, 6), (6, 5), (9, 7), (10, 9), (8, 10), (6, 9)], fill=wood[0])
    draw.line([(5, 7), (6, 6), (7, 6), (8, 5)], fill=wood[2])
    draw.line([(7, 10), (9, 8), (10, 8)], fill=wood[2])
    draw.line([(3, 7), (5, 9), (7, 11), (10, 12), (12, 11)], fill=wood[3])
    draw.point((6, 10), fill=wood[4])

    if chest:
        draw.polygon([(4, 3), (7, 1), (10, 3), (10, 7), (7, 9), (4, 7)],
                     fill="#77502e", outline="#473329")
        draw.polygon([(4, 3), (7, 1), (10, 3), (7, 5)], fill="#bd8b49")
        draw.polygon([(5, 5), (7, 6), (7, 8), (5, 7)], fill="#976638")
        draw.line([(4, 4), (7, 6), (10, 4)], fill="#5e402c")
        draw.point((6, 6), fill="#f0cc7a")

    draw.line((9, 7, 14, 13), fill=bark[2], width=2)
    draw.line((10, 8, 13, 11), fill=wood[3])
    draw.polygon([(12, 11), (13, 11), (15, 13), (15, 15), (14, 15), (12, 13)],
                 fill=wood[2], outline=bark[2])
    draw.line((13, 12, 14, 13), fill=wood[3])
    return image


def wood_utility_art():
    for index, (name, timber) in enumerate(TIMBER_TONES.items()):
        random = Random(4680 + index)
        palette = {"wood": tuple(timber["planks"][i] for i in (0, 2, 4, 5, 6)), "bark": timber["bark"]}
        for chest, folder in ((False, "boat"), (True, "chest_boat")):
            texture(f"entity/{folder}", name, boat_texture(palette, random, chest))
            texture("item", f"{name}_{folder}", boat_sprite(palette, chest))
        for hanging, suffix, folder in (
            (False, "sign", "entity/signs"), (True, "hanging_sign", "entity/signs/hanging")
        ):
            sign = sign_texture(palette, random, hanging)
            texture(folder, name, sign)
            if hanging:
                texture("gui/hanging_signs", name, hanging_sign_preview(sign))
            texture("item", f"{name}_{suffix}", sign_sprite(palette, hanging))


def station_art():
    glaze = surface(("#315253", "#42696b", "#557c78"), 31)
    draw = ImageDraw.Draw(glaze)
    for y, style in ((7, "#9bb890"), (8, "#648b75"), (12, "#2f4d51"), (13, "#58776b")):
        draw.line((2, y, 13, y), fill=style)
    for x in (3, 7, 11):
        glyph(draw, x - 1, 9, (" 1 ", "121", " 1 "), ("#486a66", "#7b9f8e", "#b4c39a"))
    texture("block", "mortar_glaze", glaze)
    texture("block", "mortar_rim", surface(("#56766c", "#739183", "#8fa78e"), 32))
    texture("block", "mortar_pestle", grain(("#57483b", "#7f6049", "#9d7858", "#b88d61", "#cfa47a"), Random(33)))

    metal = surface(("#443539", "#594348", "#725356"), 41)
    draw = ImageDraw.Draw(metal)
    for y, color in ((4, "#9b6956"), (5, "#c1906b"), (6, "#875c4e"), (10, "#805952"), (11, "#3c3238")):
        draw.line((1, y, 14, y), fill=color)
    for x in range(4, 14, 4):
        draw.point((x, 8), fill="#c68e5e")
    texture("block", "kettle_metal", metal, CUISINE_TEXTURES)
    texture("block", "kettle_rim", surface(("#744f45", "#9c6a53", "#c08d67"), 42), CUISINE_TEXTURES)
    base = surface(("#4e383e", "#704447", "#855756"), 43)
    draw = ImageDraw.Draw(base)
    draw.line((2, 14, 13, 14), fill="#a2674a")
    draw.line((5, 15, 10, 15), fill="#462e36")
    draw.line((7, 14, 8, 14), fill="#df9664")
    texture("block", "kettle_base", base, CUISINE_TEXTURES)
    liquids = {"water": ("#397380", "#5f9e9e", "#9bccc2")}
    liquids.update({name: palette["tea"] for name, palette in PALETTES.items()})
    for index, (name, palette) in enumerate(liquids.items()):
        liquid = surface(palette, 45 + index)
        draw = ImageDraw.Draw(liquid)
        draw.line((6, 6, 8, 6), fill=palette[2])
        draw.line((8, 8, 10, 8), fill=palette[2])
        draw.point((9, 5), fill=palette[2])
        texture("block", f"kettle_{name}", liquid, CUISINE_TEXTURES)

    lacquer = surface(("#4e3040", "#6c4045", "#8c504d"), 51)
    draw = ImageDraw.Draw(lacquer)
    for y in (1, 4, 6, 14):
        draw.line((2, y, 13, y), fill="#aa7256")
    texture("block", "lantern_lacquer", lacquer)
    roof = surface(("#513642", "#75494d", "#8d5652"), 52)
    draw = ImageDraw.Draw(roof)
    for radius in (2, 4):
        draw.rectangle((radius, radius, 15 - radius, 15 - radius), outline="#aa7057")
    texture("block", "lantern_roof", roof)
    for status, colors, blossom, edge in (
        ("off", ("#a47d6b", "#c8a68b", "#dcc3a1"), ("#74515b", "#956371", "#af7882", "#bd9090", "#ab725d"), "#593948"),
        ("on", ("#d9a16f", "#f3c68a", "#ffe5ad"), ("#98526b", "#b96479", "#d77b89", "#eaa5a0", "#b97460"), "#825447"),
    ):
        paper = surface(colors, 53)
        draw = ImageDraw.Draw(paper)
        draw.rectangle((4, 6, 11, 13), outline=edge)
        glyph(draw, 5, 7, FLOWER, blossom)
        draw.point((7, 13), fill=edge)
        texture("block", f"lantern_paper_{status}", paper)


def icon():
    image = Image.new("RGBA", (32, 32), "#263944")
    draw = ImageDraw.Draw(image)
    draw.ellipse((3, 3, 29, 29), fill="#405257", outline="#8ea394")
    draw.ellipse((7, 7, 25, 25), fill="#ead8b7")
    draw.polygon([(3, 25), (12, 19), (19, 20), (25, 15), (29, 26), (17, 29), (7, 28)], fill="#394d54")
    draw.line([(5, 3), (10, 7), (16, 9), (23, 15)], fill="#4c3543", width=2)
    draw.line([(10, 7), (16, 5), (21, 5)], fill="#704650")
    for x, y, variety in ((5, 4, "yae"), (11, 7, "shidare"), (17, 4, "yae"),
                          (19, 10, "yamazakura"), (22, 16, "shidare"), (12, 18, "yae")):
        glyph(draw, x, y, FLOWER if y < 9 else FLECK, PALETTES[variety]["flower"])
    save(image.resize((128, 128), Image.Resampling.NEAREST), RESOURCES / "sakura_bloom.png")


def preview():
    samples = [
        *(("block", f"{name}_{form}") for name in PALETTES for form in ("log", "log_top", "planks", "leaves", "sapling", "petals", "door_top")),
        *(("block", block) for block in ("mortar_glaze", "kettle_metal", "lantern_paper_on", "lantern_paper_off")),
        *(("item", f"{name}_tea") for name in PALETTES),
        *(("item", f"{name}_{form}") for name in PALETTES for form in ("boat", "chest_boat", "sign", "hanging_sign")),
    ]
    width, tile = 7, 72
    sheet = Image.new("RGB", (width * tile, ((len(samples) + width - 1) // width) * tile), "#253037")
    for index, (folder, path) in enumerate(samples):
        root = CUISINE_TEXTURES if path.endswith("_tea") or path.startswith("kettle_") else TEXTURES
        with Image.open(root / folder / f"{path}.png") as source:
            sprite = source.resize((64, 64), Image.Resampling.NEAREST)
        x, y = (index % width) * tile + 4, (index // width) * tile + 4
        sheet.paste(sprite, (x, y), sprite)
    save(sheet, PROJECT / "build/texture-preview.png")


if __name__ == "__main__":
    tree_art()
    wood_utility_art()
    draw_chests(TIMBER_TONES, texture)
    draw_charms(texture)
    station_art()
    icon()
    preview()
