package dev.sakurabloom.adventure.pouch;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

record CharmShape(int color, List<List<Cell>> rotations) {
    record Cell(int x, int y) {}

    private static final Map<String, CharmShape> SHAPES = Map.of(
        "shidare_wind_bell", shape(0x74acb0, "X", "X", "X"),
        "yae_knot_charm", shape(0xd58aa1, "XX", "XX"),
        "celadon_fish_pendant", shape(0x75ab91, "XX", ".X"),
        "yamazakura_ink_bracelet", shape(0x907aab, ".XX", "XX."),
        "lantern_crown", shape(0xc99556, "X.X", "XXX"),
        "tide_pearl", shape(0x709bb2, ".X.", "XXX", ".X."),
        "brocade_knot", shape(0xb46a82, "XX.", ".XX", "..X"),
        "moon_mirror", shape(0xa29bb9, ".XX", "XXX")
    );

    static CharmShape of(ItemStack item) {
        var id = BuiltInRegistries.ITEM.getKey(item.getItem());
        return id.getNamespace().equals(SakuraBloom.ID) ? SHAPES.get(id.getPath()) : null;
    }

    private static CharmShape shape(int color, String... rows) {
        List<Cell> cells = new ArrayList<>();
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++)
                if (rows[y].charAt(x) == 'X') cells.add(new Cell(x, y));
        List<List<Cell>> turns = new ArrayList<>();
        for (int turn = 0; turn < 4; turn++) {
            turns.add(List.copyOf(cells));
            int bottom = cells.stream().mapToInt(Cell::y).max().orElseThrow();
            cells = cells.stream().map(c -> new Cell(bottom - c.y, c.x)).toList();
        }
        return new CharmShape(color, List.copyOf(turns));
    }

    List<Cell> cells(int rotation) {
        return rotations.get(rotation & 3);
    }
}
