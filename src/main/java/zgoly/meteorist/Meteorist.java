//By Zgoly
package zgoly.meteorist;

import meteordevelopment.meteorclient.systems.commands.Commands;
import zgoly.meteorist.commands.Coordinates;
import zgoly.meteorist.modules.*;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.io.IOException; import java.nio.file.Files;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Meteorist extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Meteorist");
    public static final Category CATEGORY = new Category("Meteorist", Items.DIRT.getDefaultStack());

    @Override
    public void onInitialize() {
        LOG.info("Meteorist here!");
        MeteorClient.EVENT_BUS.registerLambdaFactory("zgoly.meteorist", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        // Modules
        Modules.get().add(new AutoFeed());
        Modules.get().add(new AutoHeal());
        Modules.get().add(new AutoLeave());
        Modules.get().add(new AutoLogin());
        Modules.get().add(new ContainerCleaner());
        Modules.get().add(new HighJump());
        Modules.get().add(new ItemSucker());
        Modules.get().add(new JumpFlight());
        Modules.get().add(new NewVelocity());
        Modules.get().add(new SlotClick());
        // Commands
        Commands.get().add(new Coordinates());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }
}
