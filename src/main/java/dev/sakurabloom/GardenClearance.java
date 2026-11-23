package dev.sakurabloom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Neighbouring chunks can still grow branches into the veranda after this chunk's decoration pass. */
record GardenClearance(ResourceLocation template, BlockPos origin, Rotation rotation) {
    private static final Codec<GardenClearance> CODEC = RecordCodecBuilder.create(i -> i.group(
        ResourceLocation.CODEC.fieldOf("template").forGetter(GardenClearance::template),
        BlockPos.CODEC.fieldOf("origin").forGetter(GardenClearance::origin),
        Rotation.CODEC.fieldOf("rotation").forGetter(GardenClearance::rotation)
    ).apply(i, GardenClearance::new));
    private static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SakuraBloom.ID);
    private static final Supplier<AttachmentType<List<GardenClearance>>> PENDING = TYPES.register("pending_garden_clearance",
        () -> AttachmentType.builder(() -> List.<GardenClearance>of()).serialize(CODEC.listOf()).build());

    static void register(IEventBus bus) {
        TYPES.register(bus);
        NeoForge.EVENT_BUS.addListener(GardenClearance::onLoad);
    }

    static void remember(WorldGenLevel level, ChunkPos pos, ResourceLocation template, PoolElementStructurePiece piece) {
        if (!(level instanceof WorldGenRegion)) return;
        var chunk = level.getChunk(pos.x, pos.z);
        var placement = new GardenClearance(template, piece.getPosition(), piece.getRotation());
        var pending = new ArrayList<>(chunk.getData(PENDING));
        if (!pending.contains(placement)) {
            pending.add(placement);
            chunk.setData(PENDING, List.copyOf(pending));
        }
    }

    private static void onLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)
                || !chunk.hasData(PENDING)) return;
        // Promotion can fire Load before FULL is accessible. Use the supplied chunk after
        // promotion, without requesting neighbours or re-running on an explored chunk.
        var server = level.getServer();
        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            if (!chunk.hasData(PENDING)) return;
            for (var task : chunk.getData(PENDING)) task.trim(level, chunk);
            chunk.removeData(PENDING);
        }));
    }

    private void trim(ServerLevel level, LevelChunk chunk) {
        var pos = chunk.getPos();
        var bounds = new BoundingBox(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ(),
            pos.getMaxBlockX(), level.getMaxBuildHeight()-1, pos.getMaxBlockZ());
        var settings = new StructurePlaceSettings().setRotation(rotation).setBoundingBox(bounds);
        level.getStructureManager().get(template).ifPresent(layout -> {
            for (var air : layout.filterBlocks(origin, settings, Blocks.AIR)) {
                var state = chunk.getBlockState(air.pos());
                if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
                    chunk.setBlockState(air.pos(), Blocks.AIR.defaultBlockState(), false);
                    level.getChunkSource().blockChanged(air.pos());
                }
            }
        });
    }
}
