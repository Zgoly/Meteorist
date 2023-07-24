package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.text.Text;
import org.apache.commons.lang3.RandomStringUtils;
import zgoly.meteorist.Meteorist;

import java.util.*;

public class DmSpam extends Module {
    public enum MessageMode {
        Next,
        Random
    }

    public enum PlayerMode {
        Next,
        Random
    }

    public enum DisableOn {
        None,
        MessagesEnd,
        PlayersEnd
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
            .name("command")
            .description("The command to send in direct messages.")
            .defaultValue("/msg {player} {message}")
            .build()
    );

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
            .name("messages")
            .description("Messages to use for spam.")
            .defaultValue(List.of("Meteorist :handshake: Meteor"))
            .build()
    );

    private final Setting<MessageMode> messageMode = sgGeneral.add(new EnumSetting.Builder<MessageMode>()
            .name("mode")
            .description("\"Same\" - send same message; \"Next\" - send next message; \"Random\" - send random message.")
            .defaultValue(MessageMode.Next)
            .build()
    );

    private final Setting<PlayerMode> playerMode = sgGeneral.add(new EnumSetting.Builder<PlayerMode>()
            .name("mode")
            .description("\"Next\" - send to next player; \"Random\" - send to random player.")
            .defaultValue(PlayerMode.Next)
            .build()
    );

    private final Setting<Integer> messageDelay = sgGeneral.add(new IntSetting.Builder()
            .name("message-delay")
            .description("Delay between specified messages in ticks.")
            .defaultValue(20)
            .min(1)
            .sliderMax(1200)
            .build()
    );
    private final Setting<Integer> playerDelay = sgGeneral.add(new IntSetting.Builder()
            .name("player-delay")
            .description("Delay after sending messages to all players, in ticks.")
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

    private final Setting<DisableOn> disableOn = sgGeneral.add(new EnumSetting.Builder<DisableOn>()
            .name("disable-on")
            .defaultValue(DisableOn.None)
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

    private final Setting<Boolean> log = sgGeneral.add(new BoolSetting.Builder()
            .name("debug-info")
            .description("Log debug info in chat.")
            .defaultValue(false)
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

    private long currentTick;

    private List<String> usedPlayers = new ArrayList<>();
    private List<String> usedMessages = new ArrayList<>();

    @Override
    public void onActivate() {
        currentTick = mc.world.getTime();
    }

    public void onDeactivate() {
        usedPlayers.clear();
        usedMessages.clear();
    }

    // I'm not sure if this is the best way to do it, but it seems to work like a charm
    @EventHandler
    private void onTick(TickEvent.Post event) {
        List<String> players = Arrays.asList(mc.getServer().getPlayerNames());
        if (mc.getServer() == null || players.isEmpty() || messages.get().isEmpty()) return;

        List<String> remainPlayers = new ArrayList<>(players);
        remainPlayers.removeAll(usedPlayers);

        if (ignoreSelf.get()) remainPlayers.remove(mc.player.getName().getString());

        if (!remainPlayers.isEmpty()) {
            if (currentTick <= mc.world.getTime()) {

                String selectedPlayer;
                if (playerMode.get() == PlayerMode.Next) {
                    selectedPlayer = remainPlayers.get(0);
                } else {
                    Random random = new Random();
                    selectedPlayer = remainPlayers.get(random.nextInt(remainPlayers.size()));
                }

                List<String> remainMessages = new ArrayList<>(messages.get());
                remainMessages.removeAll(usedMessages);

                if (remainMessages.isEmpty()) {
                    if (disableOn.get() == DisableOn.MessagesEnd) {
                        toggle();
                        return;
                    }
                    usedMessages.clear();
                    remainMessages = new ArrayList<>(messages.get());
                }

                String selectedMessage;

                if (messageMode.get() == MessageMode.Next) {
                    selectedMessage = remainMessages.get(0);
                } else {
                    Random random = new Random();
                    selectedMessage = remainMessages.get(random.nextInt(remainMessages.size()));
                }

                usedMessages.add(selectedMessage);

                if (bypass.get()) selectedMessage += " " + RandomStringUtils.randomAlphabetic(length.get()).toLowerCase();
                ChatUtils.sendPlayerMsg(command.get().replace("{player}", selectedPlayer).replace("{message}", selectedMessage));
                if (log.get()) info("Sent \"" + selectedMessage + "\" to \"" + selectedPlayer + "\". Handling a delay of " + messageDelay.get() + " ticks.");

                usedPlayers.add(selectedPlayer);

                if (remainPlayers.size() > 1) {
                    currentTick = mc.world.getTime() + messageDelay.get();
                }
            }
        } else {
            if (disableOn.get() == DisableOn.PlayersEnd) {
                toggle();
                return;
            }
            if (currentTick <= mc.world.getTime()) {
                currentTick = mc.world.getTime() + playerDelay.get();
                if (log.get()) info("The players ended, handling a delay of " + playerDelay.get() + " ticks.");
                usedPlayers.clear();
            }
        }
    }
}