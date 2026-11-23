"""Pixel designs for paper joinery, dyed linen, pierced woodwork and glazed earthenware."""

from random import Random

from PIL import Image, ImageColor, ImageDraw

from roof_patterns import roof
from courtyard_variants import CURTAINS, LATTICES, ROOF_FINISHES, ROOF_GLAZES, VASES, WOODS


def mix(a, b, fraction):
    if isinstance(a, str):
        a = ImageColor.getrgb(a)
    if isinstance(b, str):
        b = ImageColor.getrgb(b)
    return tuple(round(x + (y - x) * fraction) for x, y in zip(a, b))


def timber(color, seed):
    rng = Random(seed)
    frame = Image.new("RGB", (16, 16), color)
    pen = ImageDraw.Draw(frame)
    for x in range(16):
        base = mix(color, "#574946", rng.choice((0.03, 0.07, 0.12, 0.17)))
        y = -2
        while y < 16:
            length = rng.randint(2, 6)
            pen.line((x,y,x,y+length-1), fill=mix(base, "#f7dfb5", rng.choice((0.03, 0.06, 0.12, 0.17))))
            y += length
    return frame


# Coordinates are the paths of individual kumiko strips inside the frame. Partial
# rails meet another strip or the outer stile, so screens also work in long runs.
JOINERY = {
    "oak": [(5,1,5,14),(10,1,10,14),(1,6,14,6),(1,11,14,11)],
    "spruce": [(4,1,4,14),(8,1,8,14),(12,1,12,14),(1,10,14,10),(1,12,14,12)],
    "birch": [(1,4,14,4),(1,8,14,8),(1,12,14,12),(5,1,5,4),(10,4,10,8),(5,8,5,12)],
    "jungle": [(1,5,14,5),(1,11,14,11),(4,1,4,5),(11,1,11,5),(6,5,6,11),(4,11,4,14),(11,11,11,14)],
    "acacia": [(1,5,14,5),(1,12,14,12),(1,5,8,12),(8,12,14,6),(8,1,8,5)],
    "dark_oak": [(1,4,14,4),(1,11,14,11),(4,4,4,11),(11,4,11,11),(7,1,7,4),(7,11,7,14)],
    "mangrove": [(3,1,3,14),(12,1,12,14),(3,4,12,4),(1,9,14,9),(3,12,12,12)],
    "cherry": [(1,5,14,5),(1,11,14,11),(5,1,5,5),(10,1,10,5),(8,5,8,11),(5,11,5,14),(10,11,10,14)],
    "bamboo": [(4,1,4,14),(11,1,11,14),(1,4,14,4),(1,6,14,6),(1,12,14,12)],
    "crimson": [(1,7,14,7),(1,11,14,11),(1,3,5,7),(5,7,9,3),(9,3,13,7),(5,11,5,14),(11,11,11,14)],
    "warped": [(1,4,14,4),(1,10,14,10),(4,1,4,4),(11,1,11,4),(4,4,10,10),(10,4,14,8),(7,10,7,14)],
    "shidare": [(4,1,4,14),(1,4,14,4),(1,11,14,11),(10,1,10,4),(10,11,10,14)],
    "yae": [(1,4,14,4),(1,12,14,12),(4,4,4,12),(11,4,11,12),(8,1,8,4),(8,12,8,14)],
    "yamazakura": [(1,5,14,5),(1,12,14,12),(1,10,5,6),(5,6,9,10),(9,10,14,5),(7,1,7,5)],
}


def shoji(wood, seed):
    _, _, color, ground = WOODS[wood]
    frame = timber(color, seed)
    rng = Random(seed + 51)
    paper = Image.new("RGB", (16,16), ground)
    pen = ImageDraw.Draw(paper)
    # Scattered short mulberry fibers and a pale botanical wash behind the joinery.
    for _ in range(52):
        x, y = rng.randrange(16), rng.randrange(16)
        tint = rng.choice((mix(ground, "#fdf6e5", .45), mix(ground, "#aba18c", .10), mix(ground, "#bcb2ad", .13)))
        pen.line((x,y,x+rng.choice((0,1,2)),y), fill=tint)
    if wood in ("cherry","shidare","yae","mangrove"):
        stem = mix(ground, "#937e7b", .27)
        bloom = mix(ground, "#c7889a", .35)
        pen.line([(2,12),(4,10),(6,10),(9,7),(11,7)], fill=stem)
        for x,y in ((4,9),(8,8),(11,6)):
            pen.line((x-1,y,x+1,y), fill=bloom)
            pen.point((x,y-1), bloom)
    elif wood in ("bamboo","jungle","birch","warped"):
        ink = mix(ground, "#76836e", .23)
        pen.line([(10,13),(9,10),(9,8),(7,6)], fill=ink)
        for x,y in ((9,10),(8,7)):
            pen.line((x,y,x+3,y-2), fill=ink)
            pen.line((x-1,y-1,x-3,y-2), fill=ink)
    else:
        pen.line([(1,12),(4,9),(7,11),(10,8),(13,10)], fill=mix(ground, "#939b8d", .18))
    mask = Image.new("1", (16,16))
    bars = ImageDraw.Draw(mask)
    for line in JOINERY[wood]:
        bars.line(line, fill=1)
    for y in range(1,15):
        for x in range(1,15):
            if mask.getpixel((x,y)):
                pen.point((x,y), mix(frame.getpixel((x,y)), ground, .22))
            elif mask.getpixel((x,y-1)) or mask.getpixel((x-1,y)):
                pen.point((x,y), mix(paper.getpixel((x,y)), color, .20))
    return frame, paper


def linen(color):
    _, color, thread, motif = CURTAINS[color]
    image = Image.new("RGB", (16,16), color)
    pen = ImageDraw.Draw(image)
    for x in range(16):
        fold = (0.13,0.06,0.0,0.03,0.14,0.20,0.10,0.05)[x % 8]
        base = mix(color, "#282435", fold)
        for y in range(16):
            lift = (0.0,.03,.065)[(x//2 + y//3) % 3]
            pen.point((x,y), mix(base, "#f9e5ce", lift))
    ink = mix(color, thread, .91)
    accent = mix(color, thread, .64)
    if motif == "waves":
        for bottom, offset in ((10,3),(14,-1)):
            for cx in range(offset-8,24,8):
                for radius, curve, shade in ((4,(0,0,1,2,3),ink),(2,(0,0,1),accent)):
                    for dx in range(-radius,radius+1):
                        pen.point((cx+dx,bottom-radius+curve[abs(dx)]), shade)
    elif motif == "sakura":
        for x,y in ((3,8),(12,10)):
            for dx,dy in ((0,-2),(1,-2),(2,-1),(2,0),(1,1),(0,2),(-1,2),(-2,0),(-2,-1),(-1,-1)):
                pen.point((x+dx,y+dy), ink)
            pen.point((x,y), accent)
        for x,y in ((5,11),(10,6),(1,5),(14,12)):
            pen.line((x,y,x+1,y-1), fill=accent)
    elif motif == "hemp":
        for cx in (3,12):
            pen.line([(cx,5),(cx+3,7),(cx+3,10),(cx,12),(cx-3,10),(cx-3,7),(cx,5)], fill=ink)
            for dx,dy in ((0,-3),(3,-1),(3,2),(0,4),(-3,2),(-3,-1)):
                pen.line((cx,8,cx+dx,8+dy), fill=accent)
    else:
        for cx in (3,12):
            pen.line((cx,4,cx,13), fill=accent)
            for y in (5,9):
                for dx in (-1,1):
                    pen.line((cx+3*dx,y,cx,y+3), fill=ink)
                    pen.line((cx+3*dx,y+1,cx+dx,y+3), fill=accent)
    pen.line((0,2,15,2), fill=mix(color, "#443943", .35))
    for left,right,y in ((0,6,13),(9,15,14)):
        pen.line((left,y,right,y), fill=mix(color, "#51464b", .24))
        for x in range(left+1,right+1,3):
            pen.point((x,y), accent)
    return image


def lattice(pattern):
    mask = Image.new("1", (16,16))
    pen = ImageDraw.Draw(mask)
    pen.rectangle((0,0,15,15), outline=1)
    if pattern == "key_fret":
        for offset in (0,8):
            for path in ([(0,2),(6,2),(6,6),(3,6),(3,4),(5,4)], [(0,7),(2,7),(2,9),(6,9)]):
                line = [(x+offset,y) for x,y in path]
                pen.line(line, fill=1)
                pen.line([(15-x,15-y) for x,y in line], fill=1)
    elif pattern == "ice_crackle":
        for line in ([(0,5),(4,6),(7,3),(6,0)], [(7,3),(11,5),(15,3)], [(4,6),(5,10),(2,13),(0,12)],
                     [(5,10),(9,9),(11,5)], [(9,9),(12,12),(15,10)], [(12,12),(11,15)], [(2,13),(3,15)]):
            pen.line(line, fill=1)
    elif pattern == "diamond":
        for x in range(-8,24,8):
            for y in (0,8):
                pen.line([(x,y+4),(x+4,y),(x+8,y+4),(x+4,y+8),(x,y+4)], fill=1)
        pen.line([(5,8),(8,5),(11,8),(8,11),(5,8)], fill=1)
    else:
        for x,y in ((0,0),(8,8)):
            pen.line([(x,y+2),(x+5,y+2),(x+5,y+6),(x+8,y+6)], fill=1)
            pen.line([(x+2,y),(x+2,y+5),(x+6,y+5),(x+6,y+8)], fill=1)
        for line in ([(0,10),(4,10),(4,14),(8,14)], [(8,1),(11,1),(11,6),(15,6)], [(4,10),(4,5)],
                     [(6,8),(10,8),(10,5),(11,5)], [(10,8),(10,13)], [(7,14),(7,15)]):
            pen.line(line, fill=1)
    wood = WOODS[LATTICES[pattern][2]][2]
    material = timber(wood, 68)
    result = Image.new("RGBA", (16,16))
    for y in range(16):
        for x in range(16):
            if mask.getpixel((x,y)):
                tone = material.getpixel((x,y))
                if y and not mask.getpixel((x,y-1)):
                    tone = mix(tone, "#ecd1aa", .22)
                result.putpixel((x,y), (*tone,255))
    return result


def pottery(name):
    _, _, _, glaze, pigment, tiers, (lip,height) = VASES[name]
    base = Image.new("RGB", (16,16), glaze)
    pen = ImageDraw.Draw(base)
    for y in range(16):
        for x in range(16):
            edge = abs(x-7.5) / 8
            shade = mix(glaze, "#514b52", .17*edge + (.045 if (2*x+y//2)%7==0 else 0))
            if x in (4,5,6) and y in (5,6,9,10,11):
                shade = mix(shade, "#fff1db", .25)
            pen.point((x,y), shade)
    rim = base.copy()
    rim_pen = ImageDraw.Draw(rim)
    for y in (15-height, 16-tiers[-1][2], 14):
        rim_pen.line((0,y,15,y), fill=mix(pigment, glaze, .15))
    rim_pen.rectangle((lip+1,lip+1,14-lip,14-lip), outline=mix(pigment, glaze, .38))
    body, side = base.copy(), base.copy()
    line = mix(pigment, glaze, .10)
    light = mix(pigment, glaze, .45)
    for img, condensed in ((body,False),(side,True)):
        d = ImageDraw.Draw(img)
        if name == "celadon_bamboo":
            d.line([(6,13),(7,10),(7,7),(8,4)], fill=line)
            for x,y,dx in ((7,10,-1),(7,8,1),(8,6,-1)):
                d.line((x,y,x+dx*2,y-1), fill=light)
                d.line((x+dx,y-1,x+dx*3,y-3), fill=line)
            if not condensed:
                d.line((10,11,10,7), fill=light)
                d.line((10,8,12,6), fill=light)
        elif name == "rouge_plum":
            d.line([(3,10),(5,9),(7,9),(8,7),(11,6)], fill="#745c5c")
            buds = ((6,8),(10,6)) if condensed else ((5,9),(8,7),(11,6))
            for x,y in buds:
                d.line((x-1,y,x+1,y), fill="#ab6377")
                d.line((x,y-1,x,y+1), fill="#c97987")
                d.point((x,y), "#f0d6b4")
        elif name == "ink_magnolia":
            d.line([(4,12),(6,10),(8,10),(10,8),(11,6)], fill=line)
            for x,y in (((7,9),) if condensed else ((7,9),(11,7))):
                d.line((x-1,y-2,x,y), fill=light)
                d.line((x,y-2,x,y-1), fill="#f3edda")
                d.line((x+1,y-3,x+1,y-1), fill="#f9f1e0")
                d.point((x+2,y-2), light)
            d.line((7,11,9,12), fill=line)
        else:
            d.line([(2,9),(5,8),(8,9),(11,8),(13,7)], fill=line)
            for x,y in (((7,10),) if condensed else ((5,10),(10,9))):
                d.rectangle((x-1,y,x+1,y+1), fill="#a9543d")
                d.point((x-1,y), "#d97946")
                d.point((x,y-1), "#697050")
            d.line((8,9,7,7), fill=light)
        d.line((2,14,13,14), fill=light)
    interior = Image.new("RGB", (16,16), mix(glaze, "#3c3939", .67))
    ImageDraw.Draw(interior).rectangle((6,6,9,9), fill=mix(glaze, "#363238", .80))
    return {"body": body, "sprig": side, "rim": rim, "inside": interior}


def patterned_textures():
    for index, wood in enumerate(WOODS):
        frame, paper = shoji(wood, index + 73)
        yield ("honey_joinery" if wood == "cherry" else f"shoji_{wood}_frame"), frame
        yield ("shoji_paper" if wood == "cherry" else f"shoji_{wood}_paper"), paper
    for color in CURTAINS:
        yield ("noren_waves" if color == "blue" else f"noren_{color}_cloth"), linen(color)
    for name in LATTICES:
        yield f"{name}_carving", lattice(name)
    for name in VASES:
        for part, image in pottery(name).items():
            yield f"{name}_{part}", image
    for color in ROOF_GLAZES:
        prefix = "" if color == "gray" else color + "_"
        for index, (suffix, _) in enumerate(ROOF_FINISHES):
            for name, image in roof(color, index).items():
                yield prefix + name + suffix, image
