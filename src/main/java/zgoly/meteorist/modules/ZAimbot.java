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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import zgoly.meteorist.Meteorist;

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
    private final Setting<AimAxes> aimAxes = sgAim.add(new EnumSetting.Builder<AimAxes>()
            .name("aim-axes")
            .description("Which rotational axes to adjust when aiming.")
            .defaultValue(AimAxes.Both)
            .build()
    );
    private final Setting<Target> bodyTarget = sgAim.add(new EnumSetting.Builder<Target>()
            .name("aim-target")
            .description("Part of the target entity's body to aim at.")
            .defaultValue(Target.Head)
            .build()
    );
    private final Setting<Boolean> instantYaw = sgAim.add(new BoolSetting.Builder()
            .name("instant-yaw")
            .description("Aim horizontally (yaw) at the target entity instantly.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> instantPitch = sgAim.add(new BoolSetting.Builder()
            .name("instant-pitch")
            .description("Aim vertically (pitch) at the target entity instantly.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> syncYawWithCooldown = sgAim.add(new BoolSetting.Builder()
            .name("sync-yaw-with-cooldown")
            .description("Synchronize yaw aim speed with attack cooldown progress.")
            .defaultValue(false)
            .visible(() -> !instantYaw.get())
            .build()
    );
    private final Setting<Boolean> syncPitchWithCooldown = sgAim.add(new BoolSetting.Builder()
            .name("sync-pitch-with-cooldown")
            .description("Synchronize pitch aim speed with attack cooldown progress.")
            .defaultValue(false)
            .visible(() -> !instantPitch.get())
            .build()
    );
    private final Setting<Double> speedYaw = sgAim.add(new DoubleSetting.Builder()
            .name("speed-yaw")
            .description("Speed at which to adjust horizontal (yaw) aim.")
            .min(0)
            .defaultValue(1)
            .sliderRange(0.1, 10)
            .visible(() -> !instantYaw.get())
            .build()
    );
    private final Setting<Double> speedPitch = sgAim.add(new DoubleSetting.Builder()
            .name("speed-pitch")
            .description("Speed at which to adjust vertical (pitch) aim.")
            .min(0)
            .defaultValue(1)
            .sliderRange(0.1, 10)
            .visible(() -> !instantPitch.get())
            .build()
    );

    public ZAimbot() {
        super(Meteorist.CATEGORY, "z-aimbot", "Smart aimbot that takes many settings into account when targeting.");
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;

        Entity target = TargetUtils.get(this::entityCheck, priority.get());

        if (target == null) return;
        aim(mc.player, target);
    }

    private boolean entityCheck(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(mc.getCameraEntity())) return false;
        if ((entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying()) || !entity.isAlive())
            return false;

        if (!PlayerUtils.isWithin(entity, range.get())) return false;

        if (!entities.get().contains(entity.getType())) return false;
        if (ignoreNamed.get() && entity.hasCustomName()) return false;

        if (ignoreTamed.get()) {
            if (entity instanceof OwnableEntity tameable
                    && tameable.getOwner() != null
                    && tameable.getOwner().equals(mc.player)) {
                return false;
            }
        }

        if (ignorePassive.get()) {
            switch (entity) {
                case EnderMan enderman when !enderman.isCreepy() -> {
                    return false;
                }
                case ZombifiedPiglin piglin when !piglin.isAggressive() -> {
                    return false;
                }
                case Wolf wolf when !wolf.isAggressive() -> {
                    return false;
                }
                default -> {
                }
            }
        }

        if (entity instanceof Player player) {
            if (ignoreCreative.get() && player.isCreative()) return false;
            if (ignoreFriends.get() && !Friends.get().shouldAttack(player)) return false;
            if (ignoreShield.get() && player.isBlocking()) return false;
        }

        if (entity instanceof Animal animal) {
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
        float targetYaw = (float) Rotations.getYaw(target.position().add(target.getDeltaMovement().scale(targetMovementPrediction.get())));
        float targetPitch = (float) Rotations.getPitch(target, bodyTarget.get());

        float yawDifference = Mth.wrapDegrees(targetYaw - player.getYRot());
        float pitchDifference = Mth.wrapDegrees(targetPitch - player.getXRot());

        if (instantYaw.get()) {
            if (aimAxes.get() == AimAxes.Both || aimAxes.get() == AimAxes.OnlyYaw) {
                player.setYRot(targetYaw);
            }
        } else if (aimAxes.get() == AimAxes.Both || aimAxes.get() == AimAxes.OnlyYaw) {
            float cooldownProgress = syncYawWithCooldown.get() ? mc.player.getAttackStrengthScale(0) : 1;
            float yawSpeedFactor = speedYaw.get().floatValue() / 10;
            player.setYRot(player.getYRot() + yawDifference * cooldownProgress * yawSpeedFactor);
        }

        if (instantPitch.get()) {
            if (aimAxes.get() == AimAxes.Both || aimAxes.get() == AimAxes.OnlyPitch) {
                player.setXRot(targetPitch);
            }
        } else if (aimAxes.get() == AimAxes.Both || aimAxes.get() == AimAxes.OnlyPitch) {
            float cooldownProgress = syncPitchWithCooldown.get() ? mc.player.getAttackStrengthScale(0) : 1;
            float pitchSpeedFactor = speedPitch.get().floatValue() / 10;
            player.setXRot(player.getXRot() + pitchDifference * cooldownProgress * pitchSpeedFactor);
        }
    }

    public enum AimAxes {
        Both,
        OnlyYaw,
        OnlyPitch
    }
}