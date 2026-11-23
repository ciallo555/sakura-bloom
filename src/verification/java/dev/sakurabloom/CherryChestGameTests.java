package dev.sakurabloom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public final class CherryChestGameTests {
    @GameTest(template = "clearing")
    public static void cherryChestsCraftJoinAndKeepItems(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0);
        for (int index = 0; index < BloomContent.WOODS.size(); index++) {
            var wood = BloomContent.WOODS.get(index);
            var chest = wood.chest().get();
            var plank = wood.planks().asItem().getDefaultInstance();
            var grid = CraftingInput.of(3,3,List.of(plank,plank,plank,plank,Items.IRON_NUGGET.getDefaultInstance(),plank,plank,plank,plank));
            var recipes = level.getRecipeManager();
            test.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow().value()
                .assemble(grid, level.registryAccess()).is(chest.asItem()), "Wrong chest wood");
            var boat = CraftingInput.of(2,1,List.of(chest.asItem().getDefaultInstance(), wood.boats().boat().toStack()));
            test.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, boat, level).orElseThrow().value()
                .assemble(boat, level.registryAccess()).is(wood.boats().chestBoat().get()), "Chest boat rejected cherry chest");
            var pos = new BlockPos(3,2,3+index*3);
            var absolute = test.absolutePos(pos);
            for (int dx = 0; dx < 3; dx++) {
                var at = pos.east(dx);
                test.setBlock(at.below(), Blocks.STONE);
                var item = dx == 2 ? BloomContent.WOODS.get((index+1)%3).chest().asItem() : chest.asItem();
                var stack = item.getDefaultInstance();
                if (dx == 0) stack.set(DataComponents.CUSTOM_NAME, Component.literal("Sakura"));
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                var floor = test.absolutePos(at.below());
                stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false)));
                if (dx == 0) test.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, absolute, null).getSlots() == 27, "Single inventory size");
            }
            var state = test.getBlockState(pos);
            var inventory = ChestBlock.getContainer(chest, state, level, absolute, false);
            test.assertTrue(inventory != null && inventory.getContainerSize() == 54, "Matching wood failed to form a double chest");
            var menu = (ChestMenu) state.getMenuProvider(level,absolute).createMenu(1,player.getInventory(),player);
            test.assertTrue(menu != null && menu.getRowCount() == 6, "Double chest must open the six-row vanilla menu");
            menu.removed(player);
            test.assertTrue(test.getBlockState(pos.east(2)).getValue(ChestBlock.TYPE) == ChestType.SINGLE, "Mixed woods joined");
            var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, absolute, Direction.UP);
            test.assertTrue(handler.getSlots() == 54 && handler.insertItem(49, new ItemStack(Items.APPLE,3), false).isEmpty(),
                "Double chest must accept items through hoppers");
            test.assertTrue(inventory.getItem(49).getCount() == 3, "Shared slots did not update");
            var entity = (CherryChestBlock.Entity) level.getBlockEntity(absolute);
            entity.setItem(2, new ItemStack(Items.CARROT,5));
            var saved = (CherryChestBlock.Entity) BlockEntity.loadStatic(absolute, state,
                entity.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            test.assertTrue(saved != null && saved.getItem(2).getCount() == 5 && saved.getCustomName().getString().equals("Sakura"), "Chest save lost contents or name");
            var drops = Block.getDrops(state, level, absolute, entity);
            test.assertTrue(drops.size() == 1 && drops.getFirst().is(chest.asItem()) && drops.getFirst().getHoverName().getString().equals("Sakura"), "Incorrect named chest drop");
            test.setBlock(pos.above(), Blocks.STONE);
            test.assertTrue(ChestBlock.getContainer(chest, state, level, absolute, false) == null, "Blocked lid could open");
            test.setBlock(pos.east(), Blocks.AIR);
            var remaining = level.getCapability(Capabilities.ItemHandler.BLOCK, absolute, Direction.UP);
            test.assertTrue(remaining.getSlots() == 27 && remaining.extractItem(2,5,false).getCount() == 5,
                "Remaining chest must expose its original 27 slots after splitting");
        }
        test.succeed();
    }
}
