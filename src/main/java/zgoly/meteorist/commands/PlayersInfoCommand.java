package zgoly.meteorist.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import zgoly.meteorist.commands.arguments.PlayerPropertiesArgumentType;
import zgoly.meteorist.utils.SdlFileDialog;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PlayersInfoCommand extends Command {
    public PlayersInfoCommand() {
        super("playersinfo", "Saves in file / copies to clipboard info about players on current server.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(literal("copy")
                .executes(this::copyPlayersInfo)
                .then(argument("properties", PlayerPropertiesArgumentType.create()).executes(this::copyPlayersInfo)));
        builder.then(literal("save")
                .executes(this::savePlayersInfo)
                .then(argument("properties", PlayerPropertiesArgumentType.create()).executes(this::savePlayersInfo)));
    }

    private int savePlayersInfo(CommandContext<ClientSuggestionProvider> context) {
        String playersInfo = getPlayersInfo(context);

        SdlFileDialog.save("players_info.csv", "CSV File", "csv", path -> {
            try (FileOutputStream outputStream = new FileOutputStream(path)) {
                outputStream.write(playersInfo.getBytes());
                info(savedMessage(path));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return SINGLE_SUCCESS;
    }

    private Component savedMessage(String path) {
        Style style = Style.EMPTY
                .applyFormats(ChatFormatting.WHITE, ChatFormatting.UNDERLINE)
                .withClickEvent(new ClickEvent.OpenFile(path))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Open file").withStyle(ChatFormatting.GRAY)));

        MutableComponent file = Component.literal(path).withStyle(style);
        return Component.literal("Players info was saved to ").withStyle(ChatFormatting.GRAY).append(file);
    }

    private int copyPlayersInfo(CommandContext<ClientSuggestionProvider> context) {
        mc.keyboardHandler.setClipboard(getPlayersInfo(context));
        info("Players info was copied to clipboard");
        return SINGLE_SUCCESS;
    }

    private String getPlayersInfo(CommandContext<ClientSuggestionProvider> context) {
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
                    finalString.add(string2 == null ? "" : string2);
                }
                case "latency" -> finalString.add(String.valueOf(player.getLatency()));
            }
        }
        return finalString;
    }
}
