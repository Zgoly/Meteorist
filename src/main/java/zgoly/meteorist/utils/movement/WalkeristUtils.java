package zgoly.meteorist.utils.movement;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.utils.player.CustomPlayerInput;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

import zgoly.meteorist.modules.walkerist.Walkerist;
import zgoly.meteorist.modules.walkerist.Waypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WalkeristUtils {
    public enum State { Idle, Searching, Walking, Arrived, Stuck }

    public enum LogType { WalkTo, Path, Repath, Arrived, Stuck, Door }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final AtomicInteger searchId = new AtomicInteger();

    private static Walkerist module;
    private static Runnable queueChangeListener;

    public static void setModule(Walkerist module) {
        WalkeristUtils.module = module;
    }

    public static void setQueueChangeListener(Runnable listener) {
        queueChangeListener = listener;
    }

    private static void notifyQueueChange() {
        if (queueChangeListener != null) queueChangeListener.run();
    }
    private static boolean paused;
    private static final List<Waypoint> waypoints = new ArrayList<>();
    private static int current = -1;

    private static final int DEFAULT_ARRIVE_SQR = 2 * 2;
    private static int arriveSqr = DEFAULT_ARRIVE_SQR;
    private static double speedMultiplier = 1.0;

    private static Zombie dummy;
    private static BlockPos target;
    private static Path path;
    private static State state = State.Idle;
    private static int pathIndex, stuckTicks, jumpCooldown, doorGuard, repaths;
    private static double lastDist = Double.MAX_VALUE, bestDist = Double.MAX_VALUE, lastDirX, lastDirZ;
    private static ClientInput prevPlayerInput;
    private static CustomPlayerInput customInput;

    static {
        MeteorClient.EVENT_BUS.subscribe(WalkeristUtils.class);
    }

    private WalkeristUtils() {
    }

    public static void togglePaused() {
        paused = !paused;
        if (paused) releaseInput();
    }

    public static boolean isPaused() {
        return paused;
    }

    private static void applyMaluses() {
        if (dummy == null) return;
        dummy.setPathfindingMalus(PathType.WATER, module.water.get() ? 0.0f : 1.0f);
        dummy.setPathfindingMalus(PathType.RAIL, module.rails.get() ? 0.0f : 1.0f);
        dummy.setPathfindingMalus(PathType.UNPASSABLE_RAIL, module.rails.get() ? 0.0f : 1.0f);
        dummy.setPathfindingMalus(PathType.DOOR_WOOD_CLOSED, module.doors.get() ? 0.0f : 1.0f);
        dummy.setPathfindingMalus(PathType.DOOR_OPEN, module.doors.get() ? 0.0f : 1.0f);
    }

    private static double currentSpeed() {
        Vec3 vel = mc.player.getDeltaMovement();
        return Mth.length(vel.x(), vel.z());
    }

    public static void walkTo(BlockPos pos) {
        arriveSqr = DEFAULT_ARRIVE_SQR;
        walkToInternal(pos);
    }

    public static void walkTo(BlockPos pos, int range) {
        int walkRange = Math.max(range, 2);
        arriveSqr = walkRange * walkRange;
        walkToInternal(pos);
    }

    public static void setSpeedMultiplier(double multiplier) {
        speedMultiplier = Math.max(multiplier, 0);
    }

    public static boolean isBusy() {
        return state == State.Searching || state == State.Walking;
    }

    public static float steerYaw() {
        if (paused) return Float.NaN;
        BlockPos node = currentNode();
        if (node == null || mc.player == null) return Float.NaN;
        double dx = node.getX() + 0.5 - mc.player.getX();
        double dz = node.getZ() + 0.5 - mc.player.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    public static List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public static int getCurrent() {
        return current;
    }

    public static String statusText(int i) {
        if (current == -1 || i > current) return "Pending";
        if (i < current) return "Done";
        return state.name() + (paused ? " (paused)" : "");
    }

    public static void queueAdd(BlockPos pos) {
        queueAdd(pos, module.autoPlay.get());
    }

    public static void queueAdd(BlockPos pos, boolean autoStart) {
        Waypoint waypoint = new Waypoint();
        waypoint.pos.set(pos);
        waypoints.add(waypoint);

        if (autoStart && (current == -1 || current == waypoints.size() - 1)) {
            if (current == -1) current = 0;
            walkTo(waypoints.get(current).pos.get());
        }

        trimQueue();
        notifyQueueChange();
    }

    private static void trimQueue() {
        if (!module.cleanup.get()) return;
        int limit = Math.max(module.cleanupLimit.get(), 1);
        while (waypoints.size() > limit) queueRemove(waypoints.size() - 1);
    }

    public static void queueRemove(int index) {
        boolean wasActive = index == current;
        waypoints.remove(index);

        if (index < current) current--;
        else if (wasActive) {
            stop();
            if (waypoints.isEmpty()) {
                current = -1;
            } else {
                current = Math.min(index, waypoints.size() - 1);
                if (module.autoPlay.get()) walkTo(waypoints.get(current).pos.get());
            }
        }
        notifyQueueChange();
    }

    public static void queueMove(int from, int to) {
        if (from < 0 || from >= waypoints.size() || to < 0 || to > waypoints.size()) return;
        if (from == to) return;

        Waypoint waypoint = waypoints.remove(from);
        waypoints.add(to, waypoint);

        if (current == from) {
            current = to;
        } else {
            if (from < current) current--;
            if (to <= current) current++;
        }

        notifyQueueChange();
    }

    public static void queueSet(int index, BlockPos pos) {
        if (index < 0 || index >= waypoints.size()) return;
        waypoints.get(index).pos.set(pos);
        if (index == current) {
            queueSelect(index);
        } else {
            notifyQueueChange();
        }
    }

    public static void queueClear() {
        stop();
        waypoints.clear();
        current = -1;
        notifyQueueChange();
    }

    public static void queuePlay() {
        if (waypoints.isEmpty()) return;
        if (current == -1 || current == waypoints.size()) current = 0;
        walkTo(waypoints.get(current).pos.get());
    }

    public static void queueSelect(int index) {
        if (index < 0 || index >= waypoints.size()) return;
        current = index;
        target = null;
        walkTo(waypoints.get(current).pos.get());
    }

    public static void queueDeselect() {
        if (current >= 0 && current < waypoints.size()) stop();
        current = -1;
    }

    private static void walkToInternal(BlockPos pos) {
        if (mc.player == null || mc.level == null) return;

        if (pos.equals(target)) repaths++;
        else {
            target = pos;
            repaths = 0;
            bestDist = Double.MAX_VALUE;
        }
        reset();
        if (module.maxRepaths.get() >= 0 && repaths > module.maxRepaths.get()) {
            fail("gave up after " + repaths + " searches without getting closer to " + pos.toShortString());
            return;
        }

        BlockPos center = mc.player.blockPosition();
        Set<BlockPos> goals = Set.of(pos, pos.above(), pos.below());
        state = State.Searching;

        if (dummy == null || dummy.level() != mc.level) {
            dummy = EntityTypes.ZOMBIE.create(mc.level, EntitySpawnReason.COMMAND);
            if (dummy == null) {
                fail("could not create dummy zombie");
                return;
            }
        }
        applyMaluses();
        dummy.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());

        PathNavigationRegion region = new PathNavigationRegion(mc.level,
                center.offset(-module.range.get(), mc.level.getMinY() - center.getY(), -module.range.get()),
                center.offset(module.range.get(), mc.level.getMaxY() - center.getY(), module.range.get()));

        WalkNodeEvaluator evaluator = new WalkNodeEvaluator();
        evaluator.setCanPassDoors(module.doors.get());
        evaluator.setCanOpenDoors(module.doors.get());
        evaluator.setCanFloat(module.water.get());

        int id = searchId.incrementAndGet();
        log(LogType.WalkTo, "walkTo " + pos.toShortString() + " from " + center.toShortString() + (repaths > 0 ? " (repath " + repaths + ")" : ""));

        executor.submit(() -> {
            if (searchId.get() != id) return;

            Path result;
            try {
                result = new PathFinder(evaluator, module.maxNodes.get()).findPath(region, dummy, goals, module.range.get(), 0, 1f);
            } catch (Exception e) {
                if (searchId.get() == id) mc.execute(() -> {
                    if (searchId.get() == id) fail("findPath threw " + e);
                });
                return;
            }

            mc.execute(() -> {
                if (searchId.get() != id) return;

                if (result == null) {
                    fail("no path");
                    return;
                }

                if (result.getNodeCount() <= 1) {
                    BlockPos stand = standCell(pos, region);
                    if (stand.equals(center)) arrive();
                    else if (center.distSqr(pos) <= arriveSqr + 1 && Math.abs(stand.getY() - center.getY()) <= 1) {
                        path = new Path(List.of(new Node(center.getX(), center.getY(), center.getZ()),
                                new Node(stand.getX(), stand.getY(), stand.getZ())), pos, true);
                        state = State.Walking;
                        log(LogType.Path, "direct step to " + stand.toShortString());
                    } else fail("no walkable route from " + center.toShortString() + " to " + pos.toShortString());
                    return;
                }

                path = result;
                state = State.Walking;
                BlockPos end = result.getNodePos(result.getNodeCount() - 1);
                log(LogType.Path, "path: " + result.getNodeCount() + " nodes -> " + end.toShortString()
                        + (goals.contains(end) ? "" : " (partial, target is " + pos.toShortString() + ")"));
            });
        });
    }

    public static void stop() {
        doStop();
    }

    private static void doStop() {
        speedMultiplier = 1.0;
        reset();
        releaseInput();
        target = null;
        repaths = 0;
        bestDist = Double.MAX_VALUE;
    }

    public static Path getPath() {
        return path;
    }

    public static BlockPos getTarget() {
        return target;
    }

    public static State getState() {
        return state;
    }

    public static ListTag saveWaypoints() {
        ListTag list = new ListTag();
        for (Waypoint waypoint : waypoints) {
            CompoundTag mTag = new CompoundTag();
            mTag.put("waypoint", waypoint.toTag());
            list.add(mTag);
        }
        return list;
    }

    public static void loadWaypoints(ListTag list) {
        waypoints.clear();
        current = -1;

        for (Tag tagII : list) {
            if (!(tagII instanceof CompoundTag tagI)) continue;

            Waypoint waypoint = new Waypoint();
            Tag waypointTag = tagI.get("waypoint");
            if (waypointTag instanceof CompoundTag compound) waypoint.fromTag(compound);

            waypoints.add(waypoint);
        }

        trimQueue();
        notifyQueueChange();
    }

    private static void reset() {
        searchId.incrementAndGet();
        path = null;
        pathIndex = stuckTicks = jumpCooldown = doorGuard = 0;
        lastDist = Double.MAX_VALUE;
        lastDirX = lastDirZ = 0;
        paused = false;
        state = State.Idle;
    }

    private static void fail(String reason) {
        state = State.Stuck;
        path = null;
        repaths = 0;
        bestDist = Double.MAX_VALUE;
        releaseInput();
        log(LogType.Stuck, "stuck: " + reason);
    }

    private static void arrive() {
        state = State.Arrived;
        path = null;
        releaseInput();
        log(LogType.Arrived, "arrived at " + target.toShortString());

        advanceQueue();
    }

    private static void advanceQueue() {
        if (!module.autoPlay.get()) return;
        if (current < 0 || current >= waypoints.size()) return;
        if (target == null || !target.equals(waypoints.get(current).pos.get())) return;

        current++;
        if (current >= waypoints.size()) {
            stop();
        } else {
            walkTo(waypoints.get(current).pos.get());
        }
    }

    private static void repath(String why) {
        log(LogType.Repath, why + " -> searching again");
        walkToInternal(target);
    }

    private static void ensureInput() {
        if (mc.player == null) return;
        if (customInput != null) {
            if (mc.player.input != customInput) {
                prevPlayerInput = mc.player.input;
                mc.player.input = customInput;
            }
            return;
        }
        prevPlayerInput = mc.player.input;
        customInput = new CustomPlayerInput();
        mc.player.input = customInput;
    }

    private static void releaseInput() {
        if (customInput != null) {
            customInput.stop();
            if (mc.player != null && mc.player.input == customInput) mc.player.input = prevPlayerInput;
            if (mc.player != null) mc.player.setSprinting(false);
        }
        customInput = null;
        prevPlayerInput = null;
    }

    private static BlockPos currentNode() {
        if (state == State.Walking && path != null && pathIndex < path.getNodeCount()) return path.getNodePos(pathIndex);
        return state == State.Searching ? target : null;
    }

    @EventHandler
    private static void onTick(TickEvent.Pre event) {
        if (module != null) module.updateStatus();

        if (mc.player == null || mc.level == null) return;
        if (state != State.Walking && state != State.Searching) return;
        if (paused) return;
        if (doorGuard > 0) doorGuard--;
        if (module.stealControl.get()) {
            ensureInput();
        } else {
            releaseInput();
        }

        double toTarget = distSqr(target);
        if (toTarget < bestDist - 1) {
            bestDist = toTarget;
            repaths = 0;
        }

        if (state == State.Walking) {
            if (path == null) {
                fail("path became null");
                return;
            }

            int startIndex = pathIndex;
            while (pathIndex < path.getNodeCount() && isAtNode(path.getNodePos(pathIndex))) pathIndex++;
            if (pathIndex != startIndex) { lastDist = Double.MAX_VALUE; stuckTicks = 0; }

            if (pathIndex >= path.getNodeCount()) {
                if (toTarget <= arriveSqr && Math.abs(mc.player.getY() - target.getY()) <= module.arrivedDy.get()) arrive();
                else repath("path ended " + String.format("%.0f", Math.sqrt(toTarget)) + "m short of target");
                return;
            }

            BlockPos node = path.getNodePos(pathIndex);
            double dist = Math.sqrt(distSqr(node));
            if (dist < lastDist - module.progressMin.get()) { lastDist = dist; stuckTicks = 0; }
            else if (++stuckTicks > module.maxStuckTicks.get()) {
                if (node.getY() > mc.player.getY() + module.jumpDy.get() && !hasHeadroom()) fail("ceiling blocks the jump at " + node.toShortString());
                else repath("no progress toward " + node.toShortString() + " (" + String.format("%.1f", dist) + "m away)");
                return;
            }
        }

        BlockPos node = currentNode();
        if (node == null) return;

        Rotations.rotate(Rotations.getYaw(node), mc.player.getXRot(), 1000);

        if (jumpCooldown > 0) jumpCooldown--;
        else if (node.getY() > mc.player.getY() + module.jumpDy.get() && mc.player.onGround() && hasHeadroom()
                && Math.sqrt(distSqr(node)) - 0.5 <= currentSpeed() * module.jumpLead.get()) {
            mc.player.jumpFromGround();
            jumpCooldown = module.jumpCooldown.get();
        }

        boolean inWater = mc.player.isInWater();
        if (customInput != null) {
            customInput.forward(true);
            customInput.sprint(mc.player.getFoodData().hasEnoughFood() && module.sprint.get());
            customInput.jump(inWater);
            if (!module.sprint.get() && mc.player.isSprinting()) mc.player.setSprinting(false);
        }

        if (module.doors.get() && doorGuard <= 0) openDoorAhead();
    }

    @EventHandler
    private static void onPlayerMove(PlayerMoveEvent event) {
        BlockPos node = currentNode();
        if (node == null || paused) return;

        if (module.stealControl.get()) ensureInput();

        double dx = node.getX() + 0.5 - mc.player.getX();
        double dz = node.getZ() + 0.5 - mc.player.getZ();
        double dist = Mth.length(dx, dz);
        if (dist < 1e-4) {
            dx = lastDirX; dz = lastDirZ; dist = Mth.length(dx, dz);
            if (dist < 1e-4) return;
        } else {
            lastDirX = dx / dist; lastDirZ = dz / dist; dx = lastDirX; dz = lastDirZ;
        }

        double x = event.movement.x(), z = event.movement.z();
        double hSpeed = Mth.length(x, z) * Math.max(speedMultiplier, 1.0);
        ((IVec3) event.movement).meteor$set(dx * hSpeed, event.movement.y(), dz * hSpeed);
    }

    private static void openDoorAhead() {
        if (path == null) return;

        for (int i = pathIndex; i < Math.min(path.getNodeCount(), pathIndex + 4); i++) {
            BlockPos node = path.getNodePos(i);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 1; dy++) {
                BlockPos pos = node.offset(dx, dy, dz);
                BlockState s = mc.level.getBlockState(pos);
                if (!(s.getBlock() instanceof DoorBlock) || s.getValue(DoorBlock.OPEN)) continue;
                if (!mc.player.isWithinBlockInteractionRange(pos, 0)) continue;
                if (s.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) pos = pos.below();

                Vec3 eye = mc.player.getEyePosition(), hit = Vec3.atCenterOf(pos), diff = hit.subtract(eye);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit,
                        Direction.getApproximateNearest(diff.x(), diff.y(), diff.z()).getOpposite(), pos, false));
                doorGuard = 2;
                log(LogType.Door, "opened door at " + pos.toShortString());
                return;
            }
        }
    }

    private static BlockPos standCell(BlockPos pos, PathNavigationRegion region) {
        return region.getBlockState(pos).getCollisionShape(region, pos).isEmpty() ? pos : pos.above();
    }

    private static boolean hasHeadroom() {
        BlockPos feet = mc.player.blockPosition();
        return mc.level.getBlockState(feet.above()).getCollisionShape(mc.level, feet.above()).isEmpty()
                && mc.level.getBlockState(feet.above(2)).getCollisionShape(mc.level, feet.above(2)).isEmpty();
    }

    private static boolean isAtNode(BlockPos pos) {
        double reach = module.nodeBox.get() + currentSpeed() * 0.5;
        if (mc.player.isInWater()) return distSqr(pos) < reach * reach;
        return Math.abs(mc.player.getX() - pos.getX() - 0.5) < reach && Math.abs(mc.player.getZ() - pos.getZ() - 0.5) < reach
                && Math.abs(mc.player.getY() - pos.getY()) < module.nodeDy.get();
    }

    private static double distSqr(BlockPos pos) {
        return pos == null ? Double.MAX_VALUE : mc.player.distanceToSqr(pos.getX() + 0.5, mc.player.getY(), pos.getZ() + 0.5);
    }

    private static void log(LogType type, String msg) {
        if (!module.isLogEnabled(type)) return;
        if (mc.player != null) mc.execute(() -> module.info(msg));
    }
}
