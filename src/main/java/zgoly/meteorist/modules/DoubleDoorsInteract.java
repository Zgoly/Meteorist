package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import zgoly.meteorist.Meteorist;

public class DoubleDoorsInteract extends Module {
    boolean isInteracting = false;

    public DoubleDoorsInteract() {
        super(Meteorist.CATEGORY, "double-doors-interact", "Open both doors with one interaction.");
    }

    @EventHandler
    private void onInteract(InteractBlockEvent event) {
        if (isInteracting) return;

        isInteracting = true;
        BlockPos doorPos = event.result.getBlockPos();
        BlockState blockState = mc.level.getBlockState(doorPos);
        if (blockState.getBlock() instanceof DoorBlock) {
            Direction doorFacing = blockState.getValue(DoorBlock.FACING);

            DoorHingeSide doorHinge = blockState.getValue(DoorBlock.HINGE);
            BlockPos otherDoorPos;
            if (doorHinge == DoorHingeSide.LEFT) {
                otherDoorPos = doorPos.relative(doorFacing.getClockWise());
            } else {
                otherDoorPos = doorPos.relative(doorFacing.getCounterClockWise());
            }

            BlockState otherBlockState = mc.level.getBlockState(otherDoorPos);
            if (otherBlockState.getBlock() instanceof DoorBlock) {
                if (blockState.getValue(DoorBlock.HALF) == otherBlockState.getValue(DoorBlock.HALF)
                        && blockState.getValue(DoorBlock.HINGE) != otherBlockState.getValue(DoorBlock.HINGE)
                        && blockState.getValue(DoorBlock.OPEN) == otherBlockState.getValue(DoorBlock.OPEN)) {
                    BlockUtils.interact(new BlockHitResult(Utils.vec3d(otherDoorPos), Direction.UP, otherDoorPos, false), InteractionHand.MAIN_HAND, false);
                }
            }
        }

        isInteracting = false;
    }
}
