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

import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteoristUtils {
    /**
     * Determines if the given box collides with any entities.
     *
     * @param  box  the box to check for collisions
     * @return      true if the box collides with any entities, false otherwise
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
     * @param  blockPos  the block position to check for collisions
     * @return           true if the block position collides with an entity, false otherwise
     */
    public static boolean isCollidesEntity(BlockPos blockPos) {
        return isCollidesEntity(new Box(blockPos));
    }

    /**
     * Retrieves the target that the crosshair is currently pointing at within a given range.
     *
     * @param  entity       the entity from which the crosshair originates
     * @param  range        the maximum range to the target
     * @param  ignoreBlocks determines whether blocks should be ignored when checking for targets
     * @param  filter       a predicate used to filter potential targets
     * @return              the hit result representing the target that the crosshair is pointing at, or null if no target is found
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
}