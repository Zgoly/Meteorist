//By Zgoly
package zgoly.meteorist.modules;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalGetToBlock;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import zgoly.meteorist.Meteorist;

import java.util.Objects;

public class ItemSucker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Range.")
            .defaultValue(3.5)
            .range(1, 10)
            .sliderRange(1, 128)
            .build()
    );

    private final Setting<Boolean> boolSpeed = sgGeneral.add(new BoolSetting.Builder()
            .name("change-speed")
            .description("Change player moving speed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed:")
            .description("Speed???")
            .defaultValue(20)
            .range(1, 30)
            .sliderRange(1, 75)
            .visible(boolSpeed::get)
            .build()
    );

    boolean changeSpeed = false;
    BlockPos pos = null;

    public ItemSucker() {
        super(Meteorist.CATEGORY, "item-sucker", "Sucks up all items on the ground.");
    }

    //clear pos
    @Override
    public void onActivate() {pos = null;}
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {pos = null;}

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (changeSpeed) {
            Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
            double velX = vel.getX();
            double velZ = vel.getZ();
            ((IVec3d) event.movement).set(velX, event.movement.y, velZ);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (boolSpeed.get() && baritone.getPathingBehavior().isPathing()) changeSpeed = true;
        else if (boolSpeed.get() && !baritone.getPathingBehavior().isPathing()) changeSpeed = false;
        if (pos != null) {
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalGetToBlock(pos.add(0, -1, 0)));
            if (mc.player.getBlockPos().getX() == pos.getX() && mc.player.getBlockPos().getZ() == pos.getZ()) pos = null;
        }
        for (Entity entity : mc.world.getEntities()) {
            if (Objects.equals(entity.getType().toString(), "entity.minecraft.item") && mc.player.distanceTo(entity) <= range.get()) {
                if (pos == null) pos = mc.player.getBlockPos();
                if (mc.player == null || mc.world == null) return;
                baritone.getCustomGoalProcess().setGoalAndPath(new GoalGetToBlock(entity.getBlockPos().add(0, -1, 0)));
            }
        }
    }
    //Вижу цель — иду в атаку, хруст костей, как тако, а
}
