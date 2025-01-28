package zgoly.meteorist.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zgoly.meteorist.events.DisconnectedScreenEvent;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin extends Screen {
    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    public void init(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(new DisconnectedScreenEvent());
    }
}
