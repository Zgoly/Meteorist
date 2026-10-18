package zgoly.meteorist.modules;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.phys.EntityHitResult;
import com.mojang.blaze3d.platform.InputConstants;
import zgoly.meteorist.Meteorist;

public class FollowTrader extends Module {
    private static final double MAX_TRADE_DISTANCE_SQR = 16;

    public enum Mode {
        Crosshair,
        NearestTrader;
    }

    private enum State {
        Idle,
        WaitingOpen,
        Trading,
        ShowingTrades;
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgKeybinds = settings.createGroup("Keybinds");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgNotifications = settings.createGroup("Notifications");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Which trader to interact with on keybind press.")
            .defaultValue(Mode.Crosshair)
            .build()
    );
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Maximum distance to search for traders in nearest-trader mode.")
            .min(0)
            .sliderMax(8)
            .defaultValue(4)
            .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Sends rotation packets towards the trader before interacting.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
            .name("swing-hand")
            .description("Swings the main hand when interacting with the trader.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> keepAfterShow = sgGeneral.add(new BoolSetting.Builder()
            .name("keep-after-show")
            .description("Closing the shown trade screen hides it again instead of ending the hidden trade.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoReacquire = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-reacquire")
            .description("Tries to re-establish the hidden trade if the server closes it because you moved too far away.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> maxReacquires = sgGeneral.add(new IntSetting.Builder()
            .name("max-reacquires")
            .description("Maximum number of reacquire attempts per trade.")
            .min(0)
            .sliderMax(10)
            .defaultValue(3)
            .visible(autoReacquire::get)
            .build()
    );
    private final Setting<Integer> reacquireDelay = sgGeneral.add(new IntSetting.Builder()
            .name("reacquire-delay")
            .description("Delay in ticks between reacquire attempts.")
            .min(1)
            .sliderMax(60)
            .defaultValue(10)
            .visible(autoReacquire::get)
            .build()
    );

    private final Setting<Boolean> box = sgRender.add(new BoolSetting.Builder()
            .name("box")
            .description("Draws a box around the current trader.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> boxSideColor = sgRender.add(new ColorSetting.Builder()
            .name("box-side-color")
            .description("Side color of the trader box.")
            .defaultValue(new SettingColor(75, 175, 75, 50))
            .visible(box::get)
            .build()
    );
    private final Setting<SettingColor> boxLineColor = sgRender.add(new ColorSetting.Builder()
            .name("box-line-color")
            .description("Line color of the trader box.")
            .defaultValue(new SettingColor(75, 175, 75, 255))
            .visible(box::get)
            .build()
    );
    private final Setting<Boolean> tracer = sgRender.add(new BoolSetting.Builder()
            .name("tracer")
            .description("Draws a line from your eyes to the current trader.")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> tracerColor = sgRender.add(new ColorSetting.Builder()
            .name("tracer-color")
            .description("Color of the trader tracer.")
            .defaultValue(new SettingColor(75, 175, 75, 255))
            .visible(tracer::get)
            .build()
    );

    private final Setting<Keybind> interactKeybind = sgKeybinds.add(new KeybindSetting.Builder()
            .name("interact-keybind")
            .description("Starts a hidden trade with the selected trader. Pressing it again declines the current trade.")
            .defaultValue(Keybind.fromKey(InputConstants.KEY_G))
            .action(this::interact)
            .build()
    );
    private final Setting<Keybind> showTradesKeybind = sgKeybinds.add(new KeybindSetting.Builder()
            .name("show-trades-keybind")
            .description("Shows the real trade screen of the current hidden trade.")
            .defaultValue(Keybind.fromKeys(InputConstants.KEY_G, InputConstants.MOD_SHIFT))
            .action(this::showTrades)
            .build()
    );

    private final Setting<Boolean> notifications = sgNotifications.add(new BoolSetting.Builder()
            .name("notifications")
            .description("Sends messages about hidden trade state changes and problems.")
            .defaultValue(true)
            .build()
    );

    private State state = State.Idle;
    private boolean closingIntentionally;
    private boolean reacquiring;
    private AbstractVillager trader;
    private int reacquiresLeft;
    private int reacquireTimer;
    private int waitingTimer;
    private MerchantMenu tradeMenu;

    public FollowTrader() {
        super(Meteorist.CATEGORY, "follow-trader", "Makes the trader follow you by hiding the trade screen.");
    }

    @Override
    public void onActivate() {
        resetState();
    }

    @Override
    public void onDeactivate() {
        if ((state == State.Trading || state == State.ShowingTrades) && mc.player != null && mc.player.containerMenu instanceof MerchantMenu) {
            closingIntentionally = true;
            mc.player.closeContainer();
            closingIntentionally = false;
        }
        resetState();
    }

    @EventHandler
    private void onScreenSet(OpenScreenEvent event) {
        if (!(event.screen instanceof MerchantScreen merchantScreen)) return;
        if (state != State.WaitingOpen) return;

        tradeMenu = merchantScreen.getMenu();
        state = State.Trading;
        reacquiring = false;

        if (notifications.get()) {
            info("Hidden trade opened.");
        }

        event.cancel();
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundContainerClosePacket)) return;
        if (closingIntentionally) return;

        if (state == State.Trading || (state == State.ShowingTrades && keepAfterShow.get())) {
            // Closing the inventory screen sends this packet for the still-open merchant menu, killing the trade.
            event.cancel();
            state = State.Trading;
            mc.execute(this::restoreTradeMenu);
            return;
        }

        if (state == State.ShowingTrades) mc.execute(this::onTradeClosed);
    }

    private void restoreTradeMenu() {
        if (state == State.Trading && tradeMenu != null && mc.player != null && mc.player.containerMenu == mc.player.inventoryMenu) {
            mc.player.containerMenu = tradeMenu;
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof ClientboundContainerClosePacket)) return;
        if (state != State.Trading && state != State.ShowingTrades) return;

        mc.execute(this::onTradeClosed);
    }

    private void onTradeClosed() {
        boolean intentional = state == State.ShowingTrades;
        boolean wasRetrying = reacquiring;
        boolean canReacquire = !wasRetrying && autoReacquire.get() && reacquiresLeft > 0;
        AbstractVillager lostTrader = trader;
        int attemptsLeft = reacquiresLeft;

        resetState();

        if (intentional) {
            if (notifications.get()) info("Hidden trade ended.");
            return;
        }
        if (!wasRetrying && notifications.get()) warning("Hidden trade closed.");

        if (canReacquire && lostTrader != null && lostTrader.isAlive() && !lostTrader.isRemoved()) {
            trader = lostTrader;
            reacquiresLeft = attemptsLeft;
            reacquireTimer = 0;
            reacquiring = true;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!reacquiring) {
            if (state == State.WaitingOpen && --waitingTimer <= 0) {
                if (notifications.get()) warning("No trade screen opened.");
                resetState();
            }
            return;
        }

        if (state == State.Trading || state == State.ShowingTrades) {
            reacquiring = false;
            return;
        }

        if (--reacquireTimer > 0) return;

        if (trader == null || reacquiresLeft <= 0 || !trader.isAlive() || trader.isRemoved()) {
            if (notifications.get()) warning("Failed to reacquire the trader.");
            resetState();
            return;
        }

        if (mc.player.distanceToSqr(trader) > MAX_TRADE_DISTANCE_SQR) {
            reacquireTimer = 1;
            return;
        }

        reacquiresLeft--;
        reacquireTimer = reacquireDelay.get();
        state = State.WaitingOpen;
        startInteraction(trader);

        if (notifications.get()) info("Trying to reacquire the trader...");
    }

    private void interact() {
        if (state == State.Trading || state == State.ShowingTrades) {
            decline();
            return;
        }
        if (state != State.Idle && state != State.WaitingOpen) return;

        AbstractVillager target = findTarget();
        if (target == null) return;

        trader = target;
        reacquiresLeft = maxReacquires.get();
        reacquiring = false;
        state = State.WaitingOpen;
        waitingTimer = 20;
        startInteraction(target);
    }

    private void startInteraction(AbstractVillager target) {
        Runnable action = () -> {
            EntityHitResult hitResult = new EntityHitResult(target);
            mc.gameMode.interact(mc.player, target, hitResult, InteractionHand.MAIN_HAND);
            if (swingHand.get()) mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getInteractAnimation(), false);
        };

        if (rotate.get()) Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), 100, action);
        else action.run();
    }

    private void decline() {
        if (!(mc.player.containerMenu instanceof MerchantMenu)) {
            resetState();
            return;
        }

        closingIntentionally = true;
        mc.player.closeContainer();
        closingIntentionally = false;
        resetState();

        if (notifications.get()) info("Hidden trade declined.");
    }

    private AbstractVillager findTarget() {
        if (mode.get() == Mode.Crosshair) {
            Entity picked = mc.crosshairPickEntity;

            if (!(picked instanceof AbstractVillager villager)) {
                if (notifications.get()) info("No trader under the crosshair.");
                return null;
            }
            if (villager.isBaby()) {
                if (notifications.get()) warning("That trader is a baby and cannot trade.");
                return null;
            }
            if (!villager.isAlive()) return null;

            return villager;
        }

        double rangeSqr = range.get() * range.get();
        AbstractVillager best = null;
        double bestDistanceSqr = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractVillager villager) || villager.isBaby() || !villager.isAlive()) continue;

            double distanceSqr = mc.player.distanceToSqr(villager);
            if (distanceSqr <= rangeSqr && distanceSqr < bestDistanceSqr) {
                best = villager;
                bestDistanceSqr = distanceSqr;
            }
        }

        if (best == null && notifications.get()) info("No traders nearby.");
        return best;
    }

    private void showTrades() {
        if (state != State.Trading || !(mc.player.containerMenu instanceof MerchantMenu menu)) return;

        state = State.ShowingTrades;
        mc.gui.setScreen(new MerchantScreen(menu, mc.player.getInventory(), Component.literal("Trading")));
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (trader == null || !trader.isAlive() || trader.isRemoved()) return;

        double x = trader.xo + (trader.getX() - trader.xo) * event.tickDelta;
        double y = trader.yo + (trader.getY() - trader.yo) * event.tickDelta;
        double z = trader.zo + (trader.getZ() - trader.zo) * event.tickDelta;

        if (box.get()) {
            event.renderer.box(trader.getBoundingBox().move(x - trader.getX(), y - trader.getY(), z - trader.getZ()), boxSideColor.get(), boxLineColor.get(), ShapeMode.Both, 0);
        }

        if (tracer.get()) {
            double height = trader.getBbHeight();
            event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, x, y + height / 2, z, tracerColor.get());
        }
    }

    private void resetState() {
        state = State.Idle;
        closingIntentionally = false;
        reacquiring = false;
        trader = null;
        reacquiresLeft = 0;
        reacquireTimer = 0;
        waitingTimer = 0;
        tradeMenu = null;
    }
}
