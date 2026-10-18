package zgoly.meteorist.mixin;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zgoly.meteorist.events.HandRenderEvent;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void renderHand(CameraRenderState cameraState, PlayerRenderState playerState, GpuTextureView target, CallbackInfo ci) {
        if (!MeteorClient.EVENT_BUS.post(HandRenderEvent.get(true)).renderHand) ci.cancel();
    }
}
