package zgoly.meteorist.utils.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static meteordevelopment.meteorclient.MeteorClient.LOG;
import static zgoly.meteorist.Meteorist.MOD_ID;
import static zgoly.meteorist.utils.MeteoristUtils.removeInvalidChars;

public class MeteoristAddonConfig {
    /// Saves a NbtCompound to a file. If the file does not exist, it will be created.
    /// The file will be placed in the .minecraft/MOD_ID/folderName directory.
    ///
    /// @param folderName The subfolder that the file will be saved in.
    /// @param fileName   The name of the file (without extension).
    /// @param data       The data to be saved.
    public static void save(String folderName, String fileName, CompoundTag data) {
        try {
            Path basePath = FabricLoader.getInstance().getGameDir().resolve(MOD_ID);
            Path filePath = basePath.resolve(removeInvalidChars(folderName)).resolve(fileName + ".nbt");
            Files.createDirectories(filePath.getParent());

            NbtIo.write(data, filePath);
        } catch (IOException e) {
            LOG.error("Failed to save config {}/{}", folderName, fileName, e);
        }
    }

    /// Loads an NBT file from the mods data directory.
    ///
    /// @param folderName the folder name inside the mods data directory
    /// @param fileName   the file name without the extension
    /// @return the loaded NBTCompound or an empty one if the file does not exist
    public static CompoundTag load(String folderName, String fileName) {
        try {
            Path basePath = FabricLoader.getInstance().getGameDir().resolve(MOD_ID);
            Path filePath = basePath.resolve(removeInvalidChars(folderName)).resolve(fileName + ".nbt");
            if (!Files.exists(filePath)) return new CompoundTag();

            CompoundTag tag = NbtIo.read(filePath);
            return tag != null ? tag : new CompoundTag();
        } catch (IOException e) {
            LOG.error("Failed to load config {}/{}", folderName, fileName, e);
            return new CompoundTag();
        }
    }
}
