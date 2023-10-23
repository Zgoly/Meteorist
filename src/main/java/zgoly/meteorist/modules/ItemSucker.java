package zgoly.meteorist.modules;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalGetToBlock;
import meteordevelopment.meteorclient.commands.commands.SettingCommand;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import zgoly.meteorist.Meteorist;

import java.util.List;

public class ItemSucker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public enum OperationMode {
        Whitelist,
        Blacklist
    }

    private final Setting<OperationMode> itemFilteringMode = sgGeneral.add(new EnumSetting.Builder<OperationMode>()
            .name("item-filtering-mode")
            .description("Defines how items will be filtered when using the item sucker.")
            .defaultValue(OperationMode.Blacklist)
            .build()
    );

    private final Setting<List<Item>> itemWhitelist = sgGeneral.add(new ItemListSetting.Builder()
            .name("item-whitelist")
            .description("Items to be exclusively collected by the item sucker.")
            .defaultValue(Items.DIAMOND)
            .visible(() -> itemFilteringMode.get() == OperationMode.Whitelist)
            .build()
    );

    private final Setting<List<Item>> itemBlacklist = sgGeneral.add(new ItemListSetting.Builder()
            .name("item-blacklist")
            .description("Items which the item sucker should ignore.")
            .defaultValue(Items.POISONOUS_POTATO)
            .visible(() -> itemFilteringMode.get() == OperationMode.Blacklist)
            .build()
    );

    private final Setting<Double> suckingRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("sucking-range")
            .description("Range within which the item sucker can collect items, measured in blocks.")
            .defaultValue(5)
            .min(1)
            .sliderRange(1, 25)
            .build()
    );

    private final Setting<Boolean> modifySpeed = sgGeneral.add(new BoolSetting.Builder()
            .name("modify-speed")
            .description("Whether or not the speed of the player should be altered when using the item sucker.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> movementSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("movement-speed")
            .description("Modifies the player's movement speed when 'Modify Speed' is enabled.")
            .defaultValue(20)
            .min(1)
            .sliderRange(1, 30)
            .visible(modifySpeed::get)
            .build()
    );

    private final Setting<Boolean> returnToOrigin = sgGeneral.add(new BoolSetting.Builder()
            .name("return-to-origin")
            .description("Automatically return the player to their initial position once all items have been collected.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> radiusWarning = sgGeneral.add(new BoolSetting.Builder()
            .name("radius-warning")
            .description("Receive warning when your follow radius (in BaritoneAPI's settings) is not set to 0.")
            .defaultValue(true)
            .build()
    );

    public ItemSucker() {
        super(Meteorist.CATEGORY, "item-sucker", "Automatically collects items on the ground, with various customizable behaviors.");
    }

    BlockPos startPos = null;
    IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();

    @Override
    public void onActivate() {
        if (radiusWarning.get() && BaritoneAPI.getSettings().followRadius.value != 0) {
            info(Text.empty().append(Text.literal("ItemSucker uses baritone following process, and better to set ").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)))
                    .append(Text.literal(BaritoneAPI.getSettings().followRadius.getName()).setStyle(Style.EMPTY.withColor(Formatting.AQUA)))
                    .append(Text.literal(" to ").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)))
                    .append(Text.literal("0").setStyle(Style.EMPTY.withColor(Formatting.AQUA)))
                    .append(Text.literal(".\n\n").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)))
                    .append(Text.literal("[Set " + BaritoneAPI.getSettings().followRadius.getName() + " to 0]").setStyle(Style.EMPTY.withColor(Formatting.GREEN)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Set " + BaritoneAPI.getSettings().followRadius.getName() + " to 0.")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, BaritoneAPI.getSettings().prefix.value + BaritoneAPI.getSettings().followRadius.getName() + " 0")))
                    )
                    .append(Text.literal(" | ").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)))
                    .append(Text.literal("[Suppress warning]").setStyle(Style.EMPTY.withColor(Formatting.RED)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Suppress warning.")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, new SettingCommand() + " " + name + " " + radiusWarning.name + " " + false)))
                    )
                    .append(Text.literal("."))
            );
        }

        baritone.getFollowProcess().cancel();
        baritone.getFollowProcess().follow(entity -> entity instanceof ItemEntity
                && !((ItemEntity) entity).cannotPickup()
                && ((itemFilteringMode.get() == OperationMode.Blacklist && !itemBlacklist.get().contains((((ItemEntity) entity).getStack().getItem())))
                || (itemFilteringMode.get() == OperationMode.Whitelist && itemWhitelist.get().contains((((ItemEntity) entity).getStack().getItem()))))
                && (PlayerUtils.distanceTo(entity) <= suckingRange.get())
        );
    }

    @Override
    public void onDeactivate() {
        baritone.getFollowProcess().cancel();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        startPos = null;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (modifySpeed.get() && (baritone.getPathingBehavior().isPathing())) {
            Vec3d vel = PlayerUtils.getHorizontalVelocity(movementSpeed.get());
            ((IVec3d) event.movement).set(vel.getX(), event.movement.y, vel.getZ());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (baritone.getFollowProcess().isActive() && startPos == null) {
            startPos = mc.player.getBlockPos();
        } else if (!baritone.getFollowProcess().isActive() && startPos != null) {
            if (returnToOrigin.get()) baritone.getCustomGoalProcess().setGoalAndPath(new GoalGetToBlock(startPos));
            startPos = null;
        }
    }
}
