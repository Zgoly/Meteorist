package zgoly.meteorist.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.meteordev.starscript.Script;
import zgoly.meteorist.utils.ItemDataUtils;
import zgoly.meteorist.utils.misc.Filterist;

import java.util.ArrayList;
import java.util.List;

public class FilteristCommand extends Command {
    public FilteristCommand() {
        super("filterist", "Tests a Filterist Starscript expression ({...}) against your held item and all inventory items.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("expression", StringArgumentType.greedyString()).executes(this::runTest));
    }

    private int runTest(CommandContext<ClientSuggestionProvider> context) {
        if (mc.player == null) return SINGLE_SUCCESS;

        String source = StringArgumentType.getString(context, "expression");
        Script script = MeteorStarscript.compile(source);
        if (script == null) return SINGLE_SUCCESS;

        Filterist.clearNbtCache();
        info("Expression: (highlight)%s", source);

        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) {
            info("Main hand is empty.");
        } else {
            info("Held item: (highlight)%s(default) id=(highlight)%s", itemInfo(held), BuiltInRegistries.ITEM.getKey(held.getItem()));
            info("  nbt: (highlight)%s", truncate(getNbt(held)));
            String result = Filterist.render(script, held);
            info("  result: (highlight)%s", result.isEmpty() ? "(empty)" : result);
            if ("true".equals(result)) info("-> (highlight)MATCHED(default)");
            else info("-> Missed");
        }

        List<MatchedSlot> matched = new ArrayList<>();
        Inventory inventory = mc.player.getInventory();
        NonNullList<ItemStack> items = inventory.getNonEquipmentItems();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            if (Filterist.evaluate(script, stack)) matched.add(new MatchedSlot(slotName(i), stack));
        }

        if (matched.isEmpty()) {
            info("No items in the inventory match the expression.");
        } else {
            info("(highlight)%d(default) item(s) in inventory match:", matched.size());
            for (MatchedSlot slot : matched) {
                info("  (highlight)%s(default) - %s", slot.slot, itemInfo(slot.stack));
            }
        }

        return SINGLE_SUCCESS;
    }

    private String itemInfo(ItemStack stack) {
        String name = Names.get(stack.getItem());
        return stack.getCount() <= 1 ? name : String.format("%s x%d", name, stack.getCount());
    }

    private String slotName(int index) {
        return index < 9 ? "Hotbar " + (index + 1) : "Main " + (index - 8);
    }

    private String getNbt(ItemStack stack) {
        return ItemDataUtils.getStandardNbt(stack).map(Tag::toString).orElse("(no NBT)");
    }

    private String truncate(String s) {
        return s.length() <= 256 ? s : s.substring(0, 256) + "...(" + (s.length() - 256) + " more)";
    }

    private record MatchedSlot(String slot, ItemStack stack) {
    }
}
