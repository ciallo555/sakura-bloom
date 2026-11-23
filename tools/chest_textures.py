"""Paint the vanilla 64px chest UV layout, joining double-width boards across both halves."""

from random import Random
from PIL import Image, ImageColor, ImageDraw


def plank_panel(width, height, tones, trim, seed):
    rng = Random(seed)
    img = Image.new("RGB", (width,height), tones[3])
    pen = ImageDraw.Draw(img)
    for y in range(height):
        x = -rng.randrange(4)
        while x < width:
            n = rng.randint(2,4)
            mid = 3 if y % 5 in (1,2) else 4
            pen.line((x,y,x+n-1,y), fill=tones[mid+rng.choice((-1,0,0,1))])
            x += n
    if height >= 9:
        # A few stopped cuts and stepped fibers break up the broad face boards.
        for x in range(3,width-3,9):
            y = 3 + rng.randrange(height-6)
            pen.line((x,y,x+2,y), fill=tones[1])
            pen.line((x+2,y+1,x+4,y+1), fill=tones[2])
            pen.point((x+1,y+1), tones[5])
        pen.line((3,1,width-4,1), fill=tones[2])
    pen.rectangle((0,0,width-1,height-1), outline=trim[1])
    for x in (1,width-2):
        pen.line((x,1,x,height-2), fill=trim[3])
    pen.line((2,height-2,width-3,height-2), fill=tones[3])
    for x in (1,width-2):
        pen.point((x,1), trim[0])
        if height>5: pen.point((x,height-2), trim[1])
    return img


def lined_interior(width, tones, trim, deep):
    image = Image.new("RGB", (width,14), trim[1])
    d = ImageDraw.Draw(image)
    d.rectangle((1,1,width-2,12), outline=tones[1], fill=trim[2])
    d.rectangle((2,2,width-3,11), fill=trim[0] if deep else trim[1])
    if deep:
        d.line((2,2,width-3,2), fill=trim[1])
        d.line((2,3,2,10), fill=trim[1])
    if not deep:
        for y in (5,9):
            d.line((3,y,width-4,y), fill=trim[2])
    return image


def chest_atlas(tones, trim, seed, half):
    # Stain the reinforcing rails with the box timber so pale cherry doesn't
    # acquire a solid black outline. Retain the deepest shade inside the box.
    light = ImageColor.getrgb(tones[1])
    trim = [tuple(round(v+(w-v)*amount) for v,w in zip(ImageColor.getrgb(tone),light))
            for tone,amount in zip(trim,(.05,.22,.30,.43,.52))]
    width = 14 if half == "single" else 15
    full = width if half == "single" else 2*width
    atlas = Image.new("RGBA", (64,64))

    def half_panel(img, reverse=False):
        if full == width:
            return img
        offset = width if (half == "left") != reverse else 0
        return img.crop((offset,0,offset+width,img.height))

    for lid, origin, height in ((True,0,5), (False,19,10)):
        surface = plank_panel(full,14,tones,trim,seed+origin)
        lining = lined_interior(full,tones,trim,deep=not lid)
        atlas.paste(half_panel(lining if lid else surface), (14,origin))
        atlas.paste(half_panel(surface if lid else lining), (14+width,origin))
        band = origin+14
        side = plank_panel(14,height,tones,trim,seed+30+origin)
        front = plank_panel(full,height,tones,trim,seed+50+origin)
        back = plank_panel(full,height,tones,trim,seed+70+origin)
        if not lid:
            # The two hinge plates sit at the outside ends of the entire back.
            pen = ImageDraw.Draw(back)
            for x in (2,full-5):
                pen.line((x,height-2,x+2,height-2), fill="#777076")
        if half != "left": atlas.paste(side,(0,band))
        atlas.paste(half_panel(back),(14,band))
        if half != "right": atlas.paste(side.transpose(Image.Transpose.FLIP_LEFT_RIGHT),(14+width,band))
        atlas.paste(half_panel(front,reverse=True),(28+width,band))
    # The latch is its own tiny unwrapped cube, split in two on a large chest.
    lock_width = 2 if half == "single" else 1
    metal = ("#62626a","#939399","#b9bbbd","#deded5")
    pen = ImageDraw.Draw(atlas)
    pen.rectangle((1,0,2*lock_width,0), fill=metal[2])
    for y in range(1,5):
        pen.line((0,y,2*lock_width+1,y), fill=metal[(0,1,2,2)[y-1]])
        pen.point((1,y), fill=metal[1])
        pen.point((2*lock_width+1,y), fill=metal[min(3,y-1)])
    return atlas


def draw_chests(timber_palettes, write_texture):
    for index, (wood,palette) in enumerate(timber_palettes.items()):
        for half in ("single","left","right"):
            name = f"{wood}_chest" + ("" if half == "single" else "_" + half)
            image = chest_atlas(palette["planks"], palette["bark"], 137+19*index, half)
            write_texture("entity/chest", name, image)
