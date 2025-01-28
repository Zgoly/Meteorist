package zgoly.meteorist.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.text.Text;
import zgoly.meteorist.modules.instructions.Instructions;
import zgoly.meteorist.utils.config.MeteoristConfigManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InstructionArgumentType implements ArgumentType<String> {
    private static final DynamicCommandExceptionType UNKNOWN_INSTRUCTION_EXCEPTION = new DynamicCommandExceptionType(name -> Text.of("Instruction \"" + name + "\" is not found"));

    public static InstructionArgumentType instruction() {
        return new InstructionArgumentType();
    }

    private List<String> listInstructionNames() {
        List<String> instructionNames = new ArrayList<>();
        File folderPath = MeteoristConfigManager.getFolderPath(new Instructions());
        File[] files = folderPath.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String name = file.getName();
                    instructionNames.add(name.substring(0, name.lastIndexOf(".")));
                }
            }
        }

        return instructionNames;
    }

    private File findInstructionFileByName(String name) {
        File folderPath = MeteoristConfigManager.getFolderPath(new Instructions());
        File[] files = folderPath.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith(name)) {
                    return file;
                }
            }
        }

        return null;
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String result = reader.readQuotedString();
        if (findInstructionFileByName(result) == null) {
            throw UNKNOWN_INSTRUCTION_EXCEPTION.create(result);
        }
        return result;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (String key : listInstructionNames()) {
            builder.suggest("\"" + key + "\"");
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return listInstructionNames();
    }

    public File getInstructionFile(String name) throws CommandSyntaxException {
        File file = findInstructionFileByName(name);
        if (file == null) {
            throw UNKNOWN_INSTRUCTION_EXCEPTION.create(name);
        }
        return file;
    }
}
