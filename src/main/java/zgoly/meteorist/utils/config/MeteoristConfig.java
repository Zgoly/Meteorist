package zgoly.meteorist.utils.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static zgoly.meteorist.Meteorist.MOD_ID;

public class MeteoristConfig {
    public static void save(String folderName, String fileName, NbtCompound data) {
        String path = Paths.get(FabricLoader.getInstance().getGameDir().toString(), MOD_ID, folderName, fileName + ".nbt").toString();
        try {
            File file = new File(path);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            NbtIo.write(data, Path.of(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static NbtCompound load(String folderName, String fileName) {
        String path = Paths.get(FabricLoader.getInstance().getGameDir().toString(), MOD_ID, folderName, fileName + ".nbt").toString();
        NbtCompound data = new NbtCompound();
        try {
            File file = new File(path);
            if (file.exists()) data = NbtIo.read(Path.of(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}
