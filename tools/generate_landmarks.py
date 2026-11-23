"""Build the three woodland landmarks and their exploration chest tables."""

from dataclasses import replace

from generate_resources import MOD, datum, tag
from generate_shrines import AIR, THEMES, Blueprint, gable, reward_chest, surface_structure


def yard(build, x0, z0, x1, z1, surface, clear_to):
    build.fill((x0, 0, z0), (x1, 0, z1), "minecraft:cobblestone")
    build.fill((x0, 1, z0), (x1, 1, z1), surface)
    build.fill((x0, 2, z0), (x1, clear_to, z1), AIR)


def timber_frame(build, theme, x0, z0, x1, z1):
    """Posts start on a deck at y=2. The clear space below the lintel is three blocks."""
    for x in (x0, x1):
        for z in (z0, z1):
            build.fill((x, 3, z), (x, 6, z), theme.timber("log"), axis="y")
        build.fill((x, 6, z0), (x, 6, z1), theme.timber("stripped_log"), axis="z")
    for z in (z0, z1):
        build.fill((x0, 6, z), (x1, 6, z), theme.timber("stripped_log"), axis="x")


def dyehouse(build):
    t = replace(THEMES[2], roof="blue")
    yard(build, 1, 2, 16, 13, "minecraft:packed_mud", 7)
    yard(build, 2, 5, 11, 13, "minecraft:bricks", 12)
    build.fill((2, 2, 5), (11, 2, 13), t.timber("planks"))
    build.fill((5, 2, 4), (7, 2, 4), "minecraft:brick_stairs", facing="south")
    timber_frame(build, t, 3, 6, 11, 12)
    # Paper panels enclose the loom. The long east side opens onto the dye yard.
    for z in range(7, 12):
        build.place(3, 3, z, t.timber("planks"))
        build.fill((3, 4, z), (3, 5, z), f"{MOD}:yae_shoji", facing="east")
    build.fill((4, 3, 12), (10, 5, 12), t.timber("planks"))
    build.fill((6, 4, 12), (8, 5, 12), f"{MOD}:brocade_lattice", facing="north")
    for x, curtain in ((5, "red_noren"), (6, "pink_noren"), (7, "red_noren")):
        build.place(x, 5, 6, f"{MOD}:{curtain}", facing="north")
    build.place(4, 3, 11, "minecraft:loom", facing="east")
    build.fill((8, 3, 6), (9, 3, 6), t.timber("stripped_log"), axis="x")
    build.place(8, 4, 6, "minecraft:pink_carpet")
    build.place(9, 4, 6, f"{MOD}:flower_mortar")
    build.fill((7, 3, 11), (9, 3, 11), t.timber("stripped_log"), axis="x")
    build.place(8, 4, 11, "minecraft:potted_cornflower")
    build.place(9, 4, 11, f"{MOD}:rouge_plum_vase", facing="north")
    build.place(10, 5, 6, f"{MOD}:silk_palace_lantern", hanging="true")
    gable(build, t, 1, 12, 4, 14, 7)
    for x in (13, 16):
        build.fill((x, 2, 9), (x, 5, 9), t.timber("log"), axis="y")
    build.fill((13, 5, 9), (16, 5, 9), t.timber("stripped_log"), axis="x")
    for x, color in ((14, "indigo"), (15, "magenta")):
        build.place(x, 4, 9, f"{MOD}:{color}_noren", facing="north")
    for z in (6, 7):
        build.place(14, 2, z, "minecraft:water_cauldron", level="3")
    build.fill((12, 1, 3), (15, 1, 3), "minecraft:grass_block")
    for x in range(12, 16):
        build.place(x, 2, 3, t.timber("petals"), flower_amount=str(x - 11), facing="north")
    reward_chest(build, (4, 3, 10), t.wood, "flower_dyehouse", "east")


def rain_pavilion(build):
    t = THEMES[1]
    yard(build, 1, 1, 13, 13, "minecraft:mossy_cobblestone", 7)
    build.fill((7, 2, 4), (13, 11, 10), AIR)
    # The lined rain garden and low footbridge are part of the template, even on a dry meadow.
    for z, left, right in ((4, 4, 6), (5, 3, 7), (6, 2, 7), (7, 2, 7), (8, 2, 7),
                           (9, 2, 7), (10, 3, 7), (11, 4, 6)):
        build.fill((left, 1, z), (right, 1, z), "minecraft:water", level="0")
    for x, z in ((3, 6), (4, 10), (6, 5)):
        build.place(x, 2, z, "minecraft:lily_pad")
    build.fill((7, 2, 4), (13, 2, 10), t.timber("planks"))
    build.fill((4, 2, 8), (8, 2, 9), t.timber("planks"))
    build.fill((3, 2, 8), (3, 2, 9), t.timber("stairs"), facing="east")
    build.fill((2, 1, 8), (3, 1, 9), "minecraft:smooth_stone")
    timber_frame(build, t, 7, 4, 13, 10)
    # Four slopes meet above a little square lantern cap.
    for ring in range(4):
        left, right = 6 + ring, 14 - ring
        front, back = 3 + ring, 11 - ring
        y = 7 + ring
        for z, facing in ((front, "south"), (back, "north")):
            build.fill((left + 1, y, z), (right - 1, y, z), f"{MOD}:light_blue_roof_tile_stairs", facing=facing)
            for x in (left, right):
                build.place(x, y, z, f"{MOD}:light_blue_roof_tile_slab", type="bottom", axis="z")
        for x, facing in ((left, "east"), (right, "west")):
            build.fill((x, y, front + 1), (x, y, back - 1), f"{MOD}:light_blue_roof_tile_stairs", facing=facing)
    build.place(10, 11, 7, f"{MOD}:white_roof_tile_slab", type="bottom", axis="z")
    build.fill((7, 6, 7), (13, 6, 7), t.timber("stripped_log"), axis="x")
    build.place(10, 5, 7, f"{MOD}:silk_palace_lantern", hanging="true")
    for z in (6, 8):
        build.place(12, 3, z, t.timber("stairs"), facing="east")
    build.place(12, 3, 7, t.timber("slab"), type="top")
    build.place(12, 4, 7, f"{MOD}:celadon_bamboo_vase", facing="west")
    for x in (9, 10, 11):
        build.place(x, 3, 5, f"{MOD}:bound_tatami", facing="east")
    build.place(3, 2, 11, f"{MOD}:yukimi_lantern", facing="east")
    reward_chest(build, (8, 3, 9), t.wood, "rain_pavilion", "north")


def hillside_joinery(build):
    t = THEMES[3]
    yard(build, 1, 2, 16, 13, "minecraft:gravel", 7)
    yard(build, 2, 4, 10, 12, "minecraft:tuff_bricks", 12)
    build.fill((2, 2, 4), (10, 2, 12), t.timber("planks"))
    build.fill((5, 2, 3), (7, 2, 3), "minecraft:tuff_brick_stairs", facing="south")
    timber_frame(build, t, 2, 5, 10, 12)
    for z in range(6, 12):
        build.fill((2, 3, z), (2, 5, z), t.timber("planks"))
        if z in (7, 8, 10):
            build.place(2, 4, z, f"{MOD}:yamazakura_lattice", facing="west")
    build.fill((3, 3, 12), (9, 5, 12), t.timber("planks"))
    build.fill((4, 4, 12), (8, 4, 12), f"{MOD}:yamazakura_shoji", facing="north")
    build.fill((4, 3, 11), (6, 3, 11), t.timber("stripped_log"), axis="x")
    build.place(5, 4, 11, f"{MOD}:hanami_lantern", lit="true")
    build.place(3, 3, 9, "minecraft:crafting_table")
    build.fill((8, 3, 5), (9, 3, 5), t.timber("stripped_log"), axis="x")
    build.place(8, 4, 5, "minecraft:stonecutter", facing="north")
    gable(build, t, 1, 11, 4, 13, 7, transverse=True)
    build.place(6, 9, 5, f"{MOD}:yamazakura_lattice", facing="north")
    # The lower lean-to shelters a stepped stack of timber with visible end grain.
    for z in (6, 11):
        build.fill((15, 2, z), (15, 5, z), t.timber("log"), axis="y")
        build.fill((10, 5, z), (15, 5, z), t.timber("stripped_log"), axis="x")
    build.fill((15, 5, 6), (15, 5, 11), t.timber("stripped_log"), axis="z")
    for x in range(11, 17):
        y = 6 + (16 - x) // 2
        if x % 2 == 0:
            build.fill((x, y, 5), (x, y, 12), f"{MOD}:gray_roof_tile_stairs", facing="west")
        else:
            build.fill((x, y, 5), (x, y, 12), f"{MOD}:gray_roof_tile_slab", type="top", axis="x")
    for y, first, last in ((2, 12, 14), (3, 12, 13), (4, 13, 13)):
        for x in range(first, last + 1):
            build.fill((x, y, 7), (x, y, 10), t.timber("log"), axis="z")
    build.place(9, 5, 5, f"{MOD}:silk_palace_lantern", hanging="true")
    reward_chest(build, (8, 3, 10), t.wood, "hillside_joinery", "north")


def item(name, weight, low=1, high=1):
    entry = {"type": "minecraft:item", "name": name, "weight": weight}
    if high != 1 or low != 1:
        entry["functions"] = [{"function": "minecraft:set_count", "count": uniform(low, high)}]
    return entry


def uniform(low, high):
    return {"type": "minecraft:uniform", "min": low, "max": high}


def chest_table(name, materials, chance, trinkets):
    supplies = [
        item("minecraft:bread", 5, 1, 3), item("minecraft:torch", 6, 2, 6),
        item("minecraft:string", 3, 2, 5), item("minecraft:iron_nugget", 4, 3, 8),
        item("minecraft:emerald", 1, 1, 2),
    ]
    datum(f"loot_table/chests/{name}", {"type": "minecraft:chest", "pools": [
        {"rolls": uniform(2, 4), "entries": supplies},
        {"rolls": uniform(1, 2), "entries": [item(*material) for material in materials]},
        {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": chance}],
         "entries": [item(f"{MOD}:{trinket}", weight) for trinket, weight in trinkets]},
    ]})


def generate():
    bell, knot, fish, ink = "shidare_wind_bell", "yae_knot_charm", "celadon_fish_pendant", "yamazakura_ink_bracelet"
    sites = (
        ("flower_dyehouse", dyehouse, (17, 13, 15), ["minecraft:flower_forest", "minecraft:cherry_grove", f"{MOD}:yae_grove"],
         [("minecraft:white_wool", 4, 1, 3), (f"{MOD}:yae_petals", 5, 2, 5), ("minecraft:blue_dye", 3, 1, 3)],
         [(knot, 3), (bell, 1)]),
        ("rain_pavilion", rain_pavilion, (15, 12, 14), ["minecraft:meadow", "minecraft:cherry_grove", f"{MOD}:shidare_meadow"],
         [("minecraft:lily_pad", 3, 1, 3), (f"{MOD}:shidare_sapling", 2, 1, 1), (f"{MOD}:bamboo_splints", 4, 2, 5)],
         [(fish, 2), (bell, 2)]),
        ("hillside_joinery", hillside_joinery, (17, 13, 14), ["minecraft:forest", "minecraft:birch_forest", f"{MOD}:yamazakura_woodland"],
         [(f"{MOD}:yamazakura_log", 4, 2, 4), ("minecraft:copper_ingot", 2, 1, 3), ("minecraft:iron_axe", 1, 1, 1)],
         [(ink, 3), (knot, 1)]),
    )
    structures = []
    for name, layout, size, biomes, materials, trinkets in sites:
        build = Blueprint()
        layout(build)
        template = f"landmark/{name}"
        build.save(template, size)
        surface_structure(name, template, biomes, terrain="beard_thin")
        chest_table(name, materials, 0.35, trinkets)
        structures.append(f"{MOD}:{name}")
    chest_table("shrine_offerings", [(f"{MOD}:{v}_petals", 1, 2, 4) for v in ("shidare", "yae", "yamazakura")],
                0.25, [(bell, 1), (knot, 1), (fish, 1), (ink, 1)])
    tag(MOD, "worldgen/structure", "woodland_landmarks", structures)
    datum("worldgen/structure_set/woodland_landmarks", {
        "structures": [{"structure": name, "weight": 1} for name in structures],
        "placement": {
            "type": "minecraft:random_spread", "salt": 71403123, "spacing": 22, "separation": 10, "frequency": 0.8,
            "exclusion_zone": {"other_set": f"{MOD}:shrines", "chunk_count": 2},
        },
    })


if __name__ == "__main__":
    generate()
