package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityAgeTest;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.MeteoristUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ZKillaura extends Module {
    private final SettingGroup sgFilter = settings.createGroup("Filter");
    private final SettingGroup sgAttack = settings.createGroup("Attack");
    private final SettingGroup sgShield = settings.createGroup("Shield");
    private final SettingGroup sgVisual = settings.createGroup("Visual");

    private final Setting<Set<EntityType<?>>> entities = sgFilter.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to attack.")
            .onlyAttackable()
            .defaultValue(EntityTypes.PLAYER)
            .build()
    );
    private final Setting<Double> range = sgFilter.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range the entity can be to attack it.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(6)
            .build()
    );
    private final Setting<EntityAgeTest> passiveMobAgeFilter = sgFilter.add(new EnumSetting.Builder<EntityAgeTest>()
            .name("passive-mob-age-filter")
            .description("Determines the age of passive mobs to target (animals, villagers).")
            .defaultValue(EntityAgeTest.Adult)
            .build()
    );
    private final Setting<EntityAgeTest> hostileMobAgeFilter = sgFilter.add(new EnumSetting.Builder<EntityAgeTest>()
            .name("hostile-mob-age-filter")
            .description("Determines the age of hostile mobs to target (zombies, piglins, hoglins, zoglins).")
            .defaultValue(EntityAgeTest.Both)
            .build()
    );
    private final Setting<Boolean> ignoreNamed = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-named")
            .description("Whether or not to attack mobs with a name.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> ignorePassive = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-passive")
            .description("Will only attack sometimes passive mobs if they are targeting you.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreTamed = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-tamed")
            .description("Will avoid attacking mobs you tamed.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> ignoreCreative = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-creative")
            .description("Will avoid attacking players in creative mode.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreFriends = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Will avoid attacking players on your friends list.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreWalls = sgFilter.add(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Whether or not to attack mobs through walls.")
            .defaultValue(false)
            .build()
    );
    private final Setting<OperationMode> itemFilteringMode = sgFilter.add(new EnumSetting.Builder<OperationMode>()
            .name("item-filtering-mode")
            .description("Defines how held items control Z Killaura activation.")
            .defaultValue(OperationMode.Blacklist)
            .build()
    );
    private final Setting<List<Item>> itemBlacklist = sgFilter.add(new ItemListSetting.Builder()
            .name("item-blacklist")
            .description("Z Killaura will NOT activate when holding these items.")
            .visible(() -> itemFilteringMode.get() == OperationMode.Blacklist)
            .build()
    );
    private final Setting<List<Item>> itemWhitelist = sgFilter.add(new ItemListSetting.Builder()
            .name("item-whitelist")
            .description("Z Killaura only activates when holding one of these items.")
            .defaultValue(Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.NETHERITE_SPEAR)
            .visible(() -> itemFilteringMode.get() == OperationMode.Whitelist)
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

    private final Setting<ShieldMode> shieldMode = sgShield.add(new EnumSetting.Builder<ShieldMode>()
            .name("shield-mode")
            .description("Will try and use an axe to break target shields.")
            .defaultValue(ShieldMode.Break)
            .build()
    );
    private final Setting<DelayMode> swapDelayMode = sgShield.add(new EnumSetting.Builder<DelayMode>()
            .name("swap-delay-mode")
            .description("Selects a delay before swapping.")
            .defaultValue(DelayMode.Value)
            .visible(() -> shieldMode.get() == ShieldMode.Break)
            .build()
    );
    private final Setting<Integer> swapDelayValue = sgShield.add(new IntSetting.Builder()
            .name("swap-delay-value")
            .description("Defines a specific delay (in ticks) before swapping.")
            .defaultValue(2)
            .sliderRange(0, 20)
            .visible(() -> shieldMode.get() == ShieldMode.Break && swapDelayMode.get() == DelayMode.Value)
            .build()
    );
    private final Setting<Integer> swapDelayMinRandomValue = sgShield.add(new IntSetting.Builder()
            .name("swap-delay-min-random-value")
            .description("Specifies the minimum randomized delay (in ticks) before swapping.")
            .defaultValue(1)
            .sliderRange(0, 20)
            .visible(() -> shieldMode.get() == ShieldMode.Break && swapDelayMode.get() == DelayMode.RandomValue)
            .build()
    );
    private final Setting<Integer> swapDelayMaxRandomValue = sgShield.add(new IntSetting.Builder()
            .name("swap-delay-max-random-value")
            .description("Specifies the maximum randomized delay (in ticks) before swapping.")
            .defaultValue(5)
            .sliderRange(0, 20)
            .visible(() -> shieldMode.get() == ShieldMode.Break && swapDelayMode.get() == DelayMode.RandomValue)
            .build()
    );
    private final Setting<DelayMode> hitDelayMode = sgShield.add(new EnumSetting.Builder<DelayMode>()
            .name("hit-delay-mode")
            .description("Selects a delay before hitting.")
            .defaultValue(DelayMode.Value)
            .visible(() -> shieldMode.get() == ShieldMode.Break)
            .build()
    );
    private final Setting<Integer> hitDelayValue = sgShield.add(new IntSetting.Builder()
            .name("hit-delay-value")
            .description("Defines a specific delay (in ticks) before hitting.")
            .defaultValue(2)
            .sliderRange(0, 20)
            .visible(() -> shieldMode.get() == ShieldMode.Break && hitDelayMode.get() == DelayMode.Value)
            .build()
    );
    private final Setting<Integer> hitDelayMinRandomValue = sgShield.add(new IntSetting.Builder()
            .name("hit-delay-min-random-value")
            .description("Specifies the minimum randomized delay (in ticks) before hitting.")
            .defaultValue(1)
            .sliderRange(0, 20)
            .visible(() -> shieldMode.get() == ShieldMode.Break && hitDelayMode.get() == DelayMode.RandomValue)
            .build()
    );
    private final Setting<Integer> hitDelayMaxRandomValue = sgShield.add(new IntSetting.Builder()
            .name("hit-delay-max-random-value")
            .description("Specifies the maximum randomized delay (in ticks) before hitting.")
            .defaultValue(5)
            .sliderRange(0, 20)
            .visible(() -> shieldMode.get() == ShieldMode.Break && hitDelayMode.get() == DelayMode.RandomValue)
            .build()
    );
    private final Setting<DelayMode> swapBackDelayMode = sgShield.add(new EnumSetting.Builder<DelayMode>()
            .name("swap-back-delay-mode")
            .description("Selects a delay before swapping back.")
            .defaultValue(DelayMode.Value)
            .visible(() -> shieldMode.get() == ShieldMode.Break)
            .build()
    );
    private final Setting<Integer> swapBackDelayValue = sgShield.add(new IntSetting.Builder()
            .name("swap-back-delay-value")
            .description("Defines a specific delay (in ticks) before swapping back.")
            .defaultValue(2)
            .sliderRange(0, 20)
            .visible(() -> shieldMode.get() == ShieldMode.Break && swapBackDelayMode.get() == DelayMode.Value)
            .build()
    );
    private final Setting<Integer> swapBackDelayMinRandomValue = sgShield.add(new IntSetting.Builder()
            .name("swap-back-delay-min-random-value")
            .description("Specifies the minimum randomized delay (in ticks) before swapping back.")
            .defaultValue(1)
            .sliderRange(0, 20)
            .visible(() -> shieldMode.get() == ShieldMode.Break && swapBackDelayMode.get() == DelayMode.RandomValue)
            .build()
    );
    private final Setting<Integer> swapBackDelayMaxRandomValue = sgShield.add(new IntSetting.Builder()
            .name("swap-back-delay-max-random-value")
            .description("Specifies the maximum randomized delay (in ticks) before swapping back.")
            .defaultValue(5)
            .sliderRange(0, 20)
            .visible(() -> shieldMode.get() == ShieldMode.Break && swapBackDelayMode.get() == DelayMode.RandomValue)
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

    private ShieldState shieldState = ShieldState.Idle;
    private int swapTimer = 0, hitTimer = 0, swapBackTimer = 0;
    private int originalSlot = -1;

    @Override
    public void onActivate() {
        randomOnFallFloat = 0;
        randomHitSpeedFloat = 0;

        shieldState = ShieldState.Idle;
        swapTimer = hitTimer = swapBackTimer = 0;
        originalSlot = -1;
    }

    public ZKillaura() {
        super(Meteorist.CATEGORY, "z-kill-aura", "Killaura which only attacks target if you aim at it.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player.isDeadOrDying() || mc.level == null) return;

        Item held = mc.player.getMainHandItem().getItem();
        if (!(itemFilteringMode.get() == OperationMode.Whitelist
                ? itemWhitelist.get().contains(held)
                : !itemBlacklist.get().contains(held))) return;

        HitResult hitResult = MeteoristUtils.getCrosshairTarget(mc.player, range.get(), ignoreWalls.get(), this::entityCheck);
        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) return;

        Entity entity = ((EntityHitResult) hitResult).getEntity();
        LivingEntity livingEntity = entity instanceof LivingEntity ? (LivingEntity) entity : null;

        // Using labeled breaks instead of returns so the code can run in one tick, but I think there should be a simpler way to do this
        if (shieldMode.get() == ShieldMode.Break && livingEntity != null) {
            if (shieldState == ShieldState.Idle && livingEntity.isBlocking()) {
                FindItemResult axe = InvUtils.findInHotbar(stack -> stack.is(ItemTags.AXES));
                if (axe.found()) {
                    shieldState = ShieldState.SwapAxe;
                    swapTimer = calculateDelay(swapDelayMode.get(), swapDelayValue.get(),
                            swapDelayMinRandomValue.get(), swapDelayMaxRandomValue.get());
                }
            }

            swapAxeState:
            if (shieldState == ShieldState.SwapAxe) {
                if (swapTimer-- > 0) break swapAxeState;

                FindItemResult axe = InvUtils.findInHotbar(stack -> stack.is(ItemTags.AXES));
                if (axe.found() && livingEntity.isBlocking()) {
                    originalSlot = mc.player.getInventory().getSelectedSlot();
                    InvUtils.swap(axe.slot(), false);

                    shieldState = ShieldState.Attack;
                    hitTimer = calculateDelay(hitDelayMode.get(), hitDelayValue.get(),
                            hitDelayMinRandomValue.get(), hitDelayMaxRandomValue.get());
                } else {
                    resetShield();
                }
            }

            attackState:
            if (shieldState == ShieldState.Attack) {
                if (hitTimer-- > 0) break attackState;

                if (livingEntity.isBlocking()) {
                    mc.gameMode.attack(mc.player, livingEntity);
                    if (swingHand.get()) mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);

                    shieldState = ShieldState.SwapBack;
                    swapBackTimer = calculateDelay(swapBackDelayMode.get(), swapBackDelayValue.get(),
                            swapBackDelayMinRandomValue.get(), swapBackDelayMaxRandomValue.get());
                } else {
                    InvUtils.swap(originalSlot, false);
                    resetShield();
                }
            }

            swapBackState:
            if (shieldState == ShieldState.SwapBack) {
                if (swapBackTimer-- > 0) break swapBackState;

                if (originalSlot != mc.player.getInventory().getSelectedSlot()) {
                    InvUtils.swap(originalSlot, false);
                }

                resetShield();
            }

            if (shieldState == ShieldState.Attack || shieldState == ShieldState.SwapBack) return;
        }

        OnFallMode currOnFallMode = onFallMode.get();
        if (currOnFallMode != OnFallMode.None) {
            float onFall = currOnFallMode == OnFallMode.Value ? onFallValue.get().floatValue() : randomOnFallFloat;
            if (!(mc.player.fallDistance > onFall)) return;
        }

        HitSpeedMode currHitSpeedMode = hitSpeedMode.get();
        float hitSpeed = currHitSpeedMode == HitSpeedMode.Value ? hitSpeedValue.get().floatValue() : randomHitSpeedFloat;
        if (currHitSpeedMode != HitSpeedMode.None && (mc.player.getAttackStrengthScale(hitSpeed) * 17.0F) < 16)
            return;

        mc.gameMode.attack(mc.player, entity);
        if (swingHand.get()) mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);

        if (currOnFallMode == OnFallMode.RandomValue) {
            float min = Math.min(onFallMinRandomValue.get().floatValue(), onFallMaxRandomValue.get().floatValue());
            float max = Math.max(onFallMinRandomValue.get().floatValue(), onFallMaxRandomValue.get().floatValue());
            randomOnFallFloat = min + mc.level.getRandom().nextFloat() * (max - min);
        }

        if (currHitSpeedMode == HitSpeedMode.RandomValue) {
            float min = Math.min(hitSpeedMinRandomValue.get().floatValue(), hitSpeedMaxRandomValue.get().floatValue());
            float max = Math.max(hitSpeedMinRandomValue.get().floatValue(), hitSpeedMaxRandomValue.get().floatValue());
            randomHitSpeedFloat = min + mc.level.getRandom().nextFloat() * (max - min);
        }
    }

    private void resetShield() {
        shieldState = ShieldState.Idle;
        swapTimer = hitTimer = swapBackTimer = 0;
        originalSlot = -1;
    }

    private int calculateDelay(DelayMode mode, int value, int minRandom, int maxRandom) {
        return switch (mode) {
            case Value -> value;
            case RandomValue -> minRandom + mc.level.getRandom().nextInt(maxRandom - minRandom + 1);
        };
    }

    private boolean entityCheck(Entity entity) {
        if (entity == mc.player || entity == mc.getCameraEntity()) return false;
        if (!entity.isAlive()) return false;
        if (entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying()) return false;
        if (!entities.get().contains(entity.getType())) return false;
        if (ignoreNamed.get() && entity.hasCustomName()) return false;

        if (ignoreTamed.get()
                && entity instanceof OwnableEntity ownable
                && Objects.equals(ownable.getOwner(), mc.player)) {
            return false;
        }

        if (ignorePassive.get() && isPassive(entity)) return false;

        if (entity instanceof Player player) {
            if (ignoreCreative.get() && player.isCreative()) return false;
            if (ignoreFriends.get() && !Friends.get().shouldAttack(player)) return false;
            if (shieldMode.get() == ShieldMode.Ignore && player.isBlocking()) return false;
        }

        if (entity instanceof LivingEntity livingEntity) {
            // Hostile mobs with baby variants (zombies, piglins, hoglins, zoglins)
            if (entity instanceof Zombie || entity instanceof Piglin
                    || entity instanceof Hoglin || entity instanceof Zoglin) {
                return hostileMobAgeFilter.get().test(livingEntity);
            }
            // Passive mobs with baby variants (animals, villagers)
            if (entity instanceof AgeableMob && (!(entity instanceof Frog || entity instanceof Parrot))) {
                return passiveMobAgeFilter.get().test(livingEntity);
            }
        }

        return true;
    }

    private boolean isPassive(Entity entity) {
        return switch (entity) {
            case Enderman enderman -> !enderman.isCreepy();
            case ZombifiedPiglin piglin -> !piglin.isAggressive();
            case Wolf wolf -> !wolf.isAggressive();
            default -> false;
        };
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

    public enum ShieldMode {
        Ignore,
        Break,
        None
    }

    public enum DelayMode {
        Value,
        RandomValue
    }

    private enum ShieldState {
        Idle,
        SwapAxe,
        Attack,
        SwapBack
    }

    public enum OperationMode {
        Whitelist,
        Blacklist
    }
}
