package zgoly.meteorist.utils.misc;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.meteordev.starscript.Script;
import org.meteordev.starscript.Starscript;
import org.meteordev.starscript.value.Value;
import org.meteordev.starscript.value.ValueMap;
import zgoly.meteorist.utils.ItemDataUtils;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/// Adds the 'item' variable to Meteor's Starscript for matching items by id, count and NBT.
public class Filterist {
    private static final ThreadLocal<ItemStack> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Map<ItemStack, Tag>> STANDARD_NBT_CACHE = ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<Map<ItemStack, Tag>> VERBOSE_NBT_CACHE = ThreadLocal.withInitial(IdentityHashMap::new);
    private static final Map<String, Pattern> REGEX_CACHE = new HashMap<>();
    private static final Map<String, Script> SCRIPT_CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Script> eldest) {
            return size() > 8;
        }
    };

    private static final ValueMap STANDARD_NBT_VIEW = nbtView(true);
    private static final ValueMap VERBOSE_NBT_VIEW = nbtView(false);

    private static final ValueMap ITEM = new ValueMap()
            .set("_toString", () -> {
                ItemStack stack = CURRENT.get();
                if (stack == null) return Value.string("null");
                String name = stack.isEmpty() ? "" : Names.get(stack.getItem());
                return Value.string(stack.getCount() <= 1 ? name : String.format("%s %dx", name, stack.getCount()));
            })
            .set("id", () -> {
                ItemStack stack = CURRENT.get();
                if (stack == null || stack.isEmpty()) return Value.string("");
                return Value.string(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            })
            .set("name", () -> {
                ItemStack stack = CURRENT.get();
                if (stack == null || stack.isEmpty()) return Value.string("");
                return Value.string(Names.get(stack.getItem()));
            })
            .set("count", () -> {
                ItemStack stack = CURRENT.get();
                return Value.number(stack == null ? 0 : stack.getCount());
            })
            .set("durability", () -> {
                ItemStack stack = CURRENT.get();
                if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) return Value.number(0);
                return Value.number(stack.getMaxDamage() - stack.getDamageValue());
            })
            .set("max_durability", () -> {
                ItemStack stack = CURRENT.get();
                return Value.number(stack == null || stack.isEmpty() ? 0 : stack.getMaxDamage());
            })
            .set("nbt", () -> Value.map(STANDARD_NBT_VIEW))
            .set("nbt_verbose", () -> Value.map(VERBOSE_NBT_VIEW))
            .set("matches", Filterist::matches);

    private static ValueMap nbtView(boolean standard) {
        return new ValueMap()
                .set("_toString", () -> Value.string(getNbt(CURRENT.get(), standard)))
                .set("path", (ss, argCount) -> lookupPath(ss, argCount, standard))
                .set("matches", (ss, argCount) -> matchesNbt(ss, argCount, standard));
    }

    public static void init() {
        MeteorStarscript.ss.set("item", () -> Value.map(ITEM));
    }

    public static String render(Script script, ItemStack stack) {
        if (script == null) return "";

        CURRENT.set(stack);
        try {
            String result = MeteorStarscript.run(script);
            return result != null ? result : "";
        } finally {
            CURRENT.remove();
        }
    }

    /// The script must render exactly "true" for the stack to count as a match.
    public static boolean evaluate(Script script, ItemStack stack) {
        return "true".equals(render(script, stack));
    }

    public static boolean evaluate(String expression, ItemStack stack) {
        if (expression == null || expression.trim().isEmpty()) return false;
        Script script = SCRIPT_CACHE.computeIfAbsent(expression, MeteorStarscript::compile);
        return evaluate(script, stack);
    }

    /// Call at the start of each matching pass.
    public static void clearNbtCache() {
        STANDARD_NBT_CACHE.get().clear();
        VERBOSE_NBT_CACHE.get().clear();
    }

    private static String getNbt(ItemStack stack, boolean standard) {
        Tag tag = getNbtTag(stack, standard);
        return tag != null ? tag.toString() : "";
    }

    private static Tag getNbtTag(ItemStack stack, boolean standard) {
        if (stack == null || stack.isEmpty()) return null;

        Map<ItemStack, Tag> cache = (standard ? STANDARD_NBT_CACHE : VERBOSE_NBT_CACHE).get();
        Tag cached = cache.get(stack);
        if (cached == null) {
            cached = standard
                    ? ItemDataUtils.getStandardNbt(stack).orElse(null)
                    : ItemDataUtils.getAdvancedNbt(stack).orElse(null);
            if (cached != null) cache.put(stack, cached);
        }
        return cached;
    }

    private static Value lookupPath(Starscript ss, int argCount, boolean standard) {
        if (argCount != 1) ss.error("item.nbt.path()/item.nbt_verbose.path() requires 1 argument, got %d.", argCount);
        String path = ss.popString("Argument to item.nbt.path()/item.nbt_verbose.path() needs to be a string, e.g. \"components.\"minecraft:food\".\"");

        ItemStack stack = CURRENT.get();
        if (stack == null) return Value.null_();
        Tag nbt = getNbtTag(stack, standard);
        if (nbt == null) return Value.null_();

        NbtPathArgument.NbtPath nbtPath;
        try {
            nbtPath = NbtPathArgument.NbtPath.of(path);
        } catch (CommandSyntaxException e) {
            ss.error("Invalid NBT path: %s", e.getMessage());
            return Value.null_();
        }

        if (nbtPath.countMatching(nbt) == 0) return Value.null_();

        List<Tag> found;
        try {
            found = nbtPath.get(nbt);
        } catch (CommandSyntaxException e) {
            return Value.null_();
        }
        if (found.isEmpty()) return Value.null_();

        Tag first = found.getFirst();
        return Value.map(new ValueMap()
                .set("_toString", () -> Value.string(renderTags(found)))
                .set("matches", (ss2, argCount2) -> matchesTags(ss2, argCount2, found))
                .set("number", () -> first instanceof NumericTag numeric ? Value.number(numeric.doubleValue()) : Value.null_())
                .set("string", () -> first instanceof StringTag stringTag ? Value.string(stringTag.asString().orElse(first.toString())) : Value.null_())
        );
    }

    private static String renderTags(List<Tag> tags) {
        return tags.size() == 1 ? tags.getFirst().toString() : tags.stream().map(Tag::toString).collect(Collectors.joining(", "));
    }

    private static Value matchesTags(Starscript ss, int argCount, List<Tag> tags) {
        if (argCount != 1) ss.error("item.nbt.path().matches() requires 1 argument, got %d.", argCount);
        Pattern pattern = compilePattern(ss, ss.popString("Argument to item.nbt.path().matches() needs to be a string."));
        if (pattern == null) return Value.bool(false);

        for (Tag tag : tags) {
            if (pattern.matcher(tag.toString()).find()) return Value.bool(true);
        }
        return Value.bool(false);
    }

    private static Value matches(Starscript ss, int argCount) {
        return matchesNbt(ss, argCount, true);
    }

    private static Value matchesNbt(Starscript ss, int argCount, boolean standard) {
        if (argCount != 1) ss.error("item.nbt.matches() requires 1 argument, got %d.", argCount);
        Pattern pattern = compilePattern(ss, ss.popString("Argument to item.nbt.matches() needs to be a string."));
        if (pattern == null) return Value.bool(false);

        return Value.bool(pattern.matcher(getNbt(CURRENT.get(), standard)).find());
    }

    private static Pattern compilePattern(Starscript ss, String regex) {
        Pattern pattern = REGEX_CACHE.get(regex);
        if (pattern != null) return pattern;

        try {
            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        } catch (PatternSyntaxException e) {
            ss.error("Invalid regex: %s", e.getMessage());
            return null;
        }

        REGEX_CACHE.put(regex, pattern);
        return pattern;
    }
}
