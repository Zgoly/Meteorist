import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
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
/// about modules, commands, and HUD elements.
///
/// Extracts data by analyzing constructor calls and builder patterns in classes
/// that extend Module, Command, or use HudElementInfo. Output includes names,
/// descriptions, settings, and presets with source locations.
///
/// Builder arguments are statically evaluated where possible (literals, negated
/// numbers, string concatenation, `static final` constants). Anything left over
/// stays source text in backticks, marking it as unevaluated.
public class MeteoristInfoParser {
    private static final JavaParser JAVA_PARSER = new JavaParser();
    private static final Map<String, String> CATEGORY_NAME_CACHE = new HashMap<>();
    private static final Map<File, CompilationUnit> FILE_TO_CU_CACHE = new HashMap<>();
    private static final Map<String, ExpressionValue> CONSTANT_CACHE = new HashMap<>();
    private static final Set<String> ENUM_TYPES = new HashSet<>();
    private static final int MAX_FOLD_DEPTH = 8;

    static void main(String[] args) {
        String sourceDir = args.length > 0 ? args[0] : "src/main/java";
        String outputFile = args.length > 1 ? args[1] : "build/info/meteorist-info.json";

        try {
            Path start = Paths.get(sourceDir);
            List<File> javaFiles = findAndCacheJavaFiles(start);
            indexTypes(javaFiles);

            List<ModuleInfo> allModules = new ArrayList<>();
            List<CommandInfo> allCommands = new ArrayList<>();
            List<HudElementInfoWrapper> allHudElements = new ArrayList<>();
            List<String> unextracted = new ArrayList<>();
            Set<String> registeredModules = new TreeSet<>();
            Set<String> registeredCommands = new TreeSet<>();
            Set<String> registeredHudElements = new TreeSet<>();

            for (File file : javaFiles) {
                CompilationUnit cu = getCompilationUnit(file);
                if (cu == null) continue;

                String className = cu.getPrimaryTypeName().orElse(file.getName().replace(".java", ""));
                String classPath = file.getPath().replace('\\', '/');

                boolean isModule = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                        .anyMatch(cls -> extendsClass(cls, "Module"));
                boolean isCommand = !isModule && cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                        .anyMatch(cls -> extendsClass(cls, "Command"));

                collectRegistrations(cu, registeredModules, registeredCommands, registeredHudElements);
                if (isModule) {
                    ModuleInfo moduleInfo = extractModuleInfo(cu, className, classPath);
                    if (moduleInfo != null) {
                        System.out.println("Processed module: " + moduleInfo.name + " (" + className + ")");
                        allModules.add(moduleInfo);
                    } else {
                        unextracted.add(className);
                    }
                }
                if (isCommand) {
                    CommandInfo cmd = extractCommandInfo(cu, className, classPath);
                    if (cmd != null) {
                        System.out.println("Processed command: " + cmd.name + " (" + className + ")");
                        allCommands.add(cmd);
                    } else {
                        unextracted.add(className);
                    }
                }
                List<HudElementInfoWrapper> hudElements = extractHudElements(cu, className, classPath);
                for (HudElementInfoWrapper element : hudElements) {
                    System.out.println("Processed HUD element: " + element.name + " (" + className + ")");
                }
                allHudElements.addAll(hudElements);
            }

            List<String> mismatches = crossCheckRegistrations(allModules, allCommands, allHudElements, unextracted,
                    registeredModules, registeredCommands, registeredHudElements);
            if (!mismatches.isEmpty()) {
                System.err.println("Registration cross-check failed:");
                mismatches.forEach(problem -> System.err.println("  - " + problem));
                System.err.println("Every Modules.get().add(...) / Commands.add(...) entry must resolve to a");
                System.err.println("class whose super(...) passes the name and description as literals.");
                System.exit(1);
            }

            sortResults(allModules, allCommands, allHudElements);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("modules", allModules);
            result.put("commands", allCommands);
            result.put("hud", allHudElements);

            writeToJsonFile(result, outputFile);
            System.out.println("Processed " + allModules.size() + " modules, " +
                    allCommands.size() + " commands, and " + allHudElements.size() + " HUD elements. Results in " + outputFile);
        } catch (Exception e) {
            System.err.println("Error processing files: " + e);
            System.exit(1);
        }
    }

    /// Sorts the extracted metadata.
    private static void sortResults(List<ModuleInfo> modules, List<CommandInfo> commands,
                                    List<HudElementInfoWrapper> hudElements) {
        modules.sort(Comparator
                .comparing((ModuleInfo module) -> isDevCategory(module.category) ? 1 : 0)
                .thenComparing(module -> module.name, String.CASE_INSENSITIVE_ORDER));
        commands.sort(Comparator.comparing((CommandInfo command) -> command.name, String.CASE_INSENSITIVE_ORDER));
        hudElements.sort(Comparator.comparing((HudElementInfoWrapper element) -> element.name, String.CASE_INSENSITIVE_ORDER));
    }

    /// Detection of a "development" category by convention.
    private static boolean isDevCategory(String categoryName) {
        if (categoryName == null) return false;
        if (hasDevToken(categoryName)) return true;
        return CATEGORY_NAME_CACHE.entrySet().stream()
                .anyMatch(entry -> entry.getValue().equals(categoryName) && hasDevToken(entry.getKey()));
    }

    private static boolean hasDevToken(String identifier) {
        String[] tokens = identifier.split("[^A-Za-z0-9]+|(?<=[a-z0-9])(?=[A-Z])");
        for (String token : tokens) {
            if (token.equalsIgnoreCase("dev")) return true;
        }
        return false;
    }

    /// Fails the run if the parsed metadata does not match what onInitialize
    /// registers, since a module that cannot be extracted would silently vanish.
    private static List<String> crossCheckRegistrations(List<ModuleInfo> modules, List<CommandInfo> commands,
                                                        List<HudElementInfoWrapper> hudElements, List<String> unextracted,
                                                        Set<String> registeredModules, Set<String> registeredCommands,
                                                        Set<String> registeredHudElements) {
        Set<String> foundModules = new TreeSet<>();
        Set<String> foundCommands = new TreeSet<>();
        Set<String> foundHudElements = new TreeSet<>();
        modules.forEach(module -> foundModules.add(module.className));
        commands.forEach(command -> foundCommands.add(command.className));
        hudElements.forEach(element -> foundHudElements.add(element.className));

        List<String> problems = new ArrayList<>();
        unextracted.forEach(name -> problems.add(name + " extends Module/Command but its metadata was not extracted"));
        registeredModules.stream().filter(name -> !foundModules.contains(name))
                .forEach(name -> problems.add("registered module " + name + " is missing from the generated metadata"));
        registeredCommands.stream().filter(name -> !foundCommands.contains(name))
                .forEach(name -> problems.add("registered command " + name + " is missing from the generated metadata"));
        registeredHudElements.stream().filter(name -> !foundHudElements.contains(name))
                .forEach(name -> problems.add("registered HUD element " + name + " is missing from the generated metadata"));
        foundModules.stream().filter(name -> !registeredModules.contains(name))
                .forEach(name -> problems.add("module " + name + " extends Module but is never registered via Modules.get().add(...)"));
        foundCommands.stream().filter(name -> !registeredCommands.contains(name))
                .forEach(name -> problems.add("command " + name + " extends Command but is never registered via Commands.add(...)"));
        foundHudElements.stream().filter(name -> !registeredHudElements.contains(name))
                .forEach(name -> problems.add("HUD element " + name + " is never registered via Hud.get().register(...)"));
        return problems;
    }

    private static void collectRegistrations(CompilationUnit cu, Set<String> modules, Set<String> commands,
                                             Set<String> hudElements) {
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            if (call.getArguments().isEmpty()) continue;
            String scope = call.getScope().map(Object::toString).orElse("");
            if ("register".equals(call.getNameAsString()) && scope.equals("Hud.get()")) {
                if (call.getArgument(0) instanceof FieldAccessExpr field && field.getScope() instanceof NameExpr cls) {
                    hudElements.add(cls.getNameAsString());
                }
                continue;
            }
            if (!"add".equals(call.getNameAsString())) continue;
            ObjectCreationExpr creation = call.getArgument(0) instanceof ObjectCreationExpr object ? object : null;
            if (creation == null) continue;
            if (scope.equals("Commands")) commands.add(creation.getType().getNameAsString());
            else if (scope.equals("Modules.get()")) modules.add(creation.getType().getNameAsString());
        }
    }

    private static void indexTypes(List<File> files) {
        for (File file : files) {
            try {
                CompilationUnit cu = getCompilationUnit(file);
                if (cu == null) continue;

                String packageName = cu.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse("");

                for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                    String fullClassName = qualify(packageName, cls.getNameAsString());
                    for (FieldDeclaration field : cls.getFields()) {
                        if (!field.isPublic() || !field.isStatic() || !field.isFinal()) continue;
                        for (VariableDeclarator var : field.getVariables()) {
                            if (field.getCommonType().toString().endsWith("Category")
                                    && var.getInitializer().orElse(null) instanceof ObjectCreationExpr creation
                                    && !creation.getArguments().isEmpty()
                                    && creation.getArgument(0) instanceof StringLiteralExpr literal) {
                                CATEGORY_NAME_CACHE.put(fullClassName + "." + var.getNameAsString(), literal.getValue());
                            }
                            var.getInitializer().ifPresent(init -> {
                                ExpressionValue value = fold(init, 0);
                                if (!value.isCode) {
                                    CONSTANT_CACHE.put(fullClassName + "." + var.getNameAsString(), value);
                                    CONSTANT_CACHE.put(cls.getNameAsString() + "." + var.getNameAsString(), value);
                                }
                            });
                        }
                    }
                }

                for (EnumDeclaration decl : cu.findAll(EnumDeclaration.class)) {
                    ENUM_TYPES.add(decl.getNameAsString());
                }
            } catch (Exception e) {
                System.err.println("Failed to index types in file: " + file.getName());
            }
        }
    }

    private static String qualify(String packageName, String simpleName) {
        return packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
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
                    cu.getPackageDeclaration().ifPresent(_ -> FILE_TO_CU_CACHE.put(file, cu));
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
                    module.customGui = hasCustomWidget(cu);
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

    /// True when the module overrides getWidget(GuiTheme), i.e. renders its own
    /// custom settings GUI instead of the standard Meteor widget.
    private static boolean hasCustomWidget(CompilationUnit cu) {
        return cu.findAll(MethodDeclaration.class).stream()
                .anyMatch(method -> method.getNameAsString().equals("getWidget"));
    }

    /// True only for a variable declared as exactly `SettingGroup`; a plain
    /// `getTypeAsString().endsWith("SettingGroup")` also matches `List<SettingGroup>`.
    private static boolean isSettingGroup(VariableDeclarator var) {
        return var.getType() instanceof ClassOrInterfaceType type
                && type.getNameAsString().equals("SettingGroup")
                && type.getTypeArguments().isEmpty();
    }

    /// A group name that is not statically evaluatable (e.g.
    /// `settings.createGroup(slot.getName())`) keeps its source text in backticks so
    /// the settings it holds are still reported.
    private static Map<String, String> findSettingGroupNames(CompilationUnit cu) {
        Map<String, String> map = new HashMap<>();
        cu.findAll(VariableDeclarator.class).stream()
                .filter(MeteoristInfoParser::isSettingGroup)
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
                                    if (arg.isCode) {
                                        System.err.println("Warning: setting group '" + varName
                                                + "' name is not statically evaluatable (" + arg.value
                                                + "); reported as code in the JSON.");
                                        map.put(varName, "`" + arg.value + "`");
                                    } else {
                                        map.put(varName, arg.value);
                                    }
                                });
                                return;
                            }
                        }
                    }
                    System.err.println("Warning: setting group '" + varName
                            + "' is not initialised from settings; reported under its variable name.");
                    map.put(varName, varName);
                });
        return map;
    }

    private static Map<String, List<SettingInfo>> extractSettingsByGroup(CompilationUnit cu, Map<String, String> groupNameMap) {
        Map<String, List<SettingInfo>> groupedSettings = new LinkedHashMap<>();

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

                    // Not in the map means the scope is not a SettingGroup (an unrelated list.add).
                    String resolvedName = groupNameMap.get(groupName);
                    if (resolvedName == null) return;
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
        return fold(expr, 0);
    }

    /// Statically evaluates a builder argument; anything needing the game to resolve
    /// comes back as code (source text in backticks).
    private static ExpressionValue fold(Expression expr, int depth) {
        if (depth > MAX_FOLD_DEPTH) return code(expr);

        if (expr instanceof StringLiteralExpr literal) return new ExpressionValue(literal.getValue(), false);
        if (expr instanceof BooleanLiteralExpr literal)
            return new ExpressionValue(String.valueOf(literal.getValue()), false);
        if (expr instanceof CharLiteralExpr literal)
            return new ExpressionValue(String.valueOf(literal.asChar()), false);
        if (expr instanceof NullLiteralExpr) return new ExpressionValue("null", false);
        if (expr instanceof LiteralExpr) return new ExpressionValue(expr.toString(), false);

        if (expr instanceof EnclosedExpr enclosed) return fold(enclosed.getInner(), depth + 1);

        if (expr instanceof UnaryExpr unary && unary.getOperator() == UnaryExpr.Operator.MINUS) {
            ExpressionValue operand = fold(unary.getExpression(), depth + 1);
            return operand.isCode ? code(unary) : new ExpressionValue("-" + operand.value, false);
        }

        if (expr instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS) {
            ExpressionValue left = fold(binary.getLeft(), depth + 1);
            ExpressionValue right = fold(binary.getRight(), depth + 1);
            return left.isCode || right.isCode ? code(binary)
                    : new ExpressionValue(left.value + right.value, false);
        }

        if (expr instanceof NameExpr name) {
            ExpressionValue constant = CONSTANT_CACHE.get(name.getNameAsString());
            return constant != null ? constant : code(expr);
        }

        if (expr instanceof FieldAccessExpr access && access.getScope() instanceof NameExpr scope) {
            ExpressionValue constant = CONSTANT_CACHE.get(scope.getNameAsString() + "." + access.getNameAsString());
            if (constant != null) return constant;
            if (ENUM_TYPES.contains(scope.getNameAsString())) return new ExpressionValue(access.toString(), false);
        }

        return code(expr);
    }

    private static ExpressionValue code(Expression expr) {
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
                    extractCommandParameters(cu, cmd);
                    return cmd;
                })
                .orElse(null);
    }

    private static void extractCommandParameters(CompilationUnit cu, CommandInfo cmd) {
        cu.findFirst(MethodDeclaration.class, method -> "build".equals(method.getNameAsString()) && !method.getParameters().isEmpty())
                .ifPresent(build -> {
                    ArgNode root = new ArgNode();
                    parseBuilderStatements(cu, build, new HashMap<>(), root, new HashSet<>());
                    Set<String> seenPaths = new LinkedHashSet<>();
                    collectPaths(root, "", seenPaths, 0);
                    cmd.usage = new ArrayList<>(seenPaths);
                    collectParameters(root, cmd);
                });
    }

    private static void parseBuilderStatements(CompilationUnit cu, MethodDeclaration method, Map<String, String> bindings,
                                               ArgNode root, Set<String> visitedHelpers) {
        for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
            String callName = call.getNameAsString();
            String scope = call.getScope().map(Object::toString).orElse("");
            if (call.getArguments().isEmpty()) continue;

            if (!scope.equals("builder")) {
                if (call.getScope().isPresent()) continue;
                StringBuilder helperKey = new StringBuilder(callName);
                for (int i = 1; i < call.getArguments().size(); i++) {
                    Expression arg = call.getArgument(i);
                    helperKey.append('|').append(arg.toString());
                }
                if (!visitedHelpers.add(helperKey.toString())) continue;
                MethodDeclaration helper = cu.findAll(MethodDeclaration.class).stream()
                        .filter(m -> m.getNameAsString().equals(callName))
                        .findFirst().orElse(null);
                if (helper == null || helper.getParameters().isEmpty()) continue;
                var firstParamOpt = helper.getParameters().getFirst();
                if (firstParamOpt.isEmpty() || !firstParamOpt.get().getTypeAsString().contains("LiteralArgumentBuilder")) {
                    continue;
                }
                Map<String, String> newBindings = new HashMap<>(bindings);
                for (int i = 1; i < helper.getParameters().size() && i < call.getArguments().size(); i++) {
                    Expression arg = call.getArgument(i);
                    if (arg instanceof StringLiteralExpr literal) {
                        newBindings.put(helper.getParameters().get(i).getNameAsString(), literal.getValue());
                    }
                }
                parseBuilderStatements(cu, helper, newBindings, root, visitedHelpers);
                continue;
            }

            if (callName.equals("then")) {
                ArgNode child = parseChain(call.getArgument(0), bindings);
                if (child != null) root.children.add(child);
            } else if (callName.equals("executes")) {
                root.executable = true;
            }
        }
    }

    private static ArgNode parseChain(Expression expr, Map<String, String> bindings) {
        List<MethodCallExpr> calls = new ArrayList<>();
        Expression current = expr;
        while (current instanceof MethodCallExpr call) {
            calls.add(call);
            current = call.getScope().orElse(null);
        }
        Collections.reverse(calls);

        ArgNode node = null;
        for (MethodCallExpr call : calls) {
            switch (call.getNameAsString()) {
                case "literal":
                    String literalValue = boundValue(call.getArgument(0), bindings);
                    if (literalValue == null) return null;
                    node = new ArgNode();
                    node.name = literalValue;
                    break;
                case "argument":
                    String argValue = boundValue(call.getArgument(0), bindings);
                    if (argValue == null) return null;
                    node = new ArgNode();
                    node.name = argValue;
                    node.argument = true;
                    node.type = humanizeArgumentType(call.getArgument(1).toString());
                    break;
                case "executes":
                    if (node != null) node.executable = true;
                    break;
                case "then":
                    if (node != null && !call.getArguments().isEmpty()) {
                        ArgNode child = parseChain(call.getArgument(0), bindings);
                        if (child != null) node.children.add(child);
                    }
                    break;
                default:
                    break;
            }
        }
        return node;
    }

    private static String boundValue(Expression expr, Map<String, String> bindings) {
        if (expr instanceof StringLiteralExpr literal) return literal.getValue();
        if (expr instanceof NameExpr name) return bindings.get(name.getNameAsString());
        return null;
    }

    private static String humanizeArgumentType(String type) {
        int dot = type.indexOf('.');
        String receiver = dot == -1 ? type : type.substring(0, dot);
        receiver = receiver.substring(receiver.lastIndexOf(' ') + 1);
        if (receiver.endsWith("ArgumentType")) {
            receiver = receiver.substring(0, receiver.length() - "ArgumentType".length());
        }
        return receiver.isEmpty() ? type : receiver;
    }

    private static void collectPaths(ArgNode node, String prefix, Set<String> out, int depth) {
        if (depth > 8 || out.size() > 60) return;
        for (ArgNode child : node.children) {
            String token = child.argument ? "<" + child.name + ">" : child.name;
            String path = prefix.isEmpty() ? token : prefix + " " + token;
            if (child.executable || child.children.isEmpty()) out.add(path);
            collectPaths(child, path, out, depth + 1);
        }
    }

    private static void collectParameters(ArgNode node, CommandInfo cmd) {
        Set<String> seen = new HashSet<>();
        collectParameterNode(node, seen, cmd);
    }

    private static void collectParameterNode(ArgNode node, Set<String> seen, CommandInfo cmd) {
        for (ArgNode child : node.children) {
            if (child.argument && child.type != null && seen.add(child.name)) {
                ParameterInfo parameter = new ParameterInfo();
                parameter.name = child.name;
                parameter.type = child.type;
                cmd.parameters.add(parameter);
            }
            collectParameterNode(child, seen, cmd);
        }
    }

    private static List<HudElementInfoWrapper> extractHudElements(CompilationUnit cu, String className, String classPath) {
        List<HudElementInfoWrapper> elements = new ArrayList<>();
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

                    HudElementInfoWrapper element = new HudElementInfoWrapper();
                    element.name = nameExpr.value;
                    element.description = descExpr.value;
                    element.className = className;
                    element.classPath = classPath;
                    element.lineNumber = init.getBegin().map(pos -> pos.line).orElse(-1);
                    element.settings = extractSettingsByGroup(cu, findSettingGroupNames(cu));
                    element.presets = extractPresets(cu);
                    elements.add(element);
                });
        return elements;
    }

    private static List<PresetInfo> extractPresets(CompilationUnit cu) {
        List<PresetInfo> presets = new ArrayList<>();
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
                    presets.add(preset);
                });
        return presets;
    }

    private static void writeToJsonFile(Object data, String filename) {
        try {
            Path path = Paths.get(filename);
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (FileWriter writer = new FileWriter(path.toFile())) {
                new GsonBuilder().create().toJson(data, writer);
                writer.write('\n');
            }
        } catch (IOException e) {
            System.err.println("Failed to write JSON file: " + e.getMessage());
        }
    }

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
        boolean customGui;
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
        List<String> usage = new ArrayList<>();
        List<ParameterInfo> parameters = new ArrayList<>();
    }

    static class ParameterInfo {
        String name;
        String type;
    }

    static class PresetInfo {
        String title;
        String text;
        int lineNumber;
    }

    static class HudElementInfoWrapper {
        String name;
        String description;
        String className;
        String classPath;
        int lineNumber;
        Map<String, List<SettingInfo>> settings = new LinkedHashMap<>();
        List<PresetInfo> presets = new ArrayList<>();
    }

    static class ArgNode {
        String name;
        boolean argument;
        String type;
        boolean executable;
        List<ArgNode> children = new ArrayList<>();
    }
}
