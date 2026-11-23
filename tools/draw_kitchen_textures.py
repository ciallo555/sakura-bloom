"""Draw the kitchen set as original 16-pixel sprites and block surfaces."""

from random import Random

from PIL import Image, ImageDraw

from functools import partial
from draw_textures import CUISINE_TEXTURES, PROJECT, glyph, save, texture as draw_texture

texture = partial(draw_texture, root=CUISINE_TEXTURES)


LEAF = ("#263c25", "#436636", "#63924a", "#8eae5b", "#b5c97e")
TOMATO = ("#67292d", "#963b38", "#c34e40", "#eb6d4e", "#f1a16c")
GRAIN = ("#6e5340", "#ad814e", "#d3aa64", "#eed28d", "#fae3ad")
RICE = ("#a59b91", "#cac1ba", "#e4d9c9", "#f2e9d9", "#fff5dc")
EGG = ("#9c582c", "#c98239", "#e7a947", "#f6c761", "#ffe28c")
SEAWEED = ("#233435", "#344847", "#536247")
CABBAGE = ("#2d4b38", "#416945", "#61854c", "#88a658", "#adc76c", "#d1df8f", "#e6edac")
VINE = ("#2c4933", "#39643d", "#51814a", "#6c9d54", "#8ab663")
TOMATO_SKIN = ("#73302d", "#a13935", "#ce4d3c", "#e96846", "#f6875c", "#ffb289")
PADDY = ("#3b533a", "#536d43", "#71834b", "#96a656", "#bec470")
HUSK = ("#77583b", "#9a7345", "#bf9452", "#dbb568", "#efd48a")


def sprite(rows, colors, x=0, y=0):
    canvas = Image.new("RGBA", (16, 16))
    glyph(ImageDraw.Draw(canvas), x, y, rows, colors)
    return canvas


def ingredients():
    texture("item", "cabbage", sprite((
        "     233     ",
        "   1355432   ",
        "  2456655431 ",
        " 255544565332",
        "2564334564321",
        "2643534654331",
        "2543545543321",
        "2434554333421",
        " 233443334210",
        " 123333332110",
        "  1122322100 ",
        "    011110   ",
    ), CABBAGE, 1, 2))
    texture("item", "tomato", sprite((
        "        9    ",
        "       98    ",
        "    8 978    ",
        "   13888731  ",
        " 145338283211",
        "1554322723321",
        "1443333333221",
        "1433333333221",
        " 233333322210",
        "  1222222210 ",
        "   01111110  ",
        "     0000    ",
    ), TOMATO_SKIN + VINE[:4], 1, 2))
    texture("item", "rice", sprite((
        "       23   ",
        "      342   ",
        "   31 42  2 ",
        "  342 1  341",
        "  42  23141 ",
        "  1  34310  ",
        " 231 421 23 ",
        " 342 10 342 ",
        " 41 23  41  ",
        " 1  341 1   ",
        "    21      ",
    ), ("#a88761", "#baa07d", "#dfc6a1", "#f3ddba", "#fff4d8"), 2, 3))
    texture("item", "cabbage_seeds", sprite((
        "       23   ",
        " 23    321  ",
        "332    10   ",
        " 10  22     ",
        "    331  23 ",
        "    120  321",
        " 22      10 ",
        " 321        ",
        " 10         ",
    ), ("#443334", "#664a42", "#92705a", "#bf9b73"), 2, 4))
    texture("item", "tomato_seeds", sprite((
        "        3   ",
        "   34  341  ",
        "  342  210  ",
        "  210       ",
        "      24    ",
        "  3   432   ",
        " 341  210  3",
        " 420      34",
        " 10       21",
    ), ("#79583a", "#a37a4a", "#c89a60", "#e5ba7a", "#f8dca1"), 2, 3))
    texture("item", "rice_seeds", sprite((
        "         33 ",
        "        342 ",
        "       3421 ",
        "  3   3421  ",
        "  342 210   ",
        "   342      ",
        "    21   23 ",
        "        342 ",
        "       3421 ",
        "      3421  ",
        "      210   ",
    ), ("#615939", "#8a7543", "#b69952", "#d4bb6e", "#efe09b"), 2, 3))
    texture("item", "wheat_noodles", sprite((
        "     443     ",
        "  3434123    ",
        " 43112  43   ",
        "343 343342   ",
        "432431132443 ",
        "432341434132 ",
        " 432122343241",
        "  44344212431",
        "  231123431  ",
        "    112211   ",
    ), ("#86603d", "#ab7843", "#d29b54", "#ebbb71", "#ffe1a6"), 1, 3))


def harvest_sprites():
    cabbages = (
        (
            "  3  4  ",
            " 432343 ",
            " 234332 ",
            "  1221  ",
            "   21   ",
        ),
        (
            "     43     ",
            " 4  454     ",
            " 343453  3  ",
            " 234542344  ",
            "  32343332  ",
            "   133321   ",
            "    0110    ",
        ),
        (
            "     34       ",
            "  3 34543     ",
            " 454455432 33 ",
            " 345445543443 ",
            " 234345433432 ",
            "  2334434331  ",
            "   223333210  ",
            "    1222210   ",
            "      110     ",
        ),
        (
            "      454       ",
            "    45666532    ",
            "   4555655432   ",
            "  325643455431  ",
            " 23564344554314 ",
            " 345444565433332",
            " 234555543343321",
            " 12334443344321 ",
            "  1233334433210 ",
            "   11233332110  ",
            "     011110     ",
        ),
    )
    for age, rows in enumerate(cabbages):
        texture("block", f"cabbage_crop_{age}", sprite(rows, CABBAGE, (4, 2, 1, 0)[age], 16 - len(rows)))

    vines = (
        (
            "   4    ",
            "  433   ",
            " 332  3 ",
            "   2341 ",
            "   32   ",
            "   21   ",
        ),
        (
            "        43  ",
            "      3342  ",
            "  43 32     ",
            "  23431     ",
            "     33  42 ",
            "    2333431 ",
            "  432321    ",
            "   21 31    ",
            "      21    ",
        ),
        (
            "      43      ",
            "     443  4   ",
            "       33443  ",
            "   43233231   ",
            "  3433214     ",
            " 232  2333    ",
            "        32341 ",
            "      2233432 ",
            "  3  343331   ",
            " 34334332     ",
            " 232   31     ",
            "       21     ",
        ),
        (
            "        43      ",
            "   4   343  43  ",
            "   343334234432 ",
            "     33234321   ",
            "  432331333     ",
            " 3443431323433  ",
            " 232432 3213343 ",
            "      3331  232 ",
            "   423432343    ",
            "  344323443231  ",
            " 34221 33234443 ",
            "    12331  4322 ",
            "       321      ",
            "       210      ",
        ),
    )
    for age, rows in enumerate(vines):
        tomato = sprite(rows, VINE, (4, 2, 0, 0)[age], 16 - len(rows))
        draw = ImageDraw.Draw(tomato)
        if age == 2:
            glyph(draw, 10, 7, (" 3 ", "343", " 2 "), HUSK)
            glyph(draw, 4, 10, (" 4  ", "4431", " 32 "), VINE)
        elif age == 3:
            glyph(draw, 2, 8, (" 34 ", "5432", "4321", " 21 "), TOMATO_SKIN)
            glyph(draw, 9, 7, (" 343 ", "54332", "43321", " 221 "), TOMATO_SKIN)
            glyph(draw, 9, 12, (" 34 ", "5432", "4321", " 21 "), TOMATO_SKIN)
            glyph(draw, 3, 7, (" 3 ", "321"), VINE)
            glyph(draw, 10, 6, (" 3 ", "321"), VINE)
            glyph(draw, 10, 11, (" 3 ", "321"), VINE)
        texture("block", f"tomato_crop_{age}", tomato)

    shoots = (
        (
            "        3       ",
            "    3   3   2   ",
            "     3 32  2    ",
            "      221 21    ",
            "      12  1     ",
        ),
        (
            "        4       ",
            "   3    3    3  ",
            "   43   3   43  ",
            "    32  32 321  ",
            " 2   32 32 32   ",
            "  3  23232 3  3 ",
            "   213222232 2  ",
            "    121121211   ",
            "     11 11 1    ",
        ),
        (
            "       4   3    ",
            "       3   43   ",
            "  4   43   32   ",
            "  34  32   32 4 ",
            "   32 32   3242 ",
            "   33232  3232  ",
            " 3  32232 323   ",
            "  3 32332 332 3 ",
            "   23223223232  ",
            "    322312322   ",
            "    121211211   ",
            "     1 1  11    ",
        ),
    )
    for age, rows in enumerate(shoots):
        texture("block", f"rice_crop_{age}", sprite(rows, PADDY, 0, 16 - len(rows)))


def wild_crop_sprites():
    dune = ("#3f5343", "#566d53", "#768965", "#8fa479", "#acb893",
            "#b9a369", "#e0c985", "#f7e4ad")
    texture("block", "wild_cabbage", sprite((
        "         7      ",
        "    7   676  7  ",
        "   676   5  676 ",
        "    53   33  5  ",
        "     3  33   3  ",
        "  3  23332  32  ",
        " 34433233333432 ",
        " 23443434344320 ",
        " 12233233333210 ",
        "   1122222110   ",
        "      1110      ",
    ), dune, 0, 5))

    # Draw each stalk in a 32-pixel canvas so the top joins the same stem below the water.
    for wild in (False, True):
        rice = Image.new("RGBA", (16, 32))
        draw = ImageDraw.Draw(rice)
        stalks = (
            ((3, 3, 4, 8, -1), (7, 7, 6, 5, 1), (11, 11, 11, 9, 1))
            if wild else
            ((3, 4, 3, 8, -1), (6, 6, 5, 4, -1), (10, 9, 9, 2, 1), (12, 12, 12, 8, 1))
        )
        for root, join, neck, top, bend in stalks:
            draw.line([(root, 31), (join, 22), (join, 15), (neck, top + 1)], fill=PADDY[2])
            draw.line([(root, 31), (join - bend, 28), (join - 2 * bend, 23)], fill=PADDY[1])
            draw.line([(join, 24), (join + 2 * bend, 20), (join + 3 * bend, 19)], fill=PADDY[3])
            draw.line([(join, 16), (join, 15), (join - bend, 14), (join - 2 * bend, 13)], fill=PADDY[3])
            draw.line([(join, 21), (join, 14), (neck, top + 3)], fill=PADDY[3])
        for _, _, neck, top, bend in stalks:
            grains = (
                (" 23 ", " 341", " 242", " 132", "  12")
                if wild else (" 34  ", " 343 ", " 2342", " 1242", "  232", "   21")
            )
            colors = ("#625038", "#887047", "#a88958", "#c2a46c", "#dcc38b") if wild else HUSK
            rows = grains if bend > 0 else tuple(row[::-1] for row in grains)
            start = neck - 1 if bend > 0 else neck - len(grains[0]) + 2
            glyph(draw, start, top, rows, colors)
        lower, upper = ("wild_rice_lower", "wild_rice_upper") if wild else ("rice_crop_3", "rice_panicle")
        texture("block", lower, rice.crop((0, 16, 16, 32)))
        texture("block", upper, rice.crop((0, 0, 16, 16)))


def bowl_food(name, rows, colors, ceramic):
    canvas = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(canvas)
    dark, body, trim = ceramic
    draw.polygon([(1, 7), (3, 5), (12, 5), (14, 7), (14, 10), (12, 13), (4, 13), (1, 10)], fill=dark)
    draw.polygon([(2, 9), (5, 10), (11, 10), (13, 8), (12, 11), (10, 12), (5, 12), (3, 11)], fill=body)
    draw.line([(2, 8), (5, 10), (10, 10), (13, 8)], fill=trim)
    glyph(draw, 2, 5, rows, colors)
    texture("item", name, canvas)


def meals():
    lacquer = ("#48272b", "#8f4840", "#b8785b")
    stoneware = ("#343e43", "#667e84", "#a1b7b3")
    bowl_food("steamed_rice", ("   34443   ", " 434233434 ",
                               "343443244432", " 243443432 ", "  1232212  "), RICE, lacquer)
    bowl_food("chawanmushi", ("  1444441  ", " 433422334 ",
                               "444324424434", " 434442343 ", "  1212121  "), EGG + LEAF, stoneware)
    bowl_food("egg_fried_rice", ("  241332   ", " 312343234 ",
                                  "242364332414", " 231242643 ", "  2312224  "), EGG + LEAF, lacquer)
    bowl_food("yakisoba", ("  123332   ", " 235212334 ",
                           "243315343214", " 241331542 ", "  1324211  "), GRAIN + LEAF, stoneware)
    bowl_food("tomato_egg", ("  23266    ", " 233476823 ",
                             " 5678423322", " 478633768 ", "  613378   "), TOMATO + EGG, lacquer)
    onigiri = sprite((
        "     434     ",
        "    34432    ",
        "   3424332   ",
        "  344332433  ",
        " 34244432332 ",
        " 44425674422 ",
        "3343256743332",
        "3343566772322",
        " 23356667322 ",
        "  115566621  ",
        "   11556611  ",
    ), RICE + SEAWEED, 1, 2)
    glyph(ImageDraw.Draw(onigiri), 6, 4, (" 3 ", "432", " 2 "), TOMATO)
    texture("item", "salmon_onigiri", onigiri)
    texture("item", "steamed_pork_roll", sprite((
        "   2343321  ",
        "  234232221 ",
        " 23432301120",
        "234332014320",
        "243232024430",
        "133232025520",
        " 1322221230 ",
        "  00111100  ",
    ), LEAF + ("#b97b5e",), 2, 4))


def steaming_ingredients():
    texture("item", "raw_salmon_onigiri", sprite((
        "       67    ",
        "     75676   ",
        "    304657   ",
        "   3444665   ",
        "  344334450  ",
        " 34443343220 ",
        "3443434332210",
        " 233342222110",
        "  122221110  ",
        "   0011100   ",
    ), ("#a68363", "#c1a382", "#d9bb94", "#efdab4", "#f9ebcc",
        "#ba5c45", "#db8061", "#f5ac86"), 1, 3))
    texture("item", "chawanmushi_mix", sprite((
        "     64    ",
        "    6564   ",
        "  3266413  ",
        " 354434343 ",
        "34544224443",
        "34422133332",
        " 332223322 ",
        "  1222210  ",
    ), ("#995a35", "#be7e3f", "#d89a48", "#eeb360", "#f8cc77", "#b08a6b", "#895c4c"), 2, 4))
    texture("item", "raw_pork_roll", sprite((
        "   12344     ",
        "  23454342   ",
        " 3454543432  ",
        " 34443432672 ",
        "2444343278872",
        " 233432278872",
        "  23322176861",
        "   122217771 ",
        "     011110  ",
    ), CABBAGE[:6] + ("#98614c", "#c58874", "#e5ac8c"), 1, 4))
    texture("item", "unplated_steamed_rice", sprite((
        "      44     ",
        "    344443   ",
        "   43443443  ",
        "  443434433  ",
        " 443434432331",
        " 343432332321",
        "3432423232321",
        " 22332223210 ",
        "   1122110   ",
    ), ("#9c9082", "#c1b3a4", "#dbd0be", "#f1e7d7", "#fff6e7"), 1, 4))
    texture("item", "unplated_chawanmushi", sprite((
        "    34443   ",
        "  344456443 ",
        " 34446474443",
        " 34443444443",
        " 33334443332",
        " 23333333221",
        " 23332222221",
        "  222222211 ",
        "    111110  ",
    ), ("#b08554", "#cda267", "#e1b879", "#f1ca8e", "#ffdf9e",
        "#82964f", "#a5ae68", "#657f49"), 2, 4))


def surfaces():
    def woven(colors, seed, mat=False):
        random = Random(seed)
        canvas = Image.new("RGBA", (16, 16))
        draw = ImageDraw.Draw(canvas)
        for y in range(16):
            for x in range(16):
                shift = x if mat else y
                tone = 1 + int((shift + ((y if mat else x) // 3) % 2) % 3 == 0)
                draw.point((x, y), fill=colors[tone + random.choice((-1, 0, 0, 0, 1))])
        return canvas

    texture("block", "basket_wall", woven(("#a68151", "#b9925b", "#c7a66e", "#d2b681"), 41))
    texture("block", "basket_band", woven(("#866044", "#986e4b", "#ae8057", "#bc9168"), 42))
    texture("block", "basket_mat", woven(("#b59460", "#c5a46a", "#d1b479", "#debf86"), 43, mat=True))
    for index, (name, colors) in enumerate({
        "wok_iron": ("#3b3e44", "#464a51", "#50545b", "#5b5f66"),
        "wok_inside": ("#303137", "#393b41", "#41444a", "#4b4e55"),
        "wok_rim": ("#686a71", "#777a82", "#858891", "#9498a0"),
    }.items()):
        texture("block", name, woven(colors, 100 + index))


def preview():
    variants = (
        ("item", "cabbage", "tomato", "rice", "cabbage_seeds", "tomato_seeds", "rice_seeds", "wheat_noodles"),
        ("item", "steamed_rice", "salmon_onigiri", "chawanmushi", "steamed_pork_roll", "yakisoba", "tomato_egg", "egg_fried_rice"),
        ("item", "raw_salmon_onigiri", "chawanmushi_mix", "raw_pork_roll", "unplated_steamed_rice", "unplated_chawanmushi"),
        *[("block", *(f"{name}_crop_{stage}" for stage in range(4))) for name in ("cabbage", "tomato", "rice")],
        ("block", "wild_cabbage", "rice_panicle", "wild_rice_lower", "wild_rice_upper"),
    )
    step = 80
    canvas = Image.new("RGBA", (7 * step, len(variants) * step), "#293435")
    for y, (folder, *names) in enumerate(variants):
        for x, name in enumerate(names):
            with Image.open(CUISINE_TEXTURES / folder / f"{name}.png") as bitmap:
                canvas.alpha_composite(bitmap.convert("RGBA").resize((64, 64), Image.Resampling.NEAREST),
                                       (x * step + 8, y * step + 8))
    save(canvas, PROJECT / "build/kitchen-texture-preview.png")


if __name__ == "__main__":
    ingredients()
    harvest_sprites()
    wild_crop_sprites()
    meals()
    steaming_ingredients()
    surfaces()
    preview()
