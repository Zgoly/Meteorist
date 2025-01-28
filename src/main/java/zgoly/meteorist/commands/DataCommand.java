package zgoly.meteorist.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class DataCommand extends Command {
    public DataCommand() {
        super("data", "Gets NBT data of entities or blocks.", "target-nbt");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> getDataOrStates(false));
        buildCommand(builder, "get", false);
        buildCommand(builder, "copy", true);
    }

    private void buildCommand(LiteralArgumentBuilder<CommandSource> builder, String commandName, boolean copy) {
        builder.then(literal(commandName)
                .executes(context -> getDataOrStates(copy))
                .then(literal("player")
                        .executes(context -> getEntityData(mc.player, copy))
                        .then(argument("player", PlayerArgumentType.create()).executes(context -> getEntityData(PlayerArgumentType.get(context), copy)))
                )
                .then(literal("target")
                        .executes(context -> getDataOrStates(copy))
                        .then(literal("data").executes(context -> getFullData(copy)))
                        .then(literal("states").executes(context -> getFullStates(copy)))
                )
        );
    }

    public int getEntityData(Entity entity, boolean copy) {
        // NBT entity text
        NbtCompound nbt = entity.writeNbt(new NbtCompound());
        if (copy) {
            mc.keyboard.setClipboard(nbt.asString());
            info("Entity data was copied to your clipboard");
        } else {
            info(Text.literal("Entity data: ").append(NbtHelper.toPrettyPrintedText(nbt)));
        }
        return SINGLE_SUCCESS;
    }

    public int getDataOrStates(boolean copy) {
        if (getData(copy) != WarningType.NO_WARNING) {
            if (getStates(copy) != WarningType.NO_WARNING) {
                warningMessage(WarningType.NO_TARGET);
            }
        }
        return SINGLE_SUCCESS;
    }

    public int getFullData(boolean copy) {
        warningMessage(getData(copy));
        return SINGLE_SUCCESS;
    }

    public int getFullStates(boolean copy) {
        warningMessage(getStates(copy));
        return SINGLE_SUCCESS;
    }

    public WarningType getData(boolean copy) {
        if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            getEntityData(((EntityHitResult) mc.crosshairTarget).getEntity(), copy);
        } else if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
            BlockEntity blockEntity = mc.world.getBlockEntity(blockPos);
            if (blockEntity != null) {
                // NBT block entity text
                NbtCompound nbt = blockEntity.createNbtWithIdentifyingData(mc.world.getRegistryManager());
                if (copy) {
                    mc.keyboard.setClipboard(nbt.asString());
                    info("Block data was copied to your clipboard");
                } else {
                    info(Text.literal("Block data: ").append(NbtHelper.toPrettyPrintedText(nbt)));
                }
            } else {
                return WarningType.NOT_A_BLOCK_ENTITY;
            }
        } else {
            return WarningType.NO_TARGET;
        }
        return WarningType.NO_WARNING;
    }

    public WarningType getStates(boolean copy) {
        if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
            BlockState blockState = mc.world.getBlockState(blockPos);
            // NBT block states text
            NbtCompound nbt = NbtHelper.fromBlockState(blockState);
            if (copy) {
                mc.keyboard.setClipboard(nbt.asString());
                info("Block states were copied to your clipboard");
            } else {
                info(Text.literal("Block states: ").append(NbtHelper.toPrettyPrintedText(nbt)));
            }
        } else if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            return WarningType.NOT_A_BLOCK;
        } else {
            return WarningType.NO_TARGET;
        }
        return WarningType.NO_WARNING;
    }

    private void warningMessage(WarningType warning) {
        // The best way to avoid repeating stuff in code
        switch (warning) {
            case NOT_A_BLOCK_ENTITY -> warning("Target block is not a block entity");
            case NOT_A_BLOCK -> warning("Target is not a block");
            case NO_TARGET -> warning("There is no target for your cursor");
            case NO_WARNING -> {
            }
        }
    }

    public enum WarningType {
        NO_WARNING,
        NOT_A_BLOCK_ENTITY,
        NOT_A_BLOCK,
        NO_TARGET
    }
}