package dev.sakurabloom.adventure.pouch;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

record PouchLayout(List<Piece> pieces) {
    static final PouchLayout EMPTY = new PouchLayout(List.of());

    // Preserve the full charm stack, including custom names and third-party components.
    record Piece(ItemStack stack, int x, int y, int turn) {
        static final Codec<Piece> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.SINGLE_ITEM_CODEC.fieldOf("charm").forGetter(Piece::stack),
            Codec.intRange(0, 4).fieldOf("x").forGetter(Piece::x),
            Codec.intRange(0, 3).fieldOf("y").forGetter(Piece::y),
            Codec.intRange(0, 3).fieldOf("turn").forGetter(Piece::turn)
        ).apply(i, Piece::new));

        boolean covers(int col, int row) {
            var shape = CharmShape.of(stack);
            return shape != null && shape.cells(turn).stream().anyMatch(c -> x + c.x() == col && y + c.y() == row);
        }
    }

    static final Codec<PouchLayout> CODEC = Piece.CODEC.listOf(0, 20).xmap(PouchLayout::new, PouchLayout::pieces);

    PouchLayout { pieces = List.copyOf(pieces); }

    Piece at(int x, int y) {
        for (var piece : pieces) if (piece.covers(x, y)) return piece;
        return null;
    }

    boolean fits(Piece candidate, PouchItem bag) {
        var shape = CharmShape.of(candidate.stack());
        if (shape == null || pieces.stream().anyMatch(p -> p.stack().is(candidate.stack().getItem()))) return false;
        for (var cell : shape.cells(candidate.turn())) {
            int x = candidate.x() + cell.x();
            int y = candidate.y() + cell.y();
            if (x < 0 || y < 0 || x >= bag.columns || y >= bag.rows || at(x, y) != null) return false;
        }
        return true;
    }

    PouchLayout add(Piece piece) {
        var changed = new ArrayList<>(pieces);
        changed.add(piece);
        return new PouchLayout(changed);
    }

    PouchLayout remove(Piece piece) {
        var changed = new ArrayList<>(pieces);
        changed.remove(piece);
        return new PouchLayout(changed);
    }
}
