package zgoly.meteorist.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import zgoly.meteorist.utils.movement.WalkeristUtils;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Entity.class)
public abstract class EntityMoveRelativeMixin {
    @ModifyArg(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"), index = 2)
    private float steerYaw(float yaw) {
        if ((Object) this != mc.player) return yaw;
        float nodeYaw = WalkeristUtils.steerYaw();
        return Float.isNaN(nodeYaw) ? yaw : nodeYaw;
    }
}
