//By Zgoly
package zgoly.meteorist.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class TargetNbt extends Command {
    public TargetNbt() {
        super("target-nbt", "Gets NBT of target you're looking at.", "nbt-target", "target");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> getNbt());
        builder.then(literal("get").executes(context -> getNbt()));

        builder.then(literal("copy").executes(context -> {
            if (getTargetNbt() == null) return SINGLE_SUCCESS;
            mc.keyboard.setClipboard(getTargetNbt().toString());
            info("NBT successfully copied to your clipboard");
            return SINGLE_SUCCESS;
        }));
    }

    private NbtCompound getTargetNbt() {
        switch (mc.crosshairTarget.getType()) {
            case MISS -> info("There is no target for your cursor");
            case BLOCK -> {
                BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
                BlockEntity blockEntity = mc.world.getBlockEntity(pos);

                if (blockEntity != null) {
                    return blockEntity.createNbt();
                } else {
                    info("Block you're targeting doesn't have NBT");
                }
            }
            case ENTITY -> {
                return ((EntityHitResult) mc.crosshairTarget).getEntity().writeNbt(new NbtCompound());
            }

        }
        return null;
    }

    private int getNbt() {
        NbtCompound targetNbt = getTargetNbt();
        if (targetNbt == null) return SINGLE_SUCCESS;
        mc.player.sendMessage(Text.of(targetNbt.toString()));
        return SINGLE_SUCCESS;
    }
}