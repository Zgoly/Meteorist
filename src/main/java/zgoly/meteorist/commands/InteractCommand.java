package zgoly.meteorist.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.player.Reach;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class InteractCommand extends Command {
    public InteractCommand() {
        super("interact", "Interact with closest block.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("block", ItemStackArgumentType.itemStack(REGISTRY_ACCESS)).executes(context -> {
            Item item = ItemStackArgumentType.getItemStackArgument(context, "block").getItem();
            if (item instanceof BlockItem blockItem) {
                BlockPos blockPos = findClosestBlock(blockItem.getBlock());
                if (blockPos != null) {
                    BlockUtils.interact(new BlockHitResult(Vec3d.ofCenter(blockPos), Direction.UP, blockPos, true), Hand.MAIN_HAND, true);
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
        for (BlockPos pos : BlockPos.iterateOutwards(mc.player.getBlockPos(), reach, reach, reach)) {
            if (mc.world.getBlockState(pos).getBlock() == block) {
                return pos;
            }
        }
        return null;
    }
}
