package zgoly.meteorist.utils.movement;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalNear;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public class MeteoristBaritoneUtils {
    BaritoneOperations baritoneOperations;

    public MeteoristBaritoneUtils() {
        if (BaritoneUtils.IS_AVAILABLE) {
            baritoneOperations = new BasedBaritoneOperations();
        } else {
            baritoneOperations = new NoBaritoneOperations();
        }
    }

    public void cancelEverything() {
        baritoneOperations.cancelEverything();
    }

    public boolean isPathing() {
        return baritoneOperations.isPathing();
    }

    public void setGoalNear(BlockPos blockPos, int range) {
        baritoneOperations.setGoalNear(blockPos, range);
    }

    public void setGoalNear(List<BlockPos> positions, int range) {
        baritoneOperations.setGoalNear(positions, range);
    }

    public void setBlockBreaking(boolean allowed) {
        baritoneOperations.setBlockBreaking(allowed);
    }

    public interface BaritoneOperations {
        void cancelEverything();

        boolean isPathing();

        void setGoalNear(BlockPos blockPos, int range);

        void setGoalNear(List<BlockPos> positions, int range);

        void setBlockBreaking(boolean allowed);
    }

    public static class BasedBaritoneOperations implements BaritoneOperations {
        private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        private final Settings settings = BaritoneAPI.getSettings();
        private static final List<Block> ALL_BLOCKS = StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(), false).distinct().toList();

        private boolean blockBreakingAllowed = true;
        private boolean prevAllowBreak;
        private List<Block> prevAllowBreakAnyway;
        private List<Block> prevBlocksToDisallowBreaking;

        public void cancelEverything() {
            baritone.getPathingBehavior().cancelEverything();
        }

        public boolean isPathing() {
            return baritone.getPathingBehavior().isPathing();
        }

        public void setGoalNear(BlockPos blockPos, int range) {
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalNear(blockPos, range));
        }

        public void setGoalNear(List<BlockPos> positions, int range) {
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalComposite(positions.stream().map(position -> new GoalNear(position, range)).toArray(Goal[]::new)));
        }

        @Override
        public void setBlockBreaking(boolean allowed) {
            if (allowed == blockBreakingAllowed) return;
            if (allowed) {
                settings.allowBreak.value = prevAllowBreak;
                settings.allowBreakAnyway.value = prevAllowBreakAnyway;
                settings.blocksToDisallowBreaking.value = prevBlocksToDisallowBreaking;
            } else {
                prevAllowBreak = settings.allowBreak.value;
                prevAllowBreakAnyway = settings.allowBreakAnyway.value;
                prevBlocksToDisallowBreaking = settings.blocksToDisallowBreaking.value;
                settings.allowBreak.value = false;
                settings.allowBreakAnyway.value = new ArrayList<>();
                settings.blocksToDisallowBreaking.value = new ArrayList<>(ALL_BLOCKS);
            }
            blockBreakingAllowed = allowed;
        }
    }

    public static class NoBaritoneOperations implements BaritoneOperations {
        public void cancelEverything() {
        }

        public boolean isPathing() {
            return false;
        }

        public void setGoalNear(BlockPos blockPos, int range) {
        }

        public void setGoalNear(List<BlockPos> positions, int range) {
        }

        @Override
        public void setBlockBreaking(boolean allowed) {
        }
    }
}
