package zgoly.meteorist.modules.autotrade;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.gui.screens.OfferScreen;
import zgoly.meteorist.modules.autotrade.offers.BaseOffer;
import zgoly.meteorist.modules.autotrade.offers.IdOffer;
import zgoly.meteorist.modules.autotrade.offers.ItemsOffer;
import zgoly.meteorist.utils.config.MeteoristConfigManager;

import java.util.ArrayList;
import java.util.List;

import static zgoly.meteorist.Meteorist.*;

public class AutoTrade extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("Auto Trade");

    private final Setting<Boolean> oneOfferPerTick = sgGeneral.add(new BoolSetting.Builder()
            .name("one-offer-per-tick")
            .description("One offer per tick.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> closeWhenDone = sgGeneral.add(new BoolSetting.Builder()
            .name("close-when-done")
            .description("Closes the trade screen when done.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> printDebugInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("print-debug-info")
            .description("Prints debug information.")
            .defaultValue(false)
            .build()
    );

    private final OfferFactory factory = new OfferFactory();
    private final List<BaseOffer> offers = new ArrayList<>();
    private SetTradeOffersS2CPacket lastTrade = null;

    public AutoTrade() {
        super(Meteorist.CATEGORY, "auto-trade", "Automatically trades items with villagers (idea by Hiradpi).");
    }

    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();

        NbtList list = new NbtList();
        for (BaseOffer offer : offers) {
            NbtCompound mTag = new NbtCompound();
            mTag.putString("type", offer.getTypeName());
            mTag.put("offer", offer.toTag());

            list.add(mTag);
        }
        tag.put("offers", list);
        return tag;
    }

    public Module fromTag(NbtCompound tag) {
        super.fromTag(tag);

        offers.clear();
        NbtList list = tag.getList("offers", NbtElement.COMPOUND_TYPE);

        for (NbtElement tagII : list) {
            NbtCompound tagI = (NbtCompound) tagII;

            String type = tagI.getString("type");
            BaseOffer offer = factory.createOffer(type);

            if (offer != null) {
                NbtCompound offerTag = tagI.getCompound("offer");
                if (offerTag != null) offer.fromTag(offerTag);

                offers.add(offer);
            }
        }

        return this;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillWidget(theme, list);
        return list;
    }

    public void fillWidget(GuiTheme theme, WVerticalList list) {
        list.clear();

        WTable table = list.add(theme.table()).expandX().widget();

        for (BaseOffer offer : offers) {
            table.add(theme.label(offer.getTypeName())).expandX().widget();

            WHorizontalList container = table.add(theme.horizontalList()).expandX().widget();
            if (offer instanceof ItemsOffer itemsOffer) {
                int pad = 16;

                if (itemsOffer.checkFirstInputItem.get()) {
                    if (itemsOffer.checkFirstInputItemCount.get()) {
                        int min = itemsOffer.minFirstInputItemCount.get();
                        int max = itemsOffer.maxFirstInputItemCount.get();
                        String range = min == max ? "x" + min : "x" + min + " - x" + max;
                        container.add(theme.itemWithLabel(itemsOffer.firstInputItem.get().getDefaultStack(), range)).padRight(pad);
                    } else {
                        container.add(theme.item(itemsOffer.firstInputItem.get().getDefaultStack())).padRight(pad);
                    }
                }

                if (itemsOffer.checkFirstInputItem.get() && itemsOffer.checkSecondInputItem.get()) {
                    container.add(theme.label(" + ")).padRight(pad);
                }

                if (itemsOffer.checkSecondInputItem.get()) {
                    if (itemsOffer.checkSecondInputItemCount.get()) {
                        int min = itemsOffer.minSecondInputItemCount.get();
                        int max = itemsOffer.maxSecondInputItemCount.get();
                        String range = min == max ? "x" + min : "x" + min + " - x" + max;
                        container.add(theme.itemWithLabel(itemsOffer.secondInputItem.get().getDefaultStack(), range)).padRight(pad);
                    } else {
                        container.add(theme.item(itemsOffer.secondInputItem.get().getDefaultStack())).padRight(pad);
                    }
                }

                container.add(theme.label(">")).padRight(pad);

                if (itemsOffer.checkOutputItem.get()) {
                    if (itemsOffer.checkOutputItemCount.get()) {
                        int min = itemsOffer.minOutputItemCount.get();
                        int max = itemsOffer.maxOutputItemCount.get();
                        String range = min == max ? "x" + min : "x" + min + " - x" + max;
                        container.add(theme.itemWithLabel(itemsOffer.outputItem.get().getDefaultStack(), range));
                    } else {
                        container.add(theme.item(itemsOffer.outputItem.get().getDefaultStack()));
                    }
                }
            } else if (offer instanceof IdOffer idOffer) {
                WIntEdit edit = container.add(theme.intEdit(idOffer.offerId.get(), 0, Integer.MAX_VALUE, true)).expandX().widget();
                edit.tooltip = idOffer.offerId.description;
                edit.actionOnRelease = () -> idOffer.offerId.set(edit.get());
            }

            WButton edit = table.add(theme.button("Edit")).expandX().widget();
            edit.tooltip = "Edit the offer.";
            edit.action = () -> mc.setScreen(new OfferScreen(theme, offer));

            WContainer moveContainer = table.add(theme.horizontalList()).expandX().widget();
            if (offers.size() > 1) {
                int index = offers.indexOf(offer);
                if (index > 0) {
                    WButton moveUp = moveContainer.add(theme.button(ARROW_UP)).expandX().widget();
                    moveUp.tooltip = "Move offer up.";
                    moveUp.action = () -> {
                        offers.remove(index);
                        offers.add(index - 1, offer);
                        fillWidget(theme, list);
                    };
                }

                if (index < offers.size() - 1) {
                    WButton moveDown = moveContainer.add(theme.button(ARROW_DOWN)).expandX().widget();
                    moveDown.tooltip = "Move offer down.";
                    moveDown.action = () -> {
                        offers.remove(index);
                        offers.add(index + 1, offer);
                        fillWidget(theme, list);
                    };
                }
            }

            WButton copy = table.add(theme.button(COPY)).widget();
            copy.tooltip = "Copy offer.";
            copy.action = () -> {
                offers.add(offers.indexOf(offer), offer.copy());
                fillWidget(theme, list);
            };

            WMinus remove = table.add(theme.minus()).widget();
            remove.tooltip = "Remove offer.";
            remove.action = () -> {
                offers.remove(offer);
                fillWidget(theme, list);
            };

            table.row();
        }

        list.add(theme.horizontalSeparator()).expandX();
        WTable controls = list.add(theme.table()).expandX().widget();

        WButton createItemsOffer = controls.add(theme.button("New Items Offer")).expandX().widget();
        createItemsOffer.action = () -> {
            offers.add(new ItemsOffer());
            fillWidget(theme, list);
        };

        WButton createIdOffer = controls.add(theme.button("New ID Offer")).expandX().widget();
        createIdOffer.action = () -> {
            offers.add(new IdOffer());
            fillWidget(theme, list);
        };

        WButton removeAll = controls.add(theme.button("Remove All Offers")).widget();
        removeAll.action = () -> {
            offers.clear();
            fillWidget(theme, list);
        };

        MeteoristConfigManager.configManager(theme, list, this);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        boolean successfulOffer = false;
        if (lastTrade != null && mc.currentScreen instanceof MerchantScreen) {
            TradeOfferList tradeOffers = lastTrade.getOffers();
            boolean offerMatched = false;
            for (TradeOffer tradeOffer : tradeOffers) {
                for (BaseOffer offer : offers) {
                    if (!offer.enabled.get()) continue;
                    if (offer instanceof ItemsOffer itemsOffer) {
                        printInfo("");
                        printInfo("===== Trade Offer №" + tradeOffers.indexOf(tradeOffer) + " (Offer: " + offers.indexOf(offer) + ") =====");
                        boolean firstInputItemMatched = true;
                        boolean secondInputItemMatched = true;
                        boolean outputItemMatched = true;

                        printInfo("");
                        printInfo("First Input Item");
                        if (itemsOffer.checkFirstInputItem.get()) {
                            Item item = itemsOffer.firstInputItem.get();
                            Item tradeItem = tradeOffer.getFirstBuyItem().item().value();
                            printInfo("Item: " + item + ", Trade Item: " + tradeItem);
                            if (item == tradeItem) {
                                if (itemsOffer.checkFirstInputItemCount.get()) {
                                    int count;
                                    if (itemsOffer.useFinalCount.get()) {
                                        count = tradeOffer.getDisplayedFirstBuyItem().getCount();
                                    } else {
                                        count = tradeOffer.getOriginalFirstBuyItem().getCount();
                                    }

                                    int min = itemsOffer.minFirstInputItemCount.get();
                                    int max = itemsOffer.maxFirstInputItemCount.get();
                                    firstInputItemMatched = count >= min && count <= max;
                                    printInfo("Count: " + count + ", Min: " + min + ", Max: " + max);
                                }
                            } else {
                                firstInputItemMatched = false;
                            }
                        }
                        printInfo("Item matched: " + firstInputItemMatched);

                        printInfo("");
                        printInfo("Second Input Item");
                        if (itemsOffer.checkSecondInputItem.get() && tradeOffer.getSecondBuyItem().isPresent()) {
                            Item item = itemsOffer.secondInputItem.get();
                            Item tradeItem = tradeOffer.getSecondBuyItem().get().item().value();
                            printInfo("Item: " + item + ", Trade Item: " + tradeItem);
                            if (item == tradeItem) {
                                if (itemsOffer.checkSecondInputItemCount.get()) {
                                    int count = tradeOffer.getDisplayedSecondBuyItem().getCount();
                                    int min = itemsOffer.minSecondInputItemCount.get();
                                    int max = itemsOffer.maxSecondInputItemCount.get();
                                    secondInputItemMatched = count >= min && count <= max;
                                    printInfo("Count: " + count + ", Min: " + min + ", Max: " + max);
                                }
                            } else {
                                secondInputItemMatched = false;
                            }
                        }
                        printInfo("Item matched: " + secondInputItemMatched);

                        printInfo("");
                        printInfo("Output Item");
                        if (itemsOffer.checkOutputItem.get()) {
                            Item item = itemsOffer.outputItem.get();
                            Item tradeItem = tradeOffer.getSellItem().getItem();
                            printInfo("Item: " + item + ", Trade Item: " + tradeItem);
                            if (item == tradeItem) {
                                if (itemsOffer.checkOutputItemCount.get()) {
                                    int count = tradeOffer.getSellItem().getCount();
                                    int min = itemsOffer.minOutputItemCount.get();
                                    int max = itemsOffer.maxOutputItemCount.get();
                                    outputItemMatched = count >= min && count <= max;
                                    printInfo("Count: " + count + ", Min: " + min + ", Max: " + max);
                                }
                            } else {
                                outputItemMatched = false;
                            }
                        }
                        printInfo("Item matched: " + outputItemMatched);

                        if (firstInputItemMatched && secondInputItemMatched && outputItemMatched && !tradeOffer.isDisabled()) {
                            offerMatched = true;
                            break;
                        }
                    } else if (offer instanceof IdOffer idOffer) {
                        if (idOffer.offerId.get() == tradeOffers.indexOf(tradeOffer) && !tradeOffer.isDisabled()) {
                            offerMatched = true;
                            break;
                        }
                    }
                }

                if (offerMatched) {
                    offerMatched = false;
                    successfulOffer = true;
                    printInfo("Offer matched");
                    mc.world.sendPacket(new SelectMerchantTradeC2SPacket(tradeOffers.indexOf(tradeOffer)));
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 2, 0, SlotActionType.QUICK_MOVE, mc.player);
                    if (oneOfferPerTick.get()) break;
                }
            }
            if (closeWhenDone.get() && successfulOffer) mc.currentScreen.close();
        }
    }

    @EventHandler
    public void onPacket(PacketEvent.Receive event) {
        if (event.packet instanceof SetTradeOffersS2CPacket packet) lastTrade = packet;
    }

    private void printInfo(String message) {
        if (printDebugInfo.get()) info(message);
    }
}