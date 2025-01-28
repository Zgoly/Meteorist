package zgoly.meteorist.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zgoly.meteorist.events.HandRenderEvent;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void renderHand(Camera camera, float tickDelta, Matrix4f matrix4f, CallbackInfo ci) {
        if (!MeteorClient.EVENT_BUS.post(HandRenderEvent.get(true)).renderHand) ci.cancel();
    }
}
