"""Small building elements shared by the seven original Sakura Bloom templates.

Coordinates denote blocks, with +Z leading in from the road. Roof pitches are
specified in half-blocks so a wide eave can turn up without a full-block step.
"""

from nbtlib import Compound, List, String

from generate_resources import MOD


AIR = "minecraft:air"


def ground(b, x0, z0, x1, z1, *, y=2, top="grass_block", clear=17, corners=0):
    """Foot only the occupied ground; leave the cut corners to the surrounding biome."""
    for z in range(z0, z1 + 1):
        inset = max(0, corners - min(z - z0, z1 - z))
        if x0 + inset > x1 - inset:
            continue
        a, c = x0 + inset, x1 - inset
        b.fill((a, 0, z), (c, y - 1, z), "minecraft:dirt")
        b.fill((a, y, z), (c, y, z), "minecraft:" + top)
        b.fill((a, y + 1, z), (c, clear, z), AIR)


def pave(b, x0, z0, x1, z1, material, y=2):
    b.fill((x0, y, z0), (x1, y, z1), "minecraft:" + material)


def deck(b, t, x0, z0, x1, z1, y):
    for x in range(x0, x1 + 1, 4):
        for z in {z0, z1}:
            b.fill((x, 1, z), (x, y - 1, z), t.timber("log"), axis="y")
    b.fill((x0, y, z0), (x1, y, z1), t.timber("planks"))


def wall(b, t, x, z, axis, floor, tie, bays, *, facing=None):
    """A post-and-lintel wall. | post, s paper, l openwork, w plaster, t wood, . opening."""
    facing = facing or ("north" if axis == "x" else "west")
    for i, bay in enumerate(bays):
        px = x + (i if axis == "x" else 0)
        pz = z + (i if axis == "z" else 0)
        b.place(px, tie, pz, t.timber("stripped_log"), axis=axis)
        if bay == "|":
            b.fill((px, floor + 1, pz), (px, tie - 1, pz), t.timber("log"), axis="y")
        elif bay == ".":
            b.fill((px, floor + 1, pz), (px, tie - 1, pz), AIR)
        elif bay in "sl":
            b.place(px, floor + 1, pz, t.timber("planks"))
            screen = t.wood + "_shoji" if bay == "s" else t.window
            b.fill((px, floor + 2, pz), (px, tie - 1, pz), f"{MOD}:{screen}", facing=facing)
        elif bay in "wt":
            block = "minecraft:white_terracotta" if bay == "w" else t.timber("planks")
            b.fill((px, floor + 1, pz), (px, tie - 1, pz), block)
        else:
            raise ValueError(f"Unknown wall bay {bay!r}")


def curtain(b, t, x0, x1, y, z, colors=()):
    for i, x in enumerate(range(x0, x1 + 1)):
        name = colors[i % len(colors)] + "_noren" if colors else t.curtain
        b.place(x, y, z, f"{MOD}:{name}", facing="north")


def tatami(b, x0, z0, x1, z1, y):
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            b.place(x, y, z, f"{MOD}:bound_tatami", facing="east" if ((x-x0)//2 + (z-z0)//2) % 2 else "north")


def altar(b, t, cx, y, z):
    b.fill((cx-2, y, z), (cx+2, y, z), t.timber("stripped_log"), axis="x")
    b.place(cx, y+1, z, f"{MOD}:{t.vase}", facing="north")
    for x in (cx-2, cx+2):
        b.place(x, y+1, z, f"{MOD}:hanami_lantern", lit="true")


def offering_box(b, x, y, z):
    for dx in (0, 1):
        b.place(x+dx, y, z, "minecraft:barrel", facing="south")
        b.place(x+dx, y+1, z, "minecraft:spruce_trapdoor", half="bottom", facing="east")


def tiled_course(b, x, y, z, tone, axis, half, *, backed, ridge=False):
    # Back each raised course down to the preceding one. Thin diagonal slabs alone
    # meet at an edge and let daylight through the steeper upper roof.
    slab = f"{MOD}:{tone}_roof_tile_slab"
    b.place(x, y, z, slab, type="double" if ridge or (backed and half) else "top" if half else "bottom", axis=axis)
    if backed and (not half or ridge):
        b.place(x, y-1, z, "minecraft:spruce_slab", type="top")


def rail(b, t, x, z, axis, length, y):
    forward, back = ("east", "west") if axis == "x" else ("south", "north")
    for i in range(length):
        px, pz = x + (i if axis == "x" else 0), z + (i if axis == "z" else 0)
        b.place(px, y, pz, t.timber("fence"), **{forward: str(i < length-1).lower(), back: str(i > 0).lower()})


def roof(b, t, x0, z0, x1, z1, eave, *, axis="x", style="irimoya", color=None,
         edge="gray", crest=None, ridge=None, skirt=None):
    """Gable, hip, or hip-and-gable roof. For a tiered hall leave the lantern story open with skirt."""
    color = color or t.roof
    crest = crest or edge
    u1, v1 = (x1-x0, z1-z0) if axis == "x" else (z1-z0, x1-x0)
    mid = v1//2 if ridge is None else ridge
    span = min(mid, v1-mid)
    def position(u, v):
        return (x0+u, z0+v) if axis == "x" else (x0+v, z0+u)
    def elevation(depth):
        # The outer courses lie nearly flat, then the roof steepens towards the ridge.
        return depth if depth <= 3 else 3 + (3*(depth-3)+1)//2
    def depth_at(v):
        return (v*span)//mid if v <= mid else ((v1-v)*span)//(v1-mid)
    for u in range(u1 + 1):
        end = min(u, u1-u)
        for v in range(v1 + 1):
            cross = depth_at(v)
            hip = style == "hip" or (style == "irimoya" and end < 3)
            depth = min(end, cross) if hip else cross
            if skirt is not None and depth > skirt:
                continue
            x, z = position(u, v)
            upturn = int(end <= 1 and min(v, v1-v) <= 1)
            h = elevation(depth) + upturn
            y = eave + h//2
            at_ridge = v == mid and (not hip or end >= cross) and skirt is None
            fascia = v in (0, v1) or (hip and end == 0)
            tone = crest if at_ridge else edge if fascia else color
            # Along a hip the tile channels drain towards the gable end.
            drains_along = axis if hip and end < cross else ("z" if axis == "x" else "x")
            tiled_course(b, x, y, z, tone, axis if at_ridge else drains_along, h%2,
                         backed=not fascia or bool(upturn), ridge=at_ridge)
            if at_ridge:
                if u in (0, u1, 3, u1-3) and (style != "gable" or end == 0):
                    b.place(x, y+1, z, f"{MOD}:{crest}_roof_tile_slab", type="bottom", axis=axis)
            # Above the break in an irimoya roof, the triangular gable faces are set back.
            gable_end = 1 if style == "gable" else 3
            if style != "hip" and end == gable_end and skirt is None:
                sill = eave if style == "gable" else eave + 1
                for gy in range(sill, y):
                    timber = gy == sill or v == mid
                    b.place(x, gy, z, t.timber("stripped_log") if timber else "minecraft:white_terracotta",
                            **({"axis": "y" if v == mid else drains_along} if timber else {}))
            if v in (0, v1) and u%3 == 1:
                b.place(x, eave-1, z, t.timber("slab"), type="top")


def awning(b, t, x0, z0, x1, z1, eave, *, rise="south", color="gray", run=1):
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            distance = {"south": z-z0, "north": z1-z, "west": x1-x, "east": x-x0}[rise]
            distance //= run
            tiled_course(b, x, eave+distance//2, z, color, "z" if rise in ("north", "south") else "x",
                         distance%2, backed=distance > 0)


def hanging_cloth(b, x, y, z, base, patterns, facing="north"):
    layers = List[Compound]([Compound({"pattern": String("minecraft:"+motif), "color": String(dye)})
                             for motif, dye in patterns])
    b.place(x, y, z, "minecraft:"+base+"_wall_banner", facing=facing,
            data=Compound({"id": String("minecraft:banner"), "patterns": layers}))


def gallery(b, t, x, z0, z1, floor, color):
    deck(b, t, x-1, z0, x+1, z1, floor)
    for side in (x-1, x+1):
        for z in sorted({z0, (z0+z1)//2, z1}):
            b.fill((side, floor+1, z), (side, floor+3, z), t.timber("log"), axis="y")
        b.fill((side, floor+4, z0), (side, floor+4, z1), t.timber("stripped_log"), axis="z")
    for z in (z0+1, z1-1):
        b.fill((x-1, floor+4, z), (x+1, floor+4, z), t.timber("stripped_log"), axis="x")
        b.place(x, floor+3, z, f"{MOD}:silk_palace_lantern", hanging="true")
    roof(b, t, x-2, z0-1, x+2, z1+1, floor+5, axis="z", style="hip", color=color)


def stone_light(b, x, y, z):
    b.place(x, y, z, "minecraft:polished_andesite")
    b.place(x, y+1, z, f"{MOD}:yukimi_lantern", facing="north")


def pond(b, z0, shores, *, y=2):
    for i, (left, right) in enumerate(shores):
        z = z0+i
        for x in range(left, right + 1):
            b.place(x, y-1, z, "minecraft:" + ("clay" if (x+z)%5 else "gravel"))
            b.place(x, y, z, "minecraft:water", level="0")
        for bank in (left-1, right+1):
            b.place(bank, y, z, "minecraft:" + ("moss_block" if i%3 else "andesite"))


def boulder(b, x, y, z, *, east=True):
    step = 1 if east else -1
    b.place(x, y, z, "minecraft:mossy_cobblestone")
    b.place(x, y+1, z, "minecraft:moss_carpet")
    b.place(x+step, y, z, "minecraft:mossy_cobblestone_slab", type="bottom")
    b.place(x, y, z+1, "minecraft:andesite_stairs", facing="north")


def bed(b, t, x0, z0, x1, z1, *, y=2):
    bloom = "minecraft:pink_petals" if t.wood == "cherry" else t.timber("petals")
    for z in range(z0, z1+1):
        for x in range(x0, x1+1):
            b.place(x, y, z, "minecraft:grass_block")
            amount = (x*3 + z*2 + x*z)%5
            if amount:
                b.place(x, y+1, z, bloom, flower_amount=str(amount), facing=("east", "south", "west")[z%3])


def pine(b, x, y, z):
    """A pruned garden pine with three off-axis pads of needles and an exposed trunk."""
    b.fill((x, y, z), (x, y+5, z), "minecraft:spruce_log", axis="y")
    for dx, dz, dy in ((-2, 0, 2), (1, 1, 4), (0, 0, 6)):
        b.fill((x+min(dx,0), y+dy, z), (x+max(dx,0), y+dy, z), "minecraft:spruce_log", axis="x")
        for ox, oz in ((-1,0), (0,-1), (0,0), (1,0), (0,1), (1,-1)):
            b.place(x+dx+ox, y+dy+1, z+dz+oz, "minecraft:spruce_leaves", persistent="true")
        b.place(x+dx, y+dy+2, z+dz, "minecraft:spruce_leaves", persistent="true")
