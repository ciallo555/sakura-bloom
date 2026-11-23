"""Generate 1.21.1 data and model JSON using the local vanilla client as a template."""

import argparse
from contextlib import contextmanager
from contextvars import ContextVar
from copy import deepcopy
import json
from pathlib import Path
from zipfile import ZipFile
from courtyard_models import furnishing_models
from window_patterns import WINDOWS
from courtyard_variants import CURTAINS, LATTICES, MOTIFS, ROOF_FINISHES, ROOF_GLAZES, VASES, WOODS, noren_id

MOD = "sakura_bloom"
PROJECT = Path(__file__).resolve().parents[1]
CORE_OUTPUT = PROJECT / "src/generated/resources"
CUISINE_OUTPUT = PROJECT / "cuisine/src/generated/resources"
OUTPUT = ContextVar("resource_output", default=CORE_OUTPUT)


@contextmanager
def cuisine_resources():
    token = OUTPUT.set(CUISINE_OUTPUT)
    try:
        yield
    finally:
        OUTPUT.reset(token)

BASE_WOOD = (
    "cherry_log", "cherry_wood", "stripped_cherry_log", "stripped_cherry_wood",
    "cherry_planks", "cherry_stairs", "cherry_slab", "cherry_fence", "cherry_fence_gate",
    "cherry_door", "cherry_trapdoor", "cherry_pressure_plate", "cherry_button",
)
SIGN_BLOCKS = ("cherry_sign", "cherry_wall_sign", "cherry_hanging_sign", "cherry_wall_hanging_sign")
WALL_SIGNS = ("cherry_wall_sign", "cherry_wall_hanging_sign")
BOAT_ITEMS = ("cherry_boat", "cherry_chest_boat")
VANILLA_BLOCKS = (*BASE_WOOD, *SIGN_BLOCKS, "cherry_leaves", "cherry_sapling", "potted_cherry_sapling", "pink_petals")
VANILLA_ITEMS = tuple(block for block in VANILLA_BLOCKS if block not in (*WALL_SIGNS, "potted_cherry_sapling")) + BOAT_ITEMS
DIRECTIONS = ("north", "east", "south", "west")
CULINARY_CROPS = {
    "cabbage": ("卷心菜", "Cabbage", "卷心菜种子", "Cabbage Seeds"),
    "tomato": ("番茄", "Tomato", "番茄种子", "Tomato Seeds"),
    "rice": ("稻米", "Rice Grains", "稻种", "Rice Seeds"),
}

VARIETIES = {
    "shidare": {
        "names": ("枝垂樱", "Weeping Cherry"), "biome": "shidare_meadow",
        "biome_names": ("枝垂樱野", "Weeping Cherry Meadow"), "source": "meadow",
        "colors": (0x93B56F, 0x6F9D72, 0x64A8B1, 0xBFCFE9),
        "height": 9, "width": (2, 3), "radius": 3, "layer_height": 5,
        "hang": (0.82, 0.75), "branches": 2, "density": 5, "flower_tries": 40,
        "flower_count": 4, "dye": "white_dye",
    },
    "yae": {
        "names": ("八重樱", "Double Cherry"), "biome": "yae_grove",
        "biome_names": ("八重樱丘", "Double Cherry Grove"), "source": "cherry_grove",
        "colors": (0x9BB064, 0x79936A, 0x72B5C2, 0xE3CEE0),
        "height": 8, "width": (3, 5), "radius": 4, "layer_height": 5,
        "hang": (0.32, 0.4), "branches": 3, "density": 7, "flower_tries": 65,
        "flower_count": 6, "dye": "red_dye",
    },
    "yamazakura": {
        "names": ("山樱", "Mountain Cherry"), "biome": "yamazakura_woodland",
        "biome_names": ("山樱杂木林", "Mountain Cherry Woodland"), "source": "flower_forest",
        "colors": (0x7B9D66, 0x628463, 0x439DA0, 0xC2D5DC),
        "height": 6, "width": (2, 3), "radius": 3, "layer_height": 4,
        "hang": (0.15, 0.25), "branches": 2, "density": 6, "flower_tries": 44,
        "flower_count": 4, "dye": "magenta_dye",
    },
}
SUFFIXES = {
    "log": ("原木", "Log"), "wood": ("木", "Wood"), "planks": ("木板", "Planks"),
    "stairs": ("楼梯", "Stairs"), "slab": ("台阶", "Slab"), "fence": ("栅栏", "Fence"),
    "fence_gate": ("栅栏门", "Fence Gate"), "door": ("门", "Door"),
    "trapdoor": ("活板门", "Trapdoor"), "pressure_plate": ("压力板", "Pressure Plate"),
    "button": ("按钮", "Button"), "leaves": ("树叶", "Leaves"), "sapling": ("树苗", "Sapling"),
    "sign": ("告示牌", "Sign"), "wall_sign": ("墙上告示牌", "Wall Sign"),
    "hanging_sign": ("悬挂式告示牌", "Hanging Sign"), "wall_hanging_sign": ("墙上悬挂式告示牌", "Wall Hanging Sign"),
}


def write(path, data):
    file = OUTPUT.get() / f"{path}.json"
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def asset(path, data):
    write(f"assets/{MOD}/{path}", data)


def datum(path, data):
    write(f"data/{MOD}/{path}", data)


def vanilla(jar, base, name):
    return json.loads(jar.read(f"{base}/minecraft/{name}.json"))


def block_name(name, variety):
    if name.startswith("pink_petals"):
        return f"{variety}_petals{name[len('pink_petals'):]}"
    return name.replace("cherry", variety)


def adapt_assets(obj, variety):
    if isinstance(obj, str):
        namespace, _, path = obj.partition(":")
        folder, _, name = path.partition("/")
        if namespace == "minecraft" and folder in ("block", "item") and name.startswith(
            ("cherry_", "potted_cherry_", "stripped_cherry_", "pink_petals")
        ):
            return f"{MOD}:{folder}/{block_name(name, variety)}"
    if isinstance(obj, list):
        return [adapt_assets(value, variety) for value in obj]
    if isinstance(obj, dict):
        return {key: adapt_assets(value, variety) for key, value in obj.items()}
    return obj


def model_references(obj):
    if isinstance(obj, list):
        for value in obj:
            yield from model_references(value)
    elif isinstance(obj, dict):
        for key, value in obj.items():
            if key in ("model", "parent") and isinstance(value, str):
                if value.startswith(("minecraft:block/cherry_", "minecraft:block/stripped_cherry_",
                                     "minecraft:block/potted_cherry_", "minecraft:block/pink_petals")):
                    yield value.split(":", 1)[1]
            else:
                yield from model_references(value)


def adapt_wood_models(jar, variety):
    needed = set()
    for source in VANILLA_BLOCKS:
        blockstate = vanilla(jar, "assets", f"blockstates/{source}")
        needed.update(model_references(blockstate))
        if source == "cherry_leaves":
            leaf_model = blockstate["variants"][""]["model"]
            blockstate["variants"][""] = [{"model": leaf_model, "y": rotation} for rotation in (0, 90, 180, 270)]
        asset(f"blockstates/{block_name(source, variety)}", adapt_assets(blockstate, variety))

    for source in VANILLA_ITEMS:
        model = vanilla(jar, "assets", f"models/item/{source}")
        needed.update(model_references(model))
        asset(f"models/item/{block_name(source, variety)}", adapt_assets(model, variety))

    done = set()
    while needed:
        path = needed.pop()
        if path in done:
            continue
        done.add(path)
        name = path.removeprefix("block/")
        model = vanilla(jar, "assets", f"models/{path}")
        needed.update(model_references(model))
        if name == "cherry_leaves":
            model["render_type"] = "minecraft:cutout_mipped"
        elif any(token in name for token in ("sapling", "door", "petals")):
            model["render_type"] = "minecraft:cutout"
        target = adapt_assets(f"minecraft:{path}", variety).split(":", 1)[1]
        asset(f"models/{target}", adapt_assets(model, variety))


def item_result(id_, count=1):
    return {"id": id_, "count": count}


def ingredient(value):
    return {"tag": value[1:]} if value.startswith("#") else {"item": value}


def shapeless(name, result, count, *items):
    datum(f"recipe/{name}", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [ingredient(item) for item in items], "result": item_result(result, count),
    })


def shaped(name, result, count, pattern, keys):
    datum(f"recipe/{name}", {
        "type": "minecraft:crafting_shaped", "category": "building",
        "pattern": pattern, "key": {key: ingredient(item) for key, item in keys.items()},
        "result": item_result(result, count),
    })
    unlock_recipe(name, next(iter(keys.values())))


def unlock_recipe(name, item, condition=None):
    advancement = {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_ingredient": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}},
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": f"{MOD}:{name}"}},
        },
        "requirements": [["has_ingredient", "has_the_recipe"]],
        "rewards": {"recipes": [f"{MOD}:{name}"]},
    }
    if condition:
        advancement["neoforge:conditions"] = [condition]
    datum(f"advancement/recipes/decorations/{name}", advancement)


def loot_self(block):
    datum(f"loot_table/blocks/{block}", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "conditions": [{"condition": "minecraft:survives_explosion"}],
                   "entries": [{"type": "minecraft:item", "name": f"{MOD}:{block}"}]}],
    })


def remap_ids(value, ids):
    if isinstance(value, str):
        return ids.get(value, value)
    if isinstance(value, list):
        return [remap_ids(item, ids) for item in value]
    if isinstance(value, dict):
        return {key: remap_ids(item, ids) for key, item in value.items()}
    return value


def wood_recipes(jar, variety):
    ids = {}
    for source in (*VANILLA_BLOCKS, *BOAT_ITEMS):
        target = block_name(source, variety)
        ids.update({f"minecraft:{source}": f"{MOD}:{target}",
                    f"minecraft:blocks/{source}": f"{MOD}:blocks/{target}"})
    for source in VANILLA_BLOCKS:
        if source in WALL_SIGNS:
            continue
        target = block_name(source, variety)
        loot = remap_ids(vanilla(jar, "data", f"loot_table/blocks/{source}"), ids)
        if source == "cherry_leaves":
            loot["pools"].append({
                "rolls": 1, "conditions": [*loot["pools"][1]["conditions"],
                    {"condition": "minecraft:random_chance", "chance": 0.08},
                    {"condition": "minecraft:survives_explosion"}],
                "entries": [{"type": "minecraft:item", "name": f"{MOD}:{variety}_petals"}],
            })
        datum(f"loot_table/blocks/{target}", loot)

    ids[f"#minecraft:cherry_logs"] = f"#{MOD}:{variety}_logs"
    ids["minecraft:cherry_logs"] = f"{MOD}:{variety}_logs"
    for source in BASE_WOOD:
        if source.endswith("_log"):
            continue
        recipe = remap_ids(vanilla(jar, "data", f"recipe/{source}"), ids)
        target = block_name(source, variety)
        datum(f"recipe/{target}", recipe)
        if source == "cherry_planks":
            unlock_recipe(target, f"#{MOD}:{variety}_logs")
        elif source.endswith("_wood"):
            unlock_recipe(target, f"{MOD}:{'stripped_' if source.startswith('stripped_') else ''}{variety}_log")
        else:
            unlock_recipe(target, f"{MOD}:{variety}_planks")

    utility_recipes = {
        "cherry_sign": "cherry_planks", "cherry_hanging_sign": "stripped_cherry_log",
        "cherry_boat": "cherry_planks", "cherry_chest_boat": "cherry_boat",
    }
    for source, unlock_item in utility_recipes.items():
        target = block_name(source, variety)
        recipe = remap_ids(vanilla(jar, "data", f"recipe/{source}"), ids)
        if source == "cherry_chest_boat":
            recipe["ingredients"] = [ingredient("#c:chests/wooden") if entry == ingredient("minecraft:chest") else entry
                                     for entry in recipe["ingredients"]]
        datum(f"recipe/{target}", recipe)
        unlock_recipe(target, f"{MOD}:{block_name(unlock_item, variety)}")


def placed_tree(jar, variety, feature, count):
    placed = vanilla(jar, "data", "worldgen/placed_feature/trees_cherry")
    placed["feature"] = f"{MOD}:{variety}_tree_bees"
    placed["placement"][0] = count
    placed["placement"][-1]["predicate"]["state"]["Name"] = f"{MOD}:{variety}_sapling"
    datum(f"worldgen/placed_feature/{feature}", placed)


def worldgen(jar, variety, config):
    template = vanilla(jar, "data", "worldgen/configured_feature/cherry")
    tree = template["config"]
    tree["trunk_provider"]["state"]["Name"] = f"{MOD}:{variety}_log"
    tree["foliage_provider"]["state"]["Name"] = f"{MOD}:{variety}_leaves"
    trunk = tree["trunk_placer"]
    trunk["base_height"] = config["height"]
    trunk["height_rand_a"] = 2
    trunk["branch_count"]["distribution"] = [
        {"data": config["branches"] - 1, "weight": 1},
        {"data": config["branches"], "weight": 3},
    ]
    trunk["branch_horizontal_length"]["min_inclusive"], trunk["branch_horizontal_length"]["max_inclusive"] = config["width"]
    foliage = tree["foliage_placer"]
    foliage.update({"height": config["layer_height"], "radius": config["radius"]})
    foliage["hanging_leaves_chance"], foliage["hanging_leaves_extension_chance"] = config["hang"]
    datum(f"worldgen/configured_feature/{variety}_tree", template)
    tree["decorators"] = [{"type": "minecraft:beehive", "probability": 0.06}]
    datum(f"worldgen/configured_feature/{variety}_tree_bees", template)
    placed_tree(jar, variety, f"{variety}_trees", {"type": "minecraft:count", "count": config["density"]})
    placed_tree(jar, variety, f"{variety}_tree_in_cherry_grove", {"type": "minecraft:rarity_filter", "chance": 14})

    flower = vanilla(jar, "data", "worldgen/configured_feature/flower_cherry")
    patch = flower["config"]
    patch["tries"] = config["flower_tries"]
    entries = patch["feature"]["feature"]["config"]["to_place"]["entries"]
    for entry in entries:
        entry["data"]["Name"] = f"{MOD}:{variety}_petals"
        entry["weight"] = 5 - int(entry["data"]["Properties"]["flower_amount"])
    datum(f"worldgen/configured_feature/{variety}_petal_patch", flower)
    datum(f"worldgen/placed_feature/{variety}_petal_patch", {
        "feature": f"{MOD}:{variety}_petal_patch",
        "placement": [
            {"type": "minecraft:count", "count": config["flower_count"]},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
            {"type": "minecraft:biome"},
        ],
    })

    biome = vanilla(jar, "data", f"worldgen/biome/{config['source']}")
    grass, foliage_color, water, fog = config["colors"]
    biome["effects"].update({"grass_color": grass, "foliage_color": foliage_color,
                             "water_color": water, "fog_color": fog})
    vegetation = biome["features"][9]
    # Keep the relative order of shared vanilla features across all biomes.
    vegetation = [f for f in vegetation if f not in ("minecraft:flower_cherry", "minecraft:trees_cherry", "minecraft:trees_meadow")]
    biome["features"][9] = [*vegetation, f"{MOD}:{variety}_petal_patch", f"{MOD}:{variety}_trees"]
    datum(f"worldgen/biome/{config['biome']}", biome)


def names(variety, spec, zh, en):
    chinese, english = spec["names"]
    for source in VANILLA_BLOCKS:
        name = block_name(source, variety)
        if source == "pink_petals":
            cn, us = chinese + "花瓣", english + " Petals"
        elif source.startswith("potted_"):
            cn, us = "盆栽" + chinese, "Potted " + english + " Sapling"
        else:
            suffix = source.split("cherry_", 1)[1]
            cn_suffix, us_suffix = SUFFIXES[suffix]
            cn, us = chinese + cn_suffix, english + " " + us_suffix
            if source.startswith("stripped_"):
                cn, us = "去皮" + cn, "Stripped " + us
        zh[f"block.{MOD}.{name}"], en[f"block.{MOD}.{name}"] = cn, us
    zh[f"biome.{MOD}.{spec['biome']}"], en[f"biome.{MOD}.{spec['biome']}"] = spec["biome_names"]
    zh[f"item.{MOD}.{variety}_boat"] = chinese + "木船"
    en[f"item.{MOD}.{variety}_boat"] = english + " Boat"
    zh[f"item.{MOD}.{variety}_chest_boat"] = chinese + "木运输船"
    en[f"item.{MOD}.{variety}_chest_boat"] = english + " Boat with Chest"
    zh[f"structure.{MOD}.{variety}_shrine"] = chinese + "神社"
    en[f"structure.{MOD}.{variety}_shrine"] = english + " Shrine"


def tag(namespace, directory, name, values):
    write(f"data/{namespace}/tags/{directory}/{name}", {"replace": False, "values": values})


def tags_and_data_maps(jar):
    groups = {
        "planks": "planks", "stairs": "wooden_stairs", "slab": "wooden_slabs",
        "fence": "wooden_fences", "fence_gate": "fence_gates", "door": "wooden_doors",
        "trapdoor": "wooden_trapdoors", "pressure_plate": "wooden_pressure_plates",
        "button": "wooden_buttons", "leaves": "leaves", "sapling": "saplings",
    }
    petal_blocks = [f"{MOD}:{v}_petals" for v in VARIETIES]
    axes = []
    strippable, compostables = {}, {}
    for variety in VARIETIES:
        logs = [f"{MOD}:" + form.format(variety) for form in ("{}_log", "{}_wood", "stripped_{}_log", "stripped_{}_wood")]
        for kind in ("block", "item"):
            tag(MOD, kind, variety + "_logs", logs)
        axes.extend(f"{MOD}:{block_name(item, variety)}" for item in (*BASE_WOOD, *SIGN_BLOCKS))
        strippable[logs[0]], strippable[logs[1]] = logs[2], logs[3]
        compostables.update({f"{MOD}:{variety}_leaves": 0.3, f"{MOD}:{variety}_sapling": 0.3, f"{MOD}:{variety}_petals": 0.3})
    for kind in ("block", "item"):
        tag("c", kind, "chests/wooden", [f"{MOD}:{v}_chest" for v in VARIETIES])
        tag("minecraft", kind, "logs_that_burn", [f"#{MOD}:{v}_logs" for v in VARIETIES])
        tag("minecraft", kind, "stairs", [f"{MOD}:{color}_roof_tile_stairs" for color in ROOF_GLAZES])
        tag("minecraft", kind, "slabs", [f"{MOD}:{color}_roof_tile_slab" for color in ROOF_GLAZES])
        for suffix, group in groups.items():
            if f"data/minecraft/tags/{kind}/{group}.json" in jar.namelist():
                tag("minecraft", kind, group, [f"{MOD}:{v}_{suffix}" for v in VARIETIES])
        tag("minecraft", kind, "flowers", [*petal_blocks, *(f"{MOD}:{v}_leaves" for v in VARIETIES)])
    for suffix, group in {
        "sign": "standing_signs", "wall_sign": "wall_signs",
        "hanging_sign": "ceiling_hanging_signs", "wall_hanging_sign": "wall_hanging_signs",
    }.items():
        tag("minecraft", "block", group, [f"{MOD}:{v}_{suffix}" for v in VARIETIES])
    for suffix, group in {"sign": "signs", "hanging_sign": "hanging_signs", "boat": "boats", "chest_boat": "chest_boats"}.items():
        tag("minecraft", "item", group, [f"{MOD}:{v}_{suffix}" for v in VARIETIES])
    tag("minecraft", "block", "replaceable_by_trees", petal_blocks)
    tag("minecraft", "block", "guarded_by_piglins", [f"{MOD}:{v}_chest" for v in VARIETIES])
    tag("minecraft", "block", "flower_pots", [f"{MOD}:potted_{v}_sapling" for v in VARIETIES])
    tag("minecraft", "block", "mineable/axe", [*axes, *(f"{MOD}:{v}_chest" for v in VARIETIES), *(f"{MOD}:{name}" for name in
        ("hanami_lantern", "begonia_lattice", "silk_palace_lantern",
         *(f"{wood}_shoji" for wood in WOODS), *(f"{wood}_lattice" for wood in WOODS),
         *(f"{pattern}_lattice" for pattern in LATTICES)))])
    roofs = [color + suffix for color in ROOF_GLAZES for suffix in ("_roof_tiles", "_roof_tile_stairs", "_roof_tile_slab")]
    tag("minecraft", "block", "mineable/pickaxe", [f"{MOD}:{name}" for name in (
        "flower_mortar", "yukimi_lantern", "blue_blossom_vase", *roofs, *(f"{pattern}_vase" for pattern in VASES))])
    tag("minecraft", "worldgen/biome", "is_overworld", [f"{MOD}:{spec['biome']}" for spec in VARIETIES.values()])
    tag("minecraft", "worldgen/biome", "is_forest", [f"{MOD}:yae_grove", f"{MOD}:yamazakura_woodland"])
    tag("c", "worldgen/biome", "is_overworld", [f"{MOD}:{spec['biome']}" for spec in VARIETIES.values()])
    tag("c", "worldgen/biome", "is_forest", [f"{MOD}:yae_grove", f"{MOD}:yamazakura_woodland"])
    write("data/neoforge/data_maps/block/strippables", {"values": strippable})
    write("data/neoforge/data_maps/item/compostables", {"values": compostables})
    write("data/neoforge/data_maps/item/furnace_fuels", {"values": {f"{MOD}:hanami_lantern": 200}})


def box(start, end, side, top=None, bottom=None):
    x0, y0, z0 = start
    x1, y1, z1 = end
    side_textures = {
        "north": ([16 - x1, 16 - y1, 16 - x0, 16 - y0], side),
        "south": ([x0, 16 - y1, x1, 16 - y0], side),
        "east": ([16 - z1, 16 - y1, 16 - z0, 16 - y0], side),
        "west": ([z0, 16 - y1, z1, 16 - y0], side),
        "up": ([x0, z0, x1, z1], top or side),
        "down": ([x0, 16 - z1, x1, 16 - z0], bottom or side),
    }
    return {"from": start, "to": end,
            "faces": {direction: {"uv": uv, "texture": "#" + texture} for direction, (uv, texture) in side_textures.items()}}


def station_model(name, textures, elements):
    asset(f"models/block/{name}", {"parent": "minecraft:block/block",
                                 "textures": {key: f"{MOD}:block/{value}" for key, value in textures.items()},
                                 "elements": elements})


def item_model(name, model):
    asset(f"models/item/{name}", {"parent": f"{MOD}:block/{model}"})


def hollow_bowl(start, end, side, rim):
    x0, y0, z0 = start
    x1, y1, z1 = end
    return [
        box((x0, y0, z0), (x0 + 2, y1, z1), side, rim),
        box((x1 - 2, y0, z0), (x1, y1, z1), side, rim),
        box((x0 + 2, y0, z0), (x1 - 2, y1, z0 + 2), side, rim),
        box((x0 + 2, y0, z1 - 2), (x1 - 2, y1, z1), side, rim),
    ]


def crop_resources(jar, zh, en):
    wheat_loot = vanilla(jar, "data", "loot_table/blocks/wheat")
    for name, (chinese, english, chinese_seed, english_seed) in CULINARY_CROPS.items():
        item, seed, crop = (f"{MOD}:{name}{suffix}" for suffix in ("", "_seeds", "_crop"))
        zh[f"item.{MOD}.{name}"], en[f"item.{MOD}.{name}"] = chinese, english
        zh[f"item.{MOD}.{name}_seeds"], en[f"item.{MOD}.{name}_seeds"] = chinese_seed, english_seed
        zh[f"block.{MOD}.{name}_crop"] = "水稻" if name == "rice" else chinese
        en[f"block.{MOD}.{name}_crop"] = f"{english} Crop" if name != "rice" else "Rice Crop"
        for form in (name, name + "_seeds"):
            flat_item_model(form)
        # Wheat uses the same ID for the mature item and the crop block.
        crop_loot = remap_ids(wheat_loot, {
            "minecraft:blocks/wheat": f"{MOD}:blocks/{name}_crop", "minecraft:wheat_seeds": seed, "minecraft:wheat": crop,
        })
        crop_loot["pools"][0]["entries"][0]["children"][0]["name"] = item
        datum(f"loot_table/blocks/{name}_crop", crop_loot)
        if name == "rice":
            upper_loot = deepcopy(crop_loot)
            ripe, seed_pool = upper_loot["pools"]
            ripe["entries"] = [{"type": "minecraft:item", "name": item}]
            del seed_pool["conditions"]
            upper_loot["random_sequence"] = f"{MOD}:blocks/rice_panicle"
            datum("loot_table/blocks/rice_panicle", upper_loot)

        for stage in range(4):
            model = f"{name}_crop_{stage}"
            crop_model(model)
        asset(f"blockstates/{name}_crop", {"variants": {
            f"age={age}": {"model": f"{MOD}:block/{name}_crop_{stage}"}
            for age, stage in enumerate((0, 0, 1, 1, 2, 2, 2, 3))
        }})
        tag("c", "item", f"crops/{name}", [item])
        fd_seed = "rice" if name == "rice" else name + "_seeds"
        tag("c", "item", f"seeds/{name}", [seed, {"id": f"farmersdelight:{fd_seed}", "required": False}])
        if name != "rice":
            tag("c", "item", f"foods/{name}", [item])

    crops = [f"{MOD}:{name}_crop" for name in CULINARY_CROPS]
    tag("minecraft", "block", "crops", crops)
    tag("minecraft", "block", "maintains_farmland", crops)
    tag("minecraft", "item", "villager_plantable_seeds", [f"{MOD}:cabbage_seeds", f"{MOD}:tomato_seeds"])
    tag("c", "item", "crops", [f"#c:crops/{name}" for name in CULINARY_CROPS])
    tag("c", "item", "crops/grain", ["#c:crops/rice"])
    tag("c", "item", "seeds", [f"#c:seeds/{name}" for name in CULINARY_CROPS])
    tag("c", "item", "foods/vegetable", ["#c:foods/cabbage", "#c:foods/tomato"])
    tag("c", "item", "foods/leafy_green", [f"{MOD}:cabbage"])

    wild_crops(zh, en)
    village_farm_crops(jar)


def crop_model(model):
    asset(f"models/block/{model}", {"parent": "minecraft:block/crop", "render_type": "minecraft:cutout",
                                    "textures": {"crop": f"{MOD}:block/{model}"}})


def wild_crops(zh, en):
    crop_model("rice_panicle")
    crop_model("wild_cabbage")
    crop_model("wild_rice_lower")
    crop_model("wild_rice_upper")
    for simple_model in ("rice_panicle", "wild_cabbage"):
        asset(f"blockstates/{simple_model}", {"variants": {"": {"model": f"{MOD}:block/{simple_model}"}}})
    asset("blockstates/wild_rice", {"variants": {
        f"half={half}": {"model": f"{MOD}:block/wild_rice_{half}"}
        for half in ("lower", "upper")
    }})

    wild_names = {
        "wild_cabbage": ("野生卷心菜", "Wild Cabbage", "cabbage"),
        "wild_rice": ("野生水稻", "Wild Rice", "rice"),
    }
    for name, (chinese, english, crop) in wild_names.items():
        zh[f"block.{MOD}.{name}"], en[f"block.{MOD}.{name}"] = chinese, english
        datum(f"loot_table/blocks/{name}", {
            "type": "minecraft:block",
            "pools": [
                {"rolls": 1, "conditions": [{"condition": "minecraft:survives_explosion"}],
                 "entries": [{"type": "minecraft:item", "name": f"{MOD}:{crop}_seeds"}]},
                {"rolls": 1, "conditions": [{"condition": "minecraft:survives_explosion"},
                                           {"condition": "minecraft:random_chance", "chance": 0.45}],
                 "entries": [{"type": "minecraft:item", "name": f"{MOD}:{crop}"}]},
            ],
            "random_sequence": f"{MOD}:blocks/{name}",
        })
    zh[f"block.{MOD}.rice_panicle"], en[f"block.{MOD}.rice_panicle"] = "水稻穗", "Rice Panicle"

    define_wild_feature("wild_cabbage", "is_beach", "WORLD_SURFACE_WG", 3, 8, {}, [
        {"type": "minecraft:matching_blocks", "blocks": ["minecraft:air"]},
        {"type": "minecraft:matching_blocks", "blocks": "#minecraft:sand", "offset": [0, -1, 0]},
    ])
    define_wild_feature("wild_rice", "is_river", "OCEAN_FLOOR_WG", 2, 16, {"waterlogged": "true", "half": "lower"}, [
        {"type": "minecraft:matching_blocks", "blocks": ["minecraft:water"]},
        {"type": "minecraft:matching_fluids", "fluids": ["minecraft:water"]},
        {"type": "minecraft:matching_blocks", "blocks": ["minecraft:dirt"], "offset": [0, -1, 0]},
        {"type": "minecraft:matching_blocks", "blocks": ["minecraft:air"], "offset": [0, 1, 0]},
        {"type": "minecraft:any_of", "predicates": [
            {"type": "minecraft:has_sturdy_face", "direction": "up", "offset": offset}
            for offset in ([1, 0, 0], [-1, 0, 0], [0, 0, 1], [0, 0, -1])
        ]},
    ])


def define_wild_feature(name, biome, heightmap, rarity, tries, properties, predicates):
    block = {"Name": f"{MOD}:{name}"}
    if properties:
        block["Properties"] = properties
    datum(f"worldgen/configured_feature/{name}", {
        "type": "minecraft:simple_block",
        "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": block}},
    })
    datum(f"worldgen/placed_feature/{name}", {
        "feature": f"{MOD}:{name}",
        "placement": [
            {"type": "minecraft:rarity_filter", "chance": rarity},
            {"type": "minecraft:count", "count": tries},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": heightmap},
            {"type": "minecraft:block_predicate_filter",
             "predicate": {"type": "minecraft:all_of", "predicates": predicates}},
            {"type": "minecraft:biome"},
        ],
    })
    datum(f"neoforge/biome_modifier/{name}", {
        "type": "neoforge:add_features", "biomes": f"#minecraft:{biome}",
        "features": f"{MOD}:{name}", "step": "vegetal_decoration",
    })


def village_farm_crops(jar):
    # Taiga's small farm uses the mossification list. These rules only affect wheat.
    lists = ("farm_plains", "farm_desert", "farm_savanna", "farm_snowy", "farm_taiga", "mossify_10_percent")
    for name in lists:
        path = f"worldgen/processor_list/{name}"
        config = vanilla(jar, "data", path)
        rules = next(processor["rules"] for processor in config["processors"]
                     if processor["processor_type"] == "minecraft:rule")
        for crop, chance in (("tomato", 0.18), ("cabbage", 0.12)):
            rules.append({
                "input_predicate": {"predicate_type": "minecraft:random_block_match", "block": "minecraft:wheat",
                                    "probability": chance},
                "location_predicate": {"predicate_type": "minecraft:always_true"},
                "output_state": {"Name": f"{MOD}:{crop}_crop", "Properties": {"age": "7"}},
            })
        write(f"data/minecraft/{path}", config)


def flat_item_model(name):
    asset(f"models/item/{name}", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})


def cookware_models():
    stand = [
        box((1.25, 14.25, 3), (14.75, 16, 5), "iron", "rim"),
        box((1.25, 14.25, 11), (14.75, 16, 13), "iron", "rim"),
        box((3, 14.25, 5), (5, 16, 11), "iron", "rim"),
        box((11, 14.25, 5), (13, 16, 11), "iron", "rim"),
    ]
    for x in (1.25, 12.75):
        for z in (3, 11):
            stand.extend([
                box((x, 1, z), (x + 2, 12.75, z + 2), "iron"),
                box((x - 0.25, 12.75, z - 0.25), (x + 2.25, 14.25, z + 2.25), "rim"),
                box((x - 0.5, 0, z - 0.5), (x + 2.5, 1, z + 2.5), "foot", "iron"),
            ])
    station_model("campfire_cookware_stand", {
        "iron": "wok_iron", "rim": "wok_rim", "foot": "wok_inside", "particle": "wok_iron",
    }, stand)

    basket = [
        box((3, 0, 3), (13, 2, 13), "band"),
        box((2, 2, 2), (14, 4, 14), "wall", "mat"),
        *hollow_bowl((2, 4, 2), (14, 8, 14), "wall", "band"),
    ]
    lid = [
        box((2, 8, 2), (14, 9, 14), "band", "mat"),
        box((5, 9, 7), (11, 11, 9), "band"),
    ]
    wok = [
        box((4, 0, 4), (12, 2, 12), "iron"),
        box((3, 2, 3), (13, 3, 13), "iron", "inside"),
        *hollow_bowl((2, 3, 2), (14, 6, 14), "iron", "rim"),
        box((1, 5, 6), (3, 7, 10), "iron", "rim"),
        box((13, 5, 6), (15, 7, 10), "iron", "rim"),
    ]
    for station, base, cooking, textures in (
        ("bamboo_steamer", basket, lid, {"wall": "basket_wall", "band": "basket_band", "mat": "basket_mat", "particle": "basket_wall"}),
        ("iron_wok", wok, [], {
            "iron": "wok_iron", "inside": "wok_inside", "rim": "wok_rim", "particle": "wok_iron",
        }),
    ):
        station_model(station, textures, base)
        station_model(f"{station}_cooking", textures, [*base, *cooking])
        asset(f"blockstates/{station}", {"variants": {
            "cooking=false": {"model": f"{MOD}:block/{station}"},
            "cooking=true": {"model": f"{MOD}:block/{station}_cooking"},
        }})
        item_model(station, f"{station}_cooking" if station == "bamboo_steamer" else station)
        loot_self(station)


def kitchen(jar, zh, en):
    crop_resources(jar, zh, en)
    tag("minecraft", "block", "mineable/axe", [f"{MOD}:bamboo_steamer"])
    tag("minecraft", "block", "mineable/pickaxe", [f"{MOD}:tea_kettle", f"{MOD}:iron_wok"])
    compostables = {}
    for name in CULINARY_CROPS:
        compostables.update({f"{MOD}:{name}_seeds": 0.3, f"{MOD}:{name}": 0.65})
    write("data/neoforge/data_maps/item/compostables", {"values": compostables})
    cookware_models()
    steamer, wok = "bamboo_steamer", "iron_wok"
    shaped(steamer, f"{MOD}:{steamer}", 1, ["B B", "BSB"], {"B": "minecraft:bamboo", "S": "minecraft:string"})
    shaped(wok, f"{MOD}:{wok}", 1, [" I ", "I I", " I "], {"I": "minecraft:iron_ingot"})
    shapeless("wheat_noodles", f"{MOD}:wheat_noodles", 2, "minecraft:wheat", "minecraft:wheat")
    unlock_recipe("wheat_noodles", "minecraft:wheat")
    flat_item_model("wheat_noodles")
    tag("c", "item", "foods/pasta", [f"{MOD}:wheat_noodles"])
    tag("c", "item", "grain/rice", [f"{MOD}:rice"])
    tag("c", "item", "foods/cooked_rice", [
        {"id": f"{MOD}:steamed_rice", "required": False},
        {"id": "farmersdelight:cooked_rice", "required": False},
    ])
    tag("c", "item", "vegetables/tomato", [f"{MOD}:tomato"])
    tag("c", "item", "vegetables/cabbage", [f"{MOD}:cabbage"])
    tag(MOD, "block", "cooking_heat_sources", [
        "#minecraft:campfires", "minecraft:magma_block", {"id": "#farmersdelight:heat_sources", "required": False},
        {"id": "#kaleidoscope_cookery:heat_source_blocks_without_lit", "required": False},
    ])

    rice, cabbage, tomato, eggs = "#c:crops/rice", "#c:crops/cabbage", "#c:crops/tomato", "#c:eggs"
    raw_meat = "#c:foods/raw_meat"
    bowl = "minecraft:bowl"
    farmers_delight = {"type": "neoforge:mod_loaded", "modid": "farmersdelight"}
    kaleidoscope = {"type": "neoforge:mod_loaded", "modid": "kaleidoscope_cookery"}

    def kaleidoscope_steam(name, ticks, source, serving=None):
        datum(f"recipe/compat/kaleidoscope/steaming/{name}", {
            "neoforge:conditions": [kaleidoscope],
            "type": "kaleidoscope_cookery:steamer", "ingredient": ingredient(source),
            "result": item_result(f"{MOD}:{serving or name}"), "cook_tick": ticks,
        })
        if serving:
            shapeless(f"serving/{name}", f"{MOD}:{name}", 1, f"{MOD}:{serving}", bowl)
            unlock_recipe(f"serving/{name}", f"{MOD}:{serving}")

    prep_items = {
        "salmon_onigiri": ("raw_salmon_onigiri", "鲑鱼饭团胚", "Unsteamed Salmon Onigiri"),
        "chawanmushi": ("chawanmushi_mix", "茶碗蒸蛋液", "Chawanmushi Mix"),
        "steamed_pork_roll": ("raw_pork_roll", "生卷心菜肉卷", "Raw Cabbage and Meat Roll"),
    }
    for item, chinese, english in (*prep_items.values(),
                                  ("unplated_steamed_rice", "出笼米饭", "Unserved Steamed Rice"),
                                  ("unplated_chawanmushi", "出笼茶碗蒸", "Unserved Chawanmushi")):
        zh[f"item.{MOD}.{item}"], en[f"item.{MOD}.{item}"] = chinese, english
        flat_item_model(item)

    steamed_rice = {
        "type": f"{MOD}:cooking", "station": steamer, "cooking_time": 80,
        "ingredients": [ingredient(rice)], "serving": ingredient(bowl),
    }
    zh[f"item.{MOD}.steamed_rice"], en[f"item.{MOD}.steamed_rice"] = "蒸米饭", "Steamed Rice"
    flat_item_model("steamed_rice")
    datum("recipe/cooking/steamed_rice", {
        **steamed_rice, "neoforge:conditions": [{"type": "neoforge:not", "value": farmers_delight}],
        "result": item_result(f"{MOD}:steamed_rice"),
    })
    datum("recipe/cooking/cooked_rice", {
        **steamed_rice, "neoforge:conditions": [farmers_delight],
        "result": item_result("farmersdelight:cooked_rice"),
    })
    kaleidoscope_steam("steamed_rice", steamed_rice["cooking_time"], rice, "unplated_steamed_rice")

    dishes = (
        ("salmon_onigiri", "鲑鱼饭团", "Salmon Onigiri", steamer, 100, False, [rice, "minecraft:dried_kelp", "minecraft:cooked_salmon"]),
        ("chawanmushi", "茶碗蒸", "Chawanmushi", steamer, 120, True, [eggs, "minecraft:brown_mushroom"]),
        ("steamed_pork_roll", "卷心菜蒸肉卷", "Steamed Cabbage and Meat Roll", steamer, 120, False, [cabbage, raw_meat, rice]),
        ("yakisoba", "日式炒面", "Yakisoba", wok, 100, True, ["#c:foods/pasta", cabbage, raw_meat]),
        ("tomato_egg", "番茄炒蛋", "Tomato and Eggs", wok, 80, True, [tomato, eggs]),
        ("egg_fried_rice", "蛋炒饭", "Egg Fried Rice", wok, 100, True, [rice, eggs, "minecraft:carrot"]),
    )
    for name, chinese, english, station, ticks, uses_bowl, ingredients in dishes:
        zh[f"item.{MOD}.{name}"], en[f"item.{MOD}.{name}"] = chinese, english
        flat_item_model(name)
        inputs = [ingredient(item) for item in ingredients]
        result = item_result(f"{MOD}:{name}")
        recipe = {
            "type": f"{MOD}:cooking", "station": station, "cooking_time": ticks,
            "ingredients": inputs, "result": result,
        }
        if uses_bowl:
            recipe["serving"] = ingredient(bowl)
        if name == "tomato_egg":
            recipe["neoforge:conditions"] = [{"type": "neoforge:not", "value": kaleidoscope}]
        datum(f"recipe/cooking/{name}", recipe)
        if station == steamer:
            prepared = f"{MOD}:{prep_items[name][0]}"
            prep_recipe = f"compat/kaleidoscope/preparation/{name}"
            datum(f"recipe/{prep_recipe}", {
                "neoforge:conditions": [kaleidoscope],
                "type": "minecraft:crafting_shapeless", "ingredients": inputs,
                "result": item_result(prepared),
            })
            unlock_recipe(prep_recipe, next((item for item in ingredients if not item.startswith("#")), ingredients[0]), kaleidoscope)
            kaleidoscope_steam(name, ticks, prepared, f"unplated_{name}" if uses_bowl else None)
        compat = {
            "neoforge:conditions": [farmers_delight],
            "type": "farmersdelight:cooking", "cookingtime": ticks, "experience": 0.35,
            "recipe_book_tab": "meals" if uses_bowl else "misc",
            "ingredients": inputs, "result": result,
        }
        if uses_bowl:
            compat["container"] = item_result(bowl)
        datum(f"recipe/compat/farmersdelight/cooking/{name}", compat)
    tag("farmersdelight", "item", "meals", [f"{MOD}:{dish[0]}" for dish in dishes if dish[5]])
    datum("recipe/compat/farmersdelight/cutting/cabbage", {
        "neoforge:conditions": [farmers_delight],
        "type": "farmersdelight:cutting", "ingredients": [ingredient(f"{MOD}:cabbage")],
        "tool": [{"type": "farmersdelight:item_ability", "action": "knife_dig"}, ingredient("#c:tools/knife")],
        "result": [{"item": item_result("farmersdelight:cabbage_leaf", 2)}],
    })

    translations = {
        f"item.{MOD}.wheat_noodles": ("生面条", "Wheat Noodles"),
        f"block.{MOD}.{steamer}": ("蒸笼", "Steamer"),
        f"block.{MOD}.{wok}": ("炒锅", "Wok"),
        f"block.{MOD}.{steamer}.hint": ("放在热源上方。右键加入食材，空手右键开蒸。碗装菜做好后用碗盛出，手持其他容器可盛出对应的菜。",
                                       "Place above a heat source. Use ingredients to add them, then use an empty hand to steam. Serve finished dishes in their required container."),
        f"block.{MOD}.{wok}.hint": ("放在热源上方。右键加入食材，空手右键开炒。碗装菜做好后用碗盛出，手持其他容器可盛出对应的菜。",
                                   "Place above a heat source. Use ingredients to add them, then use an empty hand to start cooking. Serve finished dishes in their required container."),
        f"jei.{MOD}.{steamer}": ("蒸笼", "Steamer"),
        f"jei.{MOD}.{wok}": ("炒锅", "Wok"),
        f"jei.{MOD}.serving": ("做好后用此容器盛出", "Use this container to serve the finished dish"),
        f"jei.{MOD}.cabbage_seeds": ("可从沙滩上的野生卷心菜或原版村庄农田收获。农民村民也会播种。",
                                     "Harvest wild cabbage on beaches or cabbage in village farms. Farmers can sow these seeds."),
        f"jei.{MOD}.tomato_seeds": ("可从原版村庄农田里的番茄收获。农民村民也会播种。",
                                    "Harvest tomatoes in village farms. Farmers can sow these seeds."),
        f"jei.{MOD}.rice_seeds": ("可从河岸浅水里的野生水稻收获。种在一格水源下的耕地上，留出上方空间长稻穗。",
                                  "Harvest wild rice in shallow riverbanks. Sow in water over farmland and leave space for the panicle."),
    }
    for key, names in {
        "bowl": ("菜做好后再用碗盛出。", "Use the bowl to serve the dish once it is cooked."),
        "full": ("锅具已满。可潜行空手右键取回食材。", "The cookware is full. Sneak-use with an empty hand to return the ingredients."),
        "ingredient": ("这件物品用不上。", "This item is not used by this cookware."),
        "no_recipe": ("这些食材搭不成一道菜。潜行空手右键可取回。", "These ingredients do not match a recipe. Sneak-use with an empty hand to collect them."),
        "heat": ("锅具需要下方的热源，例如点燃的营火。", "Put a heat source under the cookware, such as a lit campfire."),
        "cooking": ("菜正在烹饪。", "The dish is cooking."),
        "ready": ("菜已做好，空手右键取出。", "The dish is ready. Use an empty hand to collect it."),
        "serve_with": ("菜已做好，用%s右键盛出。", "The dish is ready. Use %s to serve it."),
        "serving": ("菜已做好，请用配方要求的容器盛出。", "The dish is ready. Use the container shown in its recipe."),
        "returned": ("已取回食材。", "Ingredients returned."),
    }.items():
        translations[f"message.{MOD}.cooking.{key}"] = names
    for key, (chinese, english) in translations.items():
        zh[key], en[key] = chinese, english


def tea_kettle(zh, en):
    kettle = "tea_kettle"
    loot_self(kettle)
    kettle_base = [
        box((2, 0, 2), (14, 3, 14), "base"),
        box((4, 3, 4), (12, 5, 12), "metal"),
        box((3, 5, 3), (13, 8, 13), "metal"),
        *hollow_bowl((3, 8, 3), (13, 12, 13), "metal", "rim"),
        box((1, 8, 6), (3, 10, 10), "metal"),
        box((13, 8, 6), (15, 10, 10), "metal"),
    ]
    kettle_states = {}
    for stage in ("empty", "water", *[f"{step}_{v}" for v in VARIETIES for step in ("brewing", "ready")]):
        fill = "water" if stage == "water" else stage.split("_", 1)[-1]
        wet = stage != "empty"
        prefix = f"{kettle}_{stage}"
        textures = {"base": "kettle_base", "metal": "kettle_metal", "rim": "kettle_rim", "particle": "kettle_metal"}
        elements = kettle_base.copy()
        if wet:
            textures["liquid"] = f"kettle_{fill}"
            elements.append(box((5, 8, 5), (11, 10, 11), "liquid"))
        station_model(prefix, textures, elements)
        kettle_states[f"stage={stage}"] = {"model": f"{MOD}:block/{prefix}"}
    asset(f"blockstates/{kettle}", {"variants": kettle_states})
    item_model(kettle, f"{kettle}_empty")

    shaped(kettle, f"{MOD}:{kettle}", 1, ["C C", "C C", "BSB"],
           {"C": "minecraft:copper_ingot", "B": "minecraft:brick", "S": "minecraft:campfire"})
    for variety, spec in VARIETIES.items():
        item = variety + "_tea"
        asset(f"models/item/{item}", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        chinese, english = spec["names"]
        zh[f"item.{MOD}.{item}"] = chinese + "花茶"
        en[f"item.{MOD}.{item}"] = english + " Blossom Tea"
    translations = {
        "block.sakura_bloom.tea_kettle": ("煎茶釜", "Blossom Kettle"),
        "block.sakura_bloom.tea_kettle.hint": ("先放入水瓶，再加入任一变种的花瓣。煎好后空手右键取茶。",
                                              "Add a water bottle, then a blossom petal. Use an empty hand to collect the tea."),
        "block.sakura_bloom.tea_kettle.empty": ("手持水瓶右键，将水注入煎茶釜。",
                                               "Use a water bottle on the kettle to fill it."),
        "block.sakura_bloom.tea_kettle.water": ("加入枝垂樱、八重樱或山樱花瓣。空手潜行右键可取回水瓶。",
                                               "Add weeping, double, or mountain cherry petals. Sneak-use with empty hands to take the bottle back."),
        "block.sakura_bloom.tea_kettle.brewing": ("花茶正在冲泡。", "The tea is steeping."),
        "jei.sakura_bloom.tea_kettle": ("煎茶釜冲泡", "Blossom Kettle Brewing"),
        "jei.sakura_bloom.tea_kettle.instructions": ("先放水瓶，再放花瓣。4 秒后空手取茶。", "Add water, then petals. Collect with empty hands after 4 seconds."),
    }
    for key, (chinese, english) in translations.items():
        zh[key], en[key] = chinese, english


def workstations(zh, en):
    mortar = "flower_mortar"
    lantern = "hanami_lantern"
    for block in (mortar, lantern):
        loot_self(block)
    station_model(mortar, {"body": "mortar_glaze", "rim": "mortar_rim", "wood": "mortar_pestle", "particle": "mortar_glaze"}, [
        box((3, 0, 3), (13, 3, 13), "body"),
        box((2, 3, 2), (14, 5, 14), "body", "rim"),
        *hollow_bowl((2, 5, 2), (14, 9, 14), "body", "rim"),
        {**box((6, 6, 7), (9, 12, 10), "wood"),
         "rotation": {"origin": [7.5, 7, 8.5], "axis": "z", "angle": -22.5}},
    ])
    asset(f"blockstates/{mortar}", {"variants": {"": {"model": f"{MOD}:block/{mortar}"}}})
    item_model(mortar, mortar)

    lantern_boxes = [
        box((2, 0, 2), (14, 2, 14), "wood", "roof"),
        box((4, 2, 4), (12, 10, 12), "paper", "roof"),
        box((2, 10, 2), (14, 12, 14), "wood", "roof"),
        box((4, 12, 4), (12, 14, 12), "wood", "roof"),
        box((6, 14, 6), (10, 15, 10), "wood", "roof"),
    ]
    for state in ("off", "on"):
        station_model(f"{lantern}_{state}", {"wood": "lantern_lacquer", "roof": "lantern_roof",
                      "paper": f"lantern_paper_{state}", "particle": "lantern_lacquer"}, lantern_boxes)
    asset(f"blockstates/{lantern}", {"variants": {
        f"lit={lit},powered={powered}": {"model": f"{MOD}:block/{lantern}_{'on' if 'true' in (lit, powered) else 'off'}"}
        for lit in ("false", "true") for powered in ("false", "true")
    }})
    item_model(lantern, f"{lantern}_on")

    shaped(mortar, f"{MOD}:{mortar}", 1, [" B ", "T T", " S "],
           {"T": "minecraft:terracotta", "B": "minecraft:stick", "S": "minecraft:smooth_stone_slab"})
    shaped(lantern, f"{MOD}:{lantern}", 2, [" W ", "PTP", " W "],
           {"W": "#minecraft:planks", "P": "minecraft:paper", "T": "minecraft:torch"})
    for variety, spec in VARIETIES.items():
        shapeless(f"{variety}_petals_to_dye", f"minecraft:{spec['dye']}", 1, f"{MOD}:{variety}_petals")
        unlock_recipe(f"{variety}_petals_to_dye", f"{MOD}:{variety}_petals")
    dye_sources = {f"{MOD}:{variety}_petals": spec["dye"] for variety, spec in VARIETIES.items()}
    dye_sources.update({
        "minecraft:pink_petals": "pink_dye", "minecraft:poppy": "red_dye",
        "minecraft:oxeye_daisy": "light_gray_dye", "minecraft:cornflower": "blue_dye",
        "minecraft:dandelion": "yellow_dye", "minecraft:allium": "magenta_dye",
    })
    for source, color in dye_sources.items():
        datum(f"recipe/grinding/{source.split(':')[1]}", {
            "type": f"{MOD}:grinding", "ingredient": ingredient(source), "result": item_result(f"minecraft:{color}", 2),
        })

    translations = {
        "block.sakura_bloom.flower_mortar": ("花染臼", "Botanical Mortar"),
        "block.sakura_bloom.hanami_lantern": ("花见灯", "Hanami Lantern"),
        "block.sakura_bloom.flower_mortar.hint": ("手持花瓣或部分原版鲜花右键，一份材料可研磨为两份染料。",
                                                "Use petals or supported flowers on the mortar to make two dyes."),
        "block.sakura_bloom.hanami_lantern.hint": ("空手右键开关，也可用红石点亮。",
                                                  "Toggle with empty hands, or light it with redstone."),
        "jei.sakura_bloom.grinding": ("花染臼研磨", "Botanical Grinding"),
        "itemGroup.sakura_bloom": ("落樱缤纷", "Sakura Bloom"),
    }
    for key, (chinese, english) in translations.items():
        zh[key], en[key] = chinese, english


def courtyard(jar, zh, en):
    splints = f"{MOD}:bamboo_splints"
    cherry = f"#{MOD}:cherry_planks"
    tag(MOD, "item", "cherry_planks", ["minecraft:cherry_planks", *(f"{MOD}:{v}_planks" for v in VARIETIES)])
    shaped("bamboo_splints", splints, 4, ["BB"], {"B": "minecraft:bamboo"})
    flat_item_model("bamboo_splints")
    zh[f"item.{MOD}.bamboo_splints"], en[f"item.{MOD}.bamboo_splints"] = "竹篾", "Bamboo Splints"
    designs = {
        "bound_tatami": ("包边榻榻米", "Bound Tatami", 4, ["SSS", "WDW"],
                         {"S": splints, "W": "minecraft:wheat", "D": "minecraft:green_dye"}),
        "yukimi_lantern": ("雪见石灯", "Yukimi Stone Lantern", 1, ["PPP", " T ", "A A"],
                           {"P": "minecraft:polished_andesite", "T": "minecraft:torch", "A": "minecraft:andesite"}),
        "begonia_lattice": ("海棠花格窗", "Begonia Lattice Window", 4, ["SWS", "WDW", "SWS"],
                            {"W": cherry, "S": splints, "D": "minecraft:red_dye"}),
        "silk_palace_lantern": ("朱纱宫灯", "Vermilion Palace Lantern", 1, ["BGB", "RTR", "BGB"],
                                {"R": "minecraft:red_wool", "B": splints, "G": "minecraft:gold_nugget", "T": "minecraft:torch"}),
        "blue_blossom_vase": ("青花折枝瓶", "Blue Blossom Porcelain Vase", 1, [" L ", "BQB", " B "],
                              {"L": "minecraft:lapis_lazuli", "B": "minecraft:brick", "Q": "minecraft:quartz"}),
    }
    for wood, (chinese, english, _, _) in WOODS.items():
        planks = f"{MOD if wood in VARIETIES else 'minecraft'}:{wood}_planks"
        designs[f"{wood}_shoji"] = (chinese + "障子", english + " Shoji Screen", 4, ["BPB", "BPB", "WWW"],
                                    {"W": planks, "P": "minecraft:paper", "B": splints})
        motif, title, _ = WINDOWS[wood]
        designs[f"{wood}_lattice"] = (f"{motif}花格窗", f"{title} Lattice Window",
                                    4, ["WBW", "B B", "WBW"], {"W": planks, "B": splints})
    tag(MOD, "item", "noren", [f"{MOD}:{noren_id(color)}" for color in CURTAINS])
    for color, (chinese, _, _, motif) in CURTAINS.items():
        name = noren_id(color)
        title, english = MOTIFS[motif]
        wool, dye = f"minecraft:{color}_wool", f"minecraft:{color}_dye"
        designs[name] = (chinese + title + "门帘", color.replace("_", " ").title() + " " + english + " Noren",
                         2, ["BSB", "I I", "I I"], {"I": wool, "B": splints, "S": "minecraft:string"})
        recolor = f"dyeing/{name}"
        datum(f"recipe/{recolor}", {"type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [{"type": "neoforge:difference", "base": {"tag": f"{MOD}:noren"},
                             "subtracted": ingredient(f"{MOD}:{name}")}, ingredient(dye)],
            "result": item_result(f"{MOD}:{name}")})
        unlock_recipe(recolor, f"#{MOD}:noren")
    for name, (chinese, english, wood, pattern) in LATTICES.items():
        designs[f"{name}_lattice"] = (chinese, english, 4, pattern,
            {"W": f"minecraft:{wood}_planks", "B": splints, "S": "minecraft:stick"})
    for name, (chinese, english, pigment, *_) in VASES.items():
        designs[f"{name}_vase"] = (chinese, english, 1, [" L ", "BQB", " B "],
            {"L": f"minecraft:{pigment}", "B": "minecraft:brick", "Q": "minecraft:quartz"})
    for name, (chinese, english, count, pattern, keys) in designs.items():
        zh[f"block.{MOD}.{name}"], en[f"block.{MOD}.{name}"] = chinese, english
        shaped(name, f"{MOD}:{name}", count, pattern, keys)
        item_model(name, name)
        loot_self(name)
    for name, model in furnishing_models(box).items():
        asset(f"models/block/{name}", model)
    def reference(name, angle=0):
        model = {"model": f"{MOD}:block/{name}"}
        if angle:
            model["y"] = angle
        return model
    for name in designs.keys() - {"silk_palace_lantern"}:
        asset(f"blockstates/{name}", {"variants": {f"facing={d}": reference(name, i * 90)
                                                   for i, d in enumerate(DIRECTIONS)}})
    asset("blockstates/silk_palace_lantern", {"variants": {
        "hanging=false": reference("silk_palace_lantern"), "hanging=true": reference("silk_palace_lantern_hanging")}})
    for color in ROOF_GLAZES:
        roof_tiles(jar, color, zh, en)


def roof_tiles(jar, color, zh, en):
    prefix = color + "_roof_tile"
    tiles, stair, slab = prefix + "s", prefix + "_stairs", prefix + "_slab"
    china = "青灰" if color == "gray" else CURTAINS[color][0]
    english = color.replace("_", " ").title()
    block_id = f"{MOD}:{tiles}"
    dye = f"minecraft:{color}_dye"
    shaped(tiles, block_id, 2, [" B ", "BDB", " B "], {"B": "minecraft:brick", "D": dye})
    shaped(stair, f"{MOD}:{stair}", 4, ["T  ", "TT ", "TTT"], {"T": block_id})
    shaped(slab, f"{MOD}:{slab}", 6, ["TTT"], {"T": block_id})
    for name, cn, us in ((tiles, "筒瓦", "Barrel Roof Tiles"), (stair, "筒瓦楼梯", "Roof Tile Stairs"),
                         (slab, "筒瓦台阶", "Roof Tile Slab")):
        zh[f"block.{MOD}.{name}"], en[f"block.{MOD}.{name}"] = china + cn, english + " " + us
        item_model(name, name)
    loot_self(tiles)
    loot_self(stair)
    datum(f"loot_table/blocks/{slab}", remap_ids(vanilla(jar, "data", "loot_table/blocks/brick_slab"),
          {"minecraft:brick_slab": f"{MOD}:{slab}", "minecraft:blocks/brick_slab": f"{MOD}:blocks/{slab}"}))
    for name, count in ((stair, 1), (slab, 2)):
        recipe = name + "_from_stonecutting"
        datum(f"recipe/{recipe}", {"type": "minecraft:stonecutting", "ingredient": ingredient(block_id),
                                   "result": item_result(f"{MOD}:{name}", count)})
        unlock_recipe(recipe, block_id)

    def firings(name, **rotation):
        return [{"model": f"{MOD}:block/{name}{finish}", "weight": weight, **rotation}
                for finish, weight in ROOF_FINISHES]

    asset(f"blockstates/{tiles}", {"variants": {f"facing={direction}": firings(tiles, y=i*90)
                                                for i, direction in enumerate(DIRECTIONS)}})
    asset(f"blockstates/{slab}", {"variants": {
        f"axis={axis},type={kind}": firings(model, y=angle)
        for axis, angle in (("x", 90), ("z", 0))
        for kind, model in (("top", slab + "_top"), ("bottom", slab), ("double", tiles))}})
    stairs = vanilla(jar, "assets", "blockstates/stone_brick_stairs")
    for state, variant in stairs["variants"].items():
        variant_name = variant["model"].split("/")[-1].replace("stone_brick_stairs", stair)
        stairs["variants"][state] = firings(variant_name, **{k: variant[k] for k in ("x", "y") if k in variant})
    asset(f"blockstates/{stair}", stairs)

    for name, template in ((tiles, "cube"), (slab, "slab"), (slab + "_top", "slab_top"), (stair, "stairs"),
                           (stair + "_inner", "inner_stairs"), (stair + "_outer", "outer_stairs")):
        is_stair = "stairs" in template
        top = "roof_channels_cross" if is_stair else "roof_channels"
        surfaces = {"particle": "roof_clay", "side": "roof_clay", "bottom": "roof_clay", "top": top, "end": "roof_eave"}
        texture_prefix = "" if color == "gray" else color + "_"
        # All glazes share the same UVs; only the gray parent defines geometry.
        parent = "gray" + name[len(color):]
        for finish, _ in ROOF_FINISHES:
            textures = {k: f"{MOD}:block/{texture_prefix}{face}{finish}" for k, face in surfaces.items()}
            model = {"parent": f"{MOD}:block/{parent}", "textures": textures}
            if color == "gray" and not finish:
                model = vanilla(jar, "assets", f"models/block/{template}")
                model.update(parent="minecraft:block/block", textures=textures)
                end_faces = ("east", "west") if is_stair else ("north", "south")
                for part in model["elements"]:
                    for side, face in part["faces"].items():
                        face["texture"] = {"up": "#top", "down": "#bottom"}.get(side, "#end" if side in end_faces else "#side")
            asset(f"models/block/{name}{finish}", model)


def cherry_chest(jar, variety, names, zh, en):
    name = variety + "_chest"
    chest = f"{MOD}:{name}"
    planks = f"{MOD}:{variety}_planks"
    particle = {"particle": f"{MOD}:block/{variety}_planks"}
    asset(f"blockstates/{name}", {"variants": {"": {"model": f"{MOD}:block/{name}"}}})
    asset(f"models/block/{name}", {"textures": particle})
    asset(f"models/item/{name}", {"parent": "minecraft:item/chest", "textures": particle})
    shaped(name, chest, 1, ["WWW", "WIW", "WWW"], {"W": planks, "I": "minecraft:iron_nugget"})
    datum(f"loot_table/blocks/{name}", remap_ids(vanilla(jar, "data", "loot_table/blocks/chest"),
          {"minecraft:chest": chest, "minecraft:blocks/chest": f"{MOD}:blocks/{name}"}))
    zh[f"block.{MOD}.{name}"], en[f"block.{MOD}.{name}"] = names[0] + "木箱", names[1] + " Chest"


def travel_charms(zh, en):
    charms = {
        "shidare_wind_bell": ("belt", "枝垂风铃坠", "Weeping Cherry Wind Bell",
            [("+15%% 移动速度", "+15%% movement speed"), ("免疫摔落伤害", "Immune to fall damage")]),
        "yae_knot_charm": ("charm", "八重花结御守", "Double Blossom Omamori",
            [("+4 最大生命值", "+4 max health"), ("进食后获得 5 秒生命回复", "Gain Regeneration I for 5 seconds after eating")]),
        "celadon_fish_pendant": ("necklace", "青瓷游鱼佩", "Celadon Carp Pendant",
            [("在水中时", "While in water"), ("+30%% 移动速度", "+30%% movement speed"),
             ("+30%% 挖掘速度", "+30%% mining speed"), ("+1 水下呼吸", "Water Breathing I")]),
        "yamazakura_ink_bracelet": ("bracelet", "山樱墨绳腕结", "Mountain Cherry Inkcord",
            [("+2 急迫", "Haste II")]),
    }
    for name, (slot, cn_name, en_name, effects) in charms.items():
        flat_item_model(name)
        key = f"item.{MOD}.{name}"
        zh[key], en[key] = cn_name, en_name
        for line, (cn_line, en_line) in enumerate(effects, 1):
            zh[f"{key}.effect.{line}"], en[f"{key}.effect.{line}"] = cn_line, en_line
        tag("curios", "item", slot, [f"{MOD}:{name}"])
        datum(f"curios/slots/{slot}", {"size": 1})
    datum("curios/entities/travel_charms", {"entities": ["minecraft:player"], "slots": [v[0] for v in charms.values()]})
    tag(MOD, "block", "flowering_cherry_logs", ["#minecraft:cherry_logs", *(f"#{MOD}:{v}_logs" for v in VARIETIES)])
    for name, cn_name, en_name in (
            ("flower_dyehouse", "花染作坊", "Blossom Dyehouse"),
            ("rain_pavilion", "听雨歇亭", "Rain Garden Pavilion"),
            ("hillside_joinery", "山樱木作寮", "Hillside Joinery")):
        zh[f"structure.{MOD}.{name}"], en[f"structure.{MOD}.{name}"] = cn_name, en_name


def generate(client_jar):
    zh = {f"structure.{MOD}.cherry_shrine": "樱花神社"}
    en = {f"structure.{MOD}.cherry_shrine": "Cherry Shrine"}
    with ZipFile(client_jar) as jar:
        for variety, spec in VARIETIES.items():
            adapt_wood_models(jar, variety)
            wood_recipes(jar, variety)
            worldgen(jar, variety, spec)
            names(variety, spec, zh, en)
            cherry_chest(jar, variety, spec["names"], zh, en)
        tags_and_data_maps(jar)
        with cuisine_resources():
            kitchen_zh = {"itemGroup.sakura_bloom_cuisine": "落樱缤纷 · 食事"}
            kitchen_en = {"itemGroup.sakura_bloom_cuisine": "Sakura Bloom · Cuisine"}
            kitchen(jar, kitchen_zh, kitchen_en)
            tea_kettle(kitchen_zh, kitchen_en)
            for locale, strings in (("zh_cn", kitchen_zh), ("en_us", kitchen_en)):
                write(f"assets/sakura_bloom_cuisine/lang/{locale}", strings)
        courtyard(jar, zh, en)
    workstations(zh, en)
    travel_charms(zh, en)
    for variety in VARIETIES:
        asset(f"particles/{variety}_falling_petals",
              {"textures": [f"{MOD}:{variety}_petal_{index}" for index in range(4)]})
    datum("neoforge/biome_modifier/cherry_grove_variants", {
        "type": "neoforge:add_features", "biomes": "minecraft:cherry_grove",
        "features": [f"{MOD}:{v}_tree_in_cherry_grove" for v in VARIETIES],
        "step": "vegetal_decoration",
    })
    asset("lang/zh_cn", zh)
    asset("lang/en_us", en)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("client_jar", type=Path, help="Path to the vanilla Minecraft 1.21.1 client JAR")
    generate(parser.parse_args().client_jar)
