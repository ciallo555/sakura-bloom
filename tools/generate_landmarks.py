"""Build the three woodland landmarks and their exploration chest tables."""

from generate_resources import MOD, datum, tag
from generate_shrines import THEMES, Blueprint, reward_chest, surface_structure
from woodland_layouts import dyehouse, rain_pavilion, hillside_joinery


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
        ("flower_dyehouse", dyehouse, THEMES[2], (26, 18, 28), ["minecraft:flower_forest", "minecraft:cherry_grove", f"{MOD}:yae_grove"],
         [("minecraft:white_wool", 4, 1, 3), (f"{MOD}:yae_petals", 5, 2, 5), ("minecraft:blue_dye", 3, 1, 3)],
         [(knot, 3), (bell, 1)]),
        ("rain_pavilion", rain_pavilion, THEMES[1], (28, 17, 26), ["minecraft:meadow", "minecraft:cherry_grove", f"{MOD}:shidare_meadow"],
         [("minecraft:lily_pad", 3, 1, 3), (f"{MOD}:shidare_sapling", 2, 1, 1), (f"{MOD}:bamboo_splints", 4, 2, 5)],
         [(fish, 2), (bell, 2)]),
        ("hillside_joinery", hillside_joinery, THEMES[3], (29, 19, 26), ["minecraft:forest", "minecraft:birch_forest", f"{MOD}:yamazakura_woodland"],
         [(f"{MOD}:yamazakura_log", 4, 2, 4), ("minecraft:copper_ingot", 2, 1, 3), ("minecraft:iron_axe", 1, 1, 1)],
         [(ink, 3), (knot, 1)]),
    )
    structures = []
    for name, layout, theme, size, biomes, materials, trinkets in sites:
        build = Blueprint()
        chest = layout(build, theme)
        reward_chest(build, chest, theme.wood, name)
        template = f"landmark/{name}"
        build.save(template, size)
        center, width, paving = {"flower_dyehouse": (11, 4, "gravel"), "rain_pavilion": (11, 3, "andesite"),
                                 "hillside_joinery": (12, 4, "packed_mud")}[name]
        surface_structure(name, template, biomes, paving=paving, center=center, width=width)
        chest_table(name, materials, 0.35, trinkets)
        structures.append(f"{MOD}:{name}")
    chest_table("shrine_offerings", [(f"{MOD}:{v}_petals", 1, 2, 4) for v in ("shidare", "yae", "yamazakura")],
                0.25, [(bell, 1), (knot, 1), (fish, 1), (ink, 1)])
    tag(MOD, "worldgen/structure", "woodland_landmarks", structures)
    datum("worldgen/structure_set/woodland_landmarks", {
        "structures": [{"structure": name, "weight": 1} for name in structures],
        "placement": {
            "type": f"{MOD}:garden_spread", "salt": 71403123, "spacing": 22, "separation": 10, "frequency": 0.8,
            "exclusion_zone": {"other_set": f"{MOD}:shrines", "chunk_count": 2},
            "additional_exclusion_zone": {"other_set": "minecraft:ruined_portals", "chunk_count": 3},
        },
    })


if __name__ == "__main__":
    generate()
