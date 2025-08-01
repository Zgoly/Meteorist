package zgoly.meteorist.mixin;

import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MobSpawnerLogic.class)
public interface MobSpawnerLogicAccessor {
    @Invoker("isPlayerInRange")
    boolean invokeIsPlayerInRange(World world, BlockPos pos);

    @Accessor("requiredPlayerRange")
    int getRequiredPlayerRange();

    @Accessor("spawnRange")
    int getSpawnRange();
}
