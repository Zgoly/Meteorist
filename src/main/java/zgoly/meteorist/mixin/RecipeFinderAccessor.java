package zgoly.meteorist.mixin;

import net.minecraft.item.Item;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(RecipeFinder.class)
public interface RecipeFinderAccessor {
    @Invoker("isCraftable")
    boolean invokeIsCraftable(
            List<? extends RecipeMatcher.RawIngredient<RegistryEntry<Item>>> rawIngredients,
            int quantity,
            @Nullable RecipeMatcher.ItemCallback<RegistryEntry<Item>> itemCallback
    );
}
