"""Build the interlocking streets and four rooftop sanctums for the Interstice."""
import gzip
from io import BytesIO

from nbtlib import Byte, Compound, Double, File, Float, Int, List, String
from generate_resources import CORE_OUTPUT, MOD
from generate_shrines import Blueprint, THEMES, reward_chest
from timber_architecture import roof, tatami, wall

THEMES_BY_NAME = dict(zip(("lantern", "moonlit", "vermilion", "ashen"), THEMES))
M = MOD + ":"
MC = "minecraft:"


def beam(b, t, x, z, x1, z1, y):
    b.fill((x, y, z), (x1, y, z1), t.timber("stripped_log"), axis="x" if x != x1 else "z")


def post(b, t, x, z, low, high):
    b.fill((x, low, z), (x, high, z), t.timber("log"), axis="y")


def ladder(b, t, x, z, floor, top):
    post(b, t, x, z + 1, floor, top)
    b.fill((x, floor + 1, z), (x, top, z), MC + "ladder", facing="north")


def floor(b, t, x, z, x1, z1, y):
    b.fill((x, y, z), (x1, y, z1), t.timber("planks"))


def trim(b, t, x, z, x1, z1, y):
    for x0 in (x, x1):
        beam(b, t, x0, z, x0, z1, y)
    for z0 in (z, z1):
        beam(b, t, x, z0, x1, z0, y)


def streets(b, t):
    # Four-block clear pedestrian lanes continue through every turn and biome border.
    floor(b, t, 0, 13, 31, 18, 4)
    floor(b, t, 13, 0, 18, 31, 4)
    for side in (13, 18):
        for n in (*range(6), *range(26, 32)):
            for x, z in ((side, n), (n, side)):
                b.place(x, 5, z, t.timber("fence"))
                if n in (3, 28):
                    post(b, t, x, z, 1, 5)
                    b.place(x, 6, z, M + "hanami_lantern", lit="true")
                else:
                    b.place(x, 3, z, t.timber("stripped_log"), axis="z" if x == side else "x")


def room(b, t, x, z, span, floor_y, height, *, all_open=False):
    """Symmetrical central exits, solid corners and a framed screen in each bay."""
    end_x, end_z = x + span - 1, z + span - 1
    bays = ["s"] * span
    for i in (0, 3, span-4, span-1):
        bays[i] = "|"
    opening = span // 2 - 2
    bays[opening:opening+4] = "...."
    if all_open:
        bays = ["." if c == "s" else c for c in bays]
    form = "".join(bays)
    for axis, ax, az, face in (("x", x, z, "north"), ("x", x, end_z, "south"),
                               ("z", x, z, "west"), ("z", end_x, z, "east")):
        wall(b, t, ax, az, axis, floor_y, floor_y+height, form, facing=face)


def lantern_house(b, t, variant):
    floor(b, t, 6, 6, 25, 25, 4)
    trim(b, t, 6, 6, 25, 25, 3)
    if variant:
        # Two narrow teahouses face a lantern alley. Their first-floor balconies join
        # above the crossroads, leaving both ground-level streets open.
        for x, tone in ((6,"pink"), (19,"magenta")):
            floor(b,t,x,6,x+6,25,4)
            floor(b,t,x,7,x+6,24,12)
            for xx in (x+1,x+5):
                for z in (8,13,18,23):
                    post(b,t,xx,z,1,15)
            trim(b,t,x+1,8,x+5,23,10)
            for z in (8,23):
                wall(b,t,x+1,z,"x",4,10,"|sss|")
                wall(b,t,x+1,z,"x",12,16,"|lsl|")
            for z in (10,20):
                b.fill((x+2,5,z),(x+4,5,z),t.timber("slab"),type="top")
                b.place(x+3,6,z,M+t.vase,facing="north")
                b.place(x+3,11,z,MC+"lantern",hanging="true")
            roof(b,t,x,6,x+6,25,16,axis="z",color=tone,edge="white")
            ladder(b,t,x+2,11,4,12)
        floor(b,t,12,14,19,17,12)
        for z in (14,17):
            b.fill((13,13,z),(18,13,z),t.timber("fence"))
        beam(b,t,10,16,21,16,17)
        for x,length in ((13,1),(15,2),(18,1)):
            b.fill((x,17-length,16),(x,16,16),MC+"chain",axis="y")
            b.place(x,16-length,16,MC+"lantern",hanging="true")
        return
    room(b, t, 9, 9, 14, 4, 5)
    for x in (9, 22):
        for z in (9, 22):
            post(b, t, x, z, 0, 6)
    floor(b, t, 8, 8, 23, 23, 10)
    room(b, t, 10, 10, 12, 10, 4)
    tatami(b, 12, 12, 19, 19, 10)
    roof(b, t, 7, 7, 24, 24, 10, skirt=1, color="pink", edge="white")
    roof(b, t, 8, 8, 23, 23, 15, color="pink", edge="gray")
    ladder(b, t, 11, 11, 4, 10)
    for x, z in ((12,13), (19,18)):
        b.place(x, 5, z, t.timber("stairs"), facing="east")
        b.place(x, 11, z, M + t.vase, facing="north")
    for x in (7, 24):
        for z in (10, 21):
            b.place(x, 9, z, MC+"lantern", hanging="true")


def sky_barge(b, t, variant):
    # A tapered keel, stepped gunwales and raised prow make the hull readable from below.
    for z in range(2, 30):
        distance = min(z-2, 29-z)
        width = min(7, 1 + distance)
        for y in range(4):
            half = max(0, width - (3-y))
            b.fill((15-half,y,z), (16+half,y,z), t.timber("planks"))
        floor(b, t, 15-width, z, 16+width, z, 4)
        if not 13 <= z <= 18:
            for x in (15-width, 16+width):
                b.place(x, 5, z, t.timber("stairs"), facing="east" if x<16 else "west")
                if distance < 2:
                    b.place(x, 6, z, t.timber("slab"), type="bottom")
    for z in (7, 22):
        for x in (12,19):
            post(b,t,x,z,5,9)
        beam(b,t,12,z,19,z,9)
    roof(b,t,10,6,21,10,10,color="light_blue",edge="cyan", axis="x")
    if variant:
        roof(b,t,10,20,21,25,10,color="cyan",edge="blue")
        for x in (10,21):
            b.place(x, 5, 22, MC+"barrel", facing="up")
    else:
        post(b,t,15,23,5,18)
        beam(b,t,9,23,21,23,16)
        for x in range(10,22):
            hem = 10 + (abs(x-15)//3)
            b.fill((x,hem,23),(x,15,23),MC+("light_blue_wool" if x in (12,19) else "white_wool"))
        b.place(15,19,23,MC+"soul_lantern")
    for z in (5,26):
        b.place(15,5,z,M+"hanami_lantern",lit="true")


def vermilion_crossing(b, t, variant):
    for x0,z0 in ((6,6),(19,6),(6,19),(19,19)):
        floor(b,t,x0,z0,x0+6,z0+6,4)
        for x in (x0+1,x0+5):
            for z in (z0+1,z0+5):
                post(b,t,x,z,0,10)
        trim(b,t,x0+1,z0+1,x0+5,z0+5,10)
        if variant and x0==z0:
            for x in (x0+1,x0+5):
                beam(b,t,x,z0+1,x,z0+5,11)
            b.place(x0+3,9,z0+1,MC+"bell",attachment="ceiling",facing="north")
        else:
            roof(b,t,x0,z0,x0+6,z0+6,11,style="hip",color="red",edge="orange")
        b.place(x0+3,5,z0+3,M+t.vase,facing="south")
    # Upper bridge crosses at a different height. Leave the ground-level avenue clear.
    floor(b,t,3,13,28,18,13)
    for x in (4, 11, 20, 27):
        for z in (13,18):
            post(b,t,x,z,2,14)
    for z in (13,18):
        b.fill((3,14,z),(28,14,z),t.timber("fence"))
    ladder(b,t,6,15,4,13)
    if variant:
        for x in (8,23):
            for z in (13,18):
                post(b,t,x,z,14,18)
            beam(b,t,x,12,x,19,18)
            b.place(x,17,15,MC+"lantern",hanging="true")
        roof(b,t,6,12,25,19,19,axis="x",color="orange",edge="red")
    else:
        for x in (7,24):
            b.place(x,15,13,M+"hanami_lantern",lit="true")


def hanging_archive(b,t,variant):
    floor(b,t,6,6,25,25,4)
    room(b,t,8,8,16,4,6,all_open=True)
    for x in (8,23):
        for z in (8,23):
            post(b,t,x,z,0,10)
            b.place(x,0,z,MC+"chiseled_deepslate")
    floor(b,t,7,7,24,24,11)
    roof(b,t,6,6,25,25,11,skirt=1,color="gray",edge="black")
    if variant:
        # A reading tower overlooks the low west gallery and its open book court.
        floor(b,t,14,6,24,16,11)
        room(b,t,15,7,8,11,7)
        roof(b,t,13,5,24,16,18,axis="z",color="purple",edge="black")
        for x in (7,12):
            for z in (9,22):
                post(b,t,x,z,11,15)
        roof(b,t,6,8,13,23,16,axis="z",color="gray",edge="black")
        for z in (10,20):
            b.place(8,12,z,M+t.vase,facing="east")
    else:
        room(b,t,10,10,12,11,5)
        roof(b,t,8,8,23,23,17,color="gray",edge="black")
    ladder(b,t,10,11,4,11)
    for x in (11,20):
        for z in (12,13,18,19):
            b.fill((x,12,z),(x,14,z),MC+"bookshelf")
    for x in (7,24):
        for z in (10,21):
            b.place(x,10,z,MC+"soul_lantern",hanging="true")


def build_quarters():
    shapes = (lantern_house, sky_barge, vermilion_crossing, hanging_archive)
    for (theme,t), shape in zip(THEMES_BY_NAME.items(), shapes):
        for variant in range(2):
            b=Blueprint()
            streets(b,t)
            shape(b,t,variant)
            b.save(f"interstice/{theme}_quarter_{variant}", (32,24,32))


def resident(template, mob):
    """SinglePoolElement clips entities against the currently generating chunk."""
    path=CORE_OUTPUT/f"data/{MOD}/structure/{template}.nbt"
    f=File.load(path,gzipped=True)
    x,y,z=19,24,17
    f["entities"]=List[Compound]([Compound({
        "pos":List[Double]([x+.5,float(y),z+.5]), "blockPos":List[Int]([x,y,z]),
        "nbt":Compound({"id":String(M+mob), "Rotation":List[Float]([90.,0.]),"PersistenceRequired":Byte(1)}),
    })])
    out=BytesIO()
    f.write(out)
    path.write_bytes(gzip.compress(out.getvalue(),mtime=0))


def build_sanctums():
    for theme,t in THEMES_BY_NAME.items():
        b=Blueprint()
        # The western access shaft meets the crossroad of every rotated lower quarter.
        ladder(b,t,4,15,4,23)
        floor(b,t,5,5,26,26,23)
        trim(b,t,5,5,26,26,22)
        for z in (5,26):
            b.fill((5,24,z),(26,24,z),t.timber("fence"))
        for x in (5,26):
            for z in (*range(6,14),*range(18,26)):
                b.place(x,24,z,t.timber("fence"))
        room(b,t,8,8,16,23,8,all_open=theme=="vermilion")
        for x in (8,23):
            for z in (8,23):
                post(b,t,x,z,17,31)
        tatami(b,11,11,20,20,23)
        b.place(16,24,16,M+theme+"_altar")
        for x,z in ((14,16),(18,16),(16,14),(16,18)):
            b.place(x,24,z,MC+"red_candle",lit="false",candles="1")
        for x in (10,21):
            b.place(x,24,22,M+t.vase,facing="north")
            b.place(x,29,11,MC+"chain",axis="y")
            b.place(x,28,11,MC+"lantern",hanging="true")
        # Treasury and its solid backing sit away from the ritual floor.
        floor(b,t,12,21,12,21,24)
        reward_chest(b,(12,24,20),t.wood,theme+"_sanctum")
        if theme=="lantern":
            roof(b,t,5,5,26,26,32,skirt=3,color="pink",edge="red")
            roof(b,t,9,9,22,22,36,style="hip",color="white",edge="pink")
            for x in (13,18):
                for z in (7,24):
                    b.place(x,29,z,M+"pink_noren",facing="north")
        elif theme=="moonlit":
            roof(b,t,5,5,26,26,32,style="irimoya",axis="z",color="light_blue",edge="cyan")
            for z in (6,25):
                for x in (7,24):
                    b.place(x,24,z,MC+"sea_lantern")
        elif theme=="vermilion":
            roof(b,t,5,5,26,26,32,skirt=2,color="orange",edge="red")
            roof(b,t,8,8,23,23,35,color="red",edge="yellow")
            for x in (9,22):
                for z in (10,21):
                    b.place(x,24,z,MC+"bell",attachment="floor",facing="north")
        else:
            roof(b,t,5,5,26,26,32,style="gable",color="black",edge="gray")
            for x in (9,22):
                for z in (11,12,19,20):
                    b.fill((x,24,z),(x,26,z),MC+"bookshelf")
        name=f"interstice/{theme}_sanctum"
        b.save(name,(32,48,32))
        resident(name, "shrine_maiden" if theme=="lantern" else "veil_sentinel")
