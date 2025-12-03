package zgoly.meteorist.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.player.Reach;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class InteractCommand extends Command {
    public InteractCommand() {
        super("interact", "Interact with closest block.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(argument("block", ItemArgument.item(REGISTRY_ACCESS)).executes(context -> {
            Item item = ItemArgument.getItem(context, "block").getItem();
            if (item instanceof BlockItem blockItem) {
                BlockPos blockPos = findClosestBlock(blockItem.getBlock());
                if (blockPos != null) {
                    BlockUtils.interact(new BlockHitResult(Vec3.atCenterOf(blockPos), Direction.UP, blockPos, true), InteractionHand.MAIN_HAND, true);
                } else {
                    error("No block found");
                }
            } else {
                error("Provided item is not a block");
            }
            return SINGLE_SUCCESS;
        }));
    }

    public BlockPos findClosestBlock(Block block) {
        int reach = (int) new Reach().blockReach();
        for (BlockPos pos : BlockPos.withinManhattan(mc.player.blockPosition(), reach, reach, reach)) {
            if (mc.level.getBlockState(pos).getBlock() == block) {
                return pos;
            }
        }
        return null;
    }
}
