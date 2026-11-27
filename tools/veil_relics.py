"""Generate the interstice's four ritual sets, rewards and original pixel art."""
from pathlib import Path
import json
from interstice_art import sprite, guardian_skin, altar_skin

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


def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")


def img(path, canvas):
    path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(path)


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
        img(ASSETS/f"textures/entity/{family}_guardian.png",guardian_skin(i,p))
        for face in ("top","side"):
            stone=altar_skin(i,face)
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
        # Keep unopened chests in pre-update worlds readable; new caches live in ordinary buildings.
        dump(DATA/f"loot_table/chests/{family}_sanctum.json", {"type":"minecraft:chest","pools":[
            pool({"type":"minecraft:loot_table","value":MOD+"chests/"+family+"_wayhouse"})]})
        dump(DATA/f"loot_table/chests/{family}_wayhouse.json",{"type":"minecraft:chest","pools":[
            pool(stack(MOD+"sakura_talisman",1,3)),pool(stack("minecraft:bread",2,4)),
            pool(stack("minecraft:gold_ingot",1,3),.5),pool(stack(MOD+charm),.25),pool(stack(MOD+"cloud_brocade_pouch"),.22)]})
        dump(DATA/f"loot_table/entities/{family}_guardian.json",{"type":"minecraft:entity","pools":[
             pool(stack(heart,1,2)),pool(stack(MOD+family+"_blade"))]})
    lang("entity.sakura_bloom.veil_sentinel","巡殿傀","Sanctum Sentinel")
    img(ASSETS/"textures/entity/veil_sentinel.png",guardian_skin(4,["#302d39","#514656","#74636a","#a78776","#d3b090","#ffe3a6"]))
    lang("ritual.sakura_bloom.hint","双手空着点亮分殿祭台四周的花见灯，再持印右键祭台。",
         "With both hands empty, light the four Hanami Lanterns around the matching altar, then use the seal on it.")
    for code, zh, en in [
        ("location","此印只回应隙间的对应分殿。","This seal answers only within its Interstice sanctum."),
        ("peaceful","和平难度下，守护者仍在沉睡。","The guardian sleeps on Peaceful difficulty."),
        ("occupied","殿内已有守护者。","A guardian is already present."),
        ("lamps","请双手空着，点亮祭台四周的四盏花见灯。","Light all four Hanami Lanterns by hand.")]:
        lang("ritual.sakura_bloom."+code,zh,en)
    milestones = [
        ("night_watch",None,"sakura_talisman","樱林夜巡","夜里的樱林会留下樱札。带一张去神社，向巫女问问它的用处。","Grove Night Watch","Bring a sakura talisman to a shrine. Speak with the maiden or make an offering."),
        ("inner_pockets","night_watch","knotwork_pouch","结中藏玉","打开百结锦囊，旋转旅饰并排入锦格。","Room for a Charm","Open a Knotwork Pouch and fit your charms into its woven grid."),
        ("beyond_the_veil","night_watch","kamikakushi","红叶引路","完成一轮巫女委托。挥动红叶扇，踏入永夜的隙间。","A Maple Leaf Opens the Way","Complete a maiden's commission cycle. Wave the maple fan to cross into the Interstice."),
        ("four_candles","beyond_the_veil","moonlit_seal","四灯降灵","在对应分殿点亮四方花见灯，将降灵印献于中央祭台。","The Four Vigil Lights","Light the four Hanami Lanterns at a matching sanctum, then offer its seal at the central altar."),
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
