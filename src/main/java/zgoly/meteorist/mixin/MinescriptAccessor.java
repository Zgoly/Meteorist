package zgoly.meteorist.mixin;

import com.google.common.collect.ImmutableList;
import net.minescript.common.Minescript;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minescript.class)
public interface MinescriptAccessor {
    @Accessor("BUILTIN_COMMANDS")
    static ImmutableList<String> getBuiltinCommands() {
        throw new AssertionError();
    }

    @Invoker("runMinescriptCommand")
    static void runMinescriptCommand(String cmd) {
        throw new AssertionError();
    }
}