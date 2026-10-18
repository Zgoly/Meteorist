package zgoly.meteorist.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * Wraps the asynchronous SDL save dialog into a simple fire-and-forget call.
 */
public class SdlFileDialog {
    // Keeps the native callback and the dialog itself alive until SDL repports back
    private static SDL_DialogFileCallback callback;

    /**
     * Shows a native save dialog with a single file filter. {@code onAccept} runs on the render thread with the chosen path.
     */
    @SuppressWarnings("resource") // the filter memory is handed over to SDL and freed in the callback below
    public static void save(String defaultName, String filterName, String extensions, Consumer<String> onAccept) {
        if (callback != null) return;

        Minecraft mc = Minecraft.getInstance();
        SDL_DialogFileFilter.Buffer filters = SDL_DialogFileFilter.calloc(1);
        ByteBuffer name = MemoryUtil.memUTF8(filterName, true);
        ByteBuffer pattern = MemoryUtil.memUTF8(extensions, true);
        filters.get(0).name(name).pattern(pattern);

        callback = SDL_DialogFileCallback.create((_, filelist, _) -> {
            filters.free();
            MemoryUtil.memFree(name);
            MemoryUtil.memFree(pattern);

            String path = filelist == MemoryUtil.NULL ? null : MemoryUtil.memUTF8Safe(MemoryUtil.memGetAddress(filelist));
            String error = filelist == MemoryUtil.NULL ? SDLError.SDL_GetError() : null;

            callback = null;
            mc.execute(() -> {
                if (path != null) onAccept.accept(path);
                else if (error != null && mc.player != null) {
                    mc.player.sendSystemMessage(Component.literal("Failed to open save dialog: " + error).withStyle(ChatFormatting.RED));
                }
            });
        });

        SDLDialog.SDL_ShowSaveFileDialog(callback, 0, mc.getWindow().handle(), filters, defaultName);
    }
}
