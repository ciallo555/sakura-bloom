"""Inventory sprites on the same 16px grid as the cherry wood and courtyard items."""

from PIL import Image, ImageColor


SPRITES = {
    # A porcelain bell with a scalloped blossom rim and a folded paper windcatcher.
    "shidare_wind_bell": (
        dict(o="#76566b", s="#a17f99", m="#c8a8c5", p="#f1c4d8", i="#fff0e8", w="#fffbed",
             g="#d99c59", h="#ffe6a1", r="#d67996", t="#ffced7"),
        (
            ".......oo",
            "......gh.o",
            ".......gg",
            ".....smmms",
            "....mpiwips",
            "...mpwwipims",
            "...piwwippms",
            "..smiiiprpms",
            "..smrpprpmms",
            "..ommmssmsso",
            "...oooghooo",
            ".......g",
            ".......ort",
            "........rtis",
            ".........stis",
            "..........sr",
        )),
    # A mizuhiki knot, folded silk shoulders and a five-petal woven crest.
    "yae_knot_charm": (
        dict(o="#713c59", s="#a34e72", r="#d76b8b", p="#f193aa", l="#ffc3cb",
             k="#ae7b4b", g="#edb969", h="#ffe7a6", w="#fff5dd"),
        (
            ".....gg..gg",
            "....ghsgghsg",
            ".....kghhgk",
            "......oggo",
            ".....srllrs",
            "....sppplprso",
            "...srhgggggrso",
            "...splrllprgro",
            "...sgpprhprgro",
            "...sgprhwhpgro",
            "...sgplghlrgro",
            "...sgprprrsgro",
            "...sgprrsrsgro",
            "...sphgggggkso",
            "....srrssrsso",
            ".....ooooooo",
        )),
    # Celadon crackle-glaze, crescent fins and a vermilion cord threaded through the back.
    "celadon_fish_pendant": (
        dict(o="#396465", d="#51847e", m="#78b5a8", j="#9fdbbe", l="#cbf1d0",
             w="#fff9df", s="#925268", r="#de8586", g="#e8b873", h="#ffdfa1"),
        (
            "........rr",
            ".......rs.r",
            ".......r..s",
            "........rs",
            "......mdgh",
            ".....mjlmgd..om",
            "...omjlljjmo.mjo",
            "..mjllwjlljmjljd",
            ".mwloljjmjlmllmo",
            ".ljwjlmljmmjjdo",
            ".dmjjlldmmmodo",
            "..omjjmmdod..dm",
            "....dmmodm",
            ".....odjm",
            "......oo",
            "",
        )),
    # Dyed braided silk around an open wrist loop, with a boxwood ink reel and a tassel.
    "yamazakura_ink_bracelet": (
        dict(o="#594562", s="#826485", m="#b88fb0", p="#e0b2cc", l="#f7d7df",
             k="#966a4f", b="#c99767", g="#edc383", h="#fff0bb", i="#4a495d", t="#887689"),
        (
            ".....osmmso",
            "...osmplmmsmo",
            "..smpso..omps",
            ".ompo.....ompo",
            ".sps.......smo",
            ".mlo.......omp",
            ".sms.......opo",
            ".omo..okbgoops",
            "..spsokghhgbm",
            "...mmbghgitbko",
            "....okgbitibko",
            ".....okbgtbkom",
            "......okggko.pmo",
            ".......okko..mls",
            "............spsm",
            "............o..s",
        )),
}


def draw_charms(texture):
    for name, (colors, rows) in SPRITES.items():
        assert len(rows) == 16 and max(map(len, rows)) <= 16, name
        sprite = Image.new("RGBA", (16, 16))
        palette = {key: ImageColor.getrgb(color) + (255,) for key, color in colors.items()}
        for y, row in enumerate(rows):
            for x, pixel in enumerate(row):
                if pixel != ".":
                    sprite.putpixel((x, y), palette[pixel])
        texture("item", name, sprite)
