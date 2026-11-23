"""Inventory sprites on the same 16px grid as the cherry wood and courtyard items."""

from PIL import Image, ImageColor


SPRITES = {
    # A porcelain bell with a scalloped blossom rim and a folded paper windcatcher.
    "shidare_wind_bell": (
        dict(o="#54404d", s="#796475", m="#a690a1", p="#d6b4c0", i="#ebd5d7", w="#fff1df",
             g="#b98759", h="#efd197", r="#a86478", t="#efb9c6"),
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
        dict(o="#513342", s="#754455", r="#a85570", p="#cf788c", l="#e99ba8",
             k="#916b50", g="#c99766", h="#f5d79e", w="#ffe7d1"),
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
        dict(o="#35494b", d="#486562", m="#67948b", j="#87b3a2", l="#b4d1b8",
             w="#e6e6cb", s="#764657", r="#b46c72", g="#cc9b67", h="#f3d493"),
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
        dict(o="#423743", s="#624858", m="#926880", p="#ba8b9e", l="#dbadb8",
             k="#775244", b="#a97959", g="#d4a575", h="#f1d29c", i="#353642", t="#625766"),
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
