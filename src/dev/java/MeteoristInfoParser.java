import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/// Parses Meteorist's Java source code to generate a JSON file containing metadata
/// about modules, commands, and HUD preset groups.
///
/// Extracts data by analyzing constructor calls and builder patterns in classes
/// that extend Module, Command, or use HudElementInfo. Output includes names,
/// descriptions, settings, and presets with source locations.
///
/// **Note:** Relies on code structure and string literals;
/// may not resolve complex expressions. Subject to improvement.
public class MeteoristInfoParser {
    private static final JavaParser JAVA_PARSER = new JavaParser();
    private static final Map<String, String> CATEGORY_NAME_CACHE = new HashMap<>();
    private static final Map<File, CompilationUnit> FILE_TO_CU_CACHE = new HashMap<>();

    static class ExpressionValue {
        String value;
        boolean isCode;

        ExpressionValue(String value, boolean isCode) {
            this.value = value;
            this.isCode = isCode;
        }
    }

    static class ModuleInfo {
        String name;
        String description;
        String category;
        String className;
        String classPath;
        int lineNumber;
        Map<String, List<SettingInfo>> settings = new LinkedHashMap<>();
    }

    static class SettingInfo {
        String type;
        int lineNumber;
        Map<String, Object> properties = new LinkedHashMap<>();
    }

    static class CommandInfo {
        String name;
        String description;
        List<String> aliases = new ArrayList<>();
        String className;
        String classPath;
        int lineNumber;
    }

    static class PresetGroup {
        String name;
        String description;
        String className;
        String classPath;
        int lineNumber;
        List<PresetInfo> presets = new ArrayList<>();
    }

    static class PresetInfo {
        String title;
        String text;
        int lineNumber;
    }

    public static void main(String[] args) {
        String sourceDir = args.length > 0 ? args[0] : "src/main/java";
        String outputFile = args.length > 1 ? args[1] : "build/info/meteorist-info.json";

        try {
            Path start = Paths.get(sourceDir);
            List<File> javaFiles = findAndCacheJavaFiles(start);
            scanForCategories(javaFiles);

            List<ModuleInfo> allModules = new ArrayList<>();
            List<CommandInfo> allCommands = new ArrayList<>();
            List<PresetGroup> allPresetGroups = new ArrayList<>();

            for (File file : javaFiles) {
                CompilationUnit cu = getCompilationUnit(file);
                if (cu == null) continue;

                String className = cu.getPrimaryTypeName().orElse(file.getName().replace(".java", ""));
                String classPath = file.getPath();

                boolean isModule = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                        .anyMatch(cls -> extendsClass(cls, "Module"));
                boolean isCommand = !isModule && cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                        .anyMatch(cls -> extendsClass(cls, "Command"));

                if (isModule) {
                    ModuleInfo moduleInfo = extractModuleInfo(cu, className, classPath);
                    if (moduleInfo != null) {
                        System.out.println("Processed module: " + moduleInfo.name + " (" + className + ")");
                        allModules.add(moduleInfo);
                    }
                }
                if (isCommand) {
                    CommandInfo cmd = extractCommandInfo(cu, className, classPath);
                    if (cmd != null) {
                        System.out.println("Processed command: " + cmd.name + " (" + className + ")");
                        allCommands.add(cmd);
                    }
                }
                List<PresetGroup> presetGroups = extractPresetGroups(cu, className, classPath);
                for (PresetGroup group : presetGroups) {
                    System.out.println("Processed preset group: " + group.name + " (" + className + ")");
                }
                allPresetGroups.addAll(presetGroups);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("modules", allModules);
            result.put("commands", allCommands);
            result.put("presets", allPresetGroups);

            writeToJsonFile(result, outputFile);
            System.out.println("Processed " + allModules.size() + " modules, " +
                    allCommands.size() + " commands, and " + allPresetGroups.size() + " preset groups. Results in " + outputFile);
        } catch (Exception e) {
            System.err.println("Error processing files: " + e.getMessage());
        }
    }

    private static void scanForCategories(List<File> files) {
        for (File file : files) {
            try {
                CompilationUnit cu = getCompilationUnit(file);
                if (cu == null) continue;

                String packageName = cu.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse("");

                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                    String className = cls.getNameAsString();
                    String fullClassName = packageName.isEmpty() ? className : packageName + "." + className;

                    cls.getFields().forEach(field -> {
                        if (field.isPublic() && field.isStatic() && field.isFinal()) {
                            String fieldType = field.getCommonType().toString();
                            if (fieldType.endsWith("Category")) {
                                field.getVariables().forEach(var -> {
                                    String fieldName = var.getNameAsString();
                                    String fullFieldName = fullClassName + "." + fieldName;

                                    var.getInitializer().ifPresent(init -> {
                                        if (init instanceof ObjectCreationExpr objCreation) {
                                            String typeName = objCreation.getType().getNameAsString();
                                            if (typeName.equals("Category") || typeName.endsWith(".Category")) {
                                                if (!objCreation.getArguments().isEmpty()) {
                                                    Expression firstArg = objCreation.getArgument(0);
                                                    if (firstArg instanceof StringLiteralExpr strExpr) {
                                                        CATEGORY_NAME_CACHE.put(fullFieldName, strExpr.getValue());
                                                    }
                                                }
                                            }
                                        }
                                    });
                                });
                            }
                        }
                    });
                });
            } catch (Exception e) {
                System.err.println("Failed to scan categories in file: " + file.getName());
            }
        }
    }

    private static boolean extendsClass(ClassOrInterfaceDeclaration cls, String className) {
        return cls.getExtendedTypes().stream()
                .anyMatch(type -> className.equals(type.getNameAsString()));
    }

    private static List<File> findAndCacheJavaFiles(Path start) throws IOException {
        List<File> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(start)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .forEach(files::add);
        }

        for (File file : files) {
            try (FileInputStream in = new FileInputStream(file)) {
                CompilationUnit cu = JAVA_PARSER.parse(in).getResult().orElse(null);
                if (cu != null && cu.getPrimaryTypeName().isPresent()) {
                    cu.getPackageDeclaration().ifPresent(pkg -> FILE_TO_CU_CACHE.put(file, cu));
                }
            } catch (Exception e) {
                System.err.println("Failed to parse for cache: " + file.getName());
            }
        }

        return files;
    }

    private static CompilationUnit getCompilationUnit(File file) {
        return FILE_TO_CU_CACHE.computeIfAbsent(file, f -> {
            try (FileInputStream in = new FileInputStream(f)) {
                return JAVA_PARSER.parse(in).getResult().orElse(null);
            } catch (Exception e) {
                System.err.println("Failed to parse file: " + f.getName());
                return null;
            }
        });
    }

    private static ModuleInfo extractModuleInfo(CompilationUnit cu, String className, String classPath) {
        return cu.findAll(ConstructorDeclaration.class).stream()
                .flatMap(constructor -> constructor.getBody().findAll(ExplicitConstructorInvocationStmt.class).stream())
                .filter(call -> !call.isThis() && call.getArguments().size() >= 3)
                .findFirst()
                .map(call -> {
                    ExpressionValue categoryExpr = getExpressionValue(call.getArgument(0));
                    ExpressionValue nameExpr = getExpressionValue(call.getArgument(1));
                    ExpressionValue descExpr = getExpressionValue(call.getArgument(2));
                    if (nameExpr.isCode || descExpr.isCode) {
                        return null;
                    }

                    String resolvedCategory = resolveCategory(categoryExpr.value);

                    Map<String, String> settingGroupNameMap = findSettingGroupNames(cu);
                    Map<String, List<SettingInfo>> settings = extractSettingsByGroup(cu, settingGroupNameMap);

                    ModuleInfo module = new ModuleInfo();
                    module.category = resolvedCategory;
                    module.name = nameExpr.value;
                    module.description = descExpr.value;
                    module.className = className;
                    module.classPath = classPath;
                    module.lineNumber = call.getBegin().map(pos -> pos.line).orElse(-1);
                    module.settings = settings;
                    return module;
                })
                .orElse(null);
    }

    private static String resolveCategory(String exprStr) {
        if (CATEGORY_NAME_CACHE.containsKey(exprStr)) {
            return CATEGORY_NAME_CACHE.get(exprStr);
        }

        if (exprStr.contains(".")) {
            String[] parts = exprStr.split("\\.");
            String fieldName = parts[parts.length - 1];
            String className = parts[parts.length - 2];

            for (Map.Entry<String, String> entry : CATEGORY_NAME_CACHE.entrySet()) {
                String key = entry.getKey();
                if (key.endsWith("." + fieldName)) {
                    String[] keyParts = key.split("\\.");
                    if (keyParts.length >= 2 && keyParts[keyParts.length - 2].equals(className)) {
                        return entry.getValue();
                    }
                }
            }
        }

        return exprStr;
    }

    private static Map<String, String> findSettingGroupNames(CompilationUnit cu) {
        Map<String, String> map = new HashMap<>();
        cu.findAll(VariableDeclarator.class).stream()
                .filter(var -> var.getTypeAsString().endsWith("SettingGroup"))
                .forEach(var -> {
                    String varName = var.getNameAsString();
                    Expression init = var.getInitializer().orElse(null);
                    if (init == null) return;

                    if (init instanceof MethodCallExpr methodCall) {
                        String scope = methodCall.getScope().map(Expression::toString).orElse("");
                        if ("settings".equals(scope)) {
                            if ("getDefaultGroup".equals(methodCall.getNameAsString())) {
                                map.put(varName, "General");
                                return;
                            } else if ("createGroup".equals(methodCall.getNameAsString())) {
                                methodCall.getArguments().stream().findFirst().ifPresent(argExpr -> {
                                    ExpressionValue arg = getExpressionValue(argExpr);
                                    map.put(varName, arg.value);
                                });
                                return;
                            }
                        }
                    }
                    map.put(varName, "Unknown");
                });
        return map;
    }

    private static Map<String, List<SettingInfo>> extractSettingsByGroup(CompilationUnit cu, Map<String, String> groupNameMap) {
        Map<String, List<SettingInfo>> groupedSettings = new LinkedHashMap<>();
        groupedSettings.put("General", new ArrayList<>());

        cu.findAll(MethodCallExpr.class).stream()
                .filter(call -> "add".equals(call.getNameAsString()))
                .filter(call -> call.getScope().isPresent())
                .forEach(call -> {
                    Expression scope = call.getScope().get();
                    String groupName;
                    if (scope instanceof FieldAccessExpr) {
                        groupName = ((FieldAccessExpr) scope).getNameAsString();
                    } else if (scope instanceof NameExpr) {
                        groupName = ((NameExpr) scope).getNameAsString();
                    } else {
                        return;
                    }

                    String resolvedName = groupNameMap.getOrDefault(groupName, "Unknown");
                    if ("Unknown".equals(resolvedName)) return;
                    groupedSettings.putIfAbsent(resolvedName, new ArrayList<>());

                    if (call.getArguments().isEmpty()) return;
                    ObjectCreationExpr creation = findBuilderCreation(call.getArgument(0));
                    if (creation == null) return;

                    String settingType = extractSettingType(creation.getTypeAsString());
                    if (settingType == null) return;

                    Map<String, Object> properties = collectBuilderProperties(call.getArgument(0));
                    int lineNumber = call.getBegin().map(pos -> pos.line).orElse(-1);

                    SettingInfo setting = new SettingInfo();
                    setting.type = settingType;
                    setting.lineNumber = lineNumber;
                    setting.properties = properties;

                    groupedSettings.get(resolvedName).add(setting);
                });

        groupedSettings.entrySet().removeIf(e -> e.getValue().isEmpty());
        return groupedSettings;
    }

    private static ExpressionValue getExpressionValue(Expression expr) {
        if (expr instanceof StringLiteralExpr) {
            return new ExpressionValue(((StringLiteralExpr) expr).getValue(), false);
        }
        if (expr instanceof IntegerLiteralExpr) {
            return new ExpressionValue(((IntegerLiteralExpr) expr).getValue(), false);
        }
        if (expr instanceof BooleanLiteralExpr) {
            return new ExpressionValue(Boolean.toString(((BooleanLiteralExpr) expr).getValue()), false);
        }
        return new ExpressionValue(expr.toString(), true);
    }

    private static ObjectCreationExpr findBuilderCreation(Expression expr) {
        if (expr instanceof ObjectCreationExpr) {
            String type = ((ObjectCreationExpr) expr).getTypeAsString();
            if (type.contains("Setting") && type.contains(".Builder")) {
                return (ObjectCreationExpr) expr;
            }
        } else if (expr instanceof MethodCallExpr methodCall && methodCall.getScope().isPresent()) {
            return findBuilderCreation(methodCall.getScope().get());
        }
        return null;
    }

    private static String extractSettingType(String typeName) {
        int dotIndex = typeName.lastIndexOf('.');
        if (dotIndex > 0) {
            String outerClass = typeName.substring(0, dotIndex);
            if (outerClass.endsWith("Setting")) {
                return outerClass;
            }
        }
        return null;
    }

    private static Map<String, Object> collectBuilderProperties(Expression expr) {
        Map<String, Object> properties = new LinkedHashMap<>();
        if (expr instanceof MethodCallExpr methodCall) {
            if (!"build".equals(methodCall.getNameAsString())) {
                List<Object> args = new ArrayList<>();
                for (Expression arg : methodCall.getArguments()) {
                    ExpressionValue ev = getExpressionValue(arg);
                    args.add(ev.isCode ? "`" + ev.value + "`" : ev.value);
                }
                if (!args.isEmpty()) {
                    properties.put(methodCall.getNameAsString(), args.size() == 1 ? args.getFirst() : args);
                }
            }
            methodCall.getScope().ifPresent(scope -> properties.putAll(collectBuilderProperties(scope)));
        }
        return properties;
    }

    private static CommandInfo extractCommandInfo(CompilationUnit cu, String className, String classPath) {
        return cu.findAll(ConstructorDeclaration.class).stream()
                .flatMap(constructor -> constructor.getBody().findAll(ExplicitConstructorInvocationStmt.class).stream())
                .filter(call -> !call.isThis() && call.getArguments().size() >= 2)
                .findFirst()
                .map(call -> {
                    List<String> aliases = new ArrayList<>();
                    for (int i = 2; i < call.getArguments().size(); i++) {
                        ExpressionValue ev = getExpressionValue(call.getArgument(i));
                        if (!ev.isCode) {
                            aliases.add(ev.value);
                        }
                    }
                    ExpressionValue nameExpr = getExpressionValue(call.getArgument(0));
                    ExpressionValue descExpr = getExpressionValue(call.getArgument(1));
                    if (nameExpr.isCode || descExpr.isCode) return null;

                    CommandInfo cmd = new CommandInfo();
                    cmd.name = nameExpr.value;
                    cmd.description = descExpr.value;
                    cmd.aliases = aliases;
                    cmd.className = className;
                    cmd.classPath = classPath;
                    cmd.lineNumber = call.getBegin().map(pos -> pos.line).orElse(-1);
                    return cmd;
                })
                .orElse(null);
    }

    private static List<PresetGroup> extractPresetGroups(CompilationUnit cu, String className, String classPath) {
        List<PresetGroup> groups = new ArrayList<>();
        cu.findAll(VariableDeclarator.class).stream()
                .filter(var -> var.getTypeAsString().contains("HudElementInfo"))
                .filter(var -> var.getInitializer().isPresent())
                .filter(var -> var.getInitializer().get() instanceof ObjectCreationExpr)
                .forEach(var -> {
                    ObjectCreationExpr init = (ObjectCreationExpr) var.getInitializer().get();
                    if (init.getArguments().size() < 4) return;
                    ExpressionValue nameExpr = getExpressionValue(init.getArgument(1));
                    ExpressionValue descExpr = getExpressionValue(init.getArgument(2));
                    if (nameExpr.isCode || descExpr.isCode) return;

                    PresetGroup group = new PresetGroup();
                    group.name = nameExpr.value;
                    group.description = descExpr.value;
                    group.className = className;
                    group.classPath = classPath;
                    group.lineNumber = init.getBegin().map(pos -> pos.line).orElse(-1);

                    cu.findAll(MethodCallExpr.class)
                            .stream()
                            .filter(call -> "addPreset".equals(call.getNameAsString()))
                            .filter(call -> call.getArguments().size() >= 2)
                            .forEach(call -> {
                                ExpressionValue title = getExpressionValue(call.getArgument(0));
                                ExpressionValue formula = getExpressionValue(call.getArgument(1));
                                if (title.isCode || formula.isCode) return;
                                PresetInfo preset = new PresetInfo();
                                preset.title = title.value;
                                preset.text = formula.value;
                                preset.lineNumber = call.getBegin().map(pos -> pos.line).orElse(-1);
                                group.presets.add(preset);
                            });

                    if (!group.presets.isEmpty()) {
                        groups.add(group);
                    }
                });
        return groups;
    }

    private static void writeToJsonFile(Object data, String filename) {
        try {
            Path path = Paths.get(filename);
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (FileWriter writer = new FileWriter(path.toFile())) {
                new GsonBuilder().setPrettyPrinting().create().toJson(data, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to write JSON file: " + e.getMessage());
        }
    }
}