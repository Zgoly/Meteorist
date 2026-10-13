package zgoly.meteorist.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zgoly.meteorist.modules.AutoBrush;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void meteorist$suspendRelease(Player player, CallbackInfo ci) {
        if (AutoBrush.suspendRelease) ci.cancel();
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void meteorist$blockUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
        if (AutoBrush.suspendRelease) ci.setReturnValue(InteractionResult.PASS);
    }
}
