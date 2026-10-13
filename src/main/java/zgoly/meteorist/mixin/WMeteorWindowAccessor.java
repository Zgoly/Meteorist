package zgoly.meteorist.mixin;

import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WWindow.class)
public interface WMeteorWindowAccessor {
    @Accessor("title")
    String meteorist$getTitle();
}
