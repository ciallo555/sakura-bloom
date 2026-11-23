"""Original pixel carvings for the barrel, round end and exposed side of each roof tile."""

from dataclasses import dataclass
from random import Random

from PIL import Image, ImageColor

from courtyard_variants import ROOF_GLAZES


@dataclass(frozen=True)
class Design:
    crown: str
    pigment: str
    face: str
    seal: str
    stagger: int = 8


# One 16px-wide barrel takes up half of a 32px block texture. Each large carving
# and its smaller end stamp is drawn separately so the end remains legible on slabs.
# @ = pigment, + = thin wash, x = incision, * = a raised or reserved light edge.
DESIGNS = {
    # Ivory crackle: irregular forked lines in the glaze.
    "white": Design("shallow", "#8e9a98", """
        ...x........
        ...x.......+
        ...+x....xx.
        .....x..x...
        ......xx....
        .......x....
        .......x+...
        ...xxx+..x..
        ..x.......x.
        .x........+.
        .x.........x
    """, """
        .x...x.
        ..x.x..
        ...x...
        ..x.x..
        xx...x.
        .....x.
        ....x..
    """, 5),
    # Persimmon calyx: four pointed leaves curling around the fruit scar.
    "orange": Design("round", "#77492f", """
        .++.........
        .+@@*....++.
        ..+@@*.+@@+.
        ...x@+*@@+..
        ....x**@+...
        ...@@**x....
        ..@@@+x@@...
        .+@+...@@+..
        .++.....@@..
        .........+..
    """, """
        @@...@@
        .@+.+@.
        ..+*+..
        ..*@*..
        ..+*+..
        .@+.+@.
        @@...@@
    """, 7),
    # Begonia quatrefoil: four round lobes and a reserved porcelain centre.
    "magenta": Design("broad", "#ead0c8", """
        ....+@@+....
        ....@**@....
        ....+@@+....
        .++..++..++.
        @**@+..+@**@
        @**@.@@.@**@
        .@@+.@@.+@@.
        .....++.....
        ....+@@+....
        ....@**@....
        ....+@@+....
    """, """
        ..+@+..
        ...@...
        @+.x.+@
        @@x*x@@
        @+.x.+@
        ...@...
        ..+@+..
    """),
    # Current: two long threads cross the tile on either side of a shallow whirl.
    "light_blue": Design("shallow", "#dbe8dd", """
        ..+@.....+@.
        ...+@....+@.
        ....@....@..
        ...@+...@+..
        ..@+...@+...
        .@...@@.....
        .@..@..+@...
        .@..@..@@...
        ..@..@@+....
        ...@+.....+.
        ....@@..+@..
        ......@@+...
    """, """
        ..@@@..
        .@...@.
        @..@@.@
        @.@.@.@
        @..@..@
        .@....@
        ..@@@..
    """, 4),
    # Rice ear: two ranks of full grains along a bent stalk.
    "yellow": Design("round", "#8b632c", """
        .........++.
        .......+@*..
        ......@@*...
        ....+..@.++.
        ...+@*.@.@*.
        ....@@.@@*..
        ..++..@..++.
        ..@**.@.@*..
        ...@@.@++...
        .....@..+...
        ....@..+....
        ...+........
    """, """
        ...@*..
        .@*.@*.
        ..@.@..
        .@.@.@*
        ..@@@..
        ...@...
        ..+....
    """, 6),
    # Young bamboo: pale culm joints and a pair of broad pointed leaves.
    "lime": Design("broad", "#547448", """
        ....@*......
        ....@*....+@
        ...+@@+..@@*
        ....@*..@@*.
        ....@*.+....
        .@@.@*......
        ..@@@*......
        ...+@@+.....
        ....@*...+@.
        ....@*..@@..
        ....@*.@*...
        ....@*......
    """, """
        ...@..+
        ..@@+@*
        ...@@..
        @@.@...
        .@@@...
        ..@@+..
        ...@...
    """, 5),
    # Windblown cherry: a split-tipped flower and two falling petals.
    "pink": Design("round", "#a25971", """
        ....+.+.....
        ....***.....
        .+..**+..+..
        ****+*++***.
        .***x.@***..
        ..++@*@++...
        ....@*@.....
        ...**++**...
        ..+***.***..
        ...**+.+*+..
        ...+....+...
    """, """
        ..@.@..
        ..+*+..
        @@.@.@@
        .+*@*+.
        ..+.+..
        .@*.*@.
        .@...@.
    """),
    # Pressed rope: a raised two-strand twist with small diagonally cut teeth.
    "gray": Design("fluted", "#44575e", """
        ...x@@..*+..
        ...+*@@*+...
        .....*@@x...
        ....+*x@@...
        ...+*..x@@..
        ...*+..@@x..
        ....*+@@*...
        ....x@@*+...
        ...x@@..*+..
        ...@@x..+*..
        ....@@+*+...
        .....@@*....
    """, """
        ...x@*.
        ..x@*..
        .*@*...
        ..*@@..
        ...*@@.
        ..*@x..
        .+@x...
    """, 6),
    # Ginkgo fan: scalloped lobes split above a pair of radiating veins.
    "light_gray": Design("broad", "#758475", """
        ...**..**...
        .**++**++**.
        *+++x++x+++*
        +@++x++x++@+
        ..@+x++x+@..
        ...@+xx+@...
        ....@++@....
        .....@@.....
        .....@......
        ....@.......
        ...+........
    """, """
        .**.**.
        *+x*x+*
        @+x+x+@
        .@x+x@.
        ..@@@..
        ...@...
        ..+....
    """),
    # Celadon scales: shingled lobes with a single pooled edge under each scale.
    "cyan": Design("shallow", "#345f64", """
        x....x....x.
        x*..x+*..x+.
        .x++x..++x..
        ..xx....x...
        ....x....x..
        *..x+*..x+*.
        .++x..++x...
        .xx....x....
        x....x....x.
        x*..x+*..x+.
        .x++x..++x..
        ..xx....x...
    """, """
        x..x..x
        x*.x*.x
        .xx.xx.
        ..x...x
        .x+..x+
        xx..xx.
        ..xx...
    """, 4),
    # Wisteria: dropped clusters shorten towards the pointed end of the raceme.
    "purple": Design("round", "#d1b7dd", """
        .xx.....x...
        ...xxxxx....
        ..*@..x.*@+.
        .@@+.*@..@@.
        ....@@+..x..
        ..*@..x..*@.
        .+@@.*@..+@.
        ....@@+..x..
        ...*@....+..
        ..+@@.......
        ...+@.......
        ....+.......
    """, """
        .xx.xx.
        ...x...
        .*@.@*.
        .@.x.@.
        ..*@...
        ...x...
        ...*...
    """, 7),
    # Seigaiha: rows of concentric fan-shaped waves engraved in indigo slip.
    "blue": Design("broad", "#a0bdc5", """
        ....+@@+....
        ..+@....@+..
        .@...@@...@.
        @..+@..@+..@
        ..@..++..@..
        .@..@..@..@.
        @@+......+@@
        ...@+..+@...
        @@...@@...@@
        ..@+....+@..
        ++..@..@..++
        ..@..@@..@..
    """, """
        ..@@@..
        .@...@.
        @.@@@.@
        .@...@.
        @..@..@
        .@@.@@.
        .......
    """, 8),
    # Basket impression: broad alternating over-and-under reed bundles.
    "brown": Design("shallow", "#c79b70", """
        @@+..*..@@+.
        @@+..*..@@+.
        ..x@@+xx..x@
        **x@@+..**x@
        ..x@@+....x@
        @@+..*..@@+.
        @@+..*..@@+.
        ..x@@+xx..x@
        **x@@+..**x@
        ..x@@+....x@
    """, """
        @@..@@.
        @@..@@.
        ..@@..@
        ..@@..@
        @@..@@.
        @@..@@.
        ..@@..@
    """, 5),
    # Pine sprig: three sprays of needles turn away from the central twig.
    "green": Design("fluted", "#bcc39a", """
        .......+..+.
        .......@.+..
        ....*..@.@.+
        .....@.@@.@.
        ......@x@@..
        ..+..+x.....
        ...@..x...+.
        .*..@.x..@..
        ..@..@x.@..+
        ...@@.x@.@@.
        .....x.@@...
        ....x.......
    """, """
        @..@..*
        .@.@.@.
        ..@@@..
        ...x..@
        @..x.@.
        .@@x@..
        ..x....
    """, 7),
    # Vermilion ruyi cloud: a trefoil curl with a long trailing tail.
    "red": Design("round", "#e7aa79", """
        ....+@@+....
        ...@....@...
        .+@..@@..@+.
        @..+@..@...@
        @..@...@...@
        .@@...+..@@.
        .....@+.....
        ...+@.......
        ..@...+@@+..
        ..@..@....+.
        ...@@.......
    """, """
        ..+@+..
        .@...@.
        @..@..@
        @.@.@.@
        .@..@@.
        ...@...
        .@@....
    """),
    # Soot-black key fret: squared returns in a smoked silver-grey inlay.
    "black": Design("fluted", "#9d9c90", """
        .@@@@@@.....
        .@....@.@@@.
        .@.@@.@.@.@.
        .@.@..@.@.@.
        .@.@@@@.@.@.
        .@......@.@.
        .@@@@@@@+.@.
        .........+@.
        ..@@@@@+....
        ..@...@.....
        ..@.@@@.....
    """, """
        @@@@@@.
        @....@.
        @.@@.@.
        @.@..@.
        @.@@@@.
        @......
        @@@@@@@
    """, 6),
}


# Leaves and blossoms share a die with a smaller companion. Some courses carry
# a bud, a spent blossom or windblown needles, leaving a break in the roof field.
COMPANIONS = {
    "white": """
        +.........x
        .x.......x.
        ..xxx...x..
        ....x..x...
        .....xx....
        .....x.xx..
        ....x....x+
        ...x.....x.
        ..x......x.
        +x........x
    """,
    "orange": """
        ....x..
        ...+x+.
        .+@+@*+
        ..@@*..
        ..@*@@.
        ..+..+.
    """,
    "pink": """
        .......+*.
        ......***.
        ......@+..
        ..........
        ..++......
        .+**+.++..
        ..@+..***.
        .......+@.
    """,
    "purple": """
        .xxx....
        ...x....
        .+*@.+..
        .@@.x*@.
        ...*@@+.
        ..+@@...
        ...x....
        ...*@...
        ...++...
    """,
    "green": """
        .+.....*.
        ..@..*@..
        ...@.x..+
        .*..xx.@.
        ..@@.x@..
        ....xx..*
        ...x..@@.
    """,
    "light_gray": """
        ..+*+....
        .+***+...
        .@x+**+..
        ..@@xx@..
        ....@@@..
        .......x.
        ........x
    """,
}


# Crown heights in discrete bands; broad shapes read at the same scale as a 16px plank.
CROWNS = {
    "round":   (0, 1, 2, 3, 5, 6, 7, 7, 7, 6, 6, 5, 4, 3, 2, 1),
    "broad":   (0, 1, 2, 4, 5, 6, 7, 7, 6, 6, 6, 5, 5, 4, 2, 1),
    "shallow": (1, 2, 3, 5, 6, 6, 7, 6, 6, 5, 5, 5, 4, 3, 2, 2),
    "fluted":  (0, 1, 3, 4, 5, 5, 4, 6, 7, 7, 6, 5, 4, 4, 2, 1),
}

CAP = (
    ".....lllll......",
    "...llhhhhhll....",
    "..lhhbbbbbhhl...",
    ".lhbb.....bbhl..",
    ".lhb.......bbl..",
    "lhb.........bdl.",
    "lhb.........bdl.",
    "lhb.........bdl.",
    "lhb.........bdl.",
    ".hbb.......bbd..",
    ".lhbb.....bbdl..",
    "..lddbbbbbddl...",
    "...ldddddddl....",
    ".....ddddd......",
    "................",
    "................",
)

# An uncapped barrel seen end-on. Recessed ceramic and the curved drip edge
# alternate with stamped ends, keeping long stair risers from becoming solid bands of seals.
OPEN_END = (
    ".....lllll......",
    "...llhhhhhll....",
    "..lhhbbbbbhhl...",
    ".lhbbsssssbbl...",
    ".lhbsssssssbhl..",
    "lhbssiiiiissbdl.",
    "lhbsiiiiiissbdl.",
    "lhbsiiiiiiisbdl.",
    ".hbsiiiiiissbdl.",
    ".lhbsiiiiisbbl..",
    "..lhbssssbbdl...",
    "...ldbbbbddl....",
    "....ldddddl.....",
    "......ddd.......",
    "................",
    "................",
)


def pixels(drawing):
    return [row.strip() for row in drawing.split()]


def mix(a, b, fraction):
    return tuple(round(x + (y - x) * fraction) for x, y in zip(a, b))


def glaze(color, firing):
    shadow, body, light = map(ImageColor.getrgb, ROOF_GLAZES[color])
    if firing == 1:
        body = mix(body, (194, 189, 169), .12)
    elif firing == 2:
        body = mix(body, (158, 101, 77), .13)
    tones = [mix(shadow, body, f) for f in (.12, .28, .44, .59, .74, .88)]
    tones.extend(mix(body, light, f) for f in (.03, .20, .35, .48))
    return tones, shadow, light


def decorate(base, mark, pigment, shadow, light):
    if mark == "@":
        return mix(base, pigment, .75)
    if mark == "+":
        return mix(base, pigment, .46)
    if mark == "x":
        return mix(base, shadow, .65)
    if mark == "*":
        return mix(base, light, .77)
    return base


def mark_at(rows, x, y):
    if 0 <= y < len(rows) and 0 <= x < len(rows[y]):
        return rows[y][x]
    return "."


def roof(color, firing):
    design = DESIGNS[color]
    crown = CROWNS[design.crown]
    tones, shadow, light = glaze(color, firing)
    pigment = ImageColor.getrgb(design.pigment)
    seal = pixels(design.seal)
    drawings = [pixels(d) for d in (design.face, COMPANIONS.get(color, design.face))]
    offsets = [((16 - max(map(len, d))) // 2, (15 - len(d)) // 2) for d in drawings]
    top, end, side = (Image.new("RGB", (32, 32)) for _ in range(3))
    ring_tones = {"l": 3, "h": 8, "b": 5, "d": 2, "s": 2, "i": 3}
    side_bands = (7, 8, 6, 5, 5, 5, 6, 6, 6, 5, 5, 5, 4, 3, 2, 2)

    for y in range(32):
        for x in range(32):
            u, v = x % 16, y % 16
            row, course = divmod(y + (x // 16) * design.stagger, 16)
            alternate = (x // 16 + row) % 2
            # A shallow arc at each overlap cuts a lip into the crowns of the barrels.
            lip = (course + (1 if 3 <= u <= 11 else 0)) % 16
            tone = crown[u]
            if lip == 15:
                tone = max(0, tone - 3)
            elif lip == 0:
                tone = min(9, tone + 2)
            elif lip == 1 and 3 <= u <= 10:
                tone += 1
            elif u == 4 + alternate and (course + 3 * firing + 4 * alternate) % 16 in (4, 5, 6):
                tone += 1
            elif u >= 12 and (course + firing) % 13 in (2, 3):
                tone = max(0, tone - 1)
            ox, oy = offsets[alternate]
            mark = mark_at(drawings[alternate], u - ox, course - oy)
            top.putpixel((x,y), decorate(tones[tone], mark, pigment, shadow, light))

            # Round cap, curled lower lip and a separate die for the eave.
            capped = (x // 16 + y // 16) % 2 == 0
            ring = (CAP if capped else OPEN_END)[v][u]
            end_tone = ring_tones.get(ring, 7 if u < 7 and v < 7 else 6)
            if ring == "i" and v >= 8 and u < 9:
                end_tone = 4
            if ring == "h":
                if design.crown == "fluted":
                    end_tone = (9, 7, 6, 8)[(u + v) % 4]
                elif design.crown == "broad":
                    end_tone = 9 if v < 3 else 7
                elif design.crown == "shallow":
                    end_tone = 7 if (u + firing) % 8 < 5 else 8
            in_disc = capped and 3 <= v <= 10 and 3 <= u <= 11
            if ring == "." and not in_disc:
                end_tone = (2, 3, 4, 4, 3, 2, 1, 2)[(v + (1 if u > 11 else 0)) % 8]
                if v == 14 and (u < 4 or u > 10):
                    end_tone = 6
            mark = mark_at(seal, u-4, v-3) if in_disc and ring == "." else "."
            end.putpixel((x,y), decorate(tones[end_tone], mark, pigment, shadow, light))

            # A side view of the lapped clay, with the small maker's mark pressed
            # below the rim. Repeat in each half so a bottom or top slab keeps it.
            across = (x + (y // 16) * design.stagger) % 32
            joint = (across - v // 5) % 32
            side_tone = side_bands[v]
            if joint in (0, 31):
                side_tone = max(0, side_tone - 2)
            elif joint == 1:
                side_tone = min(9, side_tone + 1)
            elif v == 12 and 7 <= across <= 26 and design.crown == "broad":
                side_tone += 1
            elif v == 2 and across % 5 in (0, 1) and design.crown == "fluted":
                side_tone -= 1
            mark = mark_at(seal, across-4, v-4)
            side.putpixel((x,y), decorate(tones[side_tone], mark, pigment, shadow, light))

    # Small clusters of kiln bloom in exposed clay, away from the pressed artwork.
    rng = Random(431 + firing * 11 + sum(map(ord, color)))
    for row in range(2):
        for _ in range(8):
            sx = rng.randrange(13, 28)
            sy = row * 16 + rng.randrange(4, 13)
            tint = rng.choice((light, shadow))
            for dx in range(rng.choice((2, 3, 4))):
                px = (sx + dx - row * design.stagger) % 32
                side.putpixel((px, sy), mix(side.getpixel((px, sy)), tint, .2))
    return {"roof_channels": top, "roof_channels_cross": top.transpose(Image.Transpose.ROTATE_90),
            "roof_eave": end, "roof_clay": side}
