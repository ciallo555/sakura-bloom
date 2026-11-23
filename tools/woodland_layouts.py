"""A merchant's machiya, a roji tea garden, and a timber-framed mountain workshop."""

from dataclasses import replace

from generate_resources import MOD
from timber_architecture import (AIR, awning, basin, bed, boulder, curtain, deck, ground, hanging_cloth, pave,
                                 pine, pond, rail, roof, stone_light, tatami, wall)


def threshold(b, x, width, stairs, paving):
    for z,y in ((0,1),(1,2)):
        b.fill((x, 0, z), (x+width-1, y-1, z), "minecraft:cobblestone")
        b.fill((x, y+1, z), (x+width-1, 7, z), AIR)
        b.fill((x, y, z), (x+width-1, y, z), "minecraft:"+stairs, facing="south")
    pave(b, x, 2, x+width-1, 3, paving)


def dyehouse(b, theme):
    t = replace(theme, roof="blue")
    ground(b, 1, 1, 24, 26, clear=16, corners=3)
    pave(b, 3, 3, 20, 7, "gravel")
    pave(b, 15, 4, 17, 22, "packed_mud")
    # The street shop has a raised sales floor and an earthen passage (tori-niwa)
    # beneath the same roof. It leads from the street past the loom to the dye yard.
    deck(b, t, 4, 7, 13, 17, 3)
    b.fill((7, 3, 6), (10, 3, 6), t.timber("stairs"), facing="south")
    wall(b, t, 4, 8, "x", 3, 7, "|ll|...|ss|...|")
    wall(b, t, 4, 17, "x", 3, 7, "|ww|ss|ll.|...|")
    for x in (4,18):
        wall(b, t, x, 8, "z", 3, 7, "|ww|ss|ll|")
    # The earthen aisle is one block below the shop floor. Seat both jambs and
    # the long outside wall on stone, leaving the three-block passage open.
    for z in (8, 17):
        b.place(14, 3, z, "minecraft:stone_bricks")
    b.fill((18, 3, 8), (18, 3, 17), "minecraft:stone_bricks")
    curtain(b, t, 8, 10, 6, 8, ("indigo","white","indigo"))
    b.fill((4, 7, 8), (18, 7, 17), t.timber("planks"))
    # Up to the cloth loft via the ladder; the opening must stay clear at its head.
    b.fill((4, 4, 15), (4, 7, 15), t.timber("planks"))
    b.fill((5, 4, 15), (5, 7, 15), "minecraft:ladder", facing="east")
    b.fill((5, 8, 15), (5, 9, 15), AIR)
    for z in (8,17):
        wall(b, t, 4, z, "x", 7, 11, "|ww|lll|sss|ww|")
    for x in (4,18):
        wall(b, t, x, 8, "z", 7, 11, "|ww|ll|ww|")
    roof(b, t, 2, 6, 20, 19, 12, style="gable", color="blue", edge="gray")
    awning(b, t, 2, 4, 20, 8, 7, color="cyan")
    for x in (4,13,18):
        b.fill((x, 3, 5), (x, 6, 5), t.timber("stripped_log"), axis="y")
    b.fill((4, 6, 5), (18, 6, 5), t.timber("stripped_log"), axis="x")
    for x in (6,17):
        b.place(x, 5, 5, f"{MOD}:silk_palace_lantern", hanging="true")
    b.fill((5, 4, 10), (9, 4, 10), t.timber("slab"), type="top")
    for x, cloth in ((5,"cyan"),(6,"light_blue"),(7,"purple"),(8,"pink")):
        b.place(x, 5, 10, "minecraft:" + cloth + "_carpet")
    b.place(6, 4, 16, "minecraft:loom", facing="east")
    b.place(7, 4, 16, "minecraft:loom", facing="east")
    b.fill((10, 4, 16), (12, 4, 16), t.timber("stripped_log"), axis="x")
    b.place(10, 5, 16, f"{MOD}:flower_mortar")
    b.place(12, 5, 16, f"{MOD}:rouge_plum_vase", facing="north")
    b.place(8, 6, 13, f"{MOD}:silk_palace_lantern", hanging="true")
    for x, cloth in ((6,"blue"),(7,"white"),(8,"pink")):
        b.place(x, 8, 16, "minecraft:" + cloth + "_wool")
    # Bolt racks dry at the back, clear of the occupied shopfront.
    pave(b, 7, 19, 19, 24, "packed_mud")
    for z, colors in ((20, ("blue","cyan","light_blue")), (24, ("red","magenta","pink"))):
        for x in (4,11):
            b.place(x, 3, z, "minecraft:andesite_wall")
            b.fill((x, 4, z), (x, 6, z), t.timber("fence"))
        b.fill((4, 7, z), (11, 7, z), t.timber("slab"), type="double")
        for x,color,pattern in zip((6,8,10), colors, ("flower","rhombus","stripe_downleft")):
            hanging_cloth(b, x, 7, z-1, color, (("stripe_bottom","white"),(pattern,"white")))
    basin(b, 13, 20, length=3)
    b.place(13, 3, 18, t.timber("planks"))
    b.place(13, 4, 18, f"{MOD}:flower_mortar")
    bed(b, t, 21, 8, 23, 13)
    threshold(b, 9, 4, "brick_stairs", "gravel")
    return (8,4,16)


def rain_pavilion(b, t):
    # The moss path follows the bank: entrance stone, waiting seat, water basin, tea room.
    ground(b, 1, 1, 26, 24, clear=15, corners=4)
    pond(b, 6, [(4,7),(3,9),(3,11),(2,12),(2,13),(3,14),(4,15),(5,15),
                (6,14),(7,13),(8,11)])
    for x,z in ((4,8),(5,11),(12,12),(8,14)):
        b.place(x, 3, z, "minecraft:lily_pad")
    for x,z in ((3,6),(4,14),(13,16)):
        boulder(b, x, 3, z)
    for x,z in ((11,2),(11,3),(12,3),(13,4),(14,4),(15,5),(16,5),(17,6),
                (17,7),(18,8),(18,9),(18,10),(19,11),(19,12)):
        b.place(x, 2, z, "minecraft:andesite" if (x+z)%3 else "minecraft:stone_bricks")
    # The waiting bench sits under its own low roof beside a woven bamboo fence.
    for x in (18,23):
        for z in (4,7):
            b.place(x, 3, z, "minecraft:andesite_wall")
            b.fill((x, 4, z), (x, 6, z), "minecraft:bamboo_fence")
        b.fill((x, 6, 4), (x, 6, 7), "minecraft:bamboo_mosaic_slab", type="top")
    b.fill((18, 6, 4), (23, 6, 4), "minecraft:bamboo_mosaic_slab", type="top")
    b.fill((19, 3, 6), (22, 3, 6), "minecraft:bamboo_stairs", facing="south")
    rail(b, t, 18, 7, "x", 6, 3)
    awning(b, t, 17, 3, 24, 8, 7, rise="north", color="brown")
    basin(b, 21, 10, rim="polished_diorite")
    stone_light(b, 23, 3, 10)
    pine(b, 7, 3, 20)
    # An L-shaped engawa and a little jetty have an unrailed view of the rain garden.
    deck(b, t, 12, 14, 24, 21, 3)
    deck(b, t, 10, 11, 15, 19, 3)
    b.fill((17, 3, 13), (20, 3, 13), t.timber("stairs"), facing="south")
    for x in (11,15):
        b.fill((x, 4, 12), (x, 6, 12), "minecraft:stripped_bamboo_block", axis="y")
    b.fill((11, 6, 12), (15, 6, 12), "minecraft:stripped_bamboo_block", axis="x")
    awning(b, t, 9, 11, 16, 15, 7, color="brown")
    wall(b, t, 14, 15, "x", 3, 7, "|ll|...|s|")
    wall(b, t, 14, 20, "x", 3, 7, "|ww|sss|w|")
    wall(b, t, 14, 15, "z", 3, 7, "|..|l|")
    wall(b, t, 23, 15, "z", 3, 7, "|ss|w|")
    roof(b, t, 12, 13, 25, 22, 8, style="hip", color="brown", edge="gray")
    curtain(b, t, 18, 20, 6, 15, ("white","light_gray"))
    tatami(b, 15, 16, 22, 19, 4)
    b.fill((20, 4, 19), (22, 4, 19), t.timber("slab"), type="top")
    b.place(21, 5, 19, f"{MOD}:celadon_bamboo_vase", facing="north")
    b.place(16, 6, 15, f"{MOD}:silk_palace_lantern", hanging="true")
    b.fill((15, 7, 18), (22, 7, 18), t.timber("stripped_log"), axis="x")
    b.place(22, 6, 18, "minecraft:lantern", hanging="true")
    bed(b, t, 5, 3, 8, 4)
    threshold(b, 10, 3, "andesite_stairs", "andesite")
    for x in (8, 14):
        b.place(x, 3, 3, "minecraft:bamboo_fence", east="true", west="true")
        b.place(x+1, 3, 3, "minecraft:bamboo_fence", west="true")
    return (15,4,19)


def hillside_joinery(b, t):
    ground(b, 1, 1, 27, 24, clear=18, corners=4)
    pave(b, 4, 4, 23, 10, "gravel")
    pave(b, 10, 3, 13, 16, "packed_mud")
    # A gable-front minka: the ridge runs from the loading door to the rear loft.
    deck(b, t, 4, 10, 17, 21, 3)
    b.fill((9, 3, 9), (13, 3, 9), "minecraft:tuff_brick_stairs", facing="south")
    wall(b, t, 5, 10, "x", 3, 8, "|ll|.....|ss|")
    wall(b, t, 5, 20, "x", 3, 8, "|tt|ss|ww|ll|")
    wall(b, t, 5, 10, "z", 3, 8, "|ww|ll|sss|")
    wall(b, t, 17, 10, "z", 3, 8, "|..|ll|..t|")
    roof(b, t, 3, 8, 19, 22, 9, axis="z", style="gable", color="green", edge="gray")
    for x in (9,10,12,13):
        b.place(x, 10, 9, f"{MOD}:yamazakura_lattice", facing="north")
    # Open tie-beam trusses carry the loft floor and leave a double-height working bay.
    for z in (12,16,19):
        b.fill((5, 8, z), (17, 8, z), t.timber("stripped_log"), axis="x")
        for x in (6,16):
            b.place(x, 7, z, t.timber("stairs"), half="top", facing="west" if x==6 else "east")
    b.fill((6, 7, 17), (16, 7, 19), t.timber("planks"))
    # The elevated loft only bears on the rear frame, keeping the work floor open below.
    b.fill((7, 4, 18), (15, 6, 18), AIR)
    b.fill((5, 4, 18), (5, 7, 18), t.timber("planks"))
    for y in range(4, 8):
        b.place(6, y, 18, "minecraft:ladder", facing="east")
    b.place(8, 8, 19, "minecraft:barrel", facing="north")
    b.place(9, 8, 19, t.timber("log"), axis="z")
    b.fill((6, 4, 13), (8, 4, 13), t.timber("stripped_log"), axis="x")
    b.place(7, 5, 13, "minecraft:stonecutter", facing="north")
    b.place(6, 4, 15, "minecraft:crafting_table")
    b.place(15, 4, 18, "minecraft:barrel", facing="north")
    b.place(11, 7, 12, "minecraft:lantern", hanging="true")
    b.place(15, 4, 13, "minecraft:grindstone", face="floor", facing="west")
    # Side lean-to and log haul: the chain actually terminates on the suspended log.
    for z in (11,20):
        b.fill((24, 3, z), (24, 6, z), t.timber("log"), axis="y")
        b.fill((17, 6, z), (24, 6, z), t.timber("stripped_log"), axis="x")
    b.fill((24, 6, 11), (24, 6, 20), t.timber("stripped_log"), axis="z")
    awning(b, t, 17, 10, 25, 21, 7, rise="west", color="brown", run=2)
    for y,left,right in ((3,19,22),(4,20,22),(5,20,21)):
        for x in range(left,right+1):
            b.fill((x, y, 15), (x, y, 19), t.timber("log"), axis="z")
    for x in (7, 17):
        b.fill((x, 3, 5), (x, 7, 5), t.timber("log"), axis="y")
    b.fill((6, 8, 5), (18, 8, 5), t.timber("stripped_log"), axis="x")
    for x in (10,14):
        b.fill((x, 6, 5), (x, 7, 5), "minecraft:chain", axis="y")
    b.fill((9, 5, 5), (15, 5, 5), t.timber("log"), axis="x")
    # The side approach leaves two blocks of headroom under the hoist.
    boulder(b, 4, 3, 6)
    bed(b, t, 21, 4, 24, 6)
    threshold(b, 10, 4, "tuff_brick_stairs", "packed_mud")
    return (9,4,19)
