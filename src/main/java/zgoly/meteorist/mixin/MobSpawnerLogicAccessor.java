package zgoly.meteorist.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BaseSpawner.class)
public interface MobSpawnerLogicAccessor {
    @Invoker("isNearPlayer")
    boolean invokeIsPlayerInRange(Level world, BlockPos pos);

    @Accessor("requiredPlayerRange")
    int getRequiredPlayerRange();

    @Accessor("spawnRange")
    int getSpawnRange();
}
