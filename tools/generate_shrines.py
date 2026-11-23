"""Build the four biome-themed shrine templates and their vanilla worldgen data."""

import gzip
from dataclasses import dataclass
from io import BytesIO

from nbtlib import Compound, File, Int, List, String

from generate_resources import MOD, CORE_OUTPUT, VARIETIES, datum, tag
from shrine_layouts import SHRINES, torii


SIZE = (31, 21, 33)


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

    def save(self, name, size=SIZE):
        palette = {}
        blocks = []
        for pos, (name_id, properties, data) in sorted(self.blocks.items(), key=lambda entry: entry[0][1]):
            assert all(0 <= coord < limit for coord, limit in zip(pos, size)), pos
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
            "size": List[Int](size),
            "palette": List[Compound](states),
            "blocks": List[Compound](blocks),
            "entities": List[Compound](),
        })
        path = CORE_OUTPUT / f"data/{MOD}/structure/{name}.nbt"
        path.parent.mkdir(parents=True, exist_ok=True)
        buffer = BytesIO()
        template.write(buffer)
        path.write_bytes(gzip.compress(buffer.getvalue(), mtime=0))


def reward_chest(build, pos, wood, table):
    vanilla = wood == "cherry"
    block = "minecraft:chest" if vanilla else f"{MOD}:{wood}_chest"
    entity = "minecraft:chest" if vanilla else f"{MOD}:cherry_chest"
    x, y, z = pos
    backing = build.blocks[x, y, z+1][0]
    assert backing.endswith(("_planks", "_log", "_terracotta")), (table, pos, backing)
    build.place(*pos, block, facing="north", type="single", data=Compound({
        "id": String(entity), "LootTable": String(f"{MOD}:chests/{table}"),
    }))


def surface_structure(name, template, biomes, *, paving, center=15, width=11, relief=8):
    pool = f"{MOD}:{template}"
    datum(f"worldgen/template_pool/{template}", {
        "fallback": "minecraft:empty",
        "elements": [{"weight": 1, "element": {
            "element_type": "minecraft:single_pool_element", "location": pool,
            "processors": "minecraft:empty", "projection": "rigid",
        }}],
    })
    datum(f"worldgen/structure/{name}", {
        # Planting and surface springs run before the compound, so its veranda, yard and pond stay clear.
        "type": f"{MOD}:garden_site", "max_surface_relief": relief, "biomes": biomes, "step": "top_layer_modification",
        "entrance_center_x": center, "entrance_width": width, "entrance_paving": "minecraft:" + paving,
        "clearance_template": pool,
        "spawn_overrides": {}, "terrain_adaptation": "none", "start_pool": pool,
        "size": 1, "start_height": {"absolute": -2}, "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 32, "use_expansion_hack": False,
    })


def generate():
    shrines = []
    for theme in THEMES:
        build = Blueprint()
        layout, chest = SHRINES[theme.wood]
        layout(build, theme)
        torii(build, theme)
        reward_chest(build, chest, theme.wood, "shrine_offerings")
        template = f"shrine/{theme.wood}"
        build.save(template)

        name = f"{theme.wood}_shrine"
        shrines.append(f"{MOD}:{name}")
        surface_structure(name, template, [theme.biome], paving=theme.paving, relief=10 if theme.wood == "yamazakura" else 7)

    tag(MOD, "worldgen/structure", "shrines", shrines)
    datum("worldgen/structure_set/shrines", {
        "structures": [{"structure": name, "weight": 1} for name in shrines],
        "placement": {
            "type": "minecraft:random_spread",
            "salt": 167092968,
            "spacing": 18,
            "separation": 8,
            "frequency": 0.7,
            "exclusion_zone": {"other_set": "minecraft:ruined_portals", "chunk_count": 3},
        },
    })


if __name__ == "__main__":
    generate()
