package zgoly.meteorist.utils.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for scheduling tasks to be executed after a certain number of ticks.
 * <p>
 * This class listens to {@link TickEvent.Pre} and manages a list of scheduled tasks.
 * Each task will be executed once its delay reaches zero.
 */
public class TickScheduler {
    /**
     * List of currently scheduled tasks.
     */
    private final List<ScheduledTask> tasks = new ArrayList<>();

    /**
     * Handles the tick event by decrementing the delay of all scheduled tasks.
     * Executes and removes any tasks whose delay has reached zero.
     *
     * @param event The tick event (pre-tick phase).
     */
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        List<ScheduledTask> toRemove = new ArrayList<>();
        for (ScheduledTask task : tasks) {
            task.delay--;
            if (task.delay <= 0) {
                task.runnable.run();
                toRemove.add(task);
            }
        }
        tasks.removeAll(toRemove);
    }

    /**
     * Schedules a task to run after the specified delay in ticks.
     *
     * @param task  The task to be executed.
     * @param delay The delay in ticks before the task should be executed.
     */
    public void runTaskLater(Runnable task, int delay) {
        tasks.add(new ScheduledTask(task, delay));
    }

    /**
     * Cancels all scheduled tasks.
     * <p>
     * This method clears the list of pending tasks without executing them.
     */
    public void cancelTasks() {
        tasks.clear();
    }

    /**
     * Represents a task that is scheduled to run after a certain delay.
     */
    private static class ScheduledTask {
        /**
         * The task to execute.
         */
        private final Runnable runnable;
        /**
         * The remaining delay in ticks before execution.
         */
        private int delay;

        /**
         * Constructs a new scheduled task with the given runnable and delay.
         *
         * @param runnable The task to execute.
         * @param delay    The initial delay in ticks.
         */
        private ScheduledTask(Runnable runnable, int delay) {
            this.runnable = runnable;
            this.delay = delay;
        }
    }
}
