package zgoly.meteorist.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.ClientAsset;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import zgoly.meteorist.commands.arguments.PlayerPropertiesArgumentType;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PlayersInfoCommand extends Command {
    public PlayersInfoCommand() {
        super("playersinfo", "Saves in file / copies to clipboard info about players on current server.");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("copy")
                .executes(this::copyPlayersInfo)
                .then(argument("properties", PlayerPropertiesArgumentType.create()).executes(this::copyPlayersInfo)));
        builder.then(literal("save")
                .executes(this::savePlayersInfo)
                .then(argument("properties", PlayerPropertiesArgumentType.create()).executes(this::savePlayersInfo)));
    }

    private int savePlayersInfo(CommandContext<SharedSuggestionProvider> context) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filterBuffer = stack.mallocPointer(1);
            filterBuffer.put(stack.UTF8("*.csv"));
            filterBuffer.flip();
            String result = TinyFileDialogs.tinyfd_saveFileDialog("Save Players Info", "players_info.csv", filterBuffer, "CSV File (*.csv)");
            if (result != null) {
                try {
                    FileOutputStream outputStream = new FileOutputStream(result);
                    outputStream.write(getPlayersInfo(context).getBytes());
                    outputStream.close();
                    info("Players info was saved to (highlight)%s(default)", result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return SINGLE_SUCCESS;
    }

    private int copyPlayersInfo(CommandContext<SharedSuggestionProvider> context) {
        mc.keyboardHandler.setClipboard(getPlayersInfo(context));
        info("Players info was copied to clipboard");
        return SINGLE_SUCCESS;
    }

    private String getPlayersInfo(CommandContext<SharedSuggestionProvider> context) {
        List<String> properties;

        try {
            properties = PlayerPropertiesArgumentType.get(context);
        } catch (Exception e) {
            properties = PlayerPropertiesArgumentType.PROPERTIES;
        }

        StringBuilder info = new StringBuilder();
        info.append(String.join(",", properties.isEmpty() ? PlayerPropertiesArgumentType.PROPERTIES : properties)).append("\n");
        if (mc.getConnection() != null) {
            List<PlayerInfo> sortedPlayerList = mc.getConnection().getOnlinePlayers().stream()
                    .sorted((p1, p2) -> p1.getProfile().name().compareToIgnoreCase(p2.getProfile().name()))
                    .toList();
            for (PlayerInfo player : sortedPlayerList) {
                info.append(String.join(",", getProperties(player, properties))).append("\n");
            }
        }

        return info.toString();
    }

    private List<String> getProperties(PlayerInfo player, List<String> properties) {
        List<String> finalString = new ArrayList<>();
        for (String property : properties) {
            switch (property.toLowerCase()) {
                case "player" -> finalString.add(player.getProfile().name());
                case "uuid" -> finalString.add(String.valueOf(player.getProfile().id()));
                case "gamemode" -> finalString.add(String.valueOf(player.getGameMode()));
                case "skin_url" -> {
                    String string2 = player.getSkin().body() instanceof ClientAsset.DownloadedTexture skinAssetInfo ? skinAssetInfo.url() : null;
                    finalString.add(string2);
                }
                case "latency" -> finalString.add(String.valueOf(player.getLatency()));
            }
        }
        return finalString;
    }
}
