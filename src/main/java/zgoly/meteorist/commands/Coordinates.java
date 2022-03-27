//By Zgoly
package zgoly.meteorist.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.command.CommandSource;
import net.minecraft.text.LiteralText;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Coordinates extends Command {
    public Coordinates() {
        super("coordinates", "Copies your coordinates to the clipboard.", "coords", "position", "pos");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            String s = ", ";
            String pos = mc.player.getBlockPos().getX() + s + mc.player.getBlockPos().getY() + s + mc.player.getBlockPos().getZ();
            mc.keyboard.setClipboard(pos);
            mc.getToastManager().add(new SystemToast(SystemToast.Type.TUTORIAL_HINT,
                    new LiteralText("Coordinates copied"), new LiteralText("Paste, using Ctrl + V")));
            return SINGLE_SUCCESS;
        });
    }
}