package zgoly.meteorist.devmodules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import zgoly.meteorist.Meteorist;
import zgoly.meteorist.utils.ItemDataUtils;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ItemDataInspector extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> copyToClipboard = sgGeneral.add(new BoolSetting.Builder()
            .name("copy-to-clipboard")
            .description("Copies advanced NBT data of held item to clipboard.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> inspectOnTick = sgGeneral.add(new BoolSetting.Builder()
            .name("inspect-on-tick")
            .description("Inspect item on every tick when held item changes.")
            .defaultValue(false)
            .build()
    );

    private ItemStack lastItem = ItemStack.EMPTY;

    public ItemDataInspector() {
        super(Meteorist.DEV_CATEGORY, "item-data-inspector", "Inspects Standard vs Advanced NBT/Component data of the held item.");
    }

    @Override
    public void onActivate() {
        lastItem = ItemStack.EMPTY;
        inspectHeldItem();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!inspectOnTick.get() || mc.player == null) return;

        ItemStack current = mc.player.getMainHandItem();
        if (!ItemStack.matches(current, lastItem)) {
            lastItem = current.copy();
            inspectHeldItem();
        }
    }

    private void inspectHeldItem() {
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) {
            info("Main hand is empty.");
            return;
        }

        info("=========================================");
        info("Inspecting Item: (highlight)%s(default)", stack.getItem().toString());
        info("=========================================");

        String standardNbtStr = ItemDataUtils.getStandardNbt(stack)
                .map(tag -> tag instanceof CompoundTag compound ? NbtUtils.toPrettyComponent(compound).getString() : tag.toString())
                .orElse("{No Standard NBT Data}");
        info(Component.literal("--- 1. Standard NBT ---"));
        info(Component.literal(standardNbtStr));

        String advancedNbtStr = ItemDataUtils.getAdvancedNbt(stack).map(Tag::toString).orElse("");
        info(Component.literal("--- 2. Advanced Full NBT ---"));
        info(Component.literal(advancedNbtStr));

        info(Component.literal("--- 3. Components Direct Inspection ---"));
        for (TypedDataComponent<?> component : stack.getComponents()) {
            info("  Component: (highlight)%s(default) = %s", component.type().toString(), component.value().toString());
        }

        info("=========================================");

        if (copyToClipboard.get()) {
            String clipboardData = "--- Standard NBT ---\n" + standardNbtStr + "\n\n--- Advanced NBT ---\n" + advancedNbtStr;
            mc.keyboardHandler.setClipboard(clipboardData);
            info("Advanced NBT copied to clipboard!");
        }
    }
}
