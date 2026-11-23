package dev.sakurabloom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.neoforged.fml.ModList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public final class BloomGameTests {
    private static final BlockPos STATION = new BlockPos(2, 2, 2);

    @GameTest(template = "clearing")
    public static void cuisineFollowsInstallation(GameTestHelper test) {
        boolean installed = ModList.get().isLoaded("sakura_bloom_cuisine");
        var level = test.getLevel();
        for (String name : java.util.List.of("tea_kettle", "bamboo_steamer", "iron_wok", "rice_crop", "wild_cabbage")) {
            test.assertTrue(BuiltInRegistries.BLOCK.containsKey(SakuraBloom.id(name)) == installed, "Wrong block set for " + name);
        }
        for (String name : java.util.List.of("yae_tea", "rice", "cabbage_seeds", "tomato_seeds", "iron_wok", "yakisoba")) {
            var item = BuiltInRegistries.ITEM.getOptional(SakuraBloom.id(name));
            test.assertTrue(item.isPresent() == installed, "Wrong item set for " + name);
            item.ifPresent(value -> test.assertTrue("sakura_bloom_cuisine".equals(value.getCreatorModId(value.getDefaultInstance())),
                "Item browser attribution is incorrect for " + name));
        }
        test.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(SakuraBloom.id("cookware")) == installed,
            "Cookware save ID must follow the add-on");
        test.assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.containsKey(SakuraBloom.id("cooking")) == installed,
            "Cooking serializer must follow the add-on");
        test.assertTrue(level.getRecipeManager().byKey(SakuraBloom.id("iron_wok")).isPresent() == installed,
            "Cookware crafting data must follow the add-on");
        test.assertTrue(level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).containsKey(SakuraBloom.id("wild_rice")) == installed,
            "River worldgen must follow the add-on");
        test.assertTrue(BuiltInRegistries.ITEM.getTag(ItemTags.VILLAGER_PLANTABLE_SEEDS).orElseThrow().stream()
            .anyMatch(seed -> seed.is(SakuraBloom.id("tomato_seeds"))) == installed, "Unexpected village seed tag");
        test.succeed();
    }

    @GameTest(template = "clearing")
    public static void mortarConsumesOnePetal(GameTestHelper test) {
        test.setBlock(STATION, BloomContent.MORTAR.get());
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        ItemStack petals = new ItemStack(BloomContent.YAE.petals().asItem(), 3);
        test.assertTrue(use(test, STATION, player, petals), "Mortar did not recognize the petal recipe");
        test.assertTrue(petals.getCount() == 2, "Mortar did not consume precisely one petal");
        test.assertTrue(player.getInventory().countItem(Items.RED_DYE) == 2, "Mortar did not produce two red dyes");
        test.succeed();
    }

    @GameTest(template = "clearing")
    public static void lanternManualAndRedstone(GameTestHelper test) {
        test.setBlock(STATION, BloomContent.LANTERN.get());
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        test.assertTrue(test.getBlockState(STATION).getLightEmission() == 0, "Lantern should start off");
        test.assertTrue(use(test, STATION, player, ItemStack.EMPTY), "Lantern switch did not interact");
        test.assertTrue(test.getBlockState(STATION).getLightEmission() == 14, "Lantern switch did not light the block");
        test.setBlock(STATION.east(), Blocks.REDSTONE_BLOCK);
        use(test, STATION, player, ItemStack.EMPTY);
        test.assertTrue(test.getBlockState(STATION).getLightEmission() == 14, "Powered lantern did not stay lit");
        test.setBlock(STATION.east(), Blocks.AIR);
        test.assertTrue(test.getBlockState(STATION).getLightEmission() == 0, "Removing power did not switch off the lamp");
        test.succeed();
    }

    @GameTest(template = "clearing", skyAccess = true)
    public static void saplingsGrowAndLogsStrip(GameTestHelper test) {
        BlockPos[] positions = {new BlockPos(8, 2, 8), new BlockPos(24, 2, 8), new BlockPos(8, 2, 24)};
        for (int index = 0; index < positions.length; index++) {
            BlockPos pos = positions[index];
            BlockPos absolute = test.absolutePos(pos);
            BloomContent.CherryWood wood = BloomContent.WOODS.get(index);
            var sapling = wood.sapling().get();
            test.setBlock(pos.below(), Blocks.GRASS_BLOCK);
            test.setBlock(pos, sapling);
            BonemealableBlock bonemealable = sapling;
            for (int attempt = 0; attempt < 4 && test.getBlockState(pos).is(sapling); attempt++) {
                bonemealable.performBonemeal(test.getLevel(), test.getLevel().random, absolute, test.getBlockState(pos));
            }
            var log = wood.woodShapes().getFirst().get();
            test.assertBlockPresent(log, pos);
            test.assertTrue(test.getBlockState(pos).is(BlockTags.LOGS), "Grown log was absent from the logs tag");
            Player player = test.makeMockPlayer(GameType.SURVIVAL);
            ItemStack axe = Items.IRON_AXE.getDefaultInstance();
            player.setItemInHand(InteractionHand.MAIN_HAND, axe);
            var changed = test.getBlockState(pos).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)
                .getToolModifiedState(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(absolute)), ItemAbilities.AXE_STRIP, true);
            test.assertTrue(changed != null && changed.is(wood.woodShapes().get(2).get())
                && changed.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "Axe stripping lost the log type or axis");
        }
        test.succeed();
    }

    @GameTest(template = "clearing")
    public static void signsPlaceAndKeepTheirText(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        for (int index = 0; index < BloomContent.WOODS.size(); index++) {
            var signs = BloomContent.WOODS.get(index).signs();
            BlockPos base = STATION.south(index * 5);
            checkCraftingResult(test, signs.signItem().get(), 3);
            checkCraftingResult(test, signs.hangingItem().get(), 6);
            placeSign(test, player, base, Direction.UP, signs.signItem().get(), signs.standing().get());
            placeSign(test, player, base.east(5), Direction.NORTH, signs.signItem().get(), signs.wall().get());
            placeSign(test, player, base.east(10), Direction.DOWN, signs.hangingItem().get(), signs.hanging().get());
            placeSign(test, player, base.east(15), Direction.EAST, signs.hangingItem().get(), signs.wallHanging().get());
        }
        test.succeed();
    }

    private static void placeSign(GameTestHelper test, Player player, BlockPos pos, Direction side, Item item, Block expected) {
        BlockPos support = pos.relative(side.getOpposite());
        test.setBlock(support, Blocks.STONE);
        ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var target = new BlockHitResult(Vec3.atCenterOf(test.absolutePos(support)), side, test.absolutePos(support), false);
        test.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, target)).consumesAction(),
            "Sign item could not be placed against " + side);
        test.assertBlockPresent(expected, pos);

        var level = test.getLevel();
        BlockPos absolute = test.absolutePos(pos);
        var blockEntity = level.getBlockEntity(absolute);
        test.assertTrue(blockEntity instanceof SignBlockEntity, "Sign did not create its block entity");
        SignBlockEntity sign = (SignBlockEntity) blockEntity;
        Component text = Component.literal("Hanami");
        sign.setText(sign.getFrontText().setMessage(0, text), true);
        var state = level.getBlockState(absolute);
        var restored = BlockEntity.loadStatic(absolute, state, sign.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
        test.assertTrue(restored instanceof SignBlockEntity loaded && loaded.getFrontText().getMessage(0, false).equals(text),
            "Sign text did not survive serialization");
        test.assertTrue(Block.getDrops(state, level, absolute, sign).stream().anyMatch(drop -> drop.is(item)),
            "Sign did not drop its own item");
    }

    @GameTest(template = "clearing")
    public static void dispensersLaunchBoatsWithMatchingDrops(GameTestHelper test) {
        for (int index = 0; index < BloomContent.WOODS.size(); index++) {
            var wood = BloomContent.WOODS.get(index);
            var boats = wood.boats();
            var type = boats.type().getValue();
            test.assertTrue(type.getPlanks() == wood.planks().get(), "Boat variant has incorrect planks");
            checkCraftingResult(test, boats.boat().get(), 1);
            checkCraftingResult(test, boats.chestBoat().get(), 1);
            dispenseBoat(test, STATION.south(index * 8), type, boats.boat().get(), false);
            dispenseBoat(test, STATION.south(index * 8 + 4), type, boats.chestBoat().get(), true);
        }
        test.runAfterDelay(10, test::succeed);
    }

    private static void dispenseBoat(GameTestHelper test, BlockPos pos, Boat.Type variant, Item item, boolean chest) {
        test.setBlock(pos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        test.setBlock(pos.east().below(), Blocks.STONE);
        test.setBlock(pos.east(), Blocks.WATER);
        DispenserBlockEntity dispenser = (DispenserBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(pos));
        dispenser.setItem(0, new ItemStack(item));
        test.setBlock(pos.west(), Blocks.REDSTONE_BLOCK);
        test.runAfterDelay(8, () -> {
            var level = test.getLevel();
            var box = new AABB(test.absolutePos(pos.east())).inflate(1.5);
            var actual = level.getEntitiesOfClass(Boat.class, box);
            test.assertTrue(actual.size() == 1 && dispenser.getItem(0).isEmpty(), "Dispenser did not launch exactly one boat");
            Boat placed = actual.getFirst();
            test.assertTrue((placed instanceof ChestBoat) == chest && placed.getVariant() == variant && placed.getDropItem() == item,
                "Dispenser launched the wrong boat variant");
            Boat restored = (Boat) placed.getType().create(level);
            restored.load(placed.saveWithoutId(new CompoundTag()));
            test.assertTrue(restored.getVariant() == variant && restored.getDropItem() == item,
                "Boat variant did not survive serialization");
        });
    }

    private static void checkCraftingResult(GameTestHelper test, Item item, int count) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        var recipe = test.getLevel().getRecipeManager().byKey(id);
        test.assertTrue(recipe.isPresent(), "Missing crafting recipe for " + id);
        var output = recipe.orElseThrow().value().getResultItem(test.getLevel().registryAccess());
        test.assertTrue(output.is(item) && output.getCount() == count, "Invalid crafting result for " + id);
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    private static boolean use(GameTestHelper test, BlockPos pos, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var state = test.getBlockState(pos);
        var target = hit(test.absolutePos(pos));
        var result = state.useItemOn(stack, test.getLevel(), player, InteractionHand.MAIN_HAND, target);
        return result.consumesAction() || (result == net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
            && state.useWithoutItem(test.getLevel(), player, target).consumesAction());
    }
}
