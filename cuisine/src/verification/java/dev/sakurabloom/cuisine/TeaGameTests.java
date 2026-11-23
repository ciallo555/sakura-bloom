package dev.sakurabloom.cuisine;

import dev.sakurabloom.SakuraBloom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SakuraBloom.ID)
@PrefixGameTestTemplate(false)
public final class TeaGameTests {
    private static final BlockPos STATION = new BlockPos(2, 2, 2);

    @GameTest(template = "clearing", timeoutTicks = 110)
    public static void kettleBrewsEachTea(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        var brews = TeaKettleBlock.Stage.BREWS;
        for (int index = 0; index < brews.size(); index++) {
            BlockPos pos = STATION.offset(index * 3, 0, 0);
            test.setBlock(pos, KitchenContent.KETTLE.get());
            ItemStack water = TeaKettleBlock.waterBottle();
            ItemStack petals = brews.get(index).petalsStack();
            test.assertTrue(use(test, pos, player, water) && water.isEmpty(), "Water bottle was not consumed");
            test.assertTrue(signal(test, pos) == 4, "The full kettle did not emit signal strength four");
            test.assertTrue(use(test, pos, player, petals) && petals.isEmpty(), "Petal was not consumed");
            test.assertTrue(signal(test, pos) == 8, "The brewing kettle did not emit signal strength eight");
        }
        test.runAfterDelay(85, () -> {
            for (int index = 0; index < brews.size(); index++) {
                BlockPos pos = STATION.offset(index * 3, 0, 0);
                test.assertTrue(signal(test, pos) == 15, "The brewed kettle did not emit its full signal");
                test.assertTrue(use(test, pos, player, ItemStack.EMPTY), "Tea could not be collected");
                test.assertTrue(signal(test, pos) == 0, "The kettle did not empty");
                test.assertTrue(player.getInventory().countItem(brews.get(index).brewedTea().getItem()) == 1,
                    "The kettle did not yield the matching tea");
            }
            test.succeed();
        });
    }

    private static int signal(GameTestHelper test, BlockPos pos) {
        return test.getBlockState(pos).getAnalogOutputSignal(test.getLevel(), test.absolutePos(pos));
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
