package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.mixin.MinecraftClientAccessor;

import java.util.Set;

import static zgoly.meteorist.utils.MeteoristUtils.calculateFov;

public class ZAimbot extends Module {
    private final SettingGroup sgFilter = settings.createGroup("Filter");
    private final SettingGroup sgAim = settings.createGroup("Aim");

    private final Setting<Set<EntityType<?>>> entities = sgFilter.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to aim at.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build()
    );
    private final Setting<Double> range = sgFilter.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range the entity can be to aim at it.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build()
    );
    private final Setting<KillAura.EntityAge> mobAgeFilter = sgFilter.add(new EnumSetting.Builder<KillAura.EntityAge>()
            .name("mob-age-filter")
            .description("Determines the age of the mobs to target (baby, adult, or both).")
            .defaultValue(KillAura.EntityAge.Adult)
            .build()
    );
    private final Setting<Boolean> ignoreNamed = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-named")
            .description("Whether or not to aim at mobs with a name.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> ignorePassive = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-passive")
            .description("Will only aim at sometimes passive mobs if they are targeting you.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreTamed = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-tamed")
            .description("Will avoid aiming at mobs you tamed.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> ignoreCreative = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-creative")
            .description("Will avoid aiming at players in creative mode.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreFriends = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Will avoid aiming at players on your friends list.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreShield = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-shield")
            .description("Will avoid aiming at players who are using a shield.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> useFovRange = sgFilter.add(new BoolSetting.Builder()
            .name("use-fov-range")
            .description("Restrict aiming to entities within the specified FOV.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> fovRange = sgFilter.add(new DoubleSetting.Builder()
            .name("fov-range")
            .description("Maximum Field of View (FOV) range for targeting entities.")
            .sliderRange(0, 180)
            .defaultValue(90)
            .visible(useFovRange::get)
            .build()
    );
    private final Setting<Boolean> ignoreWalls = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Allow aiming through walls.")
            .defaultValue(false)
            .build()
    );
    private final Setting<SortPriority> priority = sgFilter.add(new EnumSetting.Builder<SortPriority>()
            .name("priority")
            .description("Sorting method to prioritize targets within range.")
            .defaultValue(SortPriority.ClosestAngle)
            .build()
    );

    public final Setting<Double> targetMovementPrediction = sgAim.add(new DoubleSetting.Builder()
            .name("target-movement-prediction")
            .description("Amount to predict the target's movement when aiming.")
            .min(0)
            .sliderMax(20)
            .defaultValue(0)
            .build()
    );
    private final Setting<Target> bodyTarget = sgAim.add(new EnumSetting.Builder<Target>()
            .name("aim-target")
            .description("Part of the target entity's body to aim at.")
            .defaultValue(Target.Head)
            .build()
    );
    private final Setting<Boolean> instantAim = sgAim.add(new BoolSetting.Builder()
            .name("instant-aim")
            .description("Aim at the target entity instantly.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> syncSpeedWithCooldown = sgAim.add(new BoolSetting.Builder()
            .name("sync-speed-with-cooldown")
            .description("Synchronize aim speed with attack cooldown progress.")
            .defaultValue(false)
            .visible(() -> !instantAim.get())
            .build()
    );
    private final Setting<Double> speed = sgAim.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Speed at which to adjust aim.")
            .min(0)
            .defaultValue(1)
            .sliderRange(0.1, 10)
            .visible(() -> !instantAim.get())
            .build()
    );

    public ZAimbot() {
        super(Meteorist.CATEGORY, "z-aimbot", "Smart aimbot that takes many settings into account when targeting.");
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        Entity target = TargetUtils.get(this::entityCheck, priority.get());

        if (target == null) return;
        aim(mc.player, target);
    }

    private boolean entityCheck(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(((MinecraftClientAccessor) MinecraftClient.getInstance()).getCameraEntity())) return false;
        if ((entity instanceof LivingEntity livingEntity && livingEntity.isDead()) || !entity.isAlive()) return false;

        if (!PlayerUtils.isWithin(entity, range.get())) return false;

        if (!entities.get().contains(entity.getType())) return false;
        if (ignoreNamed.get() && entity.hasCustomName()) return false;

        if (ignoreTamed.get()) {
            if (entity instanceof Tameable tameable
                    && tameable.getOwner() != null
                    && tameable.getOwner().equals(mc.player)) {
                return false;
            }
        }

        if (ignorePassive.get()) {
            switch (entity) {
                case EndermanEntity enderman when !enderman.isAngry() -> {
                    return false;
                }
                case ZombifiedPiglinEntity piglin when !piglin.isAttacking() -> {
                    return false;
                }
                case WolfEntity wolf when !wolf.isAttacking() -> {
                    return false;
                }
                default -> {
                }
            }
        }

        if (entity instanceof PlayerEntity player) {
            if (ignoreCreative.get() && player.isCreative()) return false;
            if (ignoreFriends.get() && !Friends.get().shouldAttack(player)) return false;
            if (ignoreShield.get() && player.isBlocking()) return false;
        }

        if (entity instanceof AnimalEntity animal) {
            return switch (mobAgeFilter.get()) {
                case Baby -> animal.isBaby();
                case Adult -> !animal.isBaby();
                case Both -> true;
            };
        }

        if (useFovRange.get() && calculateFov(mc.player, entity) > fovRange.get()) return false;
        return ignoreWalls.get() || PlayerUtils.canSeeEntity(entity);
    }

    private void aim(LivingEntity player, Entity target) {
        float targetYaw = (float) Rotations.getYaw(target.getEntityPos().add(target.getVelocity().multiply(targetMovementPrediction.get())));
        float targetPitch = (float) Rotations.getPitch(target, bodyTarget.get());

        float yawDifference = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float pitchDifference = MathHelper.wrapDegrees(targetPitch - player.getPitch());

        if (instantAim.get()) {
            player.setYaw(targetYaw);
            player.setPitch(targetPitch);
        } else {
            float cooldownProgress = syncSpeedWithCooldown.get() ? mc.player.getAttackCooldownProgress(0) : 1;
            player.setYaw(player.getYaw() + yawDifference * cooldownProgress * speed.get().floatValue() / 10);
            player.setPitch(player.getPitch() + pitchDifference * cooldownProgress * speed.get().floatValue() / 10);
        }
    }
}
