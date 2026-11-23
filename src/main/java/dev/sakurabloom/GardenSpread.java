package dev.sakurabloom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Supplier;

/** The woodland set avoids both shrine courtyards and the lava apron of ruined portals. */
final class GardenSpread extends RandomSpreadStructurePlacement {
    private record Spread(int spacing, int separation, RandomSpreadType type) {
        private static final MapCodec<Spread> CODEC = RecordCodecBuilder.<Spread>mapCodec(instance -> instance.group(
            Codec.intRange(0, 4096).fieldOf("spacing").forGetter(Spread::spacing),
            Codec.intRange(0, 4096).fieldOf("separation").forGetter(Spread::separation),
            RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(Spread::type)
        ).apply(instance, Spread::new)).validate(spread -> spread.spacing > spread.separation ? DataResult.success(spread)
            : DataResult.error(() -> "Spacing must exceed separation"));
    }

    private static final MapCodec<GardenSpread> CODEC = RecordCodecBuilder.<GardenSpread>mapCodec(instance ->
        placementCodec(instance).and(instance.group(
            Spread.CODEC.forGetter(placement -> new Spread(placement.spacing(), placement.separation(), placement.spreadType())),
            ExclusionZone.CODEC.fieldOf("additional_exclusion_zone").forGetter(placement -> placement.extra)
        )).apply(instance, GardenSpread::new)
    );
    private static final DeferredRegister<StructurePlacementType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, SakuraBloom.ID);
    private static final Supplier<StructurePlacementType<GardenSpread>> TYPE = TYPES.register("garden_spread", () -> () -> CODEC);
    private final ExclusionZone extra;

    private GardenSpread(Vec3i offset, FrequencyReductionMethod method, float frequency, int salt, Optional<ExclusionZone> exclusion,
                         Spread spread, ExclusionZone extra) {
        super(offset, method, frequency, salt, exclusion, spread.spacing, spread.separation, spread.type);
        this.extra = extra;
    }

    static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    @Override
    public boolean applyInteractionsWithOtherStructures(ChunkGeneratorStructureState state, int x, int z) {
        return super.applyInteractionsWithOtherStructures(state, x, z)
            && !state.hasStructureChunkInRange(extra.otherSet(), x, z, extra.chunkCount());
    }

    @Override
    public StructurePlacementType<?> type() {
        return TYPE.get();
    }
}
