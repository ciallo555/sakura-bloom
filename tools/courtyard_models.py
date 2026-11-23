"""Original courtyard furniture geometry, measured in pixels on a 16-pixel block."""

from courtyard_variants import CURTAINS, LATTICES, VASES, WOODS, noren_id
from window_patterns import carved_slats

def furnishing_models(box):
    models = {}

    def model(name, particle, parts, cutout=False):
        materials = {face["texture"][1:] for part in parts for face in part["faces"].values()}
        textures = {key: f"sakura_bloom:block/{key}" for key in sorted(materials)}
        textures["particle"] = f"sakura_bloom:block/{particle}"
        result = {"parent": "minecraft:block/block", "textures": textures, "elements": parts}
        if cutout:
            result["render_type"] = "minecraft:cutout"
        models[name] = result

    def panel(start, end, edge, face):
        part = box(start, end, edge)
        for direction in ("north", "south"):
            part["faces"][direction]["texture"] = "#" + face
        return part

    def window_frame(frame):
        return [box((0, 0, 6), (1, 16, 10), frame), box((15, 0, 6), (16, 16, 10), frame),
                box((1, 0, 6), (15, 1, 10), frame), box((1, 15, 6), (15, 16, 10), frame)]

    model("bound_tatami", "tatami_weave", [
        box((0, 0, 0), (16, 1, 16), "tatami_edge", "tatami_weave")])
    for name, face, frame in (("cherry_shoji", "shoji_paper", "honey_joinery"),
                              ("begonia_lattice", "begonia_carving", "rosewood_joinery")):
        model(name, frame, [
            panel((1, 1, 6.5 if name == "cherry_shoji" else 7), (15, 15, 9.5 if name == "cherry_shoji" else 9), frame, face),
            *window_frame(frame),
        ], cutout=name == "begonia_lattice")

    model("indigo_noren", "noren_waves", [
        box((0, 14, 7), (16, 16, 9), "bamboo_cane"),
        panel((0, 2, 7.5), (7, 14, 8.5), "noren_waves", "noren_waves"),
        panel((9, 1, 7.5), (16, 14, 8.5), "noren_waves", "noren_waves"),
    ], cutout=True)

    legs = [box((x, 0, z), (x + 2, 3, z + 2), "garden_granite")
            for x in (3, 11) for z in (3, 11)]
    model("yukimi_lantern", "garden_granite", [
        *legs, box((3, 3, 3), (13, 5, 13), "garden_granite"),
        box((4, 5, 4), (12, 6, 12), "garden_granite"),
        box((5, 6, 5), (11, 10, 11), "yukimi_window"),
        box((1, 10, 1), (15, 12, 15), "granite_eave", "garden_granite"),
        box((3, 12, 3), (13, 13, 13), "garden_granite"),
        box((5, 13, 5), (11, 14, 11), "garden_granite"),
        box((7, 14, 7), (9, 16, 9), "garden_granite"),
    ])

    for hanging in (False, True):
        dy = int(hanging)
        def tier(xz, low, high, texture):
            return box((xz, low + dy, xz), (16 - xz, high + dy, 16 - xz), texture)

        parts = [
            tier(4, 0, 1, "rosewood_joinery"), tier(4, 1, 2, "aged_brass"),
            tier(4, 2, 4, "palace_silk"),
            box((3, 4+dy, 4), (13, 10+dy, 12), "palace_silk"),
            box((4, 4+dy, 3), (12, 10+dy, 4), "palace_silk"),
            box((4, 4+dy, 12), (12, 10+dy, 13), "palace_silk"),
            tier(4, 10, 11, "palace_silk"), tier(3, 11, 12, "aged_brass"),
            tier(4, 12, 13, "rosewood_joinery"),
            box((6, 13+dy, 7), (7, 14+dy, 9), "aged_brass"),
            box((9, 13+dy, 7), (10, 14+dy, 9), "aged_brass"),
            box((6, 14+dy, 7), (10, 15+dy, 9), "aged_brass"),
        ]
        if hanging:
            parts.append(box((7, 0, 7), (9, 1, 9), "palace_silk"))
        model("silk_palace_lantern" + ("_hanging" if hanging else ""), "palace_silk", parts)

    model("blue_blossom_vase", "porcelain_branch", [
        box((5, 0, 5), (11, 2, 11), "porcelain_rim"),
        box((4, 2, 4), (12, 3, 12), "porcelain_branch"),
        box((3, 3, 3), (13, 8, 13), "porcelain_branch"),
        box((4, 8, 4), (12, 10, 12), "porcelain_branch"),
        box((6, 10, 6), (10, 12, 10), "porcelain_rim", "vase_inside"),
        box((5, 12, 5), (11, 13, 6), "porcelain_rim"),
        box((5, 12, 10), (11, 13, 11), "porcelain_rim"),
        box((5, 12, 6), (6, 13, 10), "porcelain_rim"),
        box((10, 12, 6), (11, 13, 10), "porcelain_rim"),
    ])

    def variant(name, parent, replacements, cutout=False):
        result = {"parent": f"sakura_bloom:block/{parent}",
                  "textures": {k: f"sakura_bloom:block/{v}" for k, v in replacements.items()}}
        if cutout:
            result["render_type"] = "minecraft:cutout"
        models[name] = result

    for wood in WOODS:
        frame = "honey_joinery" if wood == "cherry" else f"shoji_{wood}_frame"
        model(f"{wood}_lattice", frame, window_frame(frame) + carved_slats(wood, frame, box))
        if wood != "cherry":
            variant(f"{wood}_shoji", "cherry_shoji", {"honey_joinery": frame, "particle": frame,
                                                     "shoji_paper": f"shoji_{wood}_paper"})
    for color in CURTAINS:
        if color != "blue":
            cloth = f"noren_{color}_cloth"
            variant(noren_id(color), "indigo_noren", {"noren_waves": cloth, "particle": cloth}, True)
    for pattern, (_, _, wood, _) in LATTICES.items():
        frame = f"shoji_{wood}_frame"
        variant(f"{pattern}_lattice", "begonia_lattice", {"begonia_carving": f"{pattern}_carving",
                                                         "rosewood_joinery": frame, "particle": frame}, True)
    for name, (_, _, _, _, _, tiers, (lip, height)) in VASES.items():
        body, sprig, rim, inside = (f"{name}_{part}" for part in ("body", "sprig", "rim", "inside"))
        parts = []
        for index, (inset, low, high) in enumerate(tiers):
            end = 16 - inset
            if index == len(tiers)-1:
                part = box((inset, low, inset), (end, high, end), rim, inside)
            else:
                part = panel((inset, low, inset), (end, high, end), sprig, body)
                part["faces"]["up"]["texture"] = "#" + rim
            parts.append(part)
        far = 16 - lip
        for x0,z0,x1,z1 in ((lip,lip,far,lip+1), (lip,far-1,far,far),
                             (lip,lip+1,lip+1,far-1), (far-1,lip+1,far,far-1)):
            parts.append(box((x0,height,z0), (x1,height+1,z1), rim))
        model(f"{name}_vase", body, parts)
    return models
