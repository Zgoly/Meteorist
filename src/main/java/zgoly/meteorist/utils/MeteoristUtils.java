package zgoly.meteorist.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteoristUtils {
    /**
     * Determines if the given box collides with any entities.
     *
     * @param box the box to check for collisions
     * @return true if the box collides with any entities, false otherwise
     */
    public static boolean isCollidesEntity(Box box) {
        if (mc.world == null) return false;
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity)) return false;
            if (box.intersects(entity.getBoundingBox())) return true;
        }
        return false;
    }

    /**
     * Check if the given block position collides with any entities.
     *
     * @param blockPos the block position to check for collisions
     * @return true if the block position collides with an entity, false otherwise
     */
    public static boolean isCollidesEntity(BlockPos blockPos) {
        return isCollidesEntity(new Box(blockPos));
    }

    /**
     * Retrieves the target that the crosshair is currently pointing at within a given range.
     *
     * @param entity       the entity from which the crosshair originates
     * @param range        the maximum range to the target
     * @param ignoreBlocks determines whether blocks should be ignored when checking for targets
     * @param filter       a predicate used to filter potential targets
     * @return the hit result representing the target that the crosshair is pointing at, or null if no target is found
     */
    public static HitResult getCrosshairTarget(Entity entity, double range, boolean ignoreBlocks, Predicate<Entity> filter) {
        if (entity == null || mc.world == null) return null;

        Vec3d vec3d = entity.getCameraPosVec(1);
        Vec3d vec3d2 = entity.getRotationVec(1);
        Vec3d vec3d3 = vec3d.add(vec3d2.multiply(range));
        Box box = entity.getBoundingBox().stretch(vec3d2.multiply(range)).expand(1);

        RaycastContext raycastContext = new RaycastContext(vec3d, vec3d3, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);
        HitResult hitResult = mc.world.raycast(raycastContext);

        double e = range * range;
        if (hitResult != null && !ignoreBlocks) e = hitResult.getPos().squaredDistanceTo(vec3d);

        EntityHitResult entityHitResult = ProjectileUtil.raycast(entity, vec3d, vec3d3, box, filter.and(targetEntity -> !targetEntity.isSpectator()), e);
        if (entityHitResult != null) {
            return entityHitResult;
        } else if (!ignoreBlocks) {
            return hitResult;
        }

        return null;
    }

    /**
     * Removes invalid characters from the given string. Not fast, but cross-platform.
     *
     * @param text the string to remove invalid characters from
     * @return the string with invalid characters removed
     */
    public static String removeInvalidChars(final String text) {
        try {
            Paths.get(text);
            return text;
        } catch (final InvalidPathException e) {
            if (e.getInput() != null && !e.getInput().isEmpty() && e.getIndex() >= 0) {
                final StringBuilder stringBuilder = new StringBuilder(e.getInput());
                stringBuilder.deleteCharAt(e.getIndex());
                return removeInvalidChars(stringBuilder.toString());
            }
            throw e;
        }
    }

    /**
     * Converts a given number of ticks into a human-readable time format.
     * <p>
     * This method takes the number of ticks and converts it into hours, minutes, and seconds.
     * It then formats the result into a string that highlights the time components.
     *
     * @param ticks the number of ticks to convert
     * @return a formatted string representing the time in hours, minutes, and seconds
     */
    public static String ticksToTime(int ticks) {
        int ticksPerSecond = 20;
        int ticksPerMinute = ticksPerSecond * 60;
        int ticksPerHour = ticksPerMinute * 60;

        int hours = ticks / ticksPerHour;
        int minutes = (ticks % ticksPerHour) / ticksPerMinute;
        int seconds = (ticks % ticksPerMinute) / ticksPerSecond;

        StringBuilder result = new StringBuilder();

        if (hours > 0) {
            result.append(String.format("(highlight)%d(default) hour%s", hours, hours > 1 ? "s" : ""));
            if (minutes > 0 || seconds > 0) result.append(", ");
        }

        if (minutes > 0) {
            result.append(String.format("(highlight)%d(default) minute%s", minutes, minutes > 1 ? "s" : ""));
            if (seconds > 0) result.append(", ");
        }

        if (seconds > 0) {
            result.append(String.format("(highlight)%d(default) second%s", seconds, seconds > 1 ? "s" : ""));
        }

        return result.toString();
    }

    /**
     * Calculates the FOV angle in degrees between the player's look direction and the target.
     *
     * @param player the player entity
     * @param target the target entity
     * @return the FOV angle in degrees
     */
    public static float calculateFov(LivingEntity player, Entity target) {
        Vec3d lookDirection = player.getRotationVec(1.0F);
        Vec3d targetDirection = target.getPos().subtract(player.getPos()).normalize();

        return (float) Math.toDegrees(Math.acos(lookDirection.dotProduct(targetDirection)));
    }
}