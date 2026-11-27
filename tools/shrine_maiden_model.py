"""Author the shrine maiden's YSM/Bedrock geometry, box-UV silk atlas and animation pack.

All parts are expressed in 1/32 block units. The shrine renderer uses this very main.json.
Run python tools/shrine_maiden_model.py to rebuild the assets and portable YSM zip.
"""
from pathlib import Path
from io import BytesIO
from random import Random
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/generated/resources/assets/sakura_bloom"
PACK = ASSETS / "ysm/shrine_maiden"
SKIN = ("#b67e72", "#dca38c", "#edc4aa", "#ffe0c7")
HAIR = ("#322938", "#493447", "#654654", "#86616c")
VERMILION = ("#733446", "#a34854", "#c96365", "#e48a79")
PAPER = ("#ae9b9e", "#d6c6bf", "#ede3d3", "#fff3db")
GOLD = ("#836048", "#bd8a52", "#e7bc78", "#ffdf9b")
PINK = ("#a24f69", "#d28194", "#f0adad", "#ffded1")
INDIGO = ("#443946", "#685265", "#907182", "#be9ba4")
COLORS = {"skin":SKIN,"face":SKIN,"hair":HAIR,"bang":HAIR,"obi":VERMILION,"skirt":VERMILION,
          "coat":PAPER,"sleeve":PAPER,"collar":PAPER,"white":PAPER,"sandal":GOLD,"ribbon":VERMILION,
          "gold":GOLD,"petal":PINK,"plum":INDIGO,"rope":GOLD,"tabi":PAPER,"iris":INDIGO}


class Maiden:
    def __init__(self):
        self.atlas = Image.new("RGBA", (256,256))
        self.pos = [1,1,0]
        self.bones = []
        self.by_name = {}
        self.regions = {}

    def bone(self, name, parent, pivot, rotation=None):
        node={"name":name,"pivot":list(pivot)}
        if parent: node["parent"]=parent
        if rotation: node["rotation"]=rotation
        self.bones.append(node)
        self.by_name[name] = node

    def material(self, kind, size):
        w,h,d = size
        key=kind,w,h,d
        if key in self.regions: return self.regions[key]
        width,height=(w+d)*2,h+d
        x,y,band=self.pos
        if x+width>=256: x,y,band=1,y+band+1,0
        assert y+height<=256, key
        self.pos=[x+width+1,y,max(band,height)]
        self.regions[key]=[x,y]
        # Paint the six faces locally so the collar, skirt hem and eyes land on the intended polygons.
        faces=[(d,0,w,d,"top"),(d+w,0,w,d,"bottom"),(0,d,d,h,"side"),
               (d,d,w,h,"front"),(d+w,d,d,h,"side"),(2*d+w,d,w,h,"back")]
        for a,b,s,t,face in faces:
            tile=face_tile(kind,s,t,face)
            self.atlas.paste(tile,(x+a,y+b))
        return [x,y]

    def box(self,bone,origin,size,material):
        assert min(size)>0
        self.by_name[bone].setdefault("cubes",[]).append(dict(origin=list(origin),size=list(size),uv=self.material(material,size)))

    def geometry(self,identifier,bones):
        return {"format_version":"1.12.0","minecraft:geometry":[{
            "description":{"identifier":"geometry.sakura_bloom."+identifier,"texture_width":256,"texture_height":256,
                           "visible_bounds_width":4,"visible_bounds_height":5,"visible_bounds_offset":[0,1.5,0]},
            "bones":bones}]}


def blossom(draw,x,y,palette):
    a,b,c,d=palette
    for px,py in ((0,-1),(-1,0),(1,0),(-1,1),(1,1)):
        draw.point((x+px,y+py),fill=c)
    draw.point((x,y),fill=d)


def face_tile(kind,w,h,face):
    palette=COLORS[kind]
    dark,shade,base,light=palette
    im=Image.new("RGB",(w,h),shade if face in ("bottom","side") else base)
    d=ImageDraw.Draw(im)
    rng=Random(sum(map(ord,kind+face))*31+w*17+h)
    if kind in ("hair","bang"):
        d.rectangle((0,0,w-1,h-1),fill=shade)
        for x in range(0,w,2):
            y=rng.randrange(max(1,h//3))
            d.line((x,y,x,max(y,h-rng.randrange(1,max(h//2,2)))),fill=rng.choice((dark,base)))
        if h>4: d.line((1,h//3,min(w-1,3),h//3),fill=light)
    elif kind=="face" and face=="front":
        d.rectangle((0,0,w-1,2),fill=HAIR[1])
        for x in (2,w-5):
            d.line((x,4,x+2,4),fill="#754754")
            d.rectangle((x,6,x+2,8),fill="#fff1dc")
            d.line((x,6,x+2,6),fill=HAIR[0])
            d.point((x+1,7),fill="#5d4051"); d.point((x+1,8),fill="#b57b77");d.point((x+2,7),fill="#ffe5c4")
        d.line((1,9,3,9),fill="#e8a9a0")
        d.line((w-4,9,w-2,9),fill="#e8a9a0")
        d.point((w//2,10),fill="#d09481"); d.line((w//2-1,h-2,w//2,h-2),fill="#b97c78")
    elif kind in ("skirt","obi","ribbon","sleeve","coat","tabi","collar"):
        for k in range(w*h//22):
            x=rng.randrange(w);y=rng.randrange(h)
            d.line((x,y,x,min(h-1,y+1)),fill=shade if face!="side" else base)
        if kind in ("skirt","sleeve","ribbon") and face not in ("bottom","top"):
            for x in range(1,w,4):
                d.line((x,h//3,x,h-3),fill=shade)
            d.line((0,h-2,w-1,h-2),fill=GOLD[2] if kind=="skirt" else VERMILION[1])
            if w>=5 and h>=6:
                blossom(d,w-3,h-5,PINK if kind!="ribbon" else GOLD)
        if kind=="obi":
            d.line((0,h//2,w-1,h//2),fill=GOLD[2]); d.line((0,0,w-1,0),fill=dark)
        if kind=="coat" and face=="front":
            for k in range(min(w,h)):
                d.line((k,k,k,k+1),fill=VERMILION[1])
                if k < w//2: d.line((w-k-1,k,w-k-1,k+1),fill=VERMILION[2])
        if kind=="collar" and face in ("front","back"):
            d.line((0,0,w-1,0),fill=VERMILION[2])
    else:
        d.line((0,0,w-1,0),fill=light); d.line((0,h-1,w-1,h-1),fill=shade)
        if kind in ("rope","sandal"):
            for k in range(-h,w,4): d.line((k,0,k+h,h),fill=shade)
    return im


def build():
    m=Maiden()
    bone,box=m.bone,m.box
    bone("Root",None,(0,0,0))
    bone("Body","Root",(0,24,0))
    bone("Head","Body",(0,43,0))
    box("Body",(-6,23,-4),(12,20,8),"coat")
    box("Body",(-6.5,22,-4.5),(13,5,9),"obi")
    box("Body",(-2,22.7,-5.3),(4,3,1),"gold")
    box("Body",(-3,22.2,4.5),(6,5,3),"ribbon")
    box("Head",(-6,43,-5),(12,13,11),"face")
    box("Head",(-6.5,55,-5.4),(13,3,12),"hair")
    box("Head",(-6.5,43,4.5),(13,13,3),"hair")
    for side,sign in (("Left",1),("Right",-1)):
        def span(x,w): return x if sign==1 else -x-w
        # Long divided bangs leave the eyebrows and eyes readable from a distance.
        box("Head",(span(4.4,2),46,-5.6),(2,10,3),"bang")
        box("Head",(span(2,3),52.6,-5.6),(3,4,2),"bang")
        box("Head",(span(.1,2),54,-5.7),(2,3,2),"bang")
        bone(side+"Lock","Head",(sign*6,50,2),[1,0,sign*4])
        box(side+"Lock",(span(5,3),37,1),(3,14,4),"hair")
        box(side+"Lock",(span(5.4,2),34.5,2),(2,4,3),"hair")
        # Bunched sleeves hung from narrow shoulders, embroidered long cuffs.
        arm=side+"Arm"; fore=side+"ForeArm"
        bone(arm,"Body",(sign*6,41,0),[0,0,sign*5])
        box(arm,(span(5.8,4),32,-2),(4,10,5),"coat")
        box(arm,(span(5.25,5),31.5,-2.6),(5,4,6),"collar")
        bone(fore,arm,(sign*8,33,0))
        box(fore,(span(5.4,6),25,-3.2),(6,9,7),"sleeve")
        box(fore,(span(6.25,4),23,-2),(4,3,4),"skin")
        bone(side+"HandLocator",fore,(sign*8,23,0))
        leg=side+"Leg"
        bone(leg,"Root",(sign*3.5,24,0))
        box(leg,(span(.3,6),4,-2.8),(6,20,6),"skirt")
        box(leg,(span(.55,6),2,-2.9),(6,4,6),"tabi")
        box(leg,(span(.2,7),0,-4.3),(7,2,8),"sandal")
        box(leg,(span(1.5,4),1.6,-3.5),(4,1,2),"plum")
        bone(side+"Hakama","Body",(sign*4,24,0),[0,0,sign*3])
        box(side+"Hakama",(span(.1,7),5,-4.1),(7,18,9),"skirt")
        box(side+"Hakama",(span(4,2),6,-4.65),(2,16,1),"ribbon")
        bone(side+"ObiTail","Body",(sign*2.2,24,4.5),[5,0,sign*8])
        box(side+"ObiTail",(span(1,3),12,4.7),(3,11,2),"ribbon")
    bone("BackHair","Head",(0,51,5),[4,0,0])
    box("BackHair",(-5,32,5.5),(10,20,4),"hair")
    box("BackHair",(-3,28,6.2),(6,5,3),"hair")
    box("Head",(-4,48,6.5),(8,3,2),"ribbon")
    bone("HairPin","Head",(5,53,1),[0,0,-14])
    box("HairPin",(4,53,-.5),(5,1,1),"gold")
    box("HairPin",(7,52,0),(2,3,2),"petal")
    box("HairPin",(6.5,52.7,-.5),(3,2,2),"petal")
    box("HairPin",(7.3,53,-.8),(1,1,1),"gold")
    for i in range(2):
        box("HairPin",(7.6+i,49-i,0),(1,3+i,1),"rope")
        box("HairPin",(7.35+i,48-i,-.2),(1,2,1),"petal")
    return m


def anim(length,bones,loop=True):
    return {"loop":loop,"animation_length":length,"bones":{n:{"rotation":r} for n,r in bones.items()}}


def save():
    m=build()
    files={}
    def put(path,data):
        files[path]=(json.dumps(data,ensure_ascii=False,indent=2)+"\n").encode("utf8")
    prop={"height_scale":.5,"width_scale":.5,"free":True,"extra_animation":{"extra0":"祈愿 · Prayer"}}
    put("ysm.json",{"spec":2,"metadata":{"name":"守樱巫女 · Shrine Keeper","authors":[{"name":"Sakura Bloom","role":"Model & art"}],
                "license":{"type":"All Rights Reserved"},"tips":"樱枝簪、绯袴与组纽的神社守望者。"},
                    "properties":prop,"files":{"player":{"model":{"main":"models/main.json","arm":"models/arm.json"},
                    "animation":{"main":"animations/main.animation.json"},"texture":["textures/keeper.png"]}}})
    put("models/main.json",m.geometry("shrine_maiden",m.bones))
    arms=[]
    for b in m.bones:
        if any(s in b["name"] for s in ("Arm","HandLocator")):
            arm=dict(b)
            if arm.get("parent")=="Body": del arm["parent"]
            arms.append(arm)
    put("models/arm.json",m.geometry("shrine_maiden_arms",arms))
    walk={};run={}
    for side,s in (("Left",1),("Right",-1)):
        for part,mag in (("Leg",24),("Arm",-16),("Hakama",9),("ObiTail",-5)):
            walk[side+part]=[f"math.sin(query.anim_time * 480) * {mag*s}",0,0]
            run[side+part]=[f"math.sin(query.anim_time * 620) * {mag*s*1.45}",0,0]
    idle={"Body":["math.sin(query.anim_time * 125) * 0.9",0,0],"Head":["query.target_x_rotation","query.target_y_rotation",0],
          "BackHair":["math.sin(query.anim_time * 95) * 2",0,0]}
    pray={"Body":[8,0,0],"Head":[14,0,0],"RightArm":[-44,5,12],"LeftArm":[-44,-5,-12],
          "RightForeArm":[-38,22,18],"LeftForeArm":[-38,-22,-18]}
    put("animations/main.animation.json",{"format_version":"1.8.0","animations":{
        "idle":anim(3,idle),"gui":anim(3,idle),"walk":anim(1,walk),"run":anim(1,run),
        "sneak":anim(2,pray),"extra0":anim(3,pray),
        "swing_hand":anim(.35,{"RightArm":{"0.0":[0,0,0],"0.12":[-58,12,0],"0.35":[0,0,0]}},False)}})
    image=BytesIO();m.atlas.save(image,"PNG")
    files["textures/keeper.png"]=image.getvalue()
    for path,data in files.items():
        f=PACK/path
        f.parent.mkdir(parents=True,exist_ok=True);f.write_bytes(data)
    target=ROOT/"build/libs/sakura-bloom-shrine-maiden-ysm.zip"
    target.parent.mkdir(parents=True,exist_ok=True)
    with ZipFile(target,"w") as z:
        for path,data in sorted(files.items()):
            entry=ZipInfo(path,(2020,1,1,0,0,0));entry.compress_type=ZIP_DEFLATED
            z.writestr(entry,data)
    print(f"{len(m.bones)} bones, {sum(len(b.get('cubes',[])) for b in m.bones)} cubes, {target}")


if __name__=="__main__":
    save()
