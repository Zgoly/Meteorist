package zgoly.meteorist;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zgoly.meteorist.commands.Coordinates;
import zgoly.meteorist.commands.TargetNbt;
import zgoly.meteorist.hud.Presets;
import zgoly.meteorist.modules.*;

import java.util.Random;

public class Meteorist extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Meteorist");
    public static final Category CATEGORY = new Category("Meteorist", Items.FIRE_CHARGE.getDefaultStack());
    public static final HudGroup HUD_GROUP = new HudGroup("Meteorist");
    private static final String[] MESSAGES = {
            "Clear path, Meteorist incoming!",
            "Meteorist enabled",
            "Meteorist is here!",
            "Meteorist is taking off!",
            "Meteorist joined the game"
    };

    public static GuiTexture ARROW_UP;
    public static GuiTexture ARROW_DOWN;
    public static GuiTexture COPY;

    @Override
    public void onInitialize() {
        // Log random message
        Random random = new Random();
        LOG.info(MESSAGES[random.nextInt(MESSAGES.length)]);

        // Modules
        Modules.get().add(new AutoFeed());
        Modules.get().add(new AutoFix());
        Modules.get().add(new AutoHeal());
        Modules.get().add(new AutoLeave());
        Modules.get().add(new AutoLight());
        Modules.get().add(new AutoLogin());
        Modules.get().add(new AutoSleep());
        Modules.get().add(new AutoSneak());
        Modules.get().add(new DmSpam());
        Modules.get().add(new EntityUse());
        Modules.get().add(new Instructions());
        Modules.get().add(new ItemSucker());
        Modules.get().add(new JumpFlight());
        Modules.get().add(new JumpJump());
        Modules.get().add(new Placer());
        Modules.get().add(new SlotClick());
        Modules.get().add(new ZAimbot());
        Modules.get().add(new ZKillaura());

        // Commands
        Commands.add(new Coordinates());
        Commands.add(new TargetNbt());

        // Hud Presets
        Presets.starscriptAdd();
        Hud hud = Systems.get(Hud.class);
        hud.register(Presets.INFO);

        // Icons
        ARROW_UP = GuiRenderer.addTexture(new Identifier("meteorist", "textures/icons/gui/arrow_up.png"));
        ARROW_DOWN = GuiRenderer.addTexture(new Identifier("meteorist", "textures/icons/gui/arrow_down.png"));
        COPY = GuiRenderer.addTexture(new Identifier("meteorist", "textures/icons/gui/copy.png"));
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