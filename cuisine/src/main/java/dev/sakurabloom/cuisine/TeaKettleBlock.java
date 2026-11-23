package dev.sakurabloom.cuisine;

import dev.sakurabloom.BloomContent;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;
import java.util.List;

public final class TeaKettleBlock extends Block {
    public static final MapCodec<TeaKettleBlock> CODEC = simpleCodec(TeaKettleBlock::new);
    static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
    private static final int BREW_TICKS = 80;
    private static final VoxelShape SHAPE = Shapes.or(box(2, 0, 2, 14, 3, 14), box(3, 3, 3, 13, 12, 13),
        box(1, 8, 6, 3, 10, 10), box(13, 8, 6, 15, 10, 10));

    public TeaKettleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, Stage.EMPTY));
    }

    @Override
    public MapCodec<TeaKettleBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        Stage previous = state.getValue(STAGE);
        Stage next = previous;
        if (previous == Stage.EMPTY && isWaterBottle(stack)) {
            next = Stage.WATER;
        } else if (previous == Stage.WATER) {
            for (Stage brewing : Stage.BREWS) {
                if (stack.is(brewing.petals.get().asItem())) {
                    next = brewing;
                    break;
                }
            }
        }
        if (next == previous) {
            return stack.isEmpty() && player.getOffhandItem().isEmpty()
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server && server.setBlockAndUpdate(pos, state.setValue(STAGE, next))) {
            stack.consume(1, player);
            if (next.brewing()) {
                server.scheduleTick(pos, this, BREW_TICKS);
            }
            server.playSound(null, pos, next == Stage.WATER ? SoundEvents.BOTTLE_EMPTY : SoundEvents.DECORATED_POT_INSERT,
                SoundSource.BLOCKS, 0.7F, 1.0F);
            server.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            Stage stage = state.getValue(STAGE);
            ItemStack result = stage.ready() ? stage.contents()
                : (stage == Stage.WATER && player.isShiftKeyDown() ? waterBottle() : ItemStack.EMPTY);
            if (!result.isEmpty() && server.setBlockAndUpdate(pos, state.setValue(STAGE, Stage.EMPTY))) {
                player.getInventory().placeItemBackInInventory(result);
                server.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.75F, 1.0F);
                server.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            } else if (result.isEmpty()) {
                String hint = stage.brewing() ? "brewing" : stage == Stage.WATER ? "water" : "empty";
                player.displayClientMessage(Component.translatable("block.sakura_bloom.tea_kettle." + hint), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Stage stage = state.getValue(STAGE);
        if (stage.brewing() && level.setBlockAndUpdate(pos, state.setValue(STAGE, stage.finished))) {
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.05F);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(STAGE).brewing() && random.nextBoolean()) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.45 + random.nextDouble() * 0.1,
                pos.getY() + 0.77, pos.getZ() + 0.45 + random.nextDouble() * 0.1, 0, 0.025, 0);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !replacement.is(this)) {
            popResource(level, pos, state.getValue(STAGE).contents());
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(STAGE).signal;
    }

    private static boolean isWaterBottle(ItemStack stack) {
        return stack.is(Items.POTION) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
    }

    static ItemStack waterBottle() {
        return PotionContents.createItemStack(Items.POTION, Potions.WATER);
    }

    enum Stage implements StringRepresentable {
        EMPTY("empty", 0, null, null, null),
        WATER("water", 4, null, null, null),
        READY_SHIDARE("ready_shidare", 15, KitchenContent.SHIDARE_TEA, null, null),
        READY_YAE("ready_yae", 15, KitchenContent.YAE_TEA, null, null),
        READY_YAMAZAKURA("ready_yamazakura", 15, KitchenContent.YAMAZAKURA_TEA, null, null),
        BREWING_SHIDARE("brewing_shidare", 8, null, READY_SHIDARE, BloomContent.SHIDARE.petals()),
        BREWING_YAE("brewing_yae", 8, null, READY_YAE, BloomContent.YAE.petals()),
        BREWING_YAMAZAKURA("brewing_yamazakura", 8, null, READY_YAMAZAKURA, BloomContent.YAMAZAKURA.petals());

        static final List<Stage> BREWS = List.of(BREWING_SHIDARE, BREWING_YAE, BREWING_YAMAZAKURA);

        private final String name;
        private final int signal;
        private final Supplier<? extends Item> tea;
        private final Stage finished;
        private final Supplier<? extends ItemLike> petals;

        Stage(String name, int signal, Supplier<? extends Item> tea, Stage finished, Supplier<? extends ItemLike> petals) {
            this.name = name;
            this.signal = signal;
            this.tea = tea;
            this.finished = finished;
            this.petals = petals;
        }

        boolean ready() {
            return tea != null;
        }

        boolean brewing() {
            return finished != null;
        }

        ItemStack petalsStack() {
            return new ItemStack(petals.get());
        }

        ItemStack brewedTea() {
            return finished.contents();
        }

        ItemStack contents() {
            if (ready()) {
                return new ItemStack(tea.get());
            }
            return this == EMPTY ? ItemStack.EMPTY : waterBottle();
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
