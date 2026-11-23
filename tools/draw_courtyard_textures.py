"""Paint the courtyard materials as 16px tiles in the Sakura Bloom palette."""

from pathlib import Path
from random import Random

from PIL import Image, ImageDraw

from courtyard_patterns import patterned_textures

TEXTURES = Path(__file__).resolve().parents[1] / "src/main/resources/assets/sakura_bloom/textures"
STRAW = ("#675d44", "#8b794f", "#a89765", "#b8ab75", "#cbbc86", "#d9ca98", "#e1d5ac")
LEAF = ("#3b4947", "#4c5c52", "#657461", "#82896a", "#9a9e7d")
LACQUER = ("#553b41", "#75484b", "#915859", "#a36864", "#b5786c", "#c58c79")
GRANITE = ("#555e61", "#677071", "#788080", "#858b87", "#959a92", "#a6aaa0")
SILK = ("#733e48", "#924951", "#ac5457", "#c4625e", "#d57466", "#e28b73", "#ebaa85")
GOLD = ("#775842", "#976e47", "#b58c59", "#c9a56a", "#dfc18b")
MILK = ("#95a8aa", "#b2c1bb", "#c9d4c9", "#dbe1d3", "#e7e9d9", "#f3efdc")
BLUE = ("#304a68", "#415e7d", "#527596", "#7494ae", "#91acb8")


def tile(color):
    return Image.new("RGBA", (16, 16), color)


def save(name, image, folder="block"):
    image.save(TEXTURES / folder / (name + ".png"))


def grain(colors, seed, vertical=False):
    """Clusters follow a strip of timber, with breaks across the grain."""
    rng = Random(seed)
    image = tile(colors[2])
    pen = ImageDraw.Draw(image)
    for y in range(16):
        x = -rng.randrange(4)
        mid = rng.choice((2, 2, 3, 3, 4))
        while x < 16:
            length = rng.randint(2, 5)
            index = max(1, min(len(colors) - 1, mid + rng.choice((-1, 0, 0, 1))))
            pen.line((x, y, x + length - 1, y), fill=colors[index])
            x += length
    return image.transpose(Image.Transpose.TRANSPOSE) if vertical else image


def weave():
    top = tile(STRAW[4])
    pen = ImageDraw.Draw(top)
    for y in range(16):
        for x in range(2, 14):
            reed = (x + (y // 2) * 2) % 8
            value = (3, 4, 4, 4, 5, 4, 4, 3)[reed] if y % 2 == 0 else (3, 3, 3, 4, 4, 3, 3, 3)[reed]
            pen.point((x, y), STRAW[value])
        for x in (0, 1, 14, 15):
            outside = x in (0, 15)
            color = LEAF[1 if outside else 2]
            if (x + y) % 4 == 0:
                color = LEAF[3 if outside else 4]
            pen.point((x, y), color)
    save("tatami_weave", top)
    edge = grain(STRAW, 97)
    pen = ImageDraw.Draw(edge)
    for x in (0, 1, 14, 15):
        pen.line((x, 0, x, 15), fill=LEAF[1 + (x % 2)])
    save("tatami_edge", edge)


def carved_window():
    # Pierced four-lobed window with returning corner frets.
    mask = Image.new("1", (16, 16))
    carve = ImageDraw.Draw(mask)
    carve.rectangle((0,0,15,15), outline=1)
    for path in ([(7,2),(9,2),(10,3),(10,4),(9,5),(10,6),(11,5),(12,5),(13,6),(13,7)],
                 [(1,3),(3,3),(3,1)], [(3,3),(5,5)], [(7,0),(7,2)]):
        for quarter in range(4):
            carve.line(path, fill=1)
            path = [(15-y, x) for x,y in path]
    wood = grain(LACQUER, 57, True)
    img = tile((0, 0, 0, 0))
    pen = ImageDraw.Draw(img)
    for y in range(16):
        for x in range(16):
            if mask.getpixel((x,y)):
                tone = wood.getpixel((x,y))
                if y > 0 and not mask.getpixel((x,y-1)):
                    tone = LACQUER[4 if x % 3 else 5]
                elif x > 0 and not mask.getpixel((x-1,y)):
                    tone = LACQUER[1]
                pen.point((x,y), tone)
    save("begonia_carving", img)


def stone_and_ceramic():
    stone = tile(GRANITE[3])
    pen = ImageDraw.Draw(stone)
    rng = Random(51)
    for _ in range(68):
        x, y = rng.randrange(16), rng.randrange(16)
        shade = rng.choice((2,2,3,4))
        pen.line((x,y,x+rng.randrange(3),y), fill=GRANITE[shade])
        if rng.randrange(3) == 0:
            pen.point((x+1,y+1), GRANITE[shade])
    for x,y in ((0,2),(5,8),(8,4),(13,11),(2,13),(10,15)):
        pen.line((x,y,x+1,y), fill=GRANITE[1])
        pen.point((x+2,y-1), GRANITE[4])
    for x,y in ((1,11),(4,9),(11,2),(10,3)):
        pen.line((x,y,x+1,y), fill="#818879")
    save("garden_granite", stone)
    edge = stone.copy()
    pen = ImageDraw.Draw(edge)
    pen.line((0,4,15,4), fill=GRANITE[4])
    pen.line((0,5,15,5), fill=GRANITE[1])
    save("granite_eave", edge)
    window = stone.copy()
    pen = ImageDraw.Draw(window)
    pen.rectangle((5,6,10,9), fill="#666557")
    pen.rectangle((6,7,9,9), fill="#d6b36f")
    pen.line((7,6,8,6), fill="#a49266")
    pen.line((7,7,8,7), fill="#e6c888")
    pen.line((7,8,8,8), fill="#f4dda6")
    pen.line((7,9,8,9), fill="#e6c888")
    save("yukimi_window", window)

    porcelain = grain(MILK, 11)
    pen = ImageDraw.Draw(porcelain)
    pen.line((2,5,13,5), fill=BLUE[2])
    pen.line((2,14,13,14), fill=BLUE[1])
    pen.line([(3,12),(5,11),(7,11),(9,9),(11,8),(12,7)], fill=BLUE[1])
    pen.line([(7,11),(7,9),(6,8)], fill=BLUE[2])
    for x,y in ((6,8),(10,9),(12,7)):
        for dx,dy in ((0,-1),(-1,0),(1,0),(0,1)):
            pen.point((x+dx,y+dy), BLUE[2 if dx <= 0 else 3])
        pen.point((x,y), MILK[4])
    for x,y in ((4,10),(8,12),(11,10)):
        pen.line((x,y,x+1,y-1), fill=BLUE[2])
    save("porcelain_branch", porcelain)
    rim = grain(MILK, 73)
    pen = ImageDraw.Draw(rim)
    for y in (0,3,5,14):
        pen.line((0,y,15,y), fill=BLUE[1 if y in (3,14) else 3])
    pen.line((5,4,10,4), fill=BLUE[3])
    save("porcelain_rim", rim)
    save("vase_inside", grain(BLUE, 61))


def lantern():
    img = grain(SILK, 79, True)
    pen = ImageDraw.Draw(img)
    for x, index in enumerate((1,2,2,1,2,3,4,5,5,4,3,2,1,2,2,1)):
        for y in range(2,15):
            faded = int(y in (5,10,13) and index > 1)
            pen.point((x,y), SILK[index - faded])
    # Small auspicious lozenge woven into each red panel.
    pen.line([(6,8),(8,6),(10,8),(8,10),(6,8)], fill="#e6a472")
    pen.point((8,8), "#f1bd83")
    for x in (4,11):
        pen.line((x,3,x,13), fill=SILK[1])
        pen.point((x,6), SILK[2])
    save("palace_silk", img)
    brass = grain(GOLD, 21)
    pen = ImageDraw.Draw(brass)
    for x in range(1,16,4):
        pen.point((x,4), GOLD[4])
        pen.point((x,14), GOLD[1])
    save("aged_brass", brass)


def bamboo():
    cane = grain(STRAW, 77)
    pen = ImageDraw.Draw(cane)
    for x in (2,12):
        pen.line((x,0,x,15), fill=STRAW[2])
        pen.line((x+1,0,x+1,15), fill=STRAW[5])
    save("bamboo_cane", cane)
    item = tile((0,0,0,0))
    pen = ImageDraw.Draw(item)
    for x0,y0,x1,y1 in ((2,11,11,2),(3,13,13,3),(5,14,14,5)):
        pen.line((x0,y0+1,x1,y1+1), fill=STRAW[0], width=2)
        pen.line((x0,y0,x1,y1), fill=STRAW[3], width=2)
        pen.line((x0,y0,x1,y1), fill=STRAW[5])
    pen.line((5,7,9,11), fill=LEAF[0], width=2)
    pen.line((5,7,9,11), fill=LEAF[3])
    pen.point((8,9), LEAF[4])
    save("bamboo_splints", item, "item")


if __name__ == "__main__":
    for name, image in patterned_textures():
        save(name, image)
    save("rosewood_joinery", grain(LACQUER, 31, True))
    weave()
    carved_window()
    stone_and_ceramic()
    lantern()
    bamboo()
