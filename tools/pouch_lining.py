"""Weave the two pouch linings and the eight embroidered, rotatable charm footprints."""
from pathlib import Path
from random import Random
from PIL import Image, ImageDraw, ImageColor

DEST = Path(__file__).resolve().parents[1]/"src/main/resources/assets/sakura_bloom/textures/gui"
CELL = 18
STAMPS = {
    "shidare_wind_bell": (["X","X","X"], "#97aaa5"),
    "yae_knot_charm": (["XX","XX"], "#ce989d"),
    "celadon_fish_pendant": (["XX",".X"], "#96af94"),
    "yamazakura_ink_bracelet": ([".XX","XX."], "#ac99b1"),
    "lantern_crown": (["X.X","XXX"], "#cdac77"),
    "tide_pearl": ([".X.","XXX",".X."], "#92a9b8"),
    "brocade_knot": (["XX.",".XX","..X"], "#c69388"),
    "moon_mirror": ([".XX","XXX"], "#aaa3bb"),
}


def blend(a,b,t):
    return tuple(round(x+(y-x)*t) for x,y in zip(a,b))


def lining(name, width, height, silk):
    im=Image.new("RGBA",(176,204))
    d=ImageDraw.Draw(im)
    shade=ImageColor.getrgb(silk)
    d.rounded_rectangle((0,0,175,203),radius=7,fill="#4f3d3e")
    d.rounded_rectangle((2,1,173,201),radius=6,fill="#c3a78c")
    d.rounded_rectangle((4,3,171,199),radius=5,fill="#e1cfb3")
    for y in range(107,198,3):
        for x in range(5+y%2,171,4): d.point((x,y),fill="#d9c4a8")
    d.polygon([(4,19),(11,15),(165,15),(172,19),(166,98),(160,105),(16,105),(10,99)],fill="#634d50")
    d.polygon([(6,20),(12,17),(163,17),(170,20),(164,97),(159,103),(17,103),(12,97)],fill=shade)
    for y in range(20,96):
        for x in range(11,165):
            if (x+2*y)%13 == 0: d.point((x,y),fill=blend(shade,(226,201,171),.10))
    for x,direction in [(16,1),(159,-1)]:
        for y in (27,51,75):
            points=[(x,y+12),(x+5*direction,y+6),(x+3*direction,y+3),(x+7*direction,y)]
            d.line(points,fill=blend(shade,(245,210,157),.46))
            for tx,ty in (points[-1],points[1]):
                for dx,dy in ((0,-2),(-2,0),(2,0),(0,2)):
                    d.point((tx+dx,ty+dy),fill="#ddbca2")
                d.point((tx,ty),fill="#e8cc91")
    left=(176-width*CELL)//2
    top=20+(4-height)*6
    d.rounded_rectangle((left-3,top-3,left+width*CELL+2,top+height*CELL+2),radius=4,fill="#594843")
    d.rounded_rectangle((left-2,top-2,left+width*CELL+1,top+height*CELL+1),radius=3,fill="#c9ae88")
    for y in range(height*CELL):
        for x in range(width*CELL):
            v=(x%3==0)*3+(y%3==0)*4+((x//5+y//4)%2)*2
            d.point((left+x,top+y),fill=(180+v,156+v,132+v))
    for x in range(width):
        for y in range(height):
            xx=left+x*CELL; yy=top+y*CELL
            for k in (1,6,11,16):
                d.point((xx+k,yy),fill="#987f68")
                d.point((xx,yy+k),fill="#987f68")
            d.point((xx+1,yy+1),fill="#e4cba1")
    for y in [95,101]:
        for x in range(17,159,5): d.line((x,y,x+2,y),fill=blend(shade,(255,237,199),.50))
    for row in (122,140,158,180):
        for x in range(8,154,18):
            d.rectangle((x-1,row-1,x+16,row+16),fill="#93806e")
            d.rectangle((x,row,x+15,row+15),fill="#baa58e")
            d.line((x,row+16,x+16,row+16,x+16,row),fill="#ffefd0")
    im.save(DEST/(name+".png"))


def swatch(name,rows,color):
    cells={(x,y) for y,row in enumerate(rows) for x,c in enumerate(row) if c=="X"}
    for turn in range(4):
        w=max(x for x,y in cells)+1; h=max(y for x,y in cells)+1
        im=Image.new("RGBA",(w*CELL,h*CELL)); d=ImageDraw.Draw(im)
        base=ImageColor.getrgb(color)
        def inside(x,y):
            return (x//CELL,y//CELL) in cells
        for x,y in cells:
            for j in range(y*CELL,(y+1)*CELL):
                for i in range(x*CELL,(x+1)*CELL):
                    # Roll the corners. Only outer edges are bound, so cells sew into one piece.
                    horizontal=not inside(i-1,j) or not inside(i+1,j)
                    vertical=not inside(i,j-1) or not inside(i,j+1)
                    if horizontal and vertical: continue
                    if horizontal or vertical: tint=blend(base,(60,41,45),.66)
                    elif not all(inside(i+dx,j+dy) for dx,dy in [(-2,0),(2,0),(0,-2),(0,2)]):
                        tint=blend(base,(244,221,184),.40)
                    elif not all(inside(i+dx,j+dy) for dx,dy in [(-3,0),(3,0),(0,-3),(0,3)]) and (i+j)%5 in (1,2):
                        tint=blend(base,(99,64,58),.45)
                    else:
                        tint=blend(base,(246,226,184),.035*((i-j)%4)+(0.075 if (i+2*j)%17==0 else 0))
                    d.point((i,j),fill=tint)
            # Small plum sprigs worked into the silk, aligned to the weave.
            xx=x*CELL+10; yy=y*CELL+9
            d.line([(xx-5,yy+3),(xx-1,yy),(xx+2,yy-3)],fill=blend(base,(120,89,69),.34))
            for dx,dy in [(-1,-1),(1,-1),(0,1)]: d.point((xx+2+dx,yy-3+dy),fill=blend(base,(255,235,201),.60))
        im.save(DEST/"patch"/f"{name}_{turn}.png")
        cells={(h-1-y,x) for x,y in cells}


def generate():
    (DEST/"patch").mkdir(exist_ok=True,parents=True)
    lining("knotwork_pouch",4,3,"#926571")
    lining("cloud_brocade_pouch",5,4,"#697c83")
    for name,(rows,color) in STAMPS.items(): swatch(name,rows,color)


if __name__=="__main__":
    generate()
