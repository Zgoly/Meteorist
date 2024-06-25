package zgoly.meteorist;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zgoly.meteorist.commands.CoordinatesCommand;
import zgoly.meteorist.commands.DataCommand;
import zgoly.meteorist.hud.TextPresets;
import zgoly.meteorist.modules.*;
import zgoly.meteorist.modules.autologin.AutoLogin;
import zgoly.meteorist.modules.instructions.Instructions;
import zgoly.meteorist.modules.placer.Placer;
import zgoly.meteorist.modules.slotclick.SlotClick;
import zgoly.meteorist.settings.StringPairSetting;
import zgoly.meteorist.utils.misc.MeteoristStarscript;

import java.util.Random;

public class Meteorist extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Meteorist");
    public static final Category CATEGORY = new Category("Meteorist", Items.FIRE_CHARGE.getDefaultStack());
    public static final HudGroup HUD_GROUP = new HudGroup("Meteorist");
    private static final String[] MESSAGES = {
            "Meteorist is coming",
            "Meteorist enabled",
            "Meteorist is here",
            "Meteorist will save us",
            "Meteorist joined the game",
            "Meteorist is ready to go",
            "Meteorist is on the move",
            "Meteorist... Was never real?"
    };
    public static String MOD_ID = "meteorist";
    public static GuiTexture ARROW_UP;
    public static GuiTexture ARROW_DOWN;
    public static GuiTexture COPY;
    public static GuiTexture EYE;

    @Override
    public void onInitialize() {
        // Log random message
        Random random = new Random();
        LOG.info(MESSAGES[random.nextInt(MESSAGES.length)]);

        // Factories
        LOG.info("Registering custom factories...");
        StringPairSetting.register();

        // Modules
        LOG.info("Registering modules...");
        Modules.get().add(new AutoFeed());
        Modules.get().add(new AutoFix());
        Modules.get().add(new AutoHeal());
        Modules.get().add(new AutoLeave());
        Modules.get().add(new AutoLogin());
        Modules.get().add(new AutoSleep());
        Modules.get().add(new AutoSneak());
        Modules.get().add(new BoatControl());
        Modules.get().add(new DmSpam());
        Modules.get().add(new EntityUse());
        Modules.get().add(new Grid());
        Modules.get().add(new Instructions());
        Modules.get().add(new ItemSucker());
        Modules.get().add(new JumpFlight());
        Modules.get().add(new JumpJump());
        Modules.get().add(new Placer());
        Modules.get().add(new SlotClick());
        Modules.get().add(new ZAimbot());
        Modules.get().add(new ZKillaura());

        // Commands
        LOG.info("Registering commands...");
        Commands.add(new CoordinatesCommand());
        Commands.add(new DataCommand());

        // Hud TextPresets
        LOG.info("Registering HUD presets...");
        MeteoristStarscript.init();
        Hud.get().register(TextPresets.INFO);

        // Icons
        ARROW_UP = GuiRenderer.addTexture(Identifier.of(MOD_ID, "textures/icons/gui/arrow_up.png"));
        ARROW_DOWN = GuiRenderer.addTexture(Identifier.of(MOD_ID, "textures/icons/gui/arrow_down.png"));
        COPY = GuiRenderer.addTexture(Identifier.of(MOD_ID, "textures/icons/gui/copy.png"));
        EYE = GuiRenderer.addTexture(Identifier.of(MOD_ID, "textures/icons/gui/eye.png"));
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return getRepo().owner() + "." + getRepo().name();
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("zgoly", "meteorist");
    }
}