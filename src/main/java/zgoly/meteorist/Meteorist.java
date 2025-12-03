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
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zgoly.meteorist.commands.DataCommand;
import zgoly.meteorist.commands.InstructionsCommand;
import zgoly.meteorist.commands.InteractCommand;
import zgoly.meteorist.commands.PlayersInfoCommand;
import zgoly.meteorist.devmodules.DocsGenerator;
import zgoly.meteorist.hud.TextPresets;
import zgoly.meteorist.modules.*;
import zgoly.meteorist.modules.autocrafter.AutoCrafter;
import zgoly.meteorist.modules.autologin.AutoLogin;
import zgoly.meteorist.modules.autotrade.AutoTrade;
import zgoly.meteorist.modules.instructions.Instructions;
import zgoly.meteorist.modules.minescript.MinescriptIntegration;
import zgoly.meteorist.modules.placer.Placer;
import zgoly.meteorist.modules.rangeactions.RangeActions;
import zgoly.meteorist.modules.slotclick.SlotClick;
import zgoly.meteorist.settings.ItemsSetting;
import zgoly.meteorist.settings.StringPairSetting;
import zgoly.meteorist.utils.config.MeteoristConfigScreen;
import zgoly.meteorist.utils.misc.MeteoristStarscript;

import java.util.Random;

public class Meteorist extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Meteorist");

    public static final Category CATEGORY = new Category("Meteorist", Items.FIRE_CHARGE.getDefaultInstance());
    public static final Category DEV_CATEGORY = new Category("MeteoristDev", Items.REPEATING_COMMAND_BLOCK.getDefaultInstance());

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
    public static GuiTexture EYE;

    public static ResourceLocation identifier(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        // Log random message
        Random random = new Random();
        LOG.info(MESSAGES[random.nextInt(MESSAGES.length)]);

        // Icons
        ARROW_UP = GuiRenderer.addTexture(identifier("textures/icons/gui/arrow_up.png"));
        ARROW_DOWN = GuiRenderer.addTexture(identifier("textures/icons/gui/arrow_down.png"));
        EYE = GuiRenderer.addTexture(identifier("textures/icons/gui/eye.png"));

        // Config
        MeteoristConfigScreen.init();

        // Factories
        LOG.info("Registering custom factories...");
        StringPairSetting.register();
        ItemsSetting.register();

        // Modules
        LOG.info("Registering modules...");
        Modules.get().add(new AutoCrafter());
        Modules.get().add(new AutoFeed());
        Modules.get().add(new AutoRepair());
        Modules.get().add(new AutoHeal());
        Modules.get().add(new AutoInteract());
        Modules.get().add(new AutoLeave());
        Modules.get().add(new AutoLogin());
        Modules.get().add(new AutoMud());
        Modules.get().add(new AutoSleep());
        Modules.get().add(new AutoSneak());
        Modules.get().add(new AutoTrade());
        Modules.get().add(new BoatControl());
        Modules.get().add(new DisconnectSound());
        Modules.get().add(new DmSpam());
        Modules.get().add(new DoubleDoorsInteract());
        Modules.get().add(new EntityInteract());
        Modules.get().add(new Grid());
        Modules.get().add(new Instructions());
        Modules.get().add(new ItemSucker());
        Modules.get().add(new JumpFlight());
        Modules.get().add(new JumpJump());
        Modules.get().add(new Placer());
        Modules.get().add(new SlotClick());
        Modules.get().add(new MinescriptIntegration());
        Modules.get().add(new NerdVision());
        Modules.get().add(new RangeActions());
        Modules.get().add(new SignSearch());
        Modules.get().add(new ZAimbot());
        Modules.get().add(new ZAutoTotem());
        Modules.get().add(new ZKillaura());
        Modules.get().add(new ZoomPlus());

        // Dev modules
        Modules.get().add(new DocsGenerator());

        // Commands
        LOG.info("Registering commands...");
        Commands.add(new DataCommand());
        Commands.add(new InstructionsCommand());
        Commands.add(new InteractCommand());
        Commands.add(new PlayersInfoCommand());

        // HUD text presets
        LOG.info("Registering HUD text presets...");
        MeteoristStarscript.init();
        Hud.get().register(TextPresets.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
        Modules.registerCategory(DEV_CATEGORY);
    }

    @Override
    public String getWebsite() {
        return "https://github.com/Zgoly/Meteorist";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("zgoly", "meteorist");
    }

    @Override
    public String getCommit() {
        ModMetadata modMetadata = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata();
        String commit = modMetadata.getCustomValue(MOD_ID + ":commit").getAsString();
        return commit.isEmpty() ? null : commit;
    }

    @Override
    public String getPackage() {
        return getRepo().owner() + "." + getRepo().name();
    }
}
