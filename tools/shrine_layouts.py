"""Original shrine compounds. The named rooms, gardens and paths use template-local coordinates."""

from generate_resources import MOD
from timber_architecture import (altar, basin, bed, boulder, close_eaves, curtain, deck, gallery, ground, hanging_cloth,
                                 offering_box, pave, pine, pond, rail, roof, stone_light, tatami, wall, AIR)


def torii(b, t):
    # The two complete stair courses meet the projected terrain at the front of the compound.
    side_step = {"cherry": "stone_brick_stairs", "shidare": "polished_diorite_stairs",
                 "yae": "polished_andesite_stairs", "yamazakura": "tuff_brick_stairs"}[t.wood]
    for z, y in ((0, 1), (1, 2)):
        b.fill((10, 0, z), (20, y-1, z), "minecraft:" + t.stone)
        b.fill((10, y+1, z), (20, 12, z), AIR)
        for x in range(10, 21):
            step = t.paving_stairs if 13 <= x <= 17 else side_step
            b.place(x, y, z, "minecraft:" + step, facing="south")
    ground(b, 10, 2, 20, 5)
    pave(b, 13, 2, 17, 5, t.paving)
    for x in (11, 19):
        b.place(x, 3, 4, "minecraft:" + t.stone)
        b.fill((x, 4, 4), (x, 9, 4), t.timber("log"), axis="y")
        b.place(x, 9, 3, t.timber("stairs"), half="top", facing="south")
    b.fill((10, 7, 4), (20, 7, 4), t.timber("stripped_log"), axis="x")
    b.fill((9, 9, 4), (21, 9, 4), t.timber("stripped_log"), axis="x")
    for x in range(8, 23):
        b.place(x, 10, 4, f"{MOD}:gray_roof_tile_slab", axis="x", type="top" if x in (8,22) else "bottom")
    b.place(15, 8, 4, t.timber("stripped_log"), axis="y")


def veranda(b, t, x0, x1, z, floor, tie, posts):
    for x in posts:
        b.fill((x, floor+1, z), (x, tie-1, z), t.timber("log"), axis="y")
        for step, facing in ((-1,"east"), (1,"west")):
            if x0 <= x+step <= x1:
                b.place(x+step, tie-1, z, t.timber("stairs"), facing=facing, half="top")
    b.fill((x0, tie, z), (x1, tie, z), t.timber("stripped_log"), axis="x")


def cherry_court(b, t):
    # A flower-dyer's temizu pavilion opens sideways onto the enclosed gravel garden.
    ground(b, 1, 5, 29, 30, clear=19, corners=4)
    pave(b, 9, 7, 22, 16, "gravel")
    pave(b, 14, 5, 16, 17, t.paving)
    pave(b, 6, 11, 13, 12, "andesite")
    for x0, x1 in ((10,12), (18,20)):
        bed(b, t, x0, 8, x1, 10)
        bed(b, t, x0, 14, x1, 15)
        stone_light(b, x0+1, 3, 13)
    for z in (7, 14):
        b.fill((3, 3, z), (3, 6, z), t.timber("log"), axis="y")
        b.fill((7, 3, z), (7, 6, z), t.timber("log"), axis="y")
        b.fill((3, 6, z), (7, 6, z), t.timber("stripped_log"), axis="x")
    b.fill((3, 6, 7), (3, 6, 14), t.timber("stripped_log"), axis="z")
    b.fill((7, 6, 7), (7, 6, 14), t.timber("stripped_log"), axis="z")
    roof(b, t, 2, 6, 8, 15, 7, axis="z", style="hip", color="brown")
    pave(b, 3, 7, 7, 14, "polished_andesite")
    basin(b, 5, 9)
    b.fill((4, 3, 12), (6, 3, 12), t.timber("stripped_log"), axis="x")
    b.place(4, 4, 12, "minecraft:potted_cherry_sapling")
    b.place(5, 4, 12, f"{MOD}:flower_mortar")
    for z in range(7, 17):
        b.place(24, 3, z, "minecraft:stone_bricks")
        b.place(24, 4, z, "minecraft:white_terracotta")
        b.place(24, 5, z, f"{MOD}:gray_roof_tile_slab", type="bottom", axis="x")
    pine(b, 27, 3, 13)
    # Broad hip-and-gable haiden; a separate low front portico marks the place of offering.
    deck(b, t, 5, 18, 25, 28, 3)
    b.fill((12, 3, 17), (18, 3, 17), t.timber("stairs"), facing="south")
    wall(b, t, 7, 21, "x", 3, 8, "|ss|ll|...|ll|ss|")
    wall(b, t, 7, 27, "x", 3, 8, "|ww|ss|www|ss|ww|")
    for x in (7, 23):
        wall(b, t, x, 21, "z", 3, 8, "|ss|ll|")
    veranda(b, t, 6, 24, 19, 3, 8, (6,10,20,24))
    for x in (7, 20):
        rail(b, t, x, 18, "x", 4, 4)
    curtain(b, t, 14, 16, 7, 21)
    tatami(b, 9, 22, 21, 25, 4)
    altar(b, t, 15, 4, 26)
    for x in (10, 20):
        b.place(x, 7, 21, f"{MOD}:silk_palace_lantern", hanging="true")
    roof(b, t, 4, 18, 26, 29, 9, color="pink", crest="brown")
    for x in (12, 18):
        b.place(x, 3, 17, "minecraft:chiseled_stone_bricks")
        b.fill((x, 4, 17), (x, 6, 17), t.timber("log"), axis="y")
    b.fill((12, 7, 17), (18, 7, 17), t.timber("stripped_log"), axis="x")
    b.place(13, 6, 17, "minecraft:bell", attachment="ceiling", facing="north")
    offering_box(b, 12, 4, 20)
    for x in (10, 20):
        hanging_cloth(b, x, 7, 18, "white", (("stripe_bottom","pink"),("flower","pink")))
    roof(b, t, 11, 16, 19, 20, 8, axis="z", style="gable", color="pink")
    close_eaves(b, t, 7, 21, 23, 27, 8)
    boulder(b, 23, 3, 9)


def water_pavilion(b, t):
    ground(b, 1, 5, 29, 30, clear=18, corners=4)
    pond(b, 6, [(6,10),(4,12),(3,18),(3,21),(2,23),(3,25),(3,26),(4,27),
                (4,27),(3,26),(3,25),(4,23),(5,22),(5,21),(6,19),(7,17),(9,15),(10,13)], seed=415)
    for x,z in ((6,8),(4,12),(8,14),(6,16),(10,18),(19,11),(19,15)):
        b.place(x, 3, z, "minecraft:lily_pad")
    for x,z in ((4,9),(8,20),(26,8)):
        boulder(b, x, 3, z)

    # A straight five-block span shares its centreline with the torii and the hall.
    pave(b, 13, 5, 17, 7, t.paving)
    b.fill((13, 3, 7), (17, 3, 7), t.timber("stairs"), facing="south")
    deck(b, t, 13, 8, 17, 18, 3)
    for x in (13,17):
        b.fill((x, 3, 8), (x, 3, 18), t.timber("stripped_log"), axis="z")
        rail(b, t, x, 8, "z", 11, 4)
        for z in (8,13,18):
            b.fill((x, 1, z), (x, 2, z), t.timber("log"), axis="y")
        for z in (8,18):
            b.place(x, 4, z, t.timber("log"), axis="y")
            b.place(x, 5, z, f"{MOD}:hanami_lantern", lit="true")

    # The broad engawa is a single landing. The eastern opening leads to the
    # moon-viewing pavilion by a short open gallery between the two distinct eaves.
    deck(b, t, 7, 19, 26, 29, 3)
    deck(b, t, 22, 17, 26, 18, 3)
    rail(b, t, 7, 19, "x", 7, 4)
    rail(b, t, 17, 19, "x", 6, 4)
    rail(b, t, 7, 19, "z", 7, 4)
    rail(b, t, 26, 17, "z", 10, 4)
    rail(b, t, 22, 17, "z", 3, 4)
    wall(b, t, 9, 22, "x", 3, 8, "|lls|...|sll|")
    wall(b, t, 9, 28, "x", 3, 8, "|ww|ss|ww|ss|", facing="south")
    for x, facing in ((9,"west"),(21,"east")):
        wall(b, t, x, 22, "z", 3, 8, "|ss|ww|", facing=facing)
    veranda(b, t, 7, 23, 20, 3, 8, (8,12,18,22))
    curtain(b, t, 14, 16, 7, 22)
    tatami(b, 11, 23, 19, 25, 4)
    altar(b, t, 15, 4, 27)
    offering_box(b, 17, 4, 26)
    b.fill((9, 8, 25), (21, 8, 25), t.timber("stripped_log"), axis="x")
    b.place(12, 7, 25, f"{MOD}:silk_palace_lantern", hanging="true")
    roof(b, t, 6, 18, 24, 30, 9, style="hip", color="light_blue", edge="cyan")
    close_eaves(b, t, 9, 22, 21, 28, 8)

    deck(b, t, 21, 10, 28, 16, 3)
    for x in (22,27):
        for z in (11,15):
            b.fill((x, 1, z), (x, 6, z), t.timber("log"), axis="y")
            b.place(x, 3, z, "minecraft:polished_diorite")
    roof(b, t, 20, 9, 29, 16, 8, style="hip", color="light_blue", edge="cyan")
    close_eaves(b, t, 22, 11, 27, 15, 7)
    b.fill((23, 4, 11), (26, 4, 11), t.timber("stairs"), facing="north")
    for x in (21,28):
        rail(b, t, x, 10, "z", 7, 4)
    rail(b, t, 21, 10, "x", 8, 4)
    rail(b, t, 21, 16, "x", 2, 4)
    rail(b, t, 26, 16, "x", 3, 4)
    b.place(24, 6, 11, f"{MOD}:silk_palace_lantern", hanging="true")
    stone_light(b, 10, 3, 6)
    pine(b, 4, 3, 27)
    bed(b, t, 22, 6, 24, 7)


def lantern_hall(b, t):
    ground(b, 1, 5, 29, 31, clear=20, corners=4)
    pave(b, 6, 8, 24, 18, "polished_andesite")
    pave(b, 13, 5, 17, 18, t.paving)
    for x in (6, 24):
        gallery(b, t, x, 8, 16, 3, "red")
        b.fill((x-1, 3, 7), (x+1, 3, 7), t.timber("stairs"), facing="south")
    for x in (10, 19):
        bed(b, t, x, 8, x+1, 11)
        stone_light(b, x, 3, 13)
    # A broad lower hip shelters the kagura stage. A separate windowed lantern story
    # carries the narrower upper roof; the court can see between the two eaves.
    deck(b, t, 6, 18, 24, 29, 4)
    for z,y in ((16,3),(17,4)):
        b.fill((12, 2, z), (18, y-1, z), "minecraft:polished_andesite")
        b.fill((12, y, z), (18, y, z), t.timber("stairs"), facing="south")
    wall(b, t, 8, 21, "x", 4, 9, "|ss|..|...|..|ss|")
    wall(b, t, 8, 28, "x", 4, 9, "|ww|ll|www|ll|ww|")
    for x in (8, 24):
        wall(b, t, x, 21, "z", 4, 9, "|ss|lls|")
    veranda(b, t, 6, 24, 19, 4, 9, (7,10,20,23))
    curtain(b, t, 15, 17, 8, 21)
    for x in (7,10,20,23):
        b.place(x+(-1 if x<15 else 1), 8, 19, f"{MOD}:silk_palace_lantern", hanging="true")
    tatami(b, 10, 22, 21, 26, 5)
    altar(b, t, 16, 5, 27)
    offering_box(b, 18, 5, 20)
    for x in (10, 20):
        hanging_cloth(b, x, 8, 18, "red", (("border","orange"),("flower","white")))
    roof(b, t, 4, 17, 26, 30, 10, style="hip", color="red", edge="black", skirt=3)
    # The skirt meets the outside of the upper walls. Its innermost tiles must not enter the room.
    b.fill((9, 11, 21), (23, 13, 26), AIR)
    for z in (20,27):
        wall(b, t, 8, z, "x", 10, 14, "|ll|ww|lll|ww|ll|")
    for x in (8,24):
        wall(b, t, x, 20, "z", 10, 14, "|ll|wwl|")
    b.fill((8, 10, 20), (24, 10, 27), t.timber("planks"))
    roof(b, t, 6, 19, 25, 28, 15, style="irimoya", color="red", edge="black")
    close_eaves(b, t, 8, 21, 24, 28, 9)
    close_eaves(b, t, 8, 20, 24, 27, 14)
    # Musicians enter from the stage. Two drums face the tatami practice space.
    tatami(b, 10, 22, 15, 25, 11)
    for x, base, voice in ((10, "polished_andesite", "basedrum"), (13, "sand", "snare")):
        # Stone gives a bass drum and sand a snare. Leave the heads clear to play.
        b.place(x, 11, 21, "minecraft:" + base)
        b.place(x, 12, 21, "minecraft:note_block", instrument=voice)
        b.place(x, 11, 22, "minecraft:dark_oak_trapdoor", facing="south", open="true")
    b.fill((9, 14, 23), (23, 14, 23), t.timber("stripped_log"), axis="x")
    for x in (12, 20):
        b.place(x, 13, 23, f"{MOD}:silk_palace_lantern", hanging="true")
    b.fill((20, 11, 21), (22, 11, 21), t.timber("slab"), type="top")
    b.place(21, 12, 21, f"{MOD}:rouge_plum_vase", facing="south")
    # The east post backs a single hatch. The last rung rises above the music-room floor.
    b.fill((23, 5, 24), (23, 11, 24), "minecraft:ladder", facing="west")


def mountain_banks(b):
    # Work out the toe of each retaining wall from the upper platforms. An offset
    # every two vertical blocks breaks the seven-block face into planted ledges.
    top = {}
    for (x, y, z), (block, _, _) in b.blocks.items():
        if block in ("minecraft:tuff", "minecraft:mossy_stone_bricks", "minecraft:tuff_bricks"):
            top[x,z] = max(y, top.get((x,z), 2))
    for x in range(2, 26):
        for z in range(8, 31):
            if (x,z) in top or (12 <= x <= 18 and z <= 17) or (9 <= x <= 12 and 14 <= z <= 16):
                continue
            height = max(h - 2*max(abs(x-sx), abs(z-sz)) - min(abs(x-sx), abs(z-sz))//2
                         for (sx,sz),h in top.items())
            if height <= 2:
                continue
            for y in range(3, height):
                stone = "tuff_bricks" if y == 4 else "mossy_cobblestone" if (x//3+z//4)%4 == 0 else "tuff"
                b.place(x, y, z, "minecraft:" + stone)
            cap = "moss_block" if (x//2+z//3)%3 else "andesite"
            b.place(x, height, z, "minecraft:"+cap)
            if cap == "moss_block" and (x+z)%4 == 0:
                b.place(x, height+1, z, "minecraft:fern")


def mountain_sanctuary(b, t):
    ground(b, 2, 5, 28, 30, clear=20, corners=5)
    # Two flights at right angles, with a stone retaining wall and an elbow landing.
    pave(b, 14, 5, 16, 8, t.paving)
    for z in range(9, 13):
        y = z-6
        b.fill((13, 2, z), (17, y-1, z), "minecraft:tuff_bricks")
        b.fill((13, y, z), (17, y, z), "minecraft:tuff_brick_stairs", facing="south")
    for x0,z0,x1,z1,y in ((12,13,18,17,6),(4,13,9,27,9),(5,18,17,28,9),(7,22,21,27,8)):
        for z in range(z0, z1+1):
            bevel = int(z in (z0,z1))
            b.fill((x0+bevel, 2, z), (x1-bevel, y-1, z), "minecraft:tuff")
            pave(b, x0+bevel, z, x1-bevel, z, "mossy_stone_bricks", y)
    mountain_banks(b)
    for x in (12, 11, 10):
        y = 19-x
        b.fill((x, 2, 14), (x, y-1, 16), "minecraft:tuff")
        b.fill((x, y, 14), (x, y, 16), "minecraft:tuff_brick_stairs", facing="west")
    # The mountain-facing nagare roof has a deep front pitch and a short back pitch.
    deck(b, t, 5, 18, 16, 27, 9)
    wall(b, t, 6, 21, "x", 9, 13, "|ss|...|l|")
    wall(b, t, 6, 26, "x", 9, 13, "|ww|lll|w|")
    for x in (6, 15):
        wall(b, t, x, 21, "z", 9, 13, "|ss|w|")
    veranda(b, t, 5, 16, 18, 9, 13, (5,15))
    curtain(b, t, 10, 12, 12, 21)
    tatami(b, 8, 22, 14, 24, 10)
    altar(b, t, 11, 10, 25)
    offering_box(b, 13, 10, 24)
    roof(b, t, 3, 16, 18, 28, 14, style="gable", ridge=8, color="green", edge="gray")
    close_eaves(b, t, 6, 21, 15, 26, 13)
    # Crossed chigi at the gables and three short katsuogi run across the main ridge.
    for x in (4, 17):
        b.fill((x, 17, 23), (x, 17, 25), "minecraft:stripped_spruce_log", axis="z")
        for z, facing in ((23,"north"),(25,"south")):
            b.place(x, 18, z, "minecraft:spruce_stairs", facing=facing)
        for z in (22,26):
            b.place(x, 18, z, "minecraft:spruce_slab", type="top")
            b.place(x, 19, z, "minecraft:spruce_slab", type="bottom")
    for x in (7,10,13):
        b.fill((x, 17, 23), (x, 17, 25), "minecraft:stripped_spruce_log", axis="z")
    # Lookout deck clings to the east wall; its stone feet extend to the ground.
    deck(b, t, 16, 21, 21, 25, 9)
    rail(b, t, 21, 21, "z", 5, 10)
    rail(b, t, 17, 21, "x", 4, 10)
    for x,z,y in ((12,8,3),(18,11,3),(17,15,7),(7,16,10),(19,25,10)):
        stone_light(b, x, y, z)
    for x,z in ((6,9),(23,16),(24,23)):
        boulder(b, x, 3, z)
    pine(b, 24, 3, 21)
    bed(b, t, 5, 6, 9, 8)
    bed(b, t, 19, 9, 22, 11)


# Each chest occupies the side of the room, away from the altar and its entrance.
SHRINES = {
    "cherry": (cherry_court, (9,4,26)),
    "shidare": (water_pavilion, (19,4,27)),
    "yae": (lantern_hall, (11,11,26)),
    "yamazakura": (mountain_sanctuary, (8,10,25)),
}
