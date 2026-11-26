"""Generate the interstice's four ritual sets, rewards and original pixel art."""
from pathlib import Path
from random import Random
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/generated/resources"
ASSETS = RES / "assets/sakura_bloom"
DATA = RES / "data/sakura_bloom"
MOD = "sakura_bloom:"
# Each family has a material, seal, boss relic, weapon and shrine treasure.
FAMILIES = [
    ("lantern", "lantern_crown", "灯市", "灯芯琥珀", "百灯心", "万灯司", "烛照", "百灯花冠",
     "Lantern", "Wick Amber", "Everflame Heart", "Keeper of a Hundred Lights", "Wickflare", "Votive Blossom Crown",
     ["#3d2534","#873e40","#bf6946","#eba952","#ffe3a1","#fff3cc"]),
    ("moonlit", "tide_pearl", "月泊", "月潮螺片", "沉月珠", "渡月舟师", "分潮", "潮音珠",
     "Moonlit", "Tidal Nacre", "Drowned Moon", "Moonwake Ferryman", "Tidepart", "Tidesong Pearl",
     ["#253347","#365668","#428685","#73b9ac","#b9e5d8","#e7f2da"]),
    ("vermilion", "brocade_knot", "朱桥", "朱漆鳞", "赤梁鬼角", "朱梁角将", "绯渡", "百战锦结",
     "Vermilion", "Lacquer Scale", "Crimson Horn", "Horned Bridge Marshal", "Crimson Crossing", "Marshal's Brocade Knot",
     ["#382431","#703046","#aa4347","#de7158","#f1af79","#ffe4a6"]),
    ("ashen", "moon_mirror", "灰阁", "灰羽晶", "无明镜心", "灰羽镜主", "晦羽", "照夜镜",
     "Ashen", "Ashfeather Crystal", "Darkglass Heart", "Ashwing Looking-Glass Lord", "Gloamfeather", "Nightwatch Mirror",
     ["#292538","#49415b","#6b6581","#9d88ac","#ccc5dd","#e8edf0"]),
]
EFFECTS = [
    [("+4 护甲", "+4 armor"), ("+4 最大生命值", "+4 max health")],
    [("+50%% 游泳速度", "+50%% swim speed"), ("+1.5 方块交互距离", "+1.5 block interaction range")],
    [("+3 攻击伤害", "+3 attack damage"), ("+12%% 攻击速度", "+12%% attack speed")],
    [("+8 最大生命值", "+8 max health"), ("+25%% 击退抗性", "+25%% knockback resistance")],
]
BLADE_EFFECTS = [
    ("命中时有 35%% 概率点燃目标 4 秒并回复 1 点生命", "35%% on hit to ignite for 4s and heal 1 health"),
    ("命中时有 35%% 概率减速目标并使自身加速", "35%% on hit to slow the target and gain Speed"),
    ("命中时有 35%% 概率削弱目标并获得短暂力量", "35%% on hit to weaken the target and briefly gain Strength"),
    ("命中时有 35%% 概率施加 3 秒凋零 II", "35%% on hit to inflict Wither II for 3s"),
]
GLYPHS = [[(5,7),(10,7),(8,5),(8,10)],[(5,6),(5,9),(7,11),(10,9)],[(4,9),(7,5),(11,9),(6,9)],
          [(5,8),(8,5),(10,7),(8,10),(5,8)]]
CHARM_NAMES = [f[1] for f in FAMILIES]


def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")


def img(path, canvas):
    path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(path)


def glyph(d, family, color):
    d.line(GLYPHS[family], fill=color)


def sprite(kind, i, p):
    im = Image.new("RGBA", (16,16))
    d = ImageDraw.Draw(im)
    def polygon(pts, color, border=None):
        d.polygon(pts, fill=p[color], outline=p[border] if border is not None else None)
    if kind == "shard":
        shape = [[(2,12),(4,6),(9,4),(12,1),(14,5),(11,11),(6,14)],
                 [(1,10),(4,5),(10,3),(14,6),(12,10),(5,14)],
                 [(2,4),(7,6),(13,2),(14,7),(10,12),(5,14),(2,10)],
                 [(3,14),(4,8),(8,3),(14,1),(12,7),(8,12)]][i]
        polygon(shape,2,0)
        polygon([shape[1],shape[2],shape[3],(8,8)],4)
        d.line([(3,12),(7,10),(11,6)], fill=p[3])
    elif kind == "core":
        polygon([(3,5),(7,1),(12,4),(14,8),(11,13),(6,15),(2,11)],2,0)
        polygon([(4,5),(8,3),(10,6),(8,11),(5,10)],3)
        glyph(d,i,p[5])
        d.line([(9,13),(12,10),(13,7)],fill=p[1])
    elif kind == "seal":
        polygon([(4,2),(11,1),(13,12),(10,14),(4,13),(2,5)],3,0)
        d.line([(4,4),(10,3),(11,11),(5,12),(4,4)],fill=p[1])
        glyph(d,i,p[5])
        d.line([(8,13),(9,15),(11,14)], fill=p[2])
    elif kind == "blade":
        blade = [[(5,10),(9,5),(15,0),(14,5),(8,11)],[(5,10),(11,5),(14,1),(15,4),(12,8),(8,11)],
                 [(5,10),(9,4),(11,5),(15,0),(14,6),(9,11)],[(5,10),(9,5),(13,2),(15,1),(12,6),(13,6),(8,11)]][i]
        polygon(blade,3,0)
        d.line([blade[1],blade[2]], fill=p[5])
        d.line([(6,10),(10,7),(13,4)],fill=p[4])
        d.line([(1,14),(5,10)],fill=p[0],width=3)
        d.line([(1,14),(5,10)],fill=p[2])
        d.line([(3,8),(8,13)],fill=p[1],width=2)
        d.line([(3,8),(8,13)],fill=p[4])
        d.point((1,14), fill=p[5])
    elif i == 0:
        polygon([(1,5),(4,8),(6,3),(8,7),(11,2),(12,7),(15,5),(13,12),(3,13)],3,0)
        for x,y in ((4,8),(7,7),(11,6)): d.rectangle((x,y,x+1,y+1),fill=p[5])
        d.line([(3,11),(8,10),(13,10)],fill=p[1])
    elif i == 1:
        d.arc((2,0,13,12),185,363,fill=p[2],width=2)
        d.ellipse((4,6,12,14),fill=p[2],outline=p[0])
        d.ellipse((6,7,10,11),fill=p[4]); d.point((7,8),fill=p[5])
        d.line([(8,14),(7,15)],fill=p[3])
    elif i == 2:
        for x,y in ((2,3),(7,3),(4,7)):
            polygon([(x,y+3),(x+3,y),(x+6,y+3),(x+3,y+6)],3,0)
            d.point((x+3,y+3),fill=p[1])
        d.line([(6,11),(4,15)],fill=p[3],width=2)
        d.line([(9,11),(12,14)],fill=p[4],width=2)
    else:
        d.line([(9,10),(12,15)], fill=p[1], width=3)
        d.ellipse((1,1,12,12),fill=p[4],outline=p[0])
        d.ellipse((3,3,10,10),fill=p[2],outline=p[1])
        d.line([(4,8),(8,4)], fill=p[5])
        d.point((8,9), fill=p[4])
    return im


def robe(i, p):
    im = Image.new("RGBA",(64,64))
    d = ImageDraw.Draw(im)
    rng = Random(733+i)
    d.rectangle((0,0,31,15),fill=p[1])
    d.rectangle((0,16,55,31),fill=p[1])
    d.rectangle((0,32,63,63),fill=p[2])
    for top,bottom in ((16,31),(33,63)):
        for y in range(top,bottom+1):
            for x in range(64 if top==33 else 56):
                col = p[0] if (x%8==0 and y%5!=0) else p[3] if (x+2*y)%19<2 else im.getpixel((x,y))
                if rng.random()<.08: col=p[1]
                d.point((x,y),fill=col)
    # Face UV, obi belt, crossed collar, sleeves and greaves.
    d.rectangle((8,8,15,15),fill=p[2])
    d.line([(8,10),(11,11),(13,11),(15,10)],fill=p[0])
    for x in (9,14): d.point((x,11),fill=p[5])
    d.line([(9,14),(12,13),(14,14)],fill=p[0])
    d.line([(20,20),(26,26),(27,24)],fill=p[4],width=2)
    d.line([(27,20),(22,26)],fill=p[3],width=2)
    d.rectangle((16,27,39,29),fill=p[0]); d.rectangle((23,27,25,28),fill=p[4])
    d.rectangle((0,29,15,30),fill=p[0]); d.rectangle((40,27,55,28),fill=p[3])
    for y in (40,48,56): d.line([(1,y),(60,y)],fill=p[1])
    if i == 0:
        for x in range(1,39,5): d.rectangle((x,43,x+2,49),fill=p[4])
    return im


def stack(item, lo=1, hi=1):
    entry = {"type":"minecraft:item","name":item}
    if (lo,hi)!=(1,1):
        entry["functions"]=[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":lo,"max":hi}}]
    return entry


def pool(entry, probability=1):
    data = {"rolls":1,"entries":[entry]}
    if probability<1: data["conditions"]=[{"condition":"minecraft:random_chance","chance":probability}]
    return data


def craft(result, pattern, ingredients):
    dump(DATA/f"recipe/{result}.json", {"type":"minecraft:crafting_shaped","category":"equipment","pattern":pattern,
         "key":{letter:{"item":item} for letter,item in ingredients.items()},"result":{"id":MOD+result}})


def generate():
    locales=[{},{}]
    def lang(key,zh,en):
        for strings,text in zip(locales,(zh,en)): strings[key]=text
    for i,(family,charm,cn,shard,core,boss,sword,jewel,en,es,ec,eb,ew,ej,p) in enumerate(FAMILIES):
        names = {"shard":(shard,es),"core":(core,ec),"seal":(cn+"降灵印",en+" Spirit Seal"),
                 "blade":(sword,ew),"charm":(jewel,ej)}
        for kind,(zh,name) in names.items():
            id_ = charm if kind=="charm" else family+"_"+kind
            lang("item.sakura_bloom."+id_,zh,name)
            parent = "minecraft:item/handheld" if kind=="blade" else "minecraft:item/generated"
            dump(ASSETS/f"models/item/{id_}.json",{"parent":parent,"textures":{"layer0":MOD+"item/"+id_}})
            img(ASSETS/f"textures/item/{id_}.png",sprite(kind,i,p))
        for n,pair in enumerate(EFFECTS[i],1): lang(f"item.sakura_bloom.{charm}.effect.{n}",*pair)
        lang(f"item.sakura_bloom.{family}_blade.effect",*BLADE_EFFECTS[i])
        altar=family+"_altar"
        lang("block.sakura_bloom."+altar,cn+"降灵台",en+" Invocation Altar")
        lang(f"entity.sakura_bloom.{family}_guardian",boss,eb)
        img(ASSETS/f"textures/entity/{family}_guardian.png",robe(i,p))
        for face in ("top","side"):
            stone=Image.new("RGBA",(16,16),p[0]); d=ImageDraw.Draw(stone)
            if face=="top":
                for k in (1,3): d.rectangle((k,k,15-k,15-k),outline=p[k])
                glyph(d,i,p[5])
            else:
                d.rectangle((0,2,15,12),fill=p[1])
                d.line((0,3,15,3),fill=p[3]); d.line((0,13,15,13),fill=p[3])
                for k in (-4,4,12): d.line([(k,8),(k+4,5),(k+8,8),(k+4,11),(k,8)],fill=p[2])
            img(ASSETS/f"textures/block/{altar}_{face}.png",stone)
        dump(ASSETS/f"models/block/{altar}.json",{"parent":"minecraft:block/cube_bottom_top","textures":{
            "side":MOD+"block/"+altar+"_side","top":MOD+"block/"+altar+"_top","bottom":"minecraft:block/polished_deepslate"}})
        dump(ASSETS/f"models/item/{altar}.json",{"parent":MOD+"block/"+altar})
        dump(ASSETS/f"blockstates/{altar}.json",{"variants":{"":{"model":MOD+"block/"+altar}}})
        part=MOD+family+"_shard"
        heart=MOD+family+"_core"
        craft(family+"_seal",[" S ","STS"," S "],{"S":part,"T":MOD+"sakura_talisman"})
        craft(charm,[" S ","SCS"," G "],{"S":part,"C":heart,"G":"minecraft:gold_ingot"})
        dump(DATA/f"recipe/{family}_blade.json",{
            "type":"minecraft:smithing_transform","template":{"item":part},"base":{"item":"minecraft:diamond_sword"},
            "addition":{"item":heart},"result":{"id":MOD+family+"_blade"}})
        for recipe, ingredient in ((family+"_seal",part),(charm,heart),(family+"_blade",heart)):
            dump(DATA/f"advancement/recipes/veil/{recipe}.json",{
                "parent":"minecraft:recipes/root","criteria":{"found_material":{
                    "trigger":"minecraft:inventory_changed","conditions":{"items":[{"items":ingredient}]}}},
                "rewards":{"recipes":[MOD+recipe]}})
        dump(DATA/f"loot_table/chests/{family}_sanctum.json",{"type":"minecraft:chest","pools":[
            pool(stack(part,4,7)),pool(stack(MOD+"sakura_talisman",1,3)),pool(stack("minecraft:bread",2,4)),
            pool(stack("minecraft:gold_ingot",1,3),.5),pool(stack(MOD+charm),.25),pool(stack(MOD+"cloud_brocade_pouch"),.22)]})
        dump(DATA/f"loot_table/entities/{family}_guardian.json",{"type":"minecraft:entity","pools":[
             pool(stack(heart,1,2)),pool(stack(MOD+family+"_blade"))]})
    lang("entity.sakura_bloom.veil_sentinel","巡殿傀","Sanctum Sentinel")
    img(ASSETS/"textures/entity/veil_sentinel.png",robe(4,["#302d39","#514656","#74636a","#a78776","#d3b090","#ffe3a6"]))
    lang("ritual.sakura_bloom.hint","在对应分殿点燃祭台四方各两格处的红蜡烛，再持印右键祭台。",
         "Light the four red candles two blocks from the matching sanctum altar, then use the seal on it.")
    for code, zh, en in [
        ("location","此印只回应隙间的对应分殿。","This seal answers only within its Interstice sanctum."),
        ("peaceful","和平难度下，守护者仍在沉睡。","The guardian sleeps on Peaceful difficulty."),
        ("occupied","殿内已有守护者。","A guardian is already present."),
        ("candles","祭台东、南、西、北各两格处都要有点燃的红蜡烛。","Light a red candle two blocks from the altar in each cardinal direction.")]:
        lang("ritual.sakura_bloom."+code,zh,en)
    milestones = [
        ("night_watch",None,"sakura_talisman","樱林夜巡","夜里的樱林会留下樱札。带一张去神社，向巫女问问它的用处。","Grove Night Watch","Bring a sakura talisman to a shrine. Speak with the maiden or make an offering."),
        ("inner_pockets","night_watch","knotwork_pouch","结中藏玉","打开百结锦囊，旋转旅饰并排入锦格。","Room for a Charm","Open a Knotwork Pouch and fit your charms into its woven grid."),
        ("beyond_the_veil","night_watch","kamikakushi","红叶引路","完成一轮巫女委托。挥动红叶扇，踏入永夜的隙间。","A Maple Leaf Opens the Way","Complete a maiden's commission cycle. Wave the maple fan to cross into the Interstice."),
        ("four_candles","beyond_the_veil","moonlit_seal","四烛降灵","在对应分殿点燃四方红烛，将降灵印献于中央祭台。","The Four Vigil Lights","Light the four red candles at a matching sanctum, then offer its seal at the central altar."),
        ("keeper_falls","four_candles","ashen_core","殿前破晓","击败一位分殿守护者，取其核心制作旅饰或强化钻石剑。","Dawn at the Sanctum","Defeat a sanctum guardian. Use its heart to craft a relic or reforge a diamond sword."),
    ]
    for key,parent,icon,cn,desc,en,edesc in milestones:
        name = "advancements.sakura_bloom."+key
        lang(name,cn,en)
        lang(name+".desc",desc,edesc)
        display={"icon":{"id":MOD+icon},"title":{"translate":name},"description":{"translate":name+".desc"},
                 "frame":"challenge" if key=="keeper_falls" else "task","announce_to_chat":key=="keeper_falls"}
        ids = [f[0]+"_seal" for f in FAMILIES] if key=="four_candles" else [icon]
        criteria = {"found":{"trigger":"minecraft:inventory_changed","conditions":{"items":[{"items":[MOD+x for x in ids]}]}}}
        if key=="keeper_falls":
            criteria = {f[0]:{"trigger":"minecraft:player_killed_entity","conditions":{"entity":{"type":MOD+f[0]+"_guardian"}}} for f in FAMILIES}
        adv={"display":display,"criteria":criteria,"requirements":[list(criteria)]}
        if parent: adv["parent"]=MOD+"journey/"+parent
        else: display["background"]="minecraft:textures/block/cherry_planks.png"
        dump(DATA/f"advancement/journey/{key}.json",adv)
    for locale,strings in zip(("zh_cn","en_us"),locales):
        dump(ROOT/f"tools/adventure_lang/combat/{locale}.json",strings)


if __name__ == "__main__":
    generate()
