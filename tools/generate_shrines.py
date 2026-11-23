"""Build the four biome-themed shrine templates and their vanilla worldgen data."""

import gzip
from dataclasses import dataclass
from io import BytesIO
import json

from nbtlib import Compound, File, Int, List, String

from generate_resources import MOD, CORE_OUTPUT, VARIETIES, datum, tag


SIZE = (23, 20, 27)
AIR = "minecraft:air"


@dataclass(frozen=True)
class Theme:
    wood: str
    biome: str
    roof: str
    stone: str
    paving: str
    paving_stairs: str
    curtain: str
    window: str
    vase: str
    plaque: str

    def timber(self, form):
        namespace = "minecraft" if self.wood == "cherry" else MOD
        pattern = {"stripped_log": "stripped_{}_log", "potted_sapling": "potted_{}_sapling"}.get(form, "{}_" + form)
        return f"{namespace}:{pattern.format(self.wood)}"


THEMES = (
    Theme("cherry", "minecraft:cherry_grove", "pink",
          "stone_bricks", "smooth_stone", "stone_stairs", "pink_noren", "begonia_lattice", "blue_blossom_vase", "花染之庭"),
    Theme("shidare", f"{MOD}:{VARIETIES['shidare']['biome']}", "light_blue",
          "polished_diorite", "calcite", "diorite_stairs", "white_noren", "ice_crackle_lattice", "celadon_bamboo_vase", "垂樱水榭"),
    Theme("yae", f"{MOD}:{VARIETIES['yae']['biome']}", "red",
          "polished_andesite", "bricks", "brick_stairs", "red_noren", "brocade_lattice", "rouge_plum_vase", "八重灯殿"),
    Theme("yamazakura", f"{MOD}:{VARIETIES['yamazakura']['biome']}", "green",
          "tuff_bricks", "mossy_stone_bricks", "mossy_stone_brick_stairs", "green_noren", "diamond_lattice", "ink_magnolia_vase", "山樱遥拜"),
)


class Blueprint:
    def __init__(self):
        self.blocks = {}

    def place(self, x, y, z, block, *, data=None, **properties):
        self.blocks[x, y, z] = (block, tuple(sorted(properties.items())), data)

    def fill(self, start, end, block, **properties):
        x0, y0, z0 = start
        x1, y1, z1 = end
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                for x in range(x0, x1 + 1):
                    self.place(x, y, z, block, **properties)

    def save(self, name):
        palette = {}
        blocks = []
        for pos, (name_id, properties, data) in sorted(self.blocks.items(), key=lambda entry: entry[0][1]):
            assert all(0 <= coord < limit for coord, limit in zip(pos, SIZE)), pos
            state = palette.setdefault((name_id, properties), len(palette))
            entry = Compound({"pos": List[Int](pos), "state": Int(state)})
            if data is not None:
                entry["nbt"] = data
            blocks.append(entry)

        states = []
        for name_id, properties in palette:
            state = Compound({"Name": String(name_id)})
            if properties:
                state["Properties"] = Compound({key: String(value) for key, value in properties})
            states.append(state)
        template = File({
            "DataVersion": Int(3955),
            "size": List[Int](SIZE),
            "palette": List[Compound](states),
            "blocks": List[Compound](blocks),
            "entities": List[Compound](),
        })
        path = CORE_OUTPUT / f"data/{MOD}/structure/shrine/{name}.nbt"
        path.parent.mkdir(parents=True, exist_ok=True)
        buffer = BytesIO()
        template.write(buffer)
        path.write_bytes(gzip.compress(buffer.getvalue(), mtime=0))


def terrace(build, theme, x0, z0, x1, z1, height=2):
    """Clear only the occupied terrace and embed its masonry below the approach."""
    build.fill((x0, 0, z0), (x1, height, z1), f"minecraft:{theme.stone}")
    build.fill((x0, height+1, z0), (x1, 15, z1), AIR)


def gate(build, theme):
    stone = f"minecraft:{theme.stone}"
    paving = f"minecraft:{theme.paving}"
    terrace(build, theme, 6, 0, 16, 3)
    for z, y in ((0, 1), (1, 2)):
        build.fill((6, y+1, z), (16, 8, z), AIR)
        for x in range(6, 17):
            step = theme.paving_stairs if 10 <= x <= 12 else theme.stone.removesuffix("s") + "_stairs"
            build.place(x,y,z,"minecraft:"+step,facing="south")
    build.fill((10,2,2),(12,2,7),paving)
    build.fill((6,8,3),(16,11,3),AIR)
    for x in (8, 14):
        build.place(x,3,3,stone)
        build.fill((x,4,3),(x,8,3),theme.timber("log"),axis="y")
    build.fill((7,7,3),(15,7,3),theme.timber("stripped_log"),axis="x")
    build.fill((6,9,3),(16,9,3),theme.timber("log"),axis="x")
    build.fill((6,10,3),(16,10,3),f"{MOD}:{theme.roof}_roof_tile_slab",type="bottom",axis="z")
    for x, direction in ((5,"west"),(17,"east")):
        build.place(x,10,3,f"{MOD}:{theme.roof}_roof_tile_stairs",facing=direction)
    lines = List[String](json.dumps({"text": line},ensure_ascii=False) for line in ("樱庭",theme.plaque,"",""))
    text = Compound({"messages": lines, "color": String("black")})
    build.place(11,8,3,theme.timber("hanging_sign"),attached="true",rotation="8",
                data=Compound({"id": String("minecraft:hanging_sign"), "front_text":text,"back_text":text}))
    build.place(15,3,5,"minecraft:water_cauldron",level="3")


def gable(build, theme, x0, x1, z0, z1, eave, *, transverse=False):
    """Stair courses run toward a capped ridge. Bounds include the overhang."""
    beam = theme.timber("stripped_log")
    # The wall plate and projecting purlins carry the stepped roof back into the frame.
    for z in range(z0,z1+1):
        for x in range(x0,x1+1):
            support = build.blocks.get((x,eave-1,z))
            if support is None or support[0] == AIR:
                build.place(x,eave-1,z,theme.timber("slab"),type="top")
    stairs = f"{MOD}:{theme.roof}_roof_tile_stairs"
    slab = f"{MOD}:{theme.roof}_roof_tile_slab"
    low, high = (x0, x1) if transverse else (z0, z1)
    assert (high-low) % 2 == 0
    ridge = (high+low)//2
    def course(start, end, y, block, **state):
        if transverse:
            build.fill((start,y,z0),(end,y,z1),block,**state)
        else:
            build.fill((x0,y,start),(x1,y,end),block,**state)
    for n in range(ridge-low):
        course(low+n,low+n,eave+n,stairs,facing="east" if transverse else "south")
        course(high-n,high-n,eave+n,stairs,facing="west" if transverse else "north")
    top = eave + ridge-low
    course(ridge,ridge,top,slab,type="bottom",axis="x" if transverse else "z")
    # Tie the rafters together and close the recessed triangular gables.
    for y in range(eave,top):
        inset = y-eave+1
        for pos in (low+inset, high-inset):
            course(pos,pos,y,beam,axis="z" if transverse else "x")
        for edge in ((z0+1,z1-1) if transverse else (x0+1,x1-1)):
            if transverse:
                build.fill((low+inset,y,edge),(high-inset,y,edge),theme.timber("planks"))
            else:
                build.fill((edge,y,low+inset),(edge,y,high-inset),theme.timber("planks"))
    for x,z in (((ridge,z0),(ridge,z1)) if transverse else ((x0,ridge),(x1,ridge))):
        build.place(x,top,z,slab,type="double",axis="x" if transverse else "z")


def hall(build, theme, x0, x1, z0, z1, floor, *, open_front=False):
    """Deck, load-bearing frame, screens, walk-through entrance and offering alcove."""
    beam, planks = theme.timber("stripped_log"),theme.timber("planks")
    build.fill((x0-1,floor,z0-1),(x1+1,floor,z1+1),planks)
    for z in (z0,z1):
        build.fill((x0,floor+4,z),(x1,floor+4,z),beam,axis="x")
    for x in (x0,x1):
        build.fill((x,floor+4,z0),(x,floor+4,z1),beam,axis="z")
        for z in (z0,(z0+z1)//2,z1):
            build.fill((x,floor+1,z),(x,floor+4,z),theme.timber("log"),axis="y")
        for z in range(z0+1,z1):
            if z == (z0+z1)//2: continue
            build.place(x,floor+1,z,planks)
            screen = theme.window if z%2 else theme.wood+"_shoji"
            build.fill((x,floor+2,z),(x,floor+3,z),f"{MOD}:{screen}",facing="west")
    build.fill((x0+1,floor+1,z1),(x1-1,floor+3,z1),planks)
    build.fill((10,floor+2,z1),(12,floor+3,z1),f"{MOD}:{theme.window}",facing="north")
    if not open_front:
        for lo,hi in ((x0+1,9),(13,x1-1)):
            build.fill((lo,floor+1,z0),(hi,floor+3,z0),f"{MOD}:{theme.wood}_shoji",facing="north")
    for x in (10,11,12):
        build.place(x,floor+3,z0,f"{MOD}:{theme.curtain}",facing="north")
    build.fill((9,floor+1,z1-1),(13,floor+1,z1-1),beam,axis="x")
    build.place(11,floor+2,z1-1,f"{MOD}:{theme.vase}",facing="north")
    for x in (9,13):
        build.place(x,floor+2,z1-1,f"{MOD}:hanami_lantern",lit="true")
    build.fill((9,floor+1,z0+1),(13,floor+1,z1-2),f"{MOD}:bound_tatami",facing="east")


def blossom_bed(build, theme, x0, z0, x1, z1, y=2):
    petals = "minecraft:pink_petals" if theme.wood=="cherry" else theme.timber("petals")
    build.fill((x0,y,z0),(x1,y,z1),"minecraft:grass_block")
    for z in range(z0,z1+1):
        for x in range(x0,x1+1):
            amount = (3*x+z)%5
            if amount:
                build.place(x,y+1,z,petals,flower_amount=str(amount),facing=("north","east","west")[x%3])


def stone_light(build, theme, x,y,z):
    build.place(x,y,z,"minecraft:"+theme.stone)
    build.place(x,y+1,z,f"{MOD}:yukimi_lantern",facing="north")


def approach(build, theme, z0, z1, y=2):
    build.fill((10,y,z0),(12,y,z1),"minecraft:"+theme.paving)


def cherry_court(build, t):
    terrace(build,t,4,4,18,13)
    terrace(build,t,2,14,20,23)
    approach(build,t,4,13)
    for x in (5,14):
        blossom_bed(build,t,x,7,x+3,10)
        stone_light(build,t,x+1,3,12)
    # Flower-dyeing benches flank the approach and face the open veranda.
    for x in (6,16):
        build.fill((x-1,3,11),(x+1,3,11),t.timber("stripped_log"),axis="x")
        build.place(x,4,11,f"{MOD}:flower_mortar")
        build.place(x-1,4,11,"minecraft:potted_cherry_sapling")
    build.fill((9,3,14),(13,3,14),t.timber("stairs"),facing="south")
    hall(build,t,4,18,16,21,3,open_front=True)
    gable(build,t,2,20,14,22,8)


def water_pavilion(build,t):
    terrace(build,t,3,4,19,23)
    approach(build,t,4,7)
    # A lined, still pond surrounds the bridge and the feet of the pavilion.
    build.fill((4,2,8),(18,2,22),"minecraft:water",level="0")
    for x,z in ((5,9),(7,11),(15,10),(17,14),(5,20),(17,21)):
        build.place(x,3,z,"minecraft:lily_pad")
    for x in (6,16):
        stone_light(build,t,x,3,7)
    build.fill((10,3,7),(12,3,7),t.timber("stairs"),facing="south")
    build.fill((9,3,8),(13,3,14),t.timber("planks"))
    for x in (9,13):
        build.fill((x,4,8),(x,4,13),t.timber("fence"),north="true",south="true")
        for z in (8,13):
            build.fill((x,1,z),(x,4,z),t.timber("log"),axis="y")
            build.place(x,5,z,f"{MOD}:hanami_lantern",lit="true")
    for x in (7,15):
        for z in (15,18,21):
            build.fill((x,1,z),(x,3,z),t.timber("log"),axis="y")
    hall(build,t,7,15,15,21,3,open_front=True)
    gable(build,t,5,17,13,23,8,transverse=True)
    # White flowering boughs arch over the water, with hanging sprays at their tips.
    for x,z,step in ((3,11,1),(19,18,-1)):
        build.fill((x,3,z),(x,7,z),t.timber("log"),axis="y")
        for dx,dz in ((0,0),(step,0),(step,1),(2*step,0),(0,-1)):
            build.fill((x+dx,7,z+dz),(x+dx,8,z+dz),t.timber("leaves"),persistent="true")
        build.fill((x+2*step,5,z),(x+2*step,6,z),t.timber("leaves"),persistent="true")


def lantern_hall(build,t):
    terrace(build,t,2,4,20,23)
    approach(build,t,4,12)
    # Paired lantern galleries open into the festival court.
    for x in (5,17):
        build.fill((x-1,3,5),(x+1,3,12),t.timber("planks"))
        for z in (5,8,12):
            build.fill((x,4,z),(x,6,z),t.timber("log"),axis="y")
        build.fill((x,7,5),(x,7,12),t.timber("stripped_log"),axis="z")
        for z in (6,10):
            build.place(x,6,z,f"{MOD}:silk_palace_lantern",hanging="true")
        gable(build,t,x-1,x+1,5,12,8,transverse=True)
    build.fill((9,3,12),(13,3,12),t.timber("stairs"),facing="south")
    hall(build,t,5,17,14,21,3)
    # Broad lower skirts and a raised clerestory beneath a second, shorter roof.
    build.fill((4,8,13),(18,8,22),t.timber("planks"))
    for z,direction in ((12,"south"),(23,"north")):
        build.fill((3,8,z),(19,8,z),f"{MOD}:red_roof_tile_stairs",facing=direction)
    build.fill((3,9,13),(19,9,22),f"{MOD}:red_roof_tiles",facing="north")
    for x, direction in ((3,"east"),(19,"west")):
        build.fill((x,8,13),(x,8,22),f"{MOD}:red_roof_tile_stairs",facing=direction)
    build.fill((6,10,15),(16,11,20),t.timber("planks"))
    for z in (15,20):
        build.fill((7,10,z),(15,10,z),f"{MOD}:{t.window}",facing="north")
        build.fill((6,11,z),(16,11,z),t.timber("stripped_log"),axis="x")
    gable(build,t,4,18,13,21,12)


def mountain_sanctuary(build,t):
    terrace(build,t,5,4,17,10)
    terrace(build,t,4,11,18,14,4)
    terrace(build,t,5,15,17,24,6)
    approach(build,t,4,8)
    # Landings break the rise into two short flights up to a narrow gabled hall.
    for z,y in ((9,3),(10,4),(13,5),(14,6)):
        build.fill((9,2,z),(13,y-1,z),"minecraft:"+t.stone)
        build.fill((9,y,z),(13,y,z),"minecraft:tuff_brick_stairs",facing="south")
    approach(build,t,11,12,4)
    build.fill((9,6,15),(13,6,16),t.timber("planks"))
    for x,z,y in ((7,8,3),(15,8,3),(6,12,5),(16,12,5),(6,16,7),(16,16,7)):
        stone_light(build,t,x,y,z)
    for x,z,y in ((6,6,2),(14,11,4),(6,20,6)):
        blossom_bed(build,t,x,z,x+2,z+1,y)
        build.place(x,y+1,z,"minecraft:mossy_cobblestone")
        build.place(x,y+2,z,"minecraft:moss_carpet")
    hall(build,t,7,15,17,22,6)
    gable(build,t,5,17,15,24,11,transverse=True)


LAYOUTS = {"cherry": cherry_court, "shidare": water_pavilion, "yae": lantern_hall,
           "yamazakura": mountain_sanctuary}


def generate():
    shrines = []
    for theme in THEMES:
        build = Blueprint()
        LAYOUTS[theme.wood](build, theme)
        gate(build, theme)
        build.save(theme.wood)

        name = f"{theme.wood}_shrine"
        shrines.append(f"{MOD}:{name}")
        pool = f"{MOD}:shrine/{theme.wood}"
        datum(f"worldgen/template_pool/shrine/{theme.wood}", {
            "fallback": "minecraft:empty",
            "elements": [{"weight": 1, "element": {
                "element_type": "minecraft:single_pool_element",
                "location": pool,
                "processors": "minecraft:empty",
                "projection": "rigid",
            }}],
        })
        datum(f"worldgen/structure/{name}", {
            "type": "minecraft:jigsaw",
            "biomes": [theme.biome],
            "step": "surface_structures",
            "spawn_overrides": {},
            "terrain_adaptation": "beard_box",
            "start_pool": pool,
            "size": 1,
            "start_height": {"absolute": -2},
            "project_start_to_heightmap": "WORLD_SURFACE_WG",
            "max_distance_from_center": 32,
            "use_expansion_hack": False,
        })

    tag(MOD, "worldgen/structure", "shrines", shrines)
    datum("worldgen/structure_set/shrines", {
        "structures": [{"structure": name, "weight": 1} for name in shrines],
        "placement": {
            "type": "minecraft:random_spread",
            "salt": 167092968,
            "spacing": 18,
            "separation": 8,
            "frequency": 0.7,
        },
    })


if __name__ == "__main__":
    generate()
