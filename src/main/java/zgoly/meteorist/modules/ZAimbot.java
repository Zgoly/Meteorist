package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import zgoly.meteorist.Meteorist;

import java.util.Set;

public class ZAimbot extends Module {
    private final SettingGroup sgFilter = settings.createGroup("Filter");
    private final SettingGroup sgAim = settings.createGroup("Aim");
    private final SettingGroup sgVisibility = settings.createGroup("Visibility");
    private final Setting<Set<EntityType<?>>> entities = sgFilter.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Specifies the entity types to aim at.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build()
    );
    private final Setting<Double> range = sgFilter.add(new DoubleSetting.Builder()
            .name("range")
            .description("Maximum distance to target entities.")
            .min(0)
            .defaultValue(4.5)
            .build()
    );
    private final Setting<SortPriority> priority = sgFilter.add(new EnumSetting.Builder<SortPriority>()
            .name("priority")
            .description("Sorting method to prioritize targets within range.")
            .defaultValue(SortPriority.ClosestAngle)
            .build()
    );
    private final Setting<Boolean> ignoreBabies = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-babies")
            .description("Prevents aiming at baby variants of mobs.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreNamed = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-named")
            .description("Prevents aiming at named mobs.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> ignorePassive = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-passive")
            .description("Allows aiming at passive mobs only if they target you.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreTamed = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-tamed")
            .description("Prevents aiming at tamed mobs.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> ignoreFriends = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Prevents aiming at players on your friends list.")
            .defaultValue(true)
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
    public final Setting<Double> targetMovementPrediction = sgAim.add(new DoubleSetting.Builder()
            .name("target-movement-prediction")
            .description("Amount to predict the target's movement when aiming.")
            .min(0.0F)
            .sliderMax(20.0F)
            .defaultValue(0.0F)
            .build()
    );
    private final Setting<Boolean> useFovRange = sgVisibility.add(new BoolSetting.Builder()
            .name("use-fov-range")
            .description("Restrict aiming to entities within the specified FOV.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> fovRange = sgVisibility.add(new DoubleSetting.Builder()
            .name("fov-range")
            .description("Maximum Field of View (FOV) range for targeting entities.")
            .sliderRange(0, 180)
            .defaultValue(90)
            .visible(useFovRange::get)
            .build()
    );
    private final Setting<Boolean> ignoreWalls = sgVisibility.add(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Allow aiming through walls.")
            .defaultValue(false)
            .build()
    );

    public ZAimbot() {
        super(Meteorist.CATEGORY, "z-aimbot", "Smart aimbot that takes many settings into account when targeting.");
    }

    @EventHandler
    private void renderTick(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;
        LivingEntity player = mc.player;

        // filter entities
        Entity target = TargetUtils.get(e -> !e.equals(player)
                && e.isAlive()
                && entities.get().contains(e.getType())
                && !(ignoreBabies.get() && (e instanceof AnimalEntity && (((AnimalEntity) e).isBaby())))
                && !(ignoreNamed.get() && e.hasCustomName())
                && !(ignorePassive.get() && (e instanceof PassiveEntity && ((PassiveEntity) e).isAttacking()))
                && !(ignoreTamed.get() && (e instanceof Tameable && ((Tameable) e).getOwnerUuid() != null && !((Tameable) e).getOwnerUuid().equals(player.getUuid())))
                && !(ignoreFriends.get() && (e instanceof PlayerEntity && !Friends.get().shouldAttack((PlayerEntity) e)))
                && PlayerUtils.isWithin(e, range.get())
                && (!useFovRange.get() || calculateFov(player, e) <= fovRange.get())
                && (ignoreWalls.get() || PlayerUtils.canSeeEntity(e)), priority.get()
        );

        if (target == null) return;
        aim(player, target);
    }

    private float calculateFov(LivingEntity player, Entity target) {
        Vec3d lookDirection = player.getRotationVec(1.0F);
        Vec3d targetDirection = target.getPos().subtract(player.getPos()).normalize();
        return (float) Math.toDegrees(Math.acos(lookDirection.dotProduct(targetDirection)));
    }

    private void aim(LivingEntity player, Entity target) {
        float targetYaw = (float) Rotations.getYaw(target.getPos().add(target.getVelocity().multiply(targetMovementPrediction.get())));
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
