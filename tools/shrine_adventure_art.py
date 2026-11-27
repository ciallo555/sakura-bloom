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


def shrine_items():
    from interstice_art import offering_wood, talisman
    save(talisman(), "item/sakura_talisman")
    for part in ("side", "rim", "slot"):
        save(offering_wood(part), "block/offering_" + part)


if __name__ == "__main__":
    fur("dusk_fox",(48,32),["#261e33","#443348","#5b4056","#ab747e"],736,(7,11,8,6),
        [(8,14),(13,14)],"#ffc173", [[(35,5),(37,9),(39,10)],[(39,3),(40,6)],[(28,19),(31,21),(33,21)]])
    fur("ink_wolf",(64,32),["#202431","#303441","#474a58","#878699"],394,(4,4,6,6),
        [(4,7),(9,7)],"#c0e9ea", [[(27,6),(29,9),(32,10)],[(37,8),(35,11)],[(23,21),(28,25)]])
    fur("gloom_hare",(64,32),["#262436","#3f3752","#594b67","#967c99"],958,(37,5,5,4),
        [(37,7),(41,7),(34,7),(44,7)],"#ff96bb", [[(53,2),(53,6)],[(59,2),(59,6)],[(11,11),(14,13)]])
    shrine_items()
