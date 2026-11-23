package dev.sakurabloom.cuisine;

import dev.sakurabloom.SakuraBloom;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class KitchenBlock extends BaseEntityBlock {
    static final BooleanProperty COOKING = BooleanProperty.create("cooking");
    private static final TagKey<Block> HEAT_SOURCES = TagKey.create(Registries.BLOCK, SakuraBloom.id("cooking_heat_sources"));
    private static final MapCodec<KitchenBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        KitchenRecipe.Station.CODEC.fieldOf("station").forGetter(block -> block.station),
        propertiesCodec()
    ).apply(instance, KitchenBlock::new));
    private static final VoxelShape BASKET_SHAPE = box(2, 0, 2, 14, 11, 14);
    private static final VoxelShape WOK_SHAPE = box(1, 0, 2, 15, 7, 14);

    private final KitchenRecipe.Station station;

    KitchenBlock(KitchenRecipe.Station station, Properties properties) {
        super(properties);
        this.station = station;
        registerDefaultState(stateDefinition.any().setValue(COOKING, false));
    }

    @Override
    protected MapCodec<KitchenBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COOKING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return station == KitchenRecipe.Station.STEAMER ? BASKET_SHAPE : WOK_SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KitchenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, KitchenContent.COOKWARE.get(), KitchenBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return player.getOffhandItem().isEmpty()
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof KitchenBlockEntity cookware) {
            if (cookware.ready()) {
                if (!cookware.serving().isEmpty() && cookware.serving().test(stack)) {
                    stack.consume(1, player);
                    collect(level, pos, player, cookware);
                    return ItemInteractionResult.CONSUME;
                }
                showCollectionHint(player, cookware);
            } else if (cookware.cooking()) {
                message(player, hasHeat(level, pos) ? "cooking" : "heat");
            } else if (stack.is(Items.BOWL)) {
                message(player, "bowl");
            } else {
                boolean valid = KitchenRecipes.entries(level, station)
                    .flatMap(recipe -> recipe.ingredients().stream()).anyMatch(ingredient -> ingredient.test(stack));
                if (!valid) {
                    message(player, "ingredient");
                } else if (cookware.addIngredient(stack)) {
                    stack.consume(1, player);
                    level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.7F, 1.1F);
                } else {
                    message(player, "full");
                }
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof KitchenBlockEntity cookware) {
            if (cookware.ready()) {
                if (cookware.serving().isEmpty()) {
                    collect(level, pos, player, cookware);
                } else {
                    showCollectionHint(player, cookware);
                }
            } else if (cookware.cooking()) {
                message(player, hasHeat(level, pos) ? "cooking" : "heat");
            } else if (player.isShiftKeyDown()) {
                cookware.returnIngredients(player);
                message(player, "returned");
            } else {
                tryStart(state, level, pos, player, cookware);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void tryStart(BlockState state, Level level, BlockPos pos, Player player, KitchenBlockEntity cookware) {
        cookware.returnLegacyBowls(player);
        var input = cookware.input();
        if (input.isEmpty()) {
            player.displayClientMessage(Component.translatable(getDescriptionId() + ".hint"), true);
            return;
        }
        var recipe = KitchenRecipes.match(level, station, input);
        if (recipe.isEmpty()) {
            message(player, "no_recipe");
        } else if (!hasHeat(level, pos)) {
            message(player, "heat");
        } else {
            cookware.start(recipe.orElseThrow());
            level.setBlockAndUpdate(pos, state.setValue(COOKING, true));
        }
    }

    private static void collect(Level level, BlockPos pos, Player player, KitchenBlockEntity cookware) {
        player.getInventory().placeItemBackInInventory(cookware.collect());
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    private static void showCollectionHint(Player player, KitchenBlockEntity cookware) {
        if (cookware.serving().isEmpty()) {
            message(player, "ready");
            return;
        }
        var examples = cookware.serving().getItems();
        if (examples.length == 0) {
            message(player, "serving");
        } else {
            message(player, "serve_with", examples[0].getHoverName());
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof KitchenBlockEntity cookware
            && cookware.ready() && !cookware.serving().isEmpty()) {
            for (ItemStack drop : drops) {
                if (drop.is(asItem())) {
                    BlockItem.setBlockEntityData(drop, cookware.getType(), cookware.saveCustomOnly(params.getLevel().registryAccess()));
                }
            }
        }
        return drops;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !replacement.is(this) && level.getBlockEntity(pos) instanceof KitchenBlockEntity cookware) {
            cookware.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(COOKING) && random.nextInt(3) == 0 && hasHeat(level, pos)) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.4 + random.nextDouble() * 0.2,
                pos.getY() + 0.72, pos.getZ() + 0.4 + random.nextDouble() * 0.2, 0, 0.017, 0);
        }
    }

    static boolean hasHeat(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(HEAT_SOURCES) && below.getOptionalValue(BlockStateProperties.LIT).orElse(true);
    }

    private static void message(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.sakura_bloom.cooking." + key, args), true);
    }
}
