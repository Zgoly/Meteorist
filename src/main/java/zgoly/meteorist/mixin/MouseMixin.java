package zgoly.meteorist.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import zgoly.meteorist.events.MouseSensitivityEvent;
import zgoly.meteorist.events.SmoothCameraEnabledEvent;
import zgoly.meteorist.events.UpdateCameraSmoothingEvent;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    public Object redirectGetMouseSensitivityValue(OptionInstance<?> instance) {
        if (instance == mc.options.sensitivity()) {
            return MeteorClient.EVENT_BUS.post(MouseSensitivityEvent.get((double) instance.get())).sensitivity;
        }
        return instance.get();
    }

    @ModifyArg(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/SmoothDouble;getNewDeltaValue(DD)D"), index = 1)
    public double smooth(double original) {
        return MeteorClient.EVENT_BUS.post(UpdateCameraSmoothingEvent.get(original)).timeDelta;
    }

    @ModifyExpressionValue(method = "turnPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;smoothCamera:Z"))
    public boolean smoothCameraEnabled(boolean original) {
        return MeteorClient.EVENT_BUS.post(SmoothCameraEnabledEvent.get(original)).enabled;
    }
}
