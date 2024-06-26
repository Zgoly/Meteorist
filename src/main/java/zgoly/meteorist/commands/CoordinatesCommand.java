package zgoly.meteorist.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;

public class CoordinatesCommand extends Command {
    public CoordinatesCommand() {
        super("coordinates", "Copies your coordinates to the clipboard.", "coords", "position", "pos");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> copyPos());
        builder.then(literal("copy").executes(context -> copyPos()));
        builder.then(literal("share-in-chat").executes(context -> {
            ChatUtils.sendPlayerMsg("Coordinates: " + getPos());
            return SINGLE_SUCCESS;
        }));
    }

    private String getPos() {
        BlockPos pos = mc.player.getBlockPos();
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private int copyPos() {
        mc.keyboard.setClipboard(getPos());
        info("Coordinates were copied to your clipboard");
        return SINGLE_SUCCESS;
    }
}