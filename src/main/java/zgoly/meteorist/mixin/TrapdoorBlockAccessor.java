package zgoly.meteorist.mixin;

import net.minecraft.block.BlockSetType;
import net.minecraft.block.TrapdoorBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TrapdoorBlock.class)
public interface TrapdoorBlockAccessor {
    @Invoker("getBlockSetType")
    BlockSetType invokeGetBlockSetType();
}
