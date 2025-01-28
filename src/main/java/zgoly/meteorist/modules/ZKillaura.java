package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.MeteoristUtils;

import java.util.Set;

public class ZKillaura extends Module {
    private final SettingGroup sgFilter = settings.createGroup("Filter");
    private final SettingGroup sgAttack = settings.createGroup("Attack");
    private final SettingGroup sgVisual = settings.createGroup("Visual");

    private final Setting<Set<EntityType<?>>> entities = sgFilter.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Specifies the entity types to target for attack.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build()
    );
    private final Setting<Double> range = sgFilter.add(new DoubleSetting.Builder()
            .name("range")
            .description("Defines the maximum range for attacking a target entity.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build()
    );
    private final Setting<Boolean> ignoreBabies = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-babies")
            .description("Prevents attacking baby variants of mobs.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreNamed = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-named")
            .description("Prevents attacking named mobs.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> ignorePassive = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-passive")
            .description("Allows attacking passive mobs only if they target you.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreTamed = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-tamed")
            .description("Prevents attacking tamed mobs.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> ignoreFriends = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Prevents attacking players on your friends list.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreWalls = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Allows attacking through walls.")
            .defaultValue(false)
            .build()
    );
    private final Setting<OnFallMode> onFallMode = sgAttack.add(new EnumSetting.Builder<OnFallMode>()
            .name("on-fall-mode")
            .description("Chooses an attack strategy when falling to maximize critical damage.")
            .defaultValue(OnFallMode.Value)
            .build()
    );
    private final Setting<Double> onFallValue = sgAttack.add(new DoubleSetting.Builder()
            .name("on-fall-value")
            .description("Defines a specific value for attacking while falling.")
            .min(0)
            .defaultValue(0.25)
            .sliderMax(1)
            .visible(() -> onFallMode.get() == OnFallMode.Value)
            .build()
    );
    private final Setting<Double> onFallMinRandomValue = sgAttack.add(new DoubleSetting.Builder()
            .name("on-fall-min-random-value")
            .description("Specifies the minimum randomized value for attacking while falling.")
            .min(0)
            .defaultValue(0.2)
            .sliderMax(1)
            .visible(() -> onFallMode.get() == OnFallMode.RandomValue)
            .build()
    );
    private final Setting<Double> onFallMaxRandomValue = sgAttack.add(new DoubleSetting.Builder()
            .name("on-fall-max-random-value")
            .description("Specifies the maximum randomized value for attacking while falling.")
            .min(0)
            .defaultValue(0.4)
            .sliderMax(1)
            .visible(() -> onFallMode.get() == OnFallMode.RandomValue)
            .build()
    );
    private final Setting<HitSpeedMode> hitSpeedMode = sgAttack.add(new EnumSetting.Builder<HitSpeedMode>()
            .name("hit-speed-mode")
            .description("Selects a hit speed mode for attacking.")
            .defaultValue(HitSpeedMode.Value)
            .build()
    );
    private final Setting<Double> hitSpeedValue = sgAttack.add(new DoubleSetting.Builder()
            .name("hit-speed-value")
            .description("Defines a specific hit speed value for attacking.")
            .defaultValue(0)
            .sliderRange(-10, 10)
            .visible(() -> hitSpeedMode.get() == HitSpeedMode.Value)
            .build()
    );
    private final Setting<Double> hitSpeedMinRandomValue = sgAttack.add(new DoubleSetting.Builder()
            .name("hit-speed-min-random-value")
            .description("Specifies the minimum randomized hit speed value.")
            .defaultValue(0)
            .sliderRange(-10, 10)
            .visible(() -> hitSpeedMode.get() == HitSpeedMode.RandomValue)
            .build()
    );
    private final Setting<Double> hitSpeedMaxRandomValue = sgAttack.add(new DoubleSetting.Builder()
            .name("hit-speed-max-random-value")
            .description("Specifies the maximum randomized hit speed value.")
            .defaultValue(0)
            .sliderRange(-10, 10)
            .visible(() -> hitSpeedMode.get() == HitSpeedMode.RandomValue)
            .build()
    );
    private final Setting<Boolean> swingHand = sgVisual.add(new BoolSetting.Builder()
            .name("swing-hand")
            .description("Makes hand swing visible client-side.")
            .defaultValue(true)
            .build()
    );
    float randomOnFallFloat = 0;
    float randomHitSpeedFloat = 0;

    public ZKillaura() {
        super(Meteorist.CATEGORY, "z-kill-aura", "Killaura which only attacks target if you aim at it.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player.isDead() || mc.world == null) return;

        OnFallMode currOnFallMode = onFallMode.get();
        if (currOnFallMode != OnFallMode.None) {
            float onFall = currOnFallMode == OnFallMode.Value ? onFallValue.get().floatValue() : randomOnFallFloat;
            if (!(mc.player.fallDistance > onFall)) return;
        }

        HitSpeedMode currHitSpeedMode = hitSpeedMode.get();
        float hitSpeed = currHitSpeedMode == HitSpeedMode.Value ? hitSpeedValue.get().floatValue() : randomHitSpeedFloat;
        if (currHitSpeedMode != HitSpeedMode.None && (mc.player.getAttackCooldownProgress(hitSpeed) * 17.0F) < 16)
            return;

        HitResult hitResult = MeteoristUtils.getCrosshairTarget(mc.player, range.get(), ignoreWalls.get(), (e -> !e.isSpectator()
                && e.canHit()
                && entities.get().contains(e.getType())
                && !(ignoreBabies.get() && (e instanceof AnimalEntity && (((AnimalEntity) e).isBaby())))
                && !(ignoreNamed.get() && e.hasCustomName())
                && !(ignorePassive.get() && ((e instanceof EndermanEntity enderman && !enderman.isAngry()) || (e instanceof ZombifiedPiglinEntity piglin && !piglin.isAttacking()) || (e instanceof WolfEntity wolf && !wolf.isAttacking())))
                && !(ignoreTamed.get() && (e instanceof Tameable tameable && tameable.getOwnerUuid() != null && tameable.getOwnerUuid().equals(mc.player.getUuid())))
                && !(ignoreFriends.get() && (e instanceof PlayerEntity player && !Friends.get().shouldAttack(player)))
        ));

        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) return;
        Entity entity = ((EntityHitResult) hitResult).getEntity();

        LivingEntity livingEntity = (LivingEntity) entity;
        if (livingEntity.getHealth() > 0) {
            mc.interactionManager.attackEntity(mc.player, livingEntity);

            if (swingHand.get()) mc.player.swingHand(Hand.MAIN_HAND);

            if (currOnFallMode == OnFallMode.RandomValue) {
                float min = Math.min(onFallMinRandomValue.get().floatValue(), onFallMaxRandomValue.get().floatValue());
                float max = Math.max(onFallMinRandomValue.get().floatValue(), onFallMaxRandomValue.get().floatValue());
                randomOnFallFloat = min + mc.world.random.nextFloat() * (max - min);
            }

            if (currHitSpeedMode == HitSpeedMode.RandomValue) {
                float min = Math.min(hitSpeedMinRandomValue.get().floatValue(), hitSpeedMaxRandomValue.get().floatValue());
                float max = Math.max(hitSpeedMinRandomValue.get().floatValue(), hitSpeedMaxRandomValue.get().floatValue());
                randomHitSpeedFloat = min + mc.world.random.nextFloat() * (max - min);
            }
        }
    }

    public enum OnFallMode {
        None,
        Value,
        RandomValue
    }

    public enum HitSpeedMode {
        None,
        Value,
        RandomValue
    }
}