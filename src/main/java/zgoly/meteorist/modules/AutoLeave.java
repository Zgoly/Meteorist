package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import zgoly.meteorist.Meteorist;

import java.util.List;

public class AutoLeave extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> playerNames = sgGeneral.add(new StringListSetting.Builder()
            .name("player-names")
            .description("Player names to react on.")
            .defaultValue("Notch")
            .build()
    );
    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
            .name("message")
            .description("Message to show after logging out.")
            .defaultValue("[AutoLeave] Player joined: {player}")
            .build()
    );
    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Don't react to players added as friends.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> toggleOff = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-off")
            .description("Disables Auto Leave after usage.")
            .defaultValue(false)
            .build()
    );

    public AutoLeave() {
        super(Meteorist.CATEGORY, "auto-leave", "Automatically leaves if player with specific name joins the server.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        mc.world.getPlayers().stream()
                .filter(player -> !(ignoreFriends.get() && Friends.get().isFriend(player)))
                .filter(player -> playerNames.get().contains(player.getName().getString()))
                .findFirst()
                .ifPresent(player -> {
                    mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.of(message.get().replace("{player}", player.getName().getString()))));
                    if (toggleOff.get()) this.toggle();
                });
    }
}
