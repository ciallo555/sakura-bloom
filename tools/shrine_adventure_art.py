"""Pixel art for the twilight animals, their shrine keeper, and the offering chest."""
from pathlib import Path
from random import Random
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[1] / "src/generated/resources/assets/sakura_bloom/textures"


def save(image, name):
    path = OUT / (name + ".png")
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def fur(name, size, palette, seed, face, eyes, glow, markings):
    # Sparse clusters follow the coat; mark out the brow and muzzle on the vanilla model UVs.
    rng = Random(seed)
    im = Image.new("RGBA", size, palette[1])
    d = ImageDraw.Draw(im)
    for _ in range(size[0] * size[1] // 7):
        x, y = rng.randrange(size[0]), rng.randrange(size[1])
        c = rng.choice(palette)
        d.line((x, y, x+rng.choice((1,2,3)), y), fill=c)
        if rng.random() < .45:
            d.point((x+1, y-1), fill=c)
    for line in markings:
        d.line(line, fill=palette[3], width=1)
    x, y, width, height = face
    d.rectangle((x,y,x+width-1,y+height-1),fill=palette[1])
    d.line((x+1,y,x+width-2,y),fill=palette[2])
    d.line((x,y+height-2,x+2,y+height-1),fill=palette[3])
    d.line((x+width-3,y+height-1,x+width-1,y+height-2),fill=palette[3])
    glint = Image.new("RGBA", size)
    g = ImageDraw.Draw(glint)
    for ex,ey in eyes:
        d.point((ex,ey-1),fill=palette[0])
        d.point((ex,ey),fill=glow)
        g.point((ex,ey),fill=glow)
    save(im,"entity/"+name)
    save(glint,"entity/"+name+"_eyes")


def maiden():
    im = Image.new("RGBA",(64,64))
    d = ImageDraw.Draw(im)
    hair, chestnut, warm = "#291e28","#42303c","#61414a"
    skin, blush, cream = "#f1c4ab","#ce8c80","#f6eadd"
    dark_red, red, light_red = "#752f42","#ae4051","#d86368"
    d.rectangle((0,0,31,15),fill=hair)
    # Crown, side locks and the back of the head, with a two-strand fringe.
    for x in (1,5,10,13,19,23,28):
        d.line((x,1,x+1,6),fill=chestnut)
        d.line((x,9,x,14),fill=chestnut)
    d.rectangle((8,8,15,15),fill=skin)
    d.rectangle((8,8,15,9),fill=chestnut)
    for x,l in ((8,3),(9,1),(11,2),(12,1),(15,3)):
        d.line((x,9,x,9+l),fill=hair)
    d.point((10,12),fill=hair); d.point((13,12),fill=hair)
    d.point((9,13),fill=blush); d.point((14,13),fill=blush); d.point((12,14),fill="#bd807a")
    d.rectangle((16,16,39,31),fill=cream)
    for x in range(20,28):
        y=20+min(x-20,27-x)
        d.point((x,y),fill=dark_red)
        d.point((x,y+1),fill="#de9286")
    d.rectangle((16,27,39,29),fill=red)
    d.line((16,28,39,28),fill="#eac27b")
    d.rectangle((16,30,39,31),fill=dark_red)
    d.rectangle((40,16,55,31),fill=cream)
    for x in (41,45,48,52,55):
        d.line((x,21,x,26),fill="#e2cfc6")
    d.rectangle((40,27,55,28),fill=red)
    d.line((40,27,55,27),fill=light_red)
    d.rectangle((40,29,55,31),fill=skin)
    d.rectangle((0,16,15,31),fill=red)
    d.rectangle((0,27,15,29),fill=cream)
    d.rectangle((0,30,15,31),fill="#503443")
    # Solid pleated skirt with an embroidered hem and a gold-tied hair ribbon.
    d.rectangle((0,32,27,46),fill=red)
    for x in (1,5,8,12,15,19,22,26):
        d.line((x,37,x-1,46),fill=dark_red)
        d.line((x+1,37,x,44),fill=light_red)
    d.line((0,45,27,45),fill="#d98d74")
    d.rectangle((32,32,51,44),fill=red)
    d.line((33,34,46,34),fill=light_red)
    d.rectangle((37,33,39,35),fill="#efc77b")
    save(im,"entity/shrine_maiden")


def shrine_items():
    im=Image.new("RGBA",(16,16))
    d=ImageDraw.Draw(im)
    d.polygon([(5,0),(13,2),(13,4),(10,15),(2,13)],fill="#553440")
    d.polygon([(6,1),(12,3),(9,14),(3,12)],fill="#e5bfa3")
    d.polygon([(6,2),(11,3),(9,12),(4,11)],fill="#f8e8c9")
    d.line((6,3,10,4),fill="#bd535f")
    d.line((7,5,6,9),fill="#973b50");d.line((5,6,9,7),fill="#ba4858")
    d.point((8,8),fill="#ce8679");d.line((4,11,8,12),fill="#be6565")
    d.point((10,1),fill="#efb07b")
    save(im,"item/sakura_talisman")
    rng=Random(773)
    for name,base,grain,light in (("side","#8a4b46","#77403f","#a46052"),("rim","#783f37","#653737","#ad6953"),("slot","#3e3031","#32282b","#59413c")):
        im=Image.new("RGB",(16,16),base)
        d=ImageDraw.Draw(im)
        for _ in range(29):
            x,y=rng.randrange(16),rng.randrange(16)
            d.line((x,y,min(15,x+rng.randrange(1,5)),y),fill=rng.choice((grain,light)))
        if name=="side":
            for y in (0,13):
                d.line((0,y,15,y),fill="#583336")
                d.line((0,y+1,15,y+1),fill="#bb7e62")
            for x in (1,14):
                d.line((x,1,x,13),fill="#5f3438")
                for y in (3,11): d.point((x,y),fill="#ebc782")
            d.polygon([(6,5),(8,4),(9,6),(11,6),(10,8),(8,8),(7,10),(6,8),(4,7)],fill="#d6a765")
            d.point((7,7),fill="#fff0b4")
        save(im,"block/offering_"+name)


if __name__ == "__main__":
    fur("dusk_fox",(48,32),["#261e33","#443348","#5b4056","#ab747e"],736,(7,11,8,6),
        [(8,14),(13,14)],"#ffc173", [[(35,5),(37,9),(39,10)],[(39,3),(40,6)],[(28,19),(31,21),(33,21)]])
    fur("ink_wolf",(64,32),["#202431","#303441","#474a58","#878699"],394,(4,4,6,6),
        [(4,7),(9,7)],"#c0e9ea", [[(27,6),(29,9),(32,10)],[(37,8),(35,11)],[(23,21),(28,25)]])
    fur("gloom_hare",(64,32),["#262436","#3f3752","#594b67","#967c99"],958,(37,5,5,4),
        [(37,7),(41,7),(34,7),(44,7)],"#ff96bb", [[(53,2),(53,6)],[(59,2),(59,6)],[(11,11),(14,13)]])
    maiden()
    shrine_items()
