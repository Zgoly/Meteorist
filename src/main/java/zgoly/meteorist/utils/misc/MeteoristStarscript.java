package zgoly.meteorist.utils.misc;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.meteordev.starscript.value.Value;
import org.meteordev.starscript.value.ValueMap;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteoristStarscript {
    private static double maxStartY = Double.MIN_VALUE;
    private static double maxFallDamage = 0;

    // TODO Rewrite
    public static void init() {
        MeteorStarscript.ss.set("meteorist", new ValueMap()
                .set("fall_distance", () -> Value.number(getDistance(false)))
                .set("max_fall_distance", () -> Value.number(getDistance(true)))
                .set("fall_damage", () -> Value.number(getFallDamage(false)))
                .set("max_fall_damage", () -> Value.number(getFallDamage(true)))
        );
    }

    private static double getDistance(boolean max) {
        if (mc.player == null || mc.level == null) return 0;

        double startY = mc.player.getY();
        if (mc.player.onGround()) {
            maxStartY = startY;
            return 0;
        }

        if (startY > maxStartY) maxStartY = startY;

        Vec3 start = mc.player.position();
        if (max) start = new Vec3(start.x, maxStartY, start.z);

        BlockHitResult blockHitResult = mc.level.clip(new ClipContext(
                start,
                new Vec3(mc.player.getX(), mc.level.getMinY(), mc.player.getZ()),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY,
                mc.player
        ));

        if (blockHitResult == null) return 0;
        return blockHitResult.getLocation().distanceTo(start);
    }

    private static double getFallDamage(boolean max) {
        if (mc.player == null) return 0;

        float damageTaken = 0;
        float damage = !Modules.get().isActive(NoFall.class) && !EntityUtils.isAboveWater(mc.player) ? DamageUtils.fallDamage(mc.player) : 0;

        damageTaken = Math.max(damageTaken, damage);

        if (max) {
            maxFallDamage = Math.max(maxFallDamage, damageTaken);
            if (mc.player.onGround()) maxFallDamage = 0;
            return maxFallDamage;
        }

        return damageTaken;
    }
}
