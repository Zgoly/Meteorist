package zgoly.meteorist.utils.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static zgoly.meteorist.Meteorist.MOD_ID;
import static zgoly.meteorist.utils.MeteoristUtils.removeInvalidChars;

public class MeteoristConfig {
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
            e.printStackTrace();
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

            return NbtIo.read(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return new CompoundTag();
        }
    }
}
