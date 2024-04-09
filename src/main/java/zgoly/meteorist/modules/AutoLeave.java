package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import zgoly.meteorist.Meteorist;

import java.util.List;
import java.util.Set;

public class AutoLeave extends Module {
    public enum Mode {
        Logout,
        Commands
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entity types to react on.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The mode used.")
            .defaultValue(Mode.Logout)
            .build()
    );

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
            .name("message")
            .description("Message to show after logging out.")
            .defaultValue("[Auto Leave] Found entity in radius.")
            .visible(() -> mode.get() == Mode.Logout)
            .build()
    );

    private final Setting<List<String>> commands = sgGeneral.add(new StringListSetting.Builder()
            .name("commands")
            .description("Commands to send.")
            .defaultValue("/spawn")
            .visible(() -> mode.get() == Mode.Commands)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay after sending a commands in ticks (20 ticks = 1 sec).")
            .defaultValue(20)
            .min(1)
            .sliderRange(1, 40)
            .visible(() -> mode.get() == Mode.Commands)
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Range in which to react.")
            .defaultValue(5)
            .min(1)
            .sliderRange(1, 10)
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

    private int timer;
    private boolean work;

    public AutoLeave() {
        super(Meteorist.CATEGORY, "auto-leave", "Automatically leaves if entity in range.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        work = true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (Entity entity : mc.world.getEntities()) {
            if (!entities.get().contains(entity.getType())) continue;
            if (entity == mc.player || mc.player.distanceTo(entity) >= range.get()) continue;
            if (entity instanceof PlayerEntity player) {
                if (ignoreFriends.get() && Friends.get().isFriend(player)) continue;
            }
            if (work) {
                if (mode.get() == Mode.Logout) {
                    mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.of(message.get())));
                } else if (mode.get() == Mode.Commands && !commands.get().isEmpty()) {
                    for (String command : commands.get()) ChatUtils.sendPlayerMsg(command);
                }
                work = !work;
            }
            if (!work && timer >= delay.get()) {
                work = true;
                timer = 0;
            } else if (!work) timer ++;
            if (toggleOff.get()) this.toggle();
        }
    }
}
