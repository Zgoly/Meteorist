package zgoly.meteorist.utils.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalNear;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.List;

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

    public void setGoalNear(List<Entity> entities, int range) {
        baritoneOperations.setGoalNear(entities, range);
    }

    public interface BaritoneOperations {
        void cancelEverything();

        boolean isPathing();

        void setGoalNear(BlockPos blockPos, int range);

        void setGoalNear(List<Entity> entities, int range);
    }

    public static class BasedBaritoneOperations implements BaritoneOperations {
        private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();

        public void cancelEverything() {
            baritone.getPathingBehavior().cancelEverything();
        }

        public boolean isPathing() {
            return baritone.getPathingBehavior().isPathing();
        }

        public void setGoalNear(BlockPos blockPos, int range) {
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalNear(blockPos, range));
        }

        public void setGoalNear(List<Entity> entities, int range) {
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalComposite(entities.stream().map(entity -> new GoalNear(entity.getBlockPos(), range)).toArray(Goal[]::new)));
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

        public void setGoalNear(List<Entity> entities, int range) {
        }
    }
}