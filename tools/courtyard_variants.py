"""Material and pattern catalog shared by the courtyard models, recipes and pixel art."""

# Wood color is the midtone of the finished joinery, paper is the undyed ground.
WOODS = {
    "oak": ("橡木", "Oak", "#ac8958", "#f0e5ce"),
    "spruce": ("云杉木", "Spruce", "#816449", "#ede2c9"),
    "birch": ("白桦木", "Birch", "#c9b984", "#f0ebd7"),
    "jungle": ("丛林木", "Jungle", "#af8165", "#efe1c9"),
    "acacia": ("金合欢木", "Acacia", "#b57958", "#f4dfc6"),
    "dark_oak": ("深色橡木", "Dark Oak", "#6e5544", "#e9dfce"),
    "mangrove": ("红树木", "Mangrove", "#955e56", "#efdfd1"),
    "cherry": ("樱木", "Cherry", "#d7aaa4", "#f4e5dd"),
    "bamboo": ("竹", "Bamboo", "#bfac67", "#eee7cd"),
    "crimson": ("绯红木", "Crimson", "#93596e", "#ecdde0"),
    "warped": ("诡异木", "Warped", "#518379", "#e2e8dc"),
    "shidare": ("枝垂樱木", "Weeping Cherry", "#c9a385", "#f0e5d9"),
    "yae": ("八重樱木", "Double Cherry", "#be827a", "#f4dfd7"),
    "yamazakura": ("山樱木", "Mountain Cherry", "#a37e92", "#ece1e4"),
}

MOTIFS = {"waves": ("青海波", "Wave"), "sakura": ("樱花", "Blossom"),
          "hemp": ("麻叶", "Hemp Leaf"), "arrows": ("矢羽", "Arrow Feather")}
# Ground and embroidery are chosen separately so even pale linen has a legible pattern.
CURTAINS = {
    "white": ("白色", "#e6e1d0", "#a89596", "sakura"),
    "orange": ("橙色", "#d38a52", "#f0cc95", "arrows"),
    "magenta": ("品红色", "#b56c9d", "#eed0d0", "sakura"),
    "light_blue": ("淡蓝色", "#83acc3", "#d8e1d5", "waves"),
    "yellow": ("黄色", "#d3af57", "#877344", "arrows"),
    "lime": ("黄绿色", "#a5b46a", "#63744f", "hemp"),
    "pink": ("粉红色", "#dca5b1", "#995e76", "sakura"),
    "gray": ("灰色", "#656771", "#bcc0b4", "arrows"),
    "light_gray": ("淡灰色", "#b1b1a9", "#737d82", "waves"),
    "cyan": ("青色", "#588e92", "#b9d9c6", "waves"),
    "purple": ("紫色", "#896d9f", "#ccb5cf", "arrows"),
    "blue": ("蓝色", "#465c78", "#a4b4af", "waves"),
    "brown": ("棕色", "#957252", "#d8bd90", "hemp"),
    "green": ("绿色", "#73855c", "#c4cf95", "hemp"),
    "red": ("红色", "#ae6164", "#f0bd9e", "sakura"),
    "black": ("黑色", "#434550", "#aaa69d", "hemp"),
}

def noren_id(color):
    return "indigo_noren" if color == "blue" else f"{color}_noren"


# The original begonia window and blue vase keep their historical assets and recipes.
LATTICES = {
    "key_fret": ("回纹花格窗", "Key Fret Lattice Window", "dark_oak", ["WWW", "BSW", "WBB"]),
    "ice_crackle": ("冰裂花格窗", "Ice Crackle Lattice Window", "birch", ["WBW", "BSB", "WSW"]),
    "diamond": ("菱花格窗", "Diamond Lattice Window", "bamboo", [" B ", "WSW", " B "]),
    "brocade": ("步步锦花格窗", "Brocade Lattice Window", "mangrove", ["WSW", "BWB", "WSW"]),
}

# Each tier is (inset, bottom, top), measured in block pixels. The lip is open.
VASES = {
    "celadon_bamboo": ("青瓷竹枝瓶", "Celadon Bamboo Vase", "green_dye", "#b4c9ad", "#516d62",
        [(5,0,3), (4,3,10), (5,10,12), (6,12,15)], (5,15)),
    "rouge_plum": ("胭脂梅枝瓶", "Rouge Plum Vase", "red_dye", "#e8c6c4", "#97556a",
        [(5,0,3), (4,3,6), (3,6,11), (4,11,12), (6,12,13)], (6,13)),
    "ink_magnolia": ("墨彩玉兰瓶", "Ink Magnolia Vase", "ink_sac", "#d8d7c7", "#525b61",
        [(5,0,3), (4,3,9), (5,9,11), (6,11,13), (5,13,14)], (4,14)),
    "amber_persimmon": ("琥珀柿枝瓶", "Amber Persimmon Vase", "orange_dye", "#bc9163", "#674d44",
        [(5,0,2), (3,2,4), (2,4,8), (3,8,9), (5,9,10)], (5,10)),
}

ROOF_FINISHES = (("", 5), ("_ash", 3), ("_fired", 2))

# Dark trough, fired body and lit ridge for the sixteen ceramic glazes.
ROOF_GLAZES = {
    "white":      ("#85877e", "#d3d0ba", "#f5f0dc"),
    "orange":     ("#824932", "#c77b49", "#edac72"),
    "magenta":    ("#673c61", "#a36796", "#d99abe"),
    "light_blue": ("#416277", "#80a9bb", "#bce0dd"),
    "yellow":     ("#8b672e", "#c8a04d", "#f2d98c"),
    "lime":       ("#566b39", "#9aa85d", "#cbd794"),
    "pink":       ("#95646c", "#cf9da3", "#f0ccbd"),
    "gray":       ("#46555c", "#798588", "#c4c5b1"),
    "light_gray": ("#686d6c", "#a7aaa0", "#d7d7c7"),
    "cyan":       ("#325c62", "#568e90", "#a1c6ba"),
    "purple":     ("#50435f", "#866a95", "#baa1be"),
    "blue":       ("#2d405b", "#4d6c91", "#8faabb"),
    "brown":      ("#543c32", "#88684f", "#bb9575"),
    "green":      ("#374e3e", "#617f54", "#a1b38a"),
    "red":        ("#743d39", "#a95b50", "#d99778"),
    "black":      ("#25282e", "#464950", "#838889"),
}
