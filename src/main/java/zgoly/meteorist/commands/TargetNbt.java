package zgoly.meteorist.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import zgoly.meteorist.utils.MeteoristUtils;

public class TargetNbt extends Command {
    public TargetNbt() {
        super("target-nbt", "Gets NBT of target you're looking at.", "nbt-target", "target");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> performAction(false));
        builder.then(literal("get").executes(context -> performAction(false)));
        builder.then(literal("copy").executes(context -> performAction(true)));
    }

    private int performAction(boolean copyToClipboard) {
        NbtCompound targetNbt = getTargetNbt();
        if (targetNbt != null) {
            if (copyToClipboard) {
                mc.keyboard.setClipboard(targetNbt.asString());
                info("NBT successfully copied to your clipboard");
            } else {
                info(Text.literal("Target NBT: ").append(NbtHelper.toPrettyPrintedText(targetNbt)));
            }
        }
        return SINGLE_SUCCESS;
    }

    private NbtCompound getTargetNbt() {
        HitResult hitResult = MeteoristUtils.getCrosshairTarget(mc.player, 512, false, (e -> !e.isSpectator()));
        if (hitResult == null || hitResult.getType() == null) return null;

        switch (hitResult.getType()) {
            case MISS -> warning("There is no target for your cursor");
            case BLOCK -> {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
                BlockEntity blockEntity = mc.world.getBlockEntity(pos);

                if (blockEntity != null) {
                    return blockEntity.createNbtWithIdentifyingData(mc.world.getRegistryManager());
                } else {
                    warning("Block you're targeting doesn't have NBT");
                }
            }
            case ENTITY -> {
                return ((EntityHitResult) hitResult).getEntity().writeNbt(new NbtCompound());
            }
        }

        return null;
    }
}