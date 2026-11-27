"""Original 32-pixel silk, lacquer, pearl and metal inventory art for the Interstice."""
from random import Random
from PIL import Image, ImageDraw

GOLD=("#654740","#97704d","#c89761","#f0c988","#fff1bb")
CORD=("#573644","#954959","#cb6570","#eba58f")
SILK=("#876b75","#c0a49b","#e8ccb0","#f7e2c2","#fff2d8")
FAMILIES=((("#492d35","#86513e","#b97948","#d99d53","#f5c976","#fff0bf")),
          ("#253d4b","#3b586c","#4b8589","#71aaa6","#aed6cb","#e5f3dc"),
          ("#492739","#703748","#a74750","#c7675e","#ed9a78","#ffd3a1"),
          ("#302c40","#474156","#655c78","#8a839b","#b4a9bf","#e2dde0"))


def flower(d,x,y,p):
    for dx,dy in ((0,-2),(-2,-1),(2,-1),(-1,1),(1,1)):
        d.rectangle((x+dx,y+dy,x+dx+1,y+dy+1),fill=p[-2])
    d.point((x,y),fill=p[-1])


def cord(d,points):
    d.line(points,fill=GOLD[0],width=3)
    d.line(points,fill=GOLD[2])
    for x,y in points[1:-1]: d.point((x,y),fill=GOLD[4])


def crest(d,x,y,i,p):
    if i==0: flower(d,x,y,p)
    if i==1:
        d.arc((x-4,y-4,x+3,y+2),40,210,fill=p[-2],width=2)
        d.line((x-2,y+3,x+2,y+3),fill=p[-1])
    if i==2:
        d.line([(x-4,y+1),(x-1,y-2),(x+2,y-2),(x+4,y+1)],fill=p[-1],width=2)
        d.line((x-3,y-4,x+3,y-4),fill=p[-2]);d.point((x,y+1),fill=p[-2])
    if i==3:
        d.line([(x-3,y),(x,y-3),(x+3,y),(x,y+3),(x-3,y)],fill=p[-1]);d.point((x,y),fill=p[-2])


def sprite(kind,i,p=None):
    p=FAMILIES[i]
    im=Image.new("RGBA",(32,32))
    d=ImageDraw.Draw(im)
    def polygon(points,v=2,outline=0):
        d.polygon(points,fill=p[v],outline=p[outline])
    if kind=="shard":
        if i==0:
            polygon([(4,23),(6,14),(12,7),(21,4),(27,8),(26,18),(18,25),(10,28)])
            polygon([(8,17),(13,9),(21,6),(17,17),(10,22)],4,3)
            polygon([(11,24),(18,19),(25,10),(23,18),(17,23)],3,2)
            d.line([(13,17),(17,14),(17,11),(19,9)],fill="#965643",width=2)
        elif i==1:
            polygon([(3,20),(6,12),(13,8),(22,4),(28,7),(25,14),(18,21),(8,27)])
            for k in range(3):
                d.arc((5+k*3,7+k,30-k*2,26-k),45,202,fill=(p[4],p[5],"#d6b9c1")[k],width=2)
        elif i==2:
            polygon([(5,8),(13,9),(22,4),(27,3),(26,16),(19,24),(10,28),(5,24)])
            for k in range(3):
                d.line([(7,13+3*k),(13,15+3*k),(22-k,9+3*k)],fill=p[3+k%2],width=2)
            d.line([(7,8),(13,10),(24,5)],fill=p[5])
        else:
            polygon([(3,28),(7,15),(16,6),(28,2),(25,12),(20,14),(21,15),(14,22)])
            for k in range(3):
                d.line([(8+3*k,23-4*k),(10+3*k,17-3*k),(16+3*k,11-3*k)],fill=p[4])
            d.line([(4,28),(12,19),(22,8)],fill=p[5])
    elif kind=="core":
        if i==0:
            polygon([(8,26),(4,20),(5,13),(10,16),(11,7),(16,11),(22,2),(21,13),(25,11),(27,18),(23,25),(16,29)],3)
            polygon([(10,23),(10,18),(13,20),(16,12),(17,18),(20,16),(20,21),(16,26)],5,4)
        elif i==1:
            d.ellipse((4,4,27,27),fill=p[2],outline=p[0],width=2)
            d.ellipse((7,5,23,22),fill=p[3])
            d.arc((6,5,25,25),160,288,fill=p[5],width=3);crest(d,16,17,i,p)
            d.arc((9,12,28,26),350,120,fill=GOLD[3],width=2)
        elif i==2:
            polygon([(4,25),(7,18),(13,17),(19,13),(23,7),(26,2),(28,11),(24,18),(18,24),(10,29)],3)
            for x,y in ((10,23),(15,19),(20,14)):d.line((x-1,y-2,x+3,y+2),fill=p[1],width=2)
            d.line([(6,24),(14,19),(20,15),(26,6)],fill=p[5])
        else:
            polygon([(4,17),(8,8),(18,3),(26,9),(28,18),(21,26),(11,28)],2)
            d.polygon([(7,16),(10,10),(18,5),(24,10),(15,17)],fill=p[4])
            d.line([(10,24),(14,17),(18,18),(23,11)],fill=p[5],width=2)
        d.line([(7,27),(9,26),(12,27)],fill=GOLD[2])
    elif kind=="seal":
        # Lacquered seal handle, laced through the top, ivory face with a family crest.
        cord(d,[(19,6),(18,3),(22,1),(26,4),(26,8),(23,11)])
        polygon([(6,9),(18,5),(25,10),(23,22),(18,26),(5,23),(4,17)],3)
        d.polygon([(7,10),(17,7),(22,11),(19,17),(7,20)],fill=p[4],outline=p[1])
        d.polygon([(8,11),(17,9),(20,11),(17,16),(8,17)],fill=p[1])
        crest(d,14,13,i,p)
        for k in range(3):d.line((7+k,22,17+k,24-k),fill=GOLD[k+1])
        d.line([(22,18),(25,23),(24,27)],fill=CORD[0],width=3)
        d.line([(24,23),(25,28),(27,29)],fill=CORD[2],width=2)
    elif kind=="blade":
        points=(
            [(8,21),(14,13),(20,9),(29,1),(28,8),(22,15),(12,24)],
            [(8,21),(14,14),(21,11),(26,6),(28,1),(30,7),(27,13),(21,18),(12,24)],
            [(8,21),(14,12),(19,11),(18,8),(27,4),(30,1),(27,10),(22,16),(13,24)],
            [(8,21),(13,14),(16,14),(16,11),(21,9),(29,2),(27,9),(24,12),(25,14),(19,18),(12,24)])[i]
        polygon(points,3)
        d.line([(10,22),(15,17),(21,13),(27,6)],fill=p[5],width=2)
        d.line([(11,20),(16,14),(20,12)],fill=p[2])
        d.line((3,28,10,21),fill=p[0],width=5);d.line((3,28,10,21),fill=p[1],width=3)
        for k in (0,2,4):d.line((3+k,27-k,5+k,29-k),fill=GOLD[2])
        d.line([(6,18),(8,18),(13,24),(15,24)],fill=GOLD[0],width=4)
        d.line([(6,18),(8,18),(13,24),(15,24)],fill=GOLD[3],width=2)
        d.point((8,18),fill=GOLD[4]);d.line([(3,28),(2,30),(5,30)],fill=CORD[2])
    elif i==0:
        polygon([(2,7),(8,14),(12,5),(16,12),(22,3),(25,13),(29,7),(27,23),(16,26),(5,24)],3)
        d.line([(5,19),(15,22),(27,19)],fill=GOLD[4],width=2)
        for x,y in ((7,14),(15,16),(24,12)):flower(d,x,y,("#9a4854","#b65d69","#e79da0","#ffecd0"))
        cord(d,[(5,23),(4,26),(7,28)]);d.point((7,29),fill=p[3])
    elif i==1:
        cord(d,[(16,11),(8,8),(7,4),(11,2),(15,5),(17,9),(24,5),(26,6),(23,10),(17,12)])
        d.ellipse((7,10,25,27),fill=p[2],outline=p[0],width=2)
        d.ellipse((10,11,22,23),fill=p[3]);d.arc((9,11,24,26),190,275,fill=p[5],width=2)
        crest(d,16,19,1,p);cord(d,[(10,23),(16,27),(22,24)]);d.line((16,27,15,30),fill=CORD[2],width=2)
    elif i==2:
        for x,y in ((4,6),(15,6),(9,12)):
            pts=[(x,y+5),(x+5,y),(x+10,y+5),(x+5,y+10),(x,y+5)]
            d.line(pts,fill=p[0],width=5);d.line(pts,fill=p[3],width=3);d.line(pts,fill=p[5])
        for x,y in ((11,21),(18,21)):
            d.line((x,y,x-2,y+7),fill=p[2],width=4);d.line((x,y+1,x-2,y+4),fill=GOLD[3],width=2)
    else:
        d.line((16,18,24,29),fill=GOLD[0],width=5);d.line((16,18,24,29),fill=GOLD[2],width=3)
        d.ellipse((2,2,25,24),fill=GOLD[2],outline=GOLD[0],width=2)
        d.ellipse((5,5,22,21),fill=p[2],outline=GOLD[4]);d.ellipse((6,6,21,21),fill=p[3])
        d.line([(7,15),(12,9),(16,7)],fill=p[5],width=2);crest(d,16,15,3,p)
        d.line([(23,26),(27,26),(28,30)],fill=CORD[2],width=2)
    return im


def guardian_skin(i,p):
    # Body and limbs retain the humanoid's vanilla UVs; the custom shoulder armour,
    # sashes, ceremonial masks and emblems use independent islands in the 128 atlas.
    im=Image.new("RGBA",(128,128))
    d=ImageDraw.Draw(im)
    rng=Random(807+i)
    def panel(x,y,w,h,base,pattern=False):
        d.rectangle((x,y,x+w-1,y+h-1),fill=base)
        for k in range(w*h//18):
            u,v=x+rng.randrange(w),y+rng.randrange(h)
            d.line((u,v,min(u+1,x+w-1),v),fill=p[2])
        if pattern:
            for v in range(y+3,y+h-2,5):
                for u in range(x+4,x+w-3,7):
                    d.line([(u-2,v),(u,v-2),(u+2,v)],fill=p[3])
    panel(0,0,32,16,p[1]);panel(0,16,56,16,p[1],True)
    panel(0,32,64,32,p[1],True)
    for x,y,w,h in ((0,16,16,16),(40,16,16,16)):
        for v in (y+7,y+10,y+13):
            d.line((x,v,x+w-1,v),fill=p[0]);d.line((x+1,v-1,x+w-2,v-1),fill=p[3])
    d.rectangle((8,8,15,15),fill=p[2])
    d.line([(8,10),(11,11),(13,11),(15,10)],fill=p[0],width=2)
    for u in (9,13): d.line((u,11,u+1,11),fill=p[5])
    d.point((11,13),fill=p[3]); d.line((10,15,13,15),fill=p[0])
    d.line([(20,20),(26,25),(27,23)],fill=p[4],width=2)
    d.line([(27,20),(21,26)],fill=p[3],width=2)
    d.rectangle((16,27,39,30),fill=p[0]);d.line((16,28,39,28),fill=GOLD[2])
    # Separate hero model components, the sentinel shares the same lamellar armour panels.
    panel(64,0,48,25,p[2],True)
    for v in (6,13,21):d.line((64,v,109,v),fill=p[4])
    panel(64,28,48,22,p[1])
    for u in range(65,109,4):
        d.line((u,29,u-1,47),fill=p[2])
    for u in (74,89):crest(d,u,41,i%4,p)
    panel(64,52,57,22,p[2],True)
    for y in (58,62,66):
        d.line((64,y,120,y),fill=p[0]);d.line((66,y-1,118,y-1),fill=p[3])
    if i==0:
        panel(0,33,40,19,p[1]);d.rectangle((10,43,19,50),fill=p[4])
        for x in (11,15,19):d.line((x,43,x,50),fill=p[2])
        d.line((10,50,19,50),fill=GOLD[3]);d.line((13,45,13,47),fill=p[5])
    elif i==1:
        for k in range(5):
            d.line([(k*8,46),(k*8+6,53),(k*8+8,60)],fill=GOLD[2])
        d.line((0,59,59,59),fill=p[4])
    elif i==2:
        d.rectangle((4,37,15,45),fill=GOLD[2]);d.line((4,41,15,41),fill=p[1])
    elif i==3:
        for x in range(2,17,3):d.line((x,34,x-3,49),fill=p[4])
    return im


def talisman():
    im=Image.new("RGBA",(32,32));d=ImageDraw.Draw(im)
    pts=[(10,2),(26,5),(22,24),(18,29),(3,26)]
    d.polygon(pts,fill=SILK[2],outline=CORD[0])
    d.polygon([(12,4),(24,6),(20,24),(18,27),(6,25)],fill=SILK[4])
    d.line([(11,6),(22,8),(22,9)],fill=CORD[2]); d.line([(7,22),(18,24),(20,22)],fill=CORD[1])
    d.line([(11,12),(20,14),(17,16),(13,15),(14,19)],fill=CORD[1],width=2)
    d.line((15,10,12,20),fill=CORD[2]);flower(d,20,7,("#a65d72","#ad7887","#e4a1a1","#ffe3bf"))
    cord(d,[(20,5),(22,1),(26,2),(25,5)])
    return im


def pouch(cloud):
    p=FAMILIES[1 if cloud else 2]
    im=Image.new("RGBA",(32,32));d=ImageDraw.Draw(im)
    outline=[(8,4),(11,3),(15,6),(18,3),(23,4),(21,10),(25,15),(28,22),
             (27,27),(22,29),(10,29),(5,27),(4,22),(7,15),(11,10)]
    d.polygon(outline,fill=p[2],outline=p[0])
    d.polygon([(10,11),(18,10),(22,14),(24,22),(21,27),(9,26),(7,22)],fill=p[3])
    for x in range(8,25,4):
        d.line([(13+(x-13)//2,12),(x,17),(x-1,22)],fill=p[2])
    if cloud:
        for x,y in ((11,21),(19,17),(18,23)):d.arc((x-3,y-3,x+3,y+1),180,352,fill=p[5],width=1)
    else:
        for x,y in ((12,18),(18,22)):flower(d,x,y,(p[0],p[2],"#efb0a4",GOLD[4]))
    d.line([(8,26),(14,28),(22,27),(25,25)],fill=GOLD[2])
    cord(d,[(9,9),(13,11),(21,9),(25,7),(27,7),(27,10),(23,11),(20,10),(26,13),(27,18)])
    d.line((27,16,28,21),fill=GOLD[2],width=2)
    return im

def altar_skin(i,face):
    p=FAMILIES[i]
    im=Image.new("RGB",(32,32),p[0]);d=ImageDraw.Draw(im)
    if face=="top":
        for j,col in ((1,p[2]),(3,GOLD[1]),(6,p[1])):
            d.rectangle((j,j,31-j,31-j),outline=col)
        for x in (5,26):
            for y in (5,26):d.rectangle((x-1,y-1,x+1,y+1),fill=GOLD[2])
        d.ellipse((7,7,24,24),fill=p[1],outline=p[3])
        d.ellipse((9,9,22,22),outline=GOLD[1])
        crest(d,16,16,i,p)
    else:
        for a,b,col in ((1,3,p[2]),(4,5,p[1]),(7,24,p[1]),(26,27,p[3]),(29,30,p[1])):
            d.rectangle((0,a,31,b),fill=col)
        for x in range(-8,40,8):
            d.line([(x,16),(x+4,11),(x+8,16),(x+4,21),(x,16)],fill=p[2])
        d.ellipse((9,9,22,22),fill=p[0],outline=GOLD[1])
        crest(d,16,16,i,p)
    return im


def maple_fan():
    im=Image.new("RGBA",(32,32));d=ImageDraw.Draw(im)
    piv=(10,23);edge="#793a49"; scarlet=("#8e354b","#a7434e","#c9544c","#db7158","#ee966a")
    pts=[(4,24),(1,20),(6,20),(1,14),(6,15),(4,8),(10,11),(9,3),(14,9),
         (15,2),(18,9),(24,0),(22,10),(30,5),(27,12),(31,11),(27,17),
         (31,19),(23,22),(26,25),(18,24),(16,27),(11,25),(6,27)]
    d.polygon(pts,fill=scarlet[2],outline=edge)
    for k,(tx,ty) in enumerate(((2,15),(5,9),(10,5),(15,4),(23,2),(29,7),(29,13),(29,19),(24,24))):
        d.polygon([piv,(tx-2,ty+3),(tx,ty),(tx+1,ty+4)],fill=scarlet[1+k%4])
        mid=((10+tx)//2,(23+ty)//2)
        d.line([piv,mid,(tx,ty+1)],fill="#a9464b")
        d.line([(mid[0]+1,mid[1]-1),(tx,ty+1)],fill="#f4b578")
        d.line([(mid[0],mid[1]),(mid[0]+3,mid[1]-1)],fill="#e58a65")
    for x,y in ((6,18),(16,13),(22,17)):
        d.point((x,y),fill="#ffe3a1")
    d.line((3,30,11,22),fill=GOLD[0],width=4)
    d.line((4,29,10,23),fill=GOLD[2],width=2)
    d.rectangle((9,22,11,24),fill=GOLD[1]);d.point((10,22),fill=GOLD[4])
    d.line([(4,29),(6,31),(10,30),(11,27)], fill=CORD[2])
    return im


def offering_wood(part):
    """Red lacquer, inset hardware and a plum crest on the offertory chest."""
    slotted = part == "slot"
    palette = (["#382d32","#453239","#533c40","#654749"] if slotted else
               ["#633c41","#764247","#854b49","#95594f","#a66958"])
    image = Image.new("RGB", (32, 32), palette[1])
    d = ImageDraw.Draw(image)
    rng = Random(2036)
    for y in range(32):
        for _ in range(rng.randint(2,5)):
            x = rng.randrange(32)
            d.line((x,y,min(31,x+rng.randint(2,8)),y),fill=rng.choice(palette[1:]))
    if slotted:
        return image
    for y in (0,14,29):
        d.line((0,y,31,y),fill=palette[0])
        d.line((0,y+1,31,y+1),fill=palette[-1])
    if part == "side":
        for x in (3,28):
            d.line((x,1,x,30),fill=palette[0],width=2)
            for y in (5,17,27):
                d.rectangle((x-1,y,x+1,y+2),fill=GOLD[0])
                d.point((x,y),fill=GOLD[3])
        # The model maps its body onto the lower part of this face.
        d.ellipse((10,15,21,26),outline=GOLD[1])
        d.ellipse((11,16,20,25),outline=GOLD[2])
        flower(d,16,21,GOLD)
    return image
