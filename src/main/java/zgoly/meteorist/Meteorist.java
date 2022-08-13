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

import java.lang.invoke.MethodHandles;

public class Meteorist extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Meteorist");
    public static final Category CATEGORY = new Category("Meteorist", Items.DIRT.getDefaultStack());

    @Override
    public void onInitialize() {
        LOG.info("Meteorist joined the game");
        MeteorClient.EVENT_BUS.registerLambdaFactory("zgoly.meteorist", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        // Modules
        Modules.get().add(new AutoFeed());
        Modules.get().add(new AutoFloor());
        Modules.get().add(new AutoHeal());
        Modules.get().add(new AutoLeave());
        Modules.get().add(new AutoLogin());
        Modules.get().add(new ContainerCleaner());
        Modules.get().add(new AutoLight());
        Modules.get().add(new ItemSucker());
        Modules.get().add(new JumpFlight());
        Modules.get().add(new JumpJump());
        Modules.get().add(new NewVelocity());
        Modules.get().add(new SlotClick());
        Modules.get().add(new ZKillaura());
        // Commands
        Commands.get().add(new Coordinates());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "zgoly.meteorist";
    }
}