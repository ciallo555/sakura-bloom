"""Generate the Interstice dimension, district templates and the maple-leaf fan sprite."""
from pathlib import Path
from PIL import Image, ImageDraw
from generate_resources import CORE_OUTPUT, MOD, asset, datum
from interstice_layouts import build_quarters, build_sanctums

M = MOD + ":"
DISTRICTS = {
    "lantern": ("lantern_gallery", 0x2E1C36, ["lantern_wisp"], []),
    "moonlit": ("moonlit_harbor", 0x182C46, ["lantern_wisp"], ["dusk_fox","gloom_hare"]),
    "vermilion": ("vermilion_bridges", 0x432631, ["lantern_wisp"], ["ink_wolf","dusk_fox"]),
    "ashen": ("ashen_lofts", 0x241F38, [], ["ink_wolf","gloom_hare","dusk_fox"]),
}


def district_data():
    for theme,(name,fog,peaceful,hostile) in DISTRICTS.items():
        def spawns(names):
            return [{"type": n if ":" in n else M+n, "weight":6, "minCount":1, "maxCount":1} for n in names]
        datum("worldgen/biome/"+name, {
            "has_precipitation":False,"temperature":0.6,"downfall":0.0,
            "effects":{"fog_color":fog,"sky_color":0x151329,"water_color":0x45618C,"water_fog_color":0x222338,
                       "mood_sound":{"sound":"minecraft:ambient.cave","tick_delay":6400,"block_search_extent":8,"offset":2}},
            "carvers":{},"features":[[M+"interstice_quarter"]],
            "spawners":{"monster":spawns(hostile),"creature":spawns(peaceful)}, "spawn_costs":{},
        })
        datum("worldgen/structure/"+theme+"_sanctum", {
            "type":M+"interstice_sanctum","template":M+"interstice/"+theme+"_sanctum",
            "biomes":[M+name], "step":"surface_structures","terrain_adaptation":"none","spawn_overrides":{},
        })
    datum("worldgen/structure_set/interstice_sanctums",{
        "structures":[{"structure":M+t+"_sanctum","weight":1} for t in DISTRICTS],
        "placement":{"type":"minecraft:random_spread","spacing":8,"separation":3,"salt":30691183},
    })
    datum("worldgen/configured_feature/interstice_quarter",{"type":M+"interstice_quarter","config":{}})
    datum("worldgen/placed_feature/interstice_quarter",{"feature":M+"interstice_quarter","placement":[]})


def dimension():
    datum("dimension_type/interstice",{
        "fixed_time":18000,"has_skylight":True,"has_ceiling":False,"ultrawarm":False,"natural":False,
        "coordinate_scale":1.0,"bed_works":False,"respawn_anchor_works":False,"min_y":0,"height":128,"logical_height":128,
        "infiniburn":"#minecraft:infiniburn_overworld","effects":"minecraft:overworld","ambient_light":0.14,
        "piglin_safe":False,"has_raids":False,"monster_spawn_light_level":7,"monster_spawn_block_light_limit":7,
    })
    keys=("barrier fluid_level_floodedness fluid_level_spread lava temperature vegetation continents erosion depth "
          "ridges initial_density_without_jaggedness final_density vein_toggle vein_ridged vein_gap").split()
    router=dict.fromkeys(keys,0.0)
    router["final_density"]=-1.0
    router["initial_density_without_jaggedness"]=-1.0
    datum("worldgen/noise_settings/interstice",{
        "aquifers_enabled":False,"default_block":{"Name":"minecraft:air"},"default_fluid":{"Name":"minecraft:air"},
        "disable_mob_generation":False,"legacy_random_source":False,"noise":{"height":128,"min_y":0,"size_horizontal":2,"size_vertical":2},
        "noise_router":router,"ore_veins_enabled":False,"sea_level":0,"spawn_target":[],
        "surface_rule":{"type":"minecraft:block","result_state":{"Name":"minecraft:air"}},
    })
    datum("dimension/interstice",{"type":M+"interstice","generator":{
        "type":"minecraft:noise","settings":M+"interstice","biome_source":{"type":"minecraft:checkerboard","scale":3,
        "biomes":[M+v[0] for v in DISTRICTS.values()]},
    }})


def wisp_sprite():
    # The vanilla Allay UV layout wraps a five-pixel paper lantern with a gilded frame.
    im=Image.new("RGBA",(32,32))
    d=ImageDraw.Draw(im)
    d.rectangle((5,0,14,4),fill="#ca7356")
    for x in (0,5,10,15):
        d.rectangle((x,5,x+4,9),fill="#f7c687")
        d.line((x,5,x+4,5),fill="#bd6555")
        d.line((x,9,x+4,9),fill="#d68c60")
        d.line((x,6,x,8),fill="#eaa16e")
    for x in (6,8):
        d.point((x,7),fill="#673748")
    d.rectangle((0,10,9,22),fill="#cb715a")
    for x in (1,4,7):
        d.line((x,14,x,21),fill="#fbce8c")
    d.line((0,20,9,20),fill="#9d494a")
    for y in (0,6):
        d.rectangle((23,y,29,y+5),fill="#e79b6d")
        d.line((24,y+4,29,y+4),fill="#ffe3a6")
    for wx in (16,24):
        d.polygon([(wx,22),(wx+7,22),(wx+6,24),(wx+2,26),(wx,25)],fill=(255,192,131,185))
        d.line([(wx,22),(wx+6,22),(wx+1,25)],fill=(235,138,105,220))
    dest=CORE_OUTPUT/f"assets/{MOD}/textures/entity/lantern_wisp.png"
    dest.parent.mkdir(parents=True,exist_ok=True)
    im.save(dest)


def fan_sprite():
    # Pixel-cut seven lobes; each rib follows a leaf vein back to the single brass pin.
    im=Image.new("RGBA",(32,32))
    d=ImageDraw.Draw(im)
    ink="#542139"
    outline=[(3,29),(5,25),(9,22),(3,21),(6,19),(1,15),(7,15),(4,10),(10,12),(8,3),(13,8),
             (14,2),(17,9),(23,0),(22,10),(30,5),(27,13),(31,12),(27,18),(29,19),(22,21),(24,24),
             (17,24),(15,27),(10,25),(6,30)]
    d.polygon(outline,fill="#ad3542",outline=ink)
    piv=(10,23)
    for i,(tip,shade,hilite) in enumerate([
        ((3,16),"#d65042","#ff9e68"),((6,12),"#c24043","#f57d55"),((9,5),"#df5440","#ffa15e"),
        ((14,4),"#e65f41","#ffaf66"),((22,3),"#ed6845","#ffbb70"),((28,8),"#cf4543","#ff9863"),
        ((29,14),"#ce4342","#ff9e68"),((26,19),"#a93342","#e87256"),((21,22),"#bd4547","#f79065")]):
        tx,ty=tip
        d.polygon([piv,(tx-1,ty+2),tip,(tx+1,ty+2)],fill=shade)
        d.line([piv,(tx,ty+1)],fill="#813042",width=1)
        a,b=(10+tx)//2,(23+ty)//2
        d.line([(a+1,b),(tx,ty+2)],fill=hilite)
    d.line([(4,29),(10,23)],fill="#683839",width=3)
    d.line([(4,28),(10,22)],fill="#cd8e57",width=1)
    d.rectangle((9,22,11,24),fill="#9c653f")
    d.point((10,22),fill="#ffe392")
    d.line([(5,29),(6,31),(9,30)],fill="#bd4149")
    dest=CORE_OUTPUT/f"assets/{MOD}/textures/item/kamikakushi.png"
    dest.parent.mkdir(parents=True,exist_ok=True)
    im.save(dest)
    asset("models/item/kamikakushi",{"parent":"minecraft:item/handheld","textures":{"layer0":M+"item/kamikakushi"},
        "display":{"firstperson_righthand":{"rotation":[0,-90,25],"translation":[1.2,2.8,.9],"scale":[1.05,1.05,.8]},
                   "firstperson_lefthand":{"rotation":[0,90,-25],"translation":[1.2,2.8,.9],"scale":[1.05,1.05,.8]},
                   "thirdperson_righthand":{"rotation":[0,-90,55],"translation":[0,5,1],"scale":[1.4,1.4,1]},
                   "thirdperson_lefthand":{"rotation":[0,90,-55],"translation":[0,5,1],"scale":[1.4,1.4,1]}}})


if __name__=="__main__":
    dimension()
    district_data()
    build_quarters()
    build_sanctums()
    fan_sprite()
    wisp_sprite()
