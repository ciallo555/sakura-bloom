"""Small building elements shared by the seven original Sakura Bloom templates.

Coordinates denote blocks, with +Z leading in from the road. Roof pitches are
specified in half-blocks so a wide eave can turn up without a full-block step.
"""

from random import Random

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
    for x in sorted({*range(x0, x1 + 1, 4), x1}):
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


def close_eaves(b, t, x0, z0, x1, z1, tie):
    """Seat the roof on a continuous wall plate, including the short gable ends."""
    rim = [(x, z, "x") for z in (z0, z1) for x in range(x0, x1+1)]
    rim += [(x, z, "z") for x in (x0, x1) for z in range(z0+1, z1)]
    for x, z, axis in rim:
        b.place(x, tie, z, t.timber("stripped_log"), axis=axis)
        for y in range(tie+1, tie+13):
            block, props, _ = b.blocks.get((x, y, z), (AIR, (), None))
            if block.endswith("_roof_tile_slab"):
                if dict(props)["type"] == "top":
                    b.place(x, y, z, block, **(dict(props) | {"type": "double"}))
                break
            if block not in (AIR, "minecraft:spruce_slab"):
                break  # A full floor or an existing gable panel already meets the wall.
            b.place(x, y, z, t.timber("planks") if y == tie+1 else "minecraft:white_terracotta")
        else:
            raise ValueError(f"Wall at {(x,tie,z)} has no roof")


def indoor_stairs(b, t, x, floor, z, steps, facing):
    """Two-wide stair flight, including its solid carriage and the opening through the floor above."""
    dx, dz = {"west": (-1,0), "south": (0,1)}[facing]
    for i in range(steps):
        tread = floor+i+1
        for side in (0, 1):
            px, pz = x+i*dx+side*abs(dz), z+i*dz+side*abs(dx)
            b.fill((px, floor+1, pz), (px, tread-1, pz), t.timber("planks"))
            b.place(px, tread, pz, t.timber("stairs"), facing=facing)
            b.fill((px, tread+1, pz), (px, tread+2, pz), AIR)


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
        b.place(x+dx, y, z, "minecraft:stripped_spruce_log", axis="x")
        b.place(x+dx, y+1, z, "minecraft:spruce_trapdoor", half="bottom", facing="east")


def basin(b, x, z, *, length=1, rim="polished_andesite"):
    """Recess a stone-lined trough into the paving, with a low coping above the water."""
    for dx in range(-1, 2):
        for dz in range(-1, length+1):
            if dx == 0 and 0 <= dz < length:
                b.place(x, 1, z+dz, "minecraft:"+rim)
                b.place(x, 2, z+dz, "minecraft:water", level="0")
            else:
                b.place(x+dx, 2, z+dz, "minecraft:"+rim)
                b.place(x+dx, 3, z+dz, "minecraft:"+rim+"_slab", type="bottom")


def tiled_course(b, x, y, z, tone, axis, half, *, backed, ridge=False):
    # Back each raised course down to the preceding one. Thin diagonal slabs alone
    # meet at an edge and let daylight through the steeper upper roof.
    slab = f"{MOD}:{tone}_roof_tile_slab"
    b.place(x, y, z, slab, type="double" if ridge or (backed and half) else "top" if half else "bottom", axis=axis)
    if backed and (not half or ridge) and b.blocks.get((x,y-1,z), (AIR,))[0] == AIR:
        b.place(x, y-1, z, "minecraft:spruce_slab", type="top")


def seat_roof_beams(b):
    """Fit timber packing between an existing beam and the underside of a tile course."""
    tiles = [(pos, name, dict(props)) for pos, (name, props, _) in b.blocks.items() if name.endswith("_roof_tile_slab")]
    for (x,y,z), tile, properties in tiles:
        under = b.blocks.get((x,y-1,z), (AIR, (), None))
        if under[0] in (AIR, "minecraft:spruce_slab"):
            beam = b.blocks.get((x,y-2,z), (AIR, (), None))
            if beam[0].endswith(("_log", "_bamboo_block")):
                b.place(x, y-1, z, beam[0], **dict(beam[1]))
                under = beam
        if under[0].endswith(("_log", "_bamboo_block")) and properties["type"] == "top":
            b.place(x, y, z, tile, **(properties | {"type": "double"}))


def rail(b, t, x, z, axis, length, y):
    for i in range(length):
        px, pz = x + (i if axis == "x" else 0), z + (i if axis == "z" else 0)
        b.place(px, y, pz, t.timber("fence"))


def join_fences(b):
    """Jigsaw uses the stored sides. Resolve them after corner pieces, newels and benches are in place."""
    sides = (("north",0,-1,"south"), ("south",0,1,"north"), ("west",-1,0,"east"), ("east",1,0,"west"))
    for (x,y,z), (name, props, data) in b.blocks.items():
        if not name.endswith("_fence"):
            continue
        state = dict(props)
        for side, dx, dz, opposite in sides:
            neighbor, properties, _ = b.blocks.get((x+dx,y,z+dz), (AIR, (), None))
            connected = neighbor.endswith(("_fence", "_log", "_planks", "_terracotta", "_wool", "_wood", "_bamboo_block"))
            if neighbor.endswith("_stairs"):
                connected = dict(properties).get("facing") == opposite
            elif neighbor.endswith("_slab"):
                connected = dict(properties).get("type") == "double"
            state[side] = str(connected).lower()
        b.place(x, y, z, name, data=data, **state)


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
                            **({"axis": "y" if v == mid and gy > sill else drains_along} if timber else {}))
            if v in (0, v1) and u%3 == 1 and b.blocks.get((x,eave-1,z), (AIR,))[0] == AIR:
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


def pond(b, z0, shores, *, seed, y=2):
    # Interpolate a coarse sediment field so deposits continue across rows and wrap around the shore.
    wet = {(x, z0+i) for i, (left, right) in enumerate(shores) for x in range(left, right+1)}
    rng = Random(seed)
    grid = {(u,v): rng.random() for u in range(min(x for x,z in wet)//4, max(x for x,z in wet)//4+2)
            for v in range(z0//4, (z0+len(shores)-1)//4+2)}
    def sediment(x, z):
        u, dx = divmod(x, 4)
        v, dz = divmod(z, 4)
        near = grid[u,v]*(4-dx) + grid[u+1,v]*dx
        far = grid[u,v+1]*(4-dx) + grid[u+1,v+1]*dx
        return (near*(4-dz) + far*dz)/16
    for i, (left, right) in enumerate(shores):
        z = z0+i
        for x in range(left, right + 1):
            deposit = sediment(x, z)
            inner = all((x+dx,z+dz) in wet for dx,dz in ((-2,0),(2,0),(0,-2),(0,2)))
            bottom = y-2 if inner and deposit > 0.40 else y-1
            material = "andesite" if deposit < 0.24 else "gravel" if deposit < 0.53 else "clay"
            b.place(x, bottom, z, "minecraft:" + material)
            b.fill((x, bottom+1, z), (x, y, z), "minecraft:water", level="0")
        for bank, shore in ((left-1, left), (right+1, right)):
            b.place(bank, y, z, "minecraft:" + ("andesite" if sediment(shore,z) < 0.34 else "moss_block"))


def boulder(b, x, y, z, *, east=True):
    step = 1 if east else -1
    for px, pz in ((x,z), (x+step,z), (x,z+1)):
        bottom = y-1
        while b.blocks.get((px,bottom,pz), (AIR,))[0] == "minecraft:water":
            b.place(px, bottom, pz, "minecraft:andesite")
            bottom -= 1
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
        if dz:
            b.fill((x+dx, y+dy, z+1), (x+dx, y+dy, z+dz), "minecraft:spruce_log", axis="z")
        for ox, oz in ((-1,0), (0,-1), (0,0), (1,0), (0,1), (1,-1)):
            b.place(x+dx+ox, y+dy+1, z+dz+oz, "minecraft:spruce_leaves", persistent="true")
        b.place(x+dx, y+dy+2, z+dz, "minecraft:spruce_leaves", persistent="true")
