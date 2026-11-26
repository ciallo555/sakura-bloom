"""Draw the brocade pouch icons and the sewn lining of the storage screen."""
from pathlib import Path
from PIL import Image, ImageDraw

TEXTURES = Path(__file__).resolve().parents[1] / "src/main/resources/assets/sakura_bloom/textures"

# Silk gathered at the drawstring, a mizuhiki clasp and a running edge stitch.
POUCH = [
    ".....oo..oo.....",
    "....obboobbo....",
    "....oshbbhso....",
    ".....oggggo.....",
    "....okhhkggo....",
    "...osllghlssko..",
    "...obllbbllsgho.",
    "..oblbbsbbblsko.",
    "..obbhbsbhbssko.",
    ".obllghgghbssgho",
    ".obbbhgghbbsso..",
    ".osllbbhbbbsso..",
    "..osllbbbbsso...",
    "..ossllbbsso....",
    "...ookggkkoo....",
    ".....ooooo......",
]
COLORS = {
    "knotwork_pouch": dict(o="#543746", b="#b85b78", s="#884760", l="#dc91a2",
                           k="#94683f", g="#d4a75d", h="#ffe3a0"),
    "cloud_brocade_pouch": dict(o="#37405b", b="#567b93", s="#4b5878", l="#8db3b1",
                                k="#946249", g="#db9c66", h="#ffdda1"),
}


def icons():
    for name, palette in COLORS.items():
        sprite = Image.new("RGBA", (16, 16))
        draw = ImageDraw.Draw(sprite)
        for y, row in enumerate(POUCH):
            for x, char in enumerate(row):
                if char in palette:
                    draw.point((x, y), fill=palette[char])
        if name == "cloud_brocade_pouch":
            draw.line([(2, 10), (4, 10), (5, 9), (6, 9), (6, 8), (8, 8)], fill="#ece2b6")
            draw.line([(5, 12), (7, 12), (8, 11), (10, 11)], fill="#aad2c8")
            draw.point((4, 7), fill="#ffe2a9")
        dest = TEXTURES / "item" / (name + ".png")
        dest.parent.mkdir(parents=True, exist_ok=True)
        sprite.save(dest)


def lining():
    image = Image.new("RGBA", (176, 204))
    d = ImageDraw.Draw(image)
    d.rounded_rectangle((0, 0, 175, 203), radius=4, fill="#553e4a")
    d.rounded_rectangle((2, 2, 173, 201), radius=3, fill="#a96572")
    d.rectangle((4, 4, 171, 199), fill="#d8bba1")
    for x in range(5, 171, 5):
        for y in (2, 200):
            d.line((x, y, x+2, y), fill="#e3b586")
    for y in range(5, 199, 5):
        for x in (2, 173):
            d.line((x, y, x, y+2), fill="#e3b586")
    # A narrow privacy fold around the central panel, with repeated plum-blossom embroidery.
    d.rectangle((7, 17, 168, 92), fill="#825961")
    d.rectangle((9, 19, 166, 90), fill="#aa707b")
    for x in (21, 154):
        for y in (31, 54, 77):
            for dx, dy in ((-2, 0), (2, 0), (0, -2), (0, 2)):
                d.rectangle((x+dx-1, y+dy-1, x+dx+1, y+dy+1), fill="#c68e97")
            d.point((x,y), fill="#efd29a")
    d.rectangle((7, 93, 168, 106), fill="#76515c")
    for row in (122, 140, 158, 180):
        for x in range(8, 154, 18):
            d.rectangle((x-1, row-1, x+16, row+16), fill="#826a64")
            d.rectangle((x, row, x+15, row+15), fill="#b9a391")
            d.line((x, row+16, x+16, row+16, x+16, row), fill="#f2ddbd")
    dest = TEXTURES / "gui/knotwork_pouch.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    image.save(dest)


if __name__ == "__main__":
    icons()
    lining()
