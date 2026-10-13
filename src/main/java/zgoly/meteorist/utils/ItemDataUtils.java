package zgoly.meteorist.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ItemDataUtils {
    // I have no idea what going on here, but it works
    public static final Codec<ItemStack> VERBOSE_CODEC = RecordCodecBuilder.<ItemStack>mapCodec(instance -> instance.group(
            BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id").forGetter(stack -> BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem())),
            ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(ItemStack::getCount),
            DataComponentMap.CODEC.fieldOf("components").forGetter(ItemStack::immutableComponents)
    ).apply(instance, (holder, count, map) -> {
        ItemStack stack = new ItemStack(holder, count);
        stack.applyComponents(map);
        return stack;
    })).codec();

    public static Optional<Tag> getStandardNbt(ItemStack stack) {
        return encode(stack, ItemStack.CODEC);
    }

    public static Optional<Tag> getAdvancedNbt(ItemStack stack) {
        return encode(stack, VERBOSE_CODEC);
    }

    private static Optional<Tag> encode(ItemStack stack, Codec<ItemStack> codec) {
        try {
            var player = mc.player;
            if (player == null) return Optional.empty();
            var registryAccess = player.registryAccess();
            var result = codec.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), stack);
            return result.result();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
