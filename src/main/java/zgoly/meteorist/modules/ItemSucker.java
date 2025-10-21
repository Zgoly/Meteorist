package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.prompts.OkPrompt;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.baritone.MeteoristBaritoneUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public class ItemSucker extends Module {
    private final SettingGroup sgFilter = settings.createGroup("Filter");
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

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
            .description("Items to be exclusively picked up by the item sucker.")
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
            .description("Range within which the Baritone can pick up items.")
            .defaultValue(5)
            .min(1)
            .sliderRange(1, 25)
            .build()
    );
    private final Setting<Boolean> onlyOnGround = sgFilter.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Only pick up items that are on the floor.")
            .defaultValue(true)
            .build()
    );

    private final Setting<MoveMode> moveMode = sgGeneral.add(new EnumSetting.Builder<MoveMode>()
            .name("move-mode")
            .description("Set the move mode of the item sucker.")
            .defaultValue(MoveMode.Teleport)
            .build()
    );
    private final Setting<Boolean> checkCollisions = sgGeneral.add(new BoolSetting.Builder()
            .name("check-collisions")
            .description("Check if player can teleport to an item and not collide with blocks.")
            .defaultValue(true)
            .visible(() -> moveMode.get() == MoveMode.Teleport)
            .build()
    );
    private final Setting<Integer> itemRange = sgGeneral.add(new IntSetting.Builder()
            .name("item-range")
            .description("The radius within which Baritone will attempt to pick up items (relative to the item's position).")
            .defaultValue(0)
            .min(0)
            .visible(() -> moveMode.get() == MoveMode.Baritone)
            .build()
    );
    private final Setting<Boolean> tpToOrigin = sgGeneral.add(new BoolSetting.Builder()
            .name("tp-to-origin")
            .description("Automatically teleport player to initial position once all items have been picked up.")
            .defaultValue(true)
            .visible(() -> moveMode.get() == MoveMode.Teleport)
            .build()
    );
    private final Setting<Integer> waitTime = sgGeneral.add(new IntSetting.Builder()
            .name("wait-time")
            .description("Time to wait after teleport (in ticks).")
            .min(1)
            .defaultValue(10)
            .visible(() -> moveMode.get() == MoveMode.Teleport && tpToOrigin.get())
            .build()
    );
    private final Setting<Boolean> resetTimeAfterTp = sgGeneral.add(new BoolSetting.Builder()
            .name("reset-time-after-tp")
            .description("Resets wait time after teleport.")
            .defaultValue(true)
            .visible(() -> moveMode.get() == MoveMode.Teleport && tpToOrigin.get())
            .build()
    );
    private final Setting<Boolean> returnToOrigin = sgGeneral.add(new BoolSetting.Builder()
            .name("return-to-origin")
            .description("Automatically return player to initial position once all items have been picked up.")
            .defaultValue(true)
            .visible(() -> moveMode.get() == MoveMode.Baritone)
            .build()
    );
    private final Setting<Integer> returnRange = sgGeneral.add(new IntSetting.Builder()
            .name("return-range")
            .description("The radius within which Baritone will return to its initial position (relative to the initial position).")
            .defaultValue(0)
            .min(0)
            .visible(() -> moveMode.get() == MoveMode.Baritone && returnToOrigin.get())
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
    private final Setting<Boolean> disableOnItemCount = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-item-count")
            .description("Disables the module when a certain number of items are picked up.")
            .defaultValue(false)
            .build()
    );
    private final Setting<ItemCountMode> itemCountMode = sgGeneral.add(new EnumSetting.Builder<ItemCountMode>()
            .name("item-count-mode")
            .description("Defines how the maximum number of items to pick up is calculated.")
            .defaultValue(ItemCountMode.Stacks)
            .visible(disableOnItemCount::get)
            .build()
    );
    private final Setting<Integer> maxItemCount = sgGeneral.add(new IntSetting.Builder()
            .name("max-item-count")
            .description("Maximum number of items to pick up.")
            .defaultValue(10)
            .min(1)
            .sliderRange(1, 10)
            .visible(disableOnItemCount::get)
            .build()
    );

    private final Setting<Integer> maxItemsAtOnce = sgRender.add(new IntSetting.Builder()
            .name("max-item-at-once")
            .description("Maximum number of hitboxes to render at once.")
            .defaultValue(10)
            .min(1)
            .sliderRange(1, 10)
            .build()
    );
    private final Setting<Boolean> showTeleportBox = sgRender.add(new BoolSetting.Builder()
            .name("show-teleport-box")
            .description("Displays player hitbox at items position when using \"Teleport\" move mode.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of box.")
            .defaultValue(new SettingColor(0, 0, 255, 40))
            .visible(showTeleportBox::get)
            .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of box.")
            .defaultValue(new SettingColor(0, 0, 255, 100))
            .visible(showTeleportBox::get)
            .build()
    );

    int timer = 0;
    Vec3d startPos = null;

    int pickedUpStacksCount = 0;
    int pickedUpItemsCount = 0;

    MeteoristBaritoneUtils baritoneUtils = new MeteoristBaritoneUtils();

    public ItemSucker() {
        super(Meteorist.CATEGORY, "item-sucker", "Automatically picks up dropped items.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        startPos = null;

        pickedUpStacksCount = 0;
        pickedUpItemsCount = 0;

        baritoneUtils.cancelEverything();
    }

    @Override
    public void onDeactivate() {
        baritoneUtils.cancelEverything();
    }

    public Box getBoundingBoxAtPosition(Vec3d pos) {
        Vec3d offset = pos.subtract(mc.player.getBoundingBox().getHorizontalCenter());
        return mc.player.getBoundingBox().offset(offset.getX(), offset.getY(), offset.getZ());
    }

    public boolean canTeleportToItem(Vec3d pos) {
        Box box = getBoundingBoxAtPosition(pos);

        Iterable<VoxelShape> collisions = mc.world.getBlockCollisions(mc.player, box);
        List<VoxelShape> collisionsList = StreamSupport.stream(collisions.spliterator(), false)
                .filter(voxelShape -> !voxelShape.isEmpty()).toList();

        return collisionsList.isEmpty();
    }

    private boolean filter(Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            boolean isPickupable = true;
            if (onlyPickupable.get()) {
                isPickupable = !itemEntity.cannotPickup();
            }
            boolean isWithinRange = PlayerUtils.isWithin(entity, suckingRange.get());
            boolean isOnGround = true;
            if (onlyOnGround.get()) {
                isOnGround = entity.isOnGround();
            }
            boolean isItemAllowed = (itemFilteringMode.get() == OperationMode.Blacklist && !itemBlacklist.get().contains(itemEntity.getStack().getItem()))
                    || (itemFilteringMode.get() == OperationMode.Whitelist && itemWhitelist.get().contains(itemEntity.getStack().getItem()));
            boolean canTeleport = true;
            if (moveMode.get() == MoveMode.Teleport) {
                canTeleport = checkCollisions.get() && canTeleportToItem(itemEntity.getEntityPos());
            }

            return isPickupable && isWithinRange && isOnGround && isItemAllowed && canTeleport;
        }
        return false;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (modifySpeed.get() && baritoneUtils.isPathing()) {
            Vec3d vel = PlayerUtils.getHorizontalVelocity(moveSpeed.get());
            ((IVec3d) event.movement).meteor$set(vel.getX(), event.movement.y, vel.getZ());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (disableOnItemCount.get()) {
            int currentItemCount = (itemCountMode.get() == ItemCountMode.Stacks) ? pickedUpStacksCount : pickedUpItemsCount;
            if (currentItemCount > maxItemCount.get()) toggle();
        }

        if (moveMode.get() == MoveMode.Teleport) {
            Entity target = TargetUtils.get(this::filter, SortPriority.LowestDistance);

            if (timer > 0) timer -= 1;

            if (target != null) {
                if (tpToOrigin.get()) {
                    if (resetTimeAfterTp.get()) timer = waitTime.get();
                    if (startPos == null) startPos = mc.player.getEntityPos();
                }
                mc.player.setPosition(target.getX(), target.getY(), target.getZ());
            }

            if (timer <= 0 && tpToOrigin.get() && startPos != null) {
                mc.player.setPosition(startPos.getX(), startPos.getY(), startPos.getZ());
                startPos = null;
                timer = waitTime.get();
            }
        } else {
            if (BaritoneUtils.IS_AVAILABLE) {
                List<Entity> targets = new ArrayList<>();
                TargetUtils.getList(targets, this::filter, SortPriority.LowestDistance, maxItemsAtOnce.get());

                if (!targets.isEmpty()) {
                    baritoneUtils.setGoalNear(targets, itemRange.get());
                    if (returnToOrigin.get() && startPos == null) startPos = mc.player.getBlockPos().toCenterPos();
                } else if (returnToOrigin.get() && startPos != null) {
                    baritoneUtils.setGoalNear(BlockPos.ofFloored(startPos), returnRange.get());
                    startPos = null;
                }
            } else {
                OkPrompt.create().title("Baritone is not available")
                        .message("Looks like Baritone is not installed. Install Baritone to use this move mode.")
                        .dontShowAgainCheckboxVisible(false)
                        .show();
                moveMode.set(MoveMode.Teleport);
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (moveMode.get() == MoveMode.Teleport) {
            List<Entity> entities = new ArrayList<>();
            TargetUtils.getList(entities, this::filter, SortPriority.LowestDistance, maxItemsAtOnce.get());
            entities.forEach(entity -> event.renderer.box(getBoundingBoxAtPosition(entity.getEntityPos()), sideColor.get(), lineColor.get(), ShapeMode.Both, 0));
        }
    }

    @EventHandler
    public void onItemPickup(PacketEvent.Receive event) {
        if (event.packet instanceof ItemPickupAnimationS2CPacket packet) {
            pickedUpStacksCount += 1;
            pickedUpItemsCount += packet.getStackAmount();
        }
    }

    public enum ItemCountMode {
        Stacks,
        Items
    }

    public enum OperationMode {
        Whitelist,
        Blacklist
    }

    public enum MoveMode {
        Teleport,
        Baritone
    }
}
