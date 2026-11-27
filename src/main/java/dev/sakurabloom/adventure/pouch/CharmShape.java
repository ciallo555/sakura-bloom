package dev.sakurabloom.adventure.pouch;

import dev.sakurabloom.SakuraBloom;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

record CharmShape(List<Patch> rotations) {
    record Patch(List<Cell> cells, int width, int height, Cell anchor) {}
    record Cell(int x, int y) {}

    private static final Map<String, CharmShape> SHAPES = Map.of(
        "shidare_wind_bell", shape("X", "X", "X"),
        "yae_knot_charm", shape("XX", "XX"),
        "celadon_fish_pendant", shape("XX", ".X"),
        "yamazakura_ink_bracelet", shape(".XX", "XX."),
        "lantern_crown", shape("X.X", "XXX"),
        "tide_pearl", shape(".X.", "XXX", ".X."),
        "brocade_knot", shape("XX.", ".XX", "..X"),
        "moon_mirror", shape(".XX", "XXX")
    );

    static CharmShape of(ItemStack item) {
        var id = BuiltInRegistries.ITEM.getKey(item.getItem());
        return id.getNamespace().equals(SakuraBloom.ID) ? SHAPES.get(id.getPath()) : null;
    }

    private static CharmShape shape(String... rows) {
        List<Cell> cells = new ArrayList<>();
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++)
                if (rows[y].charAt(x) == 'X') cells.add(new Cell(x, y));
        List<Patch> turns = new ArrayList<>();
        for (int turn = 0; turn < 4; turn++) {
            int width = 1 + cells.stream().mapToInt(Cell::x).max().orElseThrow();
            int height = 1 + cells.stream().mapToInt(Cell::y).max().orElseThrow();
            double centerX = cells.stream().mapToInt(Cell::x).average().orElseThrow();
            double centerY = cells.stream().mapToInt(Cell::y).average().orElseThrow();
            var anchor = cells.stream().min(java.util.Comparator.comparingDouble(c ->
                (c.x-centerX)*(c.x-centerX) + (c.y-centerY)*(c.y-centerY))).orElseThrow();
            turns.add(new Patch(List.copyOf(cells),width,height,anchor));
            int bottom = cells.stream().mapToInt(Cell::y).max().orElseThrow();
            cells = cells.stream().map(c -> new Cell(bottom - c.y, c.x)).toList();
        }
        return new CharmShape(List.copyOf(turns));
    }

    List<Cell> cells(int rotation) { return patch(rotation).cells(); }

    Patch patch(int rotation) {
        return rotations.get(rotation & 3);
    }
}
