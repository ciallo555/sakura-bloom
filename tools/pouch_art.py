"""Draw the brocade pouch icons and the sewn lining of the storage screen."""
from pathlib import Path
from interstice_art import pouch

TEXTURES = Path(__file__).resolve().parents[1] / "src/main/resources/assets/sakura_bloom/textures"


def icons():
    for name in ("knotwork_pouch","cloud_brocade_pouch"):
        dest=TEXTURES/"item"/(name+".png")
        dest.parent.mkdir(parents=True,exist_ok=True)
        pouch(name=="cloud_brocade_pouch").save(dest)

if __name__ == "__main__":
    icons()
    from pouch_lining import generate
    generate()
