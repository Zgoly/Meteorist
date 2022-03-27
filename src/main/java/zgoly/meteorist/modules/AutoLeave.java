//By Zgoly
package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.LiteralText;
import zgoly.meteorist.Meteorist;

public class AutoLeave extends Module {
    public enum Mode {
        Logout,
        Command
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The mode used.")
            .defaultValue(Mode.Logout)
            .build()
    );

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
            .name("command:")
            .description("Send command in chat.")
            .defaultValue("/spawn")
            .visible(() -> mode.get() == Mode.Command)
            .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range:")
            .description("Disconnects if player in range.")
            .defaultValue(5)
            .min(1)
            .range(1, 25)
            .sliderRange(1, 25)
            .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Ignore friends.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> toggleOff = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-off")
            .description("Disables Auto Leave after usage.")
            .defaultValue(true)
            .build()
    );

    public AutoLeave() {
        super(Meteorist.CATEGORY, "auto-leave", "Automatically leaves if player in range.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity) {
                if (entity.getUuid() != mc.player.getUuid() && mc.player.distanceTo(entity) < range.get()) {
                    if (ignoreFriends.get() && Friends.get().isFriend((PlayerEntity) entity)) return;
                    if (mode.get() == Mode.Logout)
                        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("[AutoLeave] Found player in radius.")));
                    else if (mode.get() == Mode.Command && !command.get().isEmpty()) mc.player.sendChatMessage(command.get());
                    if (toggleOff.get()) this.toggle();
                }
            }
        }
    }
}
