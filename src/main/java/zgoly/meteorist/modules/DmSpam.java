package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import org.apache.commons.lang3.RandomStringUtils;
import zgoly.meteorist.Meteorist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DmSpam extends Module {
    public enum Mode {
        Same,
        Next,
        Random
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
            .name("command")
            .description("The command to send in direct messages.")
            .defaultValue("/msg")
            .build()
    );

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
            .name("messages")
            .description("Messages to use for spam.")
            .defaultValue(List.of("Meteorist :handshake: Meteor"))
            .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("\"Same\" - send same message; \"Next\" - send next message; \"Random\" - send random message.")
            .defaultValue(Mode.Next)
            .build()
    );

    private final Setting<Integer> messageDelay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay between specified messages in ticks.")
            .defaultValue(10)
            .min(1)
            .sliderMax(1200)
            .build()
    );
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay after sending all messages in ticks.")
            .defaultValue(100)
            .min(1)
            .sliderMax(1200)
            .build()
    );

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-self")
            .description("If true, don't send messages to yourself.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnLeave = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-leave")
            .description("Disables spam when you leave a server.")
            .defaultValue(true)
            .build()
    );


    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-disconnect")
            .description("Disables spam when you are disconnected from a server.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder()
            .name("bypass")
            .description("Add random text at the end of the message to try to bypass anti spams.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> length = sgGeneral.add(new IntSetting.Builder()
            .name("length")
            .description("Number of characters used to bypass anti spam.")
            .visible(bypass::get)
            .defaultValue(16)
            .sliderRange(1, 256)
            .build()
    );

    public DmSpam() {
        super(Meteorist.CATEGORY, "dm-spam", "Spams messages in players direct messages.");
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnLeave.get()) toggle();
    }

    private int playerIndex, messageIndex, messageTick, delayTick;
    private boolean delayActive;

    @Override
    public void onActivate() {
        playerIndex = 0;
        messageIndex = 0;
        messageTick = 0;
        delayTick = 0;
        delayActive = true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        List<String> msgs = messages.get();
        List<String> players = new ArrayList<>(Arrays.asList(mc.getServer().getPlayerNames()));
        if (ignoreSelf.get()) players.remove(mc.player.getName().getString());
        if (msgs.size() > 0 && players.size() > 0) {
            if (delayActive && playerIndex < players.size()) {
                delayActive = false;

                if (messageIndex >= msgs.size()) messageIndex = 0;

                String text = msgs.get(messageIndex);
                if (bypass.get()) {
                    text += " " + RandomStringUtils.randomAlphabetic(length.get()).toLowerCase();
                }
                mc.getNetworkHandler().sendChatCommand(command + " " + players.get(playerIndex) + " " + text);
                playerIndex++;

                if (mode.get() == Mode.Next) {
                    messageIndex++;
                } else if (mode.get() == Mode.Random) {
                    messageIndex = new Random().nextInt(msgs.size());
                }
            }

            if (!delayActive) {
                messageTick++;
                if (messageTick > messageDelay.get()) {
                    messageTick = 0;
                    delayActive = true;
                }
            }

            if (playerIndex >= players.size()) {
                delayTick++;
                if (delayTick > delay.get()) {
                    if (mode.get() == Mode.Same) messageIndex++;
                    delayTick = 0;
                    playerIndex = 0;
                }
            }
        }
    }
}