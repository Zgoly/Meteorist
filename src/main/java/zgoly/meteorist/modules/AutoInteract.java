package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.mixin.TrapdoorBlockAccessor;

import java.util.List;

public class AutoInteract extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("The block to interact with.")
            .filter(block -> block instanceof DoorBlock || block instanceof FenceGateBlock || block instanceof TrapDoorBlock || block instanceof ButtonBlock || block instanceof LeverBlock)
            .defaultValue(BuiltInRegistries.BLOCK.stream().filter(block -> block instanceof DoorBlock || block instanceof FenceGateBlock).toList())
            .build()
    );
    private final Setting<Double> innerRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("inner-range")
            .description("The range to interact with blocks.")
            .defaultValue(3)
            .min(0)
            .build()
    );
    private final Setting<Integer> outerRange = sgGeneral.add(new IntSetting.Builder()
            .name("outer-range")
            .description("The range to stop interacting with blocks.")
            .defaultValue(4)
            .min(1)
            .build()
    );
    private final Setting<BlockPos> rangePosOffset = sgGeneral.add(new BlockPosSetting.Builder()
            .name("range-pos-offset")
            .description("The offset of the range position.")
            .defaultValue(new BlockPos(0, 1, 0))
            .build()
    );
    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
            .name("swing-hand")
            .description("Swing hand client-side.")
            .defaultValue(true)
            .build()
    );

    public AutoInteract() {
        super(Meteorist.CATEGORY, "auto-interact", "Automatically interacts with interactable blocks like doors, trapdoors, etc.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (BlockPos blockPos : BlockPos.withinManhattan(mc.player.blockPosition().offset(rangePosOffset.get()), outerRange.get(), outerRange.get(), outerRange.get())) {
            BlockState blockState = mc.level.getBlockState(blockPos);

            if (blockState.getBlock() instanceof DoorBlock && blockState.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER)
                continue;
            if (blockState.getBlock() instanceof DoorBlock doorBlock && !doorBlock.type().canOpenByHand())
                continue;
            if (blockState.getBlock() instanceof TrapDoorBlock trapdoorBlock && !((TrapdoorBlockAccessor) trapdoorBlock).invokeGetBlockSetType().canOpenByHand())
                continue;

            if (blocks.get().contains(blockState.getBlock())) {
                boolean shouldOpen = PlayerUtils.distanceTo(blockPos.getCenter()) <= innerRange.get();
                boolean isOpen = switch (blockState.getBlock()) {
                    case DoorBlock ignored -> blockState.getValue(DoorBlock.OPEN);
                    case FenceGateBlock ignored -> blockState.getValue(FenceGateBlock.OPEN);
                    case TrapDoorBlock ignored -> blockState.getValue(TrapDoorBlock.OPEN);
                    case ButtonBlock ignored -> blockState.getValue(ButtonBlock.POWERED);
                    case LeverBlock ignored -> blockState.getValue(LeverBlock.POWERED);
                    default -> false;
                };

                if (shouldOpen != isOpen) {
                    BlockUtils.interact(new BlockHitResult(Utils.vec3d(blockPos), Direction.UP, blockPos, false), InteractionHand.MAIN_HAND, swingHand.get());
                    break;
                }
            }
        }
    }
}
