package zgoly.meteorist.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.meteordev.starscript.value.Value;
import org.meteordev.starscript.value.ValueMap;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteoristStarscript {
    private static final PlayerFallState FALL_STATE = new PlayerFallState();

    public static void init() {
        MeteorStarscript.ss.set("meteorist", new ValueMap()
                .set("fall_distance", () -> Value.number(FALL_STATE.getDistance(false)))
                .set("max_fall_distance", () -> Value.number(FALL_STATE.getDistance(true)))
                .set("fall_damage", () -> Value.number(FALL_STATE.getFallDamage(false)))
                .set("max_fall_damage", () -> Value.number(FALL_STATE.getFallDamage(true)))
                .set("is_falling", () -> Value.bool(FALL_STATE.isFalling()))
                .set("would_take_damage", () -> Value.bool(FALL_STATE.wouldTakeDamage()))
        );

        MeteorClient.EVENT_BUS.subscribe(FALL_STATE);
    }

    private static class PlayerFallState {
        private double maxY = 0;
        private boolean wasOnGround = true;
        private double cachedMaxDamage = 0;

        @EventHandler
        private void onGameJoined(GameJoinedEvent event) {
            reset();
        }

        public void reset() {
            maxY = 0;
            wasOnGround = true;
            cachedMaxDamage = 0;
        }

        public double getDistance(boolean max) {
            Player player = mc.player;
            if (player == null || mc.level == null) return 0;

            double currentY = player.getY();
            boolean onGround = player.onGround();

            if (!onGround) maxY = wasOnGround ? currentY : Math.max(maxY, currentY);

            wasOnGround = onGround;
            if (onGround) return 0;

            Vec3 start = max ? new Vec3(player.getX(), maxY, player.getZ()) : player.position();
            Vec3 end = new Vec3(player.getX(), mc.level.getMinY(), player.getZ());

            BlockHitResult result = mc.level.clip(new ClipContext(
                    start, end,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.ANY,
                    player
            ));

            return result.getLocation().distanceTo(start);
        }

        public double getFallDamage(boolean max) {
            Player player = mc.player;
            if (player == null) return 0;

            if (player.getAbilities().flying ||
                    player.hasEffect(MobEffects.SLOW_FALLING) ||
                    player.hasEffect(MobEffects.LEVITATION) ||
                    Modules.get().isActive(NoFall.class) ||
                    EntityUtils.isAboveWater(player)) {
                if (player.onGround()) cachedMaxDamage = 0;
                return max ? cachedMaxDamage : 0;
            }

            float damage = DamageUtils.fallDamage(player);

            if (max) {
                cachedMaxDamage = Math.max(cachedMaxDamage, damage);
                if (player.onGround()) {
                    double result = cachedMaxDamage;
                    cachedMaxDamage = 0;
                    return result;
                }
                return cachedMaxDamage;
            }
            return damage;
        }

        public boolean isFalling() {
            Player player = mc.player;
            return player != null && player.fallDistance > 0;
        }

        public boolean wouldTakeDamage() {
            Player player = mc.player;
            if (player == null || player.onGround()) return false;
            return getFallDamage(false) > 0;
        }
    }
}