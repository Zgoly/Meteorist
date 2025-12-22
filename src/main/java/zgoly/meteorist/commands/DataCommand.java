package zgoly.meteorist.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import net.minecraft.advancements.criterion.NbtPredicate;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class DataCommand extends Command {
    public DataCommand() {
        super("data", "Gets NBT data of entities or blocks.", "target-nbt");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> getDataOrStates(false));
        buildCommand(builder, "get", false);
        buildCommand(builder, "copy", true);
    }

    private void buildCommand(LiteralArgumentBuilder<SharedSuggestionProvider> builder, String commandName, boolean copy) {
        builder.then(literal(commandName)
                .executes(context -> getDataOrStates(copy))
                .then(literal("player")
                        .executes(context -> getEntityData(mc.player, copy))
                        .then(argument("player", PlayerArgumentType.create())
                                .executes(context -> getEntityData(PlayerArgumentType.get(context), copy)))
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
        CompoundTag nbt = NbtPredicate.getEntityTagToCompare(entity);
        if (copy) {
            mc.keyboardHandler.setClipboard(nbt.asString().orElse(""));
            info("Entity data was copied to your clipboard");
        } else {
            info(Component.literal("Entity data: ").append(NbtUtils.toPrettyComponent(nbt)));
        }
        return SINGLE_SUCCESS;
    }

    public int getDataOrStates(boolean copy) {
        WarningType dataWarning = getData(copy);
        WarningType statesWarning = getStates(copy);

        if (dataWarning != WarningType.NO_WARNING && statesWarning != WarningType.NO_WARNING) {
            warningMessage(WarningType.NO_TARGET);
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
        if (mc.hitResult.getType() == HitResult.Type.ENTITY) {
            getEntityData(((EntityHitResult) mc.hitResult).getEntity(), copy);
        } else if (mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) mc.hitResult).getBlockPos();
            BlockEntity blockEntity = mc.level.getBlockEntity(blockPos);
            if (blockEntity != null) {
                // NBT block entity text
                CompoundTag nbt = blockEntity.saveWithFullMetadata(mc.level.registryAccess());
                if (copy) {
                    mc.keyboardHandler.setClipboard(nbt.asString().orElse(""));
                    info("Block data was copied to your clipboard");
                } else {
                    info(Component.literal("Block data: ").append(NbtUtils.toPrettyComponent(nbt)));
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
        if (mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) mc.hitResult).getBlockPos();
            BlockState blockState = mc.level.getBlockState(blockPos);
            // NBT block states text
            CompoundTag nbt = NbtUtils.writeBlockState(blockState);
            if (copy) {
                mc.keyboardHandler.setClipboard(nbt.asString().orElse(""));
                info("Block states were copied to your clipboard");
            } else {
                info(Component.literal("Block states: ").append(NbtUtils.toPrettyComponent(nbt)));
            }
        } else if (mc.hitResult.getType() == HitResult.Type.ENTITY) {
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
                // No warning to display
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
