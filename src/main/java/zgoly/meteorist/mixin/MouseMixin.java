package zgoly.meteorist.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import zgoly.meteorist.events.MouseSensitivityEvent;
import zgoly.meteorist.events.SmoothCameraEnabledEvent;
import zgoly.meteorist.events.UpdateCameraSmoothingEvent;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Mouse.class)
public class MouseMixin {
    @Redirect(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    public Object redirectGetMouseSensitivityValue(SimpleOption<?> instance) {
        if (instance == mc.options.getMouseSensitivity()) {
            return MeteorClient.EVENT_BUS.post(MouseSensitivityEvent.get((double) instance.getValue())).sensitivity;
        }
        return instance.getValue();
    }

    @ModifyArg(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Smoother;smooth(DD)D"), index = 1)
    public double smooth(double original) {
        return MeteorClient.EVENT_BUS.post(UpdateCameraSmoothingEvent.get(original)).timeDelta;
    }

    @ModifyExpressionValue(method = "updateMouse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;smoothCameraEnabled:Z"))
    public boolean smoothCameraEnabled(boolean original) {
        return MeteorClient.EVENT_BUS.post(SmoothCameraEnabledEvent.get(original)).enabled;
    }
}