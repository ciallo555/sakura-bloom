package dev.sakurabloom.adventure.realm;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import java.util.Optional;

final class SanctumStructure extends Structure {
    static final MapCodec<SanctumStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        settingsCodec(instance), ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template)
    ).apply(instance, SanctumStructure::new));
    private final ResourceLocation template;

    private SanctumStructure(StructureSettings settings, ResourceLocation template) {
        super(settings);
        this.template = template;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        var chunk = ctx.chunkPos();
        var ward = FloatingWard.containing(ctx.seed(), chunk.getMinBlockX(), chunk.getMinBlockZ());
        var corner = ward.corner();
        var manager = ctx.structureTemplateManager();
        var element = StructurePoolElement.single(template.toString()).apply(StructureTemplatePool.Projection.RIGID);
        var rotation = Rotation.NONE;
        return Optional.of(new GenerationStub(corner.offset(16, 23, 16), pieces -> pieces.addPiece(
            new PoolElementStructurePiece(manager, element, corner, 0, rotation, element.getBoundingBox(manager, corner, rotation),
                LiquidSettings.IGNORE_WATERLOGGING))));
    }

    @Override
    public StructureType<?> type() { return Interstice.SANCTUM.get(); }
}
