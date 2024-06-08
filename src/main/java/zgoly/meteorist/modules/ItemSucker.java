package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.prompts.OkPrompt;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.baritone.MeteoristBaritoneUtils;

import java.util.List;
import java.util.stream.StreamSupport;

public class ItemSucker extends Module {
    private final SettingGroup sgFilter = settings.createGroup("Filter");
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> onlyPickupable = sgFilter.add(new BoolSetting.Builder()
            .name("only-pickupable")
            .description("Only pickup items that can be picked up.")
            .defaultValue(true)
            .build()
    );
    private final Setting<OperationMode> itemFilteringMode = sgFilter.add(new EnumSetting.Builder<OperationMode>()
            .name("item-filtering-mode")
            .description("Defines how items will be filtered when using the item sucker.")
            .defaultValue(OperationMode.Blacklist)
            .build()
    );
    private final Setting<List<Item>> itemWhitelist = sgFilter.add(new ItemListSetting.Builder()
            .name("item-whitelist")
            .description("Items to be exclusively collected by the item sucker.")
            .defaultValue(Items.DIAMOND)
            .visible(() -> itemFilteringMode.get() == OperationMode.Whitelist)
            .build()
    );
    private final Setting<List<Item>> itemBlacklist = sgFilter.add(new ItemListSetting.Builder()
            .name("item-blacklist")
            .description("Items which the item sucker should ignore.")
            .defaultValue(Items.POISONOUS_POTATO)
            .visible(() -> itemFilteringMode.get() == OperationMode.Blacklist)
            .build()
    );
    private final Setting<Double> suckingRange = sgFilter.add(new DoubleSetting.Builder()
            .name("sucking-range")
            .description("Range within which the Baritone can collect items.")
            .defaultValue(5)
            .min(1)
            .sliderRange(1, 25)
            .build()
    );
    private final Setting<Boolean> onlyOnGround = sgFilter.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Only collect items that are on the floor.")
            .defaultValue(true)
            .build()
    );
    private final Setting<MoveMode> moveMode = sgGeneral.add(new EnumSetting.Builder<MoveMode>()
            .name("move-mode")
            .description("Set the move mode of the item sucker.")
            .defaultValue(MoveMode.TP)
            .build()
    );
    private final Setting<Integer> itemRange = sgGeneral.add(new IntSetting.Builder()
            .name("item-range")
            .description("Range to which Baritone will go to collect items.")
            .defaultValue(0)
            .min(0)
            .visible(() -> moveMode.get() == MoveMode.Baritone)
            .build()
    );
    private final Setting<Boolean> tpToOrigin = sgGeneral.add(new BoolSetting.Builder()
            .name("tp-to-origin")
            .description("Automatically teleport player to initial position once all items have been collected.")
            .defaultValue(true)
            .visible(() -> moveMode.get() == MoveMode.TP)
            .build()
    );
    private final Setting<Boolean> returnToOrigin = sgGeneral.add(new BoolSetting.Builder()
            .name("return-to-origin")
            .description("Automatically return player to initial position once all items have been collected.")
            .defaultValue(true)
            .visible(() -> moveMode.get() == MoveMode.Baritone)
            .build()
    );

    private final Setting<Integer> returnRange = sgGeneral.add(new IntSetting.Builder()
            .name("return-range")
            .description("Range within which the Baritone will return to its initial position.")
            .defaultValue(0)
            .min(0)
            .visible(() -> moveMode.get() == MoveMode.Baritone && returnToOrigin.get())
            .build()
    );
    private final Setting<Integer> maxWaitTime = sgGeneral.add(new IntSetting.Builder()
            .name("max-wait-time")
            .description("Maximum time after teleport to wait.")
            .min(1)
            .sliderMin(1)
            .defaultValue(10)
            .visible(() -> moveMode.get() == MoveMode.TP && tpToOrigin.get())
            .build()
    );
    private final Setting<Boolean> resetTimeAfterTp = sgGeneral.add(new BoolSetting.Builder()
            .name("reset-time-after-tp")
            .description("Reset wait time after teleport.")
            .defaultValue(true)
            .visible(() -> moveMode.get() == MoveMode.TP && tpToOrigin.get())
            .build()
    );
    private final Setting<Boolean> modifySpeed = sgGeneral.add(new BoolSetting.Builder()
            .name("modify-speed")
            .description("Whether or not the speed of the player should be altered when using Baritone.")
            .defaultValue(true)
            .visible(() -> moveMode.get() == MoveMode.Baritone)
            .build()
    );
    private final Setting<Double> moveSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("move-speed")
            .description("Modifies the player's movement speed when 'Modify Speed' is enabled.")
            .defaultValue(10)
            .min(1)
            .sliderRange(1, 30)
            .visible(() -> moveMode.get() == MoveMode.Baritone && modifySpeed.get())
            .build()
    );

    int timer = 0;
    Vec3d startPos = null;
    MeteoristBaritoneUtils baritoneUtils = new MeteoristBaritoneUtils();

    public ItemSucker() {
        super(Meteorist.CATEGORY, "item-sucker", "Automatically collects items on the ground");
    }

    private boolean filter(Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            return (!onlyPickupable.get() || !itemEntity.cannotPickup())
                    && ((itemFilteringMode.get() == OperationMode.Blacklist && !itemBlacklist.get().contains((itemEntity.getStack().getItem())))
                    || (itemFilteringMode.get() == OperationMode.Whitelist && itemWhitelist.get().contains((itemEntity.getStack().getItem()))))
                    && (PlayerUtils.distanceTo(entity) <= suckingRange.get())
                    && (!onlyOnGround.get() || entity.isOnGround());
        }
        return false;
    }

    @Override
    public void onActivate() {
        timer = 0;
        startPos = null;
        baritoneUtils.cancelEverything();
    }

    @Override
    public void onDeactivate() {
        baritoneUtils.cancelEverything();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        timer = 0;
        startPos = null;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (modifySpeed.get() && baritoneUtils.isPathing()) {
            Vec3d vel = PlayerUtils.getHorizontalVelocity(moveSpeed.get());
            ((IVec3d) event.movement).set(vel.getX(), event.movement.y, vel.getZ());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (moveMode.get() == MoveMode.TP) {
            Entity target = TargetUtils.get(this::filter, SortPriority.LowestDistance);

            if (timer > 0) timer -= 1;

            if (target != null) {
                if (!tpToOrigin.get()) {
                    mc.player.setPosition(target.getX(), target.getY(), target.getZ());
                } else {
                    if (resetTimeAfterTp.get()) timer = maxWaitTime.get();
                    if (startPos == null) startPos = mc.player.getPos();
                    mc.player.setPosition(target.getX(), target.getY(), target.getZ());
                }
            }

            if (timer <= 0 && tpToOrigin.get() && startPos != null) {
                mc.player.setPosition(startPos.getX(), startPos.getY(), startPos.getZ());
                startPos = null;
                timer = maxWaitTime.get();
            }
        } else {
            if (BaritoneUtils.IS_AVAILABLE) {
                List<Entity> entities = StreamSupport.stream(mc.world.getEntities().spliterator(), false).filter(this::filter).toList();

                if (!entities.isEmpty()) {
                    baritoneUtils.setGoalNear(entities, itemRange.get());
                    if (returnToOrigin.get() && startPos == null) startPos = mc.player.getBlockPos().toCenterPos();
                } else if (returnToOrigin.get() && startPos != null) {
                    baritoneUtils.setGoalNear(BlockPos.ofFloored(startPos), returnRange.get());
                    startPos = null;
                }
            } else {
                OkPrompt.create().title("Baritone is not available").message("Looks like Baritone is not installed. Install Baritone to use this move mode.").dontShowAgainCheckboxVisible(false).show();
                moveMode.set(MoveMode.TP);
            }
        }
    }

    public enum OperationMode {
        Whitelist,
        Blacklist
    }

    public enum MoveMode {
        TP,
        Baritone
    }
}
