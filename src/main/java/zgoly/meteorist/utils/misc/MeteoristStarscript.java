package zgoly.meteorist.utils.misc;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.starscript.value.Value;
import meteordevelopment.starscript.value.ValueMap;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteoristStarscript {
    private static double maxStartY = Double.MIN_VALUE;
    private static double maxFallDamage = 0;

    public static void init() {
        MeteorStarscript.ss.set("meteorist", new ValueMap()
                .set("fall_distance", () -> Value.number(getDistance(false)))
                .set("max_fall_distance", () -> Value.number(getDistance(true)))
                .set("fall_damage", () -> Value.number(getFallDamage(false)))
                .set("max_fall_damage", () -> Value.number(getFallDamage(true)))
        );
    }

    private static double getDistance(boolean max) {
        if (mc.player == null || mc.world == null) return 0;

        double startY = mc.player.getPos().getY();
        if (mc.player.isOnGround()) {
            maxStartY = startY;
            return 0;
        }

        if (startY > maxStartY) maxStartY = startY;

        Vec3d start = mc.player.getPos();
        if (max) start = new Vec3d(start.x, maxStartY, start.z);

        BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(
                start,
                new Vec3d(mc.player.getX(), mc.world.getBottomY(), mc.player.getZ()),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.ANY,
                mc.player
        ));

        if (blockHitResult == null) return 0;
        return blockHitResult.getPos().distanceTo(start);
    }

    private static double getFallDamage(boolean max) {
        if (mc.player == null) return 0;

        float damageTaken = 0;
        float damage = !Modules.get().isActive(NoFall.class) && !EntityUtils.isAboveWater(mc.player) ? DamageUtils.fallDamage(mc.player) : 0;

        damageTaken = Math.max(damageTaken, damage);

        if (max) {
            maxFallDamage = Math.max(maxFallDamage, damageTaken);
            if (mc.player.isOnGround()) maxFallDamage = 0;
            return maxFallDamage;
        }

        return damageTaken;
    }
}
