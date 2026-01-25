package zgoly.meteorist.modules.autotrade;

import com.mojang.serialization.DataResult;
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
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.util.Tuple;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.gui.screens.OfferScreen;
import zgoly.meteorist.modules.autotrade.offers.BaseOffer;
import zgoly.meteorist.modules.autotrade.offers.IdOffer;
import zgoly.meteorist.modules.autotrade.offers.ItemsOffer;
import zgoly.meteorist.utils.config.MeteoristConfigManager;
import zgoly.meteorist.utils.misc.DebugLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static meteordevelopment.meteorclient.gui.renderer.GuiRenderer.COPY;
import static meteordevelopment.meteorclient.gui.renderer.GuiRenderer.EDIT;
import static zgoly.meteorist.Meteorist.ARROW_DOWN;
import static zgoly.meteorist.Meteorist.ARROW_UP;

public class AutoTrade extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

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

    private final OfferFactory factory = new OfferFactory();
    private final List<BaseOffer> offers = new ArrayList<>();
    private final DebugLogger debugLogger;
    private ClientboundMerchantOffersPacket lastTrade = null;

    public AutoTrade() {
        super(Meteorist.CATEGORY, "auto-trade", "Automatically trades items with villagers (idea by Hiradpi).");

        debugLogger = new DebugLogger(this, settings);
    }

    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();

        ListTag list = new ListTag();
        for (BaseOffer offer : offers) {
            CompoundTag mTag = new CompoundTag();
            mTag.putString("type", offer.getTypeName());
            mTag.put("offer", offer.toTag());

            list.add(mTag);
        }
        tag.put("offers", list);
        return tag;
    }

    public Module fromTag(CompoundTag tag) {
        super.fromTag(tag);

        offers.clear();
        ListTag list = tag.getListOrEmpty("offers");

        for (Tag tagII : list) {
            CompoundTag tagI = (CompoundTag) tagII;

            String type = tagI.getStringOr("type", "");
            BaseOffer offer = factory.createOffer(type);

            if (offer != null) {
                CompoundTag offerTag = (CompoundTag) tagI.get("offer");
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
            table.add(theme.label(offer.getTypeName())).expandX();

            WHorizontalList container = table.add(theme.horizontalList()).expandX().widget();
            if (offer instanceof ItemsOffer itemsOffer) {
                int pad = 16;

                if (itemsOffer.checkFirstInputItem.get() && itemsOffer.firstInputFilterMode.get() == ItemsOffer.FilterMode.Item) {
                    if (itemsOffer.checkFirstInputItemCount.get()) {
                        int min = itemsOffer.minFirstInputItemCount.get();
                        int max = itemsOffer.maxFirstInputItemCount.get();
                        String range = min == max ? "x" + min : "x" + min + " - x" + max;
                        container.add(theme.itemWithLabel(itemsOffer.firstInputItem.get().getDefaultInstance(), range)).padRight(pad);
                    } else {
                        container.add(theme.item(itemsOffer.firstInputItem.get().getDefaultInstance())).padRight(pad);
                    }
                }

                if (itemsOffer.checkFirstInputItem.get() && itemsOffer.checkSecondInputItem.get()) {
                    container.add(theme.label(" + ")).padRight(pad);
                }

                if (itemsOffer.checkSecondInputItem.get() && itemsOffer.secondInputFilterMode.get() == ItemsOffer.FilterMode.Item) {
                    if (itemsOffer.checkSecondInputItemCount.get()) {
                        int min = itemsOffer.minSecondInputItemCount.get();
                        int max = itemsOffer.maxSecondInputItemCount.get();
                        String range = min == max ? "x" + min : "x" + min + " - x" + max;
                        container.add(theme.itemWithLabel(itemsOffer.secondInputItem.get().getDefaultInstance(), range)).padRight(pad);
                    } else {
                        container.add(theme.item(itemsOffer.secondInputItem.get().getDefaultInstance())).padRight(pad);
                    }
                }

                container.add(theme.label(">")).padRight(pad);

                if (itemsOffer.checkOutputItem.get() && itemsOffer.outputFilterMode.get() == ItemsOffer.FilterMode.Item) {
                    if (itemsOffer.checkOutputItemCount.get()) {
                        int min = itemsOffer.minOutputItemCount.get();
                        int max = itemsOffer.maxOutputItemCount.get();
                        String range = min == max ? "x" + min : "x" + min + " - x" + max;
                        container.add(theme.itemWithLabel(itemsOffer.outputItem.get().getDefaultInstance(), range));
                    } else {
                        container.add(theme.item(itemsOffer.outputItem.get().getDefaultInstance()));
                    }
                }
            } else if (offer instanceof IdOffer idOffer) {
                WIntEdit edit = container.add(theme.intEdit(idOffer.offerId.get(), 0, Integer.MAX_VALUE, true)).expandX().widget();
                edit.tooltip = idOffer.offerId.description;
                edit.actionOnRelease = () -> idOffer.offerId.set(edit.get());
            }

            WButton edit = table.add(theme.button(EDIT)).widget();
            edit.tooltip = "Edit the offer.";
            edit.action = () -> mc.setScreen(new OfferScreen(theme, offer));

            if (offers.size() > 1) {
                WContainer moveContainer = table.add(theme.horizontalList()).expandX().widget();
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

        if (!offers.isEmpty()) list.add(theme.horizontalSeparator()).expandX();

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

    private boolean matchesNbtFilters(ItemStack stack, List<Tuple<String, String>> filters, boolean matchAll) {
        if (filters.isEmpty()) return true;
        if (stack.isEmpty()) return false;

        DataResult<Tag> result = ItemStack.CODEC.encodeStart(mc.player.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack);
        if (result.result().isEmpty()) return false;

        Tag nbt = result.result().get();
        boolean anyMatch = false;
        boolean allMatch = true;

        for (Tuple<String, String> pair : filters) {
            try {
                String path = pair.getA();
                String regex = pair.getB();
                Pattern pattern = Pattern.compile(regex);
                List<Tag> found = NbtPathArgument.NbtPath.of(path).get(nbt);
                boolean matched = false;

                debugLogger.info("Evaluating NBT filter: path=(highlight)%s(default), regex=(highlight)%s", path, regex);
                debugLogger.info("Found (highlight)%d(default) elements", found.size());

                for (Tag tag : found) {
                    String tagStr = tag.toString();
                    debugLogger.info("Checking element: (highlight)%s", tagStr);
                    if (pattern.matcher(tagStr).find()) {
                        debugLogger.info("Match succeeded");
                        matched = true;
                        break;
                    } else {
                        debugLogger.info("Match failed");
                    }
                }

                if (matched) {
                    anyMatch = true;
                } else {
                    allMatch = false;
                }
            } catch (Exception e) {
                debugLogger.error("Exception during NBT filter evaluation: (highlight)%s", e.getMessage());
                allMatch = false;
            }
        }

        boolean resultBool = matchAll ? allMatch : anyMatch;
        debugLogger.info("NBT filter result: match mode=(highlight)%s(default), anyMatch=(highlight)%s(default), allMatch=(highlight)%s(default) -> returning (highlight)%s",
                matchAll ? "All" : "Any", anyMatch, allMatch, resultBool);

        return resultBool;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        boolean successfulOffer = false;
        if (lastTrade != null && mc.screen instanceof MerchantScreen) {
            MerchantOffers tradeOffers = lastTrade.getOffers();
            boolean offerMatched = false;
            for (MerchantOffer tradeOffer : tradeOffers) {
                int tradeIndex = tradeOffers.indexOf(tradeOffer);
                for (BaseOffer offer : offers) {
                    if (!offer.enabled.get()) continue;
                    if (offer instanceof ItemsOffer itemsOffer) {
                        boolean firstInputItemMatched = true;
                        boolean secondInputItemMatched = true;
                        boolean outputItemMatched = true;

                        if (itemsOffer.checkFirstInputItem.get()) {
                            if (itemsOffer.firstInputFilterMode.get() == ItemsOffer.FilterMode.Item) {
                                Item item = itemsOffer.firstInputItem.get();
                                Item tradeItem = tradeOffer.getItemCostA().item().value();
                                if (item == tradeItem) {
                                    if (itemsOffer.checkFirstInputItemCount.get()) {
                                        int count = itemsOffer.useFinalCount.get() ? tradeOffer.getCostA().getCount() : tradeOffer.getBaseCostA().getCount();
                                        int min = itemsOffer.minFirstInputItemCount.get();
                                        int max = itemsOffer.maxFirstInputItemCount.get();
                                        firstInputItemMatched = count >= min && count <= max;
                                        if (!firstInputItemMatched) {
                                            debugLogger.info("First input count mismatch: got (highlight)%d(default), expected [%d, %d]", count, min, max);
                                        }
                                    }
                                } else {
                                    firstInputItemMatched = false;
                                    debugLogger.info("First input item mismatch: expected (highlight)%s(default), got (highlight)%s", item, tradeItem);
                                }
                            } else {
                                firstInputItemMatched = matchesNbtFilters(
                                        tradeOffer.getCostA(),
                                        itemsOffer.firstInputNbtFilter.get(),
                                        itemsOffer.firstInputMatchMode.get() == ItemsOffer.MatchMode.All
                                );
                                if (!firstInputItemMatched) {
                                    debugLogger.info("First input NBT filter failed");
                                }
                            }
                        }

                        if (itemsOffer.checkSecondInputItem.get()) {
                            if (tradeOffer.getItemCostB().isPresent()) {
                                if (itemsOffer.secondInputFilterMode.get() == ItemsOffer.FilterMode.Item) {
                                    Item item = itemsOffer.secondInputItem.get();
                                    Item tradeItem = tradeOffer.getItemCostB().get().item().value();
                                    if (item == tradeItem) {
                                        if (itemsOffer.checkSecondInputItemCount.get()) {
                                            int count = tradeOffer.getCostB().getCount();
                                            int min = itemsOffer.minSecondInputItemCount.get();
                                            int max = itemsOffer.maxSecondInputItemCount.get();
                                            secondInputItemMatched = count >= min && count <= max;
                                            if (!secondInputItemMatched) {
                                                debugLogger.info("Second input count mismatch: got (highlight)%d(default), expected [%d, %d]", count, min, max);
                                            }
                                        }
                                    } else {
                                        secondInputItemMatched = false;
                                        debugLogger.info("Second input item mismatch: expected (highlight)%s(default), got (highlight)%s", item, tradeItem);
                                    }
                                } else {
                                    secondInputItemMatched = matchesNbtFilters(
                                            tradeOffer.getCostB(),
                                            itemsOffer.secondInputNbtFilter.get(),
                                            itemsOffer.secondInputMatchMode.get() == ItemsOffer.MatchMode.All
                                    );
                                    if (!secondInputItemMatched) {
                                        debugLogger.info("Second input NBT filter failed");
                                    }
                                }
                            } else {
                                secondInputItemMatched = false;
                                debugLogger.info("Second input expected but trade has no second cost");
                            }
                        }

                        if (itemsOffer.checkOutputItem.get()) {
                            if (itemsOffer.outputFilterMode.get() == ItemsOffer.FilterMode.Item) {
                                Item item = itemsOffer.outputItem.get();
                                Item tradeItem = tradeOffer.getResult().getItem();
                                if (item == tradeItem) {
                                    if (itemsOffer.checkOutputItemCount.get()) {
                                        int count = tradeOffer.getResult().getCount();
                                        int min = itemsOffer.minOutputItemCount.get();
                                        int max = itemsOffer.maxOutputItemCount.get();
                                        outputItemMatched = count >= min && count <= max;
                                        if (!outputItemMatched) {
                                            debugLogger.info("Output count mismatch: got (highlight)%d(default), expected [%d, %d]", count, min, max);
                                        }
                                    }
                                } else {
                                    outputItemMatched = false;
                                    debugLogger.info("Output item mismatch: expected (highlight)%s(default), got (highlight)%s", item, tradeItem);
                                }
                            } else {
                                outputItemMatched = matchesNbtFilters(
                                        tradeOffer.getResult(),
                                        itemsOffer.outputNbtFilter.get(),
                                        itemsOffer.outputMatchMode.get() == ItemsOffer.MatchMode.All
                                );
                                if (!outputItemMatched) {
                                    debugLogger.info("Output NBT filter failed");
                                }
                            }
                        }

                        if (firstInputItemMatched && secondInputItemMatched && outputItemMatched && !tradeOffer.isOutOfStock()) {
                            debugLogger.info("Matched trade offer (highlight)#%d", tradeIndex);
                            offerMatched = true;
                            break;
                        }
                    } else if (offer instanceof IdOffer idOffer) {
                        if (idOffer.offerId.get() == tradeIndex && !tradeOffer.isOutOfStock()) {
                            debugLogger.info("Matched trade offer (highlight)#%d(default) by ID", tradeIndex);
                            offerMatched = true;
                            break;
                        }
                    }
                }

                if (offerMatched) {
                    successfulOffer = true;
                    mc.level.sendPacketToServer(new ServerboundSelectTradePacket(tradeIndex));
                    mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 2, 0, ClickType.QUICK_MOVE, mc.player);
                    if (oneOfferPerTick.get()) break;
                }
            }
            if (closeWhenDone.get() && successfulOffer) mc.screen.onClose();
        }
    }

    @EventHandler
    public void onPacket(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundMerchantOffersPacket packet) {
            lastTrade = packet;
        }
    }
}