package com.java.ere;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.java.ere.config.ExtractionConfig;
import com.java.ere.config.ResolverConfig;

public class CodeParser {
    private Set<String> projectPackages = new HashSet<>();
    private ExtractionConfig extractionConfig = new ExtractionConfig();
    
    /**
     * 默认构造函数
     */
    public CodeParser() {
        // 使用默认配置
    }
    
    /**
     * 带配置的构造函数
     */
    public CodeParser(List<String> projectPackages, ExtractionConfig extractionConfig) {
        if (projectPackages != null) {
            this.projectPackages = new HashSet<>(projectPackages);
        }
        if (extractionConfig != null) {
            this.extractionConfig = extractionConfig;
        }
    }
    
    /**
     * 简单初始化（向后兼容）
     */
    public void init(String projectPath) {
        TypeSolver typeSolver = new CombinedTypeSolver(
            new JavaParserTypeSolver(new File(projectPath)),
            new ReflectionTypeSolver()
        );
        ParserConfiguration config = new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
            .setSymbolResolver(new JavaSymbolSolver(typeSolver));
        StaticJavaParser.setConfiguration(config);
    }
    /**
     * 高级初始化：支持多源码路径和第三方库
     */
    public void initSymbolResolver(String projectRoot, 
                                   List<String> sourcePaths,
                                   ResolverConfig config) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        
        // 1. JDK类
        if (config.isIncludeJdk()) {
            typeSolver.add(new ReflectionTypeSolver());
        }
        
        // 2. 项目源码（全部，不过滤）
        for (String sourcePath : sourcePaths) {
            File sourceDir = new File(projectRoot, sourcePath);
            if (sourceDir.exists() && sourceDir.isDirectory()) {
                typeSolver.add(new JavaParserTypeSolver(sourceDir));
                System.out.println("[符号解析器] 加载源码路径: " + sourcePath);
            }
        }
        
        // 3. 第三方库
        String mode = config.getDependencyMode();
        if ("essential".equals(mode)) {
            loadEssentialDependencies(typeSolver, projectRoot, config);
        } else if ("all".equals(mode)) {
            loadAllDependencies(typeSolver, projectRoot, config);
        }
        
        ParserConfiguration parserConfig = new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
            .setSymbolResolver(new JavaSymbolSolver(typeSolver));
        StaticJavaParser.setConfiguration(parserConfig);
        
        System.out.println("[符号解析器] 初始化完成");
    }
    
    /**
     * 加载核心第三方库
     */
    private void loadEssentialDependencies(CombinedTypeSolver typeSolver, 
                                          String projectRoot,
                                          ResolverConfig config) {
        File depDir = new File(projectRoot, config.getLocalDependencyDir());
        if (!depDir.exists() || !depDir.isDirectory()) {
            System.out.println("[符号解析器] 依赖目录不存在: " + depDir.getAbsolutePath());
            return;
        }
        
        File[] jars = depDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            System.out.println("[符号解析器] 未找到依赖JAR文件");
            return;
        }
        
        int loaded = 0;
        for (File jar : jars) {
            for (String pattern : config.getEssentialPatterns()) {
                if (jar.getName().contains(pattern)) {
                    try {
                        typeSolver.add(new JarTypeSolver(jar.getAbsolutePath()));
                        System.out.println("[符号解析器] 加载核心库: " + jar.getName());
                        loaded++;
                        break;
                    } catch (IOException e) {
                        System.err.println("[符号解析器] 无法加载: " + jar.getName());
                    }
                }
            }
        }
        System.out.println("[符号解析器] 共加载 " + loaded + " 个核心库");
    }
    
    /**
     * 加载所有第三方库
     */
    private void loadAllDependencies(CombinedTypeSolver typeSolver, 
                                    String projectRoot,
                                    ResolverConfig config) {
        File depDir = new File(projectRoot, config.getLocalDependencyDir());
        if (!depDir.exists() || !depDir.isDirectory()) {
            return;
        }
        
        File[] jars = depDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null) return;
        
        int loaded = 0;
        for (File jar : jars) {
            try {
                typeSolver.add(new JarTypeSolver(jar.getAbsolutePath()));
                loaded++;
            } catch (IOException e) {
                System.err.println("[符号解析器] 无法加载: " + jar.getName());
            }
        }
        System.out.println("[符号解析器] 共加载 " + loaded + " 个依赖库");
    }
    
    /**
     * 批量解析文件
     */
    public Map<String, Entity> parseFiles(List<File> javaFiles) {
        Map<String, Entity> allEntities = new HashMap<>();
        List<CompilationUnit> allCompilationUnits = new ArrayList<>();
        
        System.out.println("[实体提取] 开始解析 " + javaFiles.size() + " 个文件...");
        int success = 0;
        int failed = 0;
        
        // 阶段1: 提取所有实体（不构建跨文件关系）
        for (File file : javaFiles) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(file);
                String filePath = file.getAbsolutePath();
                Map<String, Entity> entities = extractEntitiesFromFile(cu, filePath);
                allEntities.putAll(entities);
                allCompilationUnits.add(cu);
                success++;
            } catch (Exception e) {
                System.err.println("[实体提取] 解析失败: " + file.getName() + " - " + e.getMessage());
                failed++;
            }
        }
        
        System.out.println("[实体提取] 完成: 成功 " + success + " 个, 失败 " + failed + " 个");
        System.out.println("[实体提取] 共提取 " + allEntities.size() + " 个实体");
        
        // 阶段2: 构建跨文件关系（此时allEntities已包含所有实体）
        System.out.println("[关系构建] 开始构建跨文件关系...");
        for (CompilationUnit cu : allCompilationUnits) {
            buildRelations(cu, allEntities);
        }
        System.out.println("[关系构建] 完成");
        
        return allEntities;
    }
    
    /**
     * 从单个文件提取实体（不构建跨文件关系）
     */
    private Map<String, Entity> extractEntitiesFromFile(CompilationUnit cu, String filePath) {
        Map<String, Entity> entities = new HashMap<>();
        
        // 根据配置决定提取哪些实体
        if (extractionConfig.isEntityEnabled("ClassOrInterface")) {
            extractClassesAndInterfaces(cu, entities, filePath);
        }
        
        if (extractionConfig.isEntityEnabled("Method")) {
            extractMethods(cu, entities, filePath);
        }
        
        if (extractionConfig.isEntityEnabled("Field")) {
            extractFields(cu, entities, filePath);
        }

        return entities;
    }
    
    /**
     * 单个文件解析（用于调试）
     */
    public Map<String, Entity> parseFile(File javaFile) throws Exception {
        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        String filePath = javaFile.getAbsolutePath();
        return extractEntitiesFromFile(cu, filePath);
    }

    private void extractClassesAndInterfaces(CompilationUnit cu, Map<String, Entity> entities, String filePath) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String name = classDecl.getNameAsString();
            String prefix = classDecl.isInterface() ? "iface_" : "class_";
            Entity entity = new Entity(prefix + name, "ClassOrInterface");
            entity.addProperty("name", name);
            entity.addProperty("isInterface", String.valueOf(classDecl.isInterface()));
            entity.addProperty("purpose", extractJavadoc(classDecl.getJavadoc()));
            entity.addProperty("filePath", filePath);
            entities.put(entity.getId(), entity);
        });
    }

    private void extractMethods(CompilationUnit cu, Map<String, Entity> entities, String filePath) {
        cu.findAll(MethodDeclaration.class).forEach(methodDecl -> {
            String methodName = methodDecl.getNameAsString();
            String className = methodDecl.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse("Unknown");
            
            // 生成参数签名以支持方法重载
            String paramSignature = methodDecl.getParameters().stream()
                .map(p -> p.getType().asString())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
            
            // 方法ID包含参数签名，格式：method_ClassName_methodName(param1Type,param2Type)
            String id = "method_" + className + "_" + methodName + "(" + paramSignature + ")";
            Entity methodEntity = new Entity(id, "Method");
            methodEntity.addProperty("name", methodName);
            methodEntity.addProperty("owner", className);
            methodEntity.addProperty("signature", methodName + "(" + paramSignature + ")");
            methodEntity.addProperty("is_external", "false");  // 业务代码方法
            methodEntity.addProperty("filePath", filePath);
            
            // 根据配置决定是否提取Javadoc
            if (extractionConfig.isIncludeJavadoc()) {
                methodEntity.addProperty("business_role", extractJavadoc(methodDecl.getJavadoc()));
            }
            
            // 分析方法注解，推断框架回调关系
            inferFrameworkCallbacksFromAnnotations(methodEntity, methodDecl);
            
            entities.put(id, methodEntity);

            // 根据配置决定是否提取参数
            if (extractionConfig.isRelationEnabled("has_parameter") && 
                extractionConfig.isEntityEnabled("Parameter")) {
                methodDecl.getParameters().forEach(param -> {
                    String paramName = param.getNameAsString();
                    // 参数ID包含方法签名以支持重载
                    String paramId = "param_" + className + "_" + methodName + "(" + paramSignature + ")_" + paramName;
                    Entity paramEntity = new Entity(paramId, "Parameter");
                    paramEntity.addProperty("name", paramName);
                    paramEntity.addProperty("type", param.getType().asString());
                    entities.put(paramId, paramEntity);
                    methodEntity.addRelation("has_parameter", paramId);
                });
            }

            // 根据配置决定是否提取返回值
            if (extractionConfig.isRelationEnabled("returns") && 
                extractionConfig.isEntityEnabled("Return")) {
                // 返回值ID包含方法签名以支持重载
                String returnId = "return_" + className + "_" + methodName + "(" + paramSignature + ")";
                String returnType = methodDecl.getType().asString();
                Entity returnEntity = new Entity(returnId, "Return");
                returnEntity.addProperty("name", returnType);  // 添加name属性用于显示
                returnEntity.addProperty("type", returnType);
                entities.put(returnId, returnEntity);
                methodEntity.addRelation("returns", returnId);
            }
            
            // 根据配置决定是否提取异常
            if (extractionConfig.isRelationEnabled("throws") && 
                extractionConfig.isEntityEnabled("Exception")) {
                methodDecl.getThrownExceptions().forEach(thrownType -> {
                    String exceptionType = thrownType.asString();
                    String exceptionId = "exception_" + exceptionType;
                    
                    // 创建Exception实体（如果不存在）
                    if (!entities.containsKey(exceptionId)) {
                        Entity exceptionEntity = new Entity(exceptionId, "Exception");
                        exceptionEntity.addProperty("name", exceptionType);  // 添加name属性用于显示
                        exceptionEntity.addProperty("type", exceptionType);
                        entities.put(exceptionId, exceptionEntity);
                    }
                    
                    // 建立throws关系
                    methodEntity.addRelation("throws", exceptionId);
                });
            }
            
            // 根据配置决定是否提取注解
            if (extractionConfig.isRelationEnabled("has_annotation") && 
                extractionConfig.isIncludeAnnotations()) {
                methodDecl.getAnnotations().forEach(annotation -> {
                    String annotationName = annotation.getNameAsString();
                    String annotationId = "annotation_" + annotationName;
                    
                    // 创建Annotation实体（如果不存在）
                    if (!entities.containsKey(annotationId)) {
                        Entity annotationEntity = new Entity(annotationId, "Annotation");
                        annotationEntity.addProperty("name", annotationName);
                        entities.put(annotationId, annotationEntity);
                    }
                    
                    // 建立has_annotation关系
                    methodEntity.addRelation("has_annotation", annotationId);
                });
            }
        });
    }

    private void extractFields(CompilationUnit cu, Map<String, Entity> entities, String filePath) {
        cu.findAll(FieldDeclaration.class).forEach(fieldDecl -> {
            String fieldName = fieldDecl.getVariable(0).getNameAsString();
            String className = fieldDecl.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse("Unknown");
            String id = "field_" + className + "_" + fieldName;
            Entity fieldEntity = new Entity(id, "Field");
            fieldEntity.addProperty("name", fieldName);
            fieldEntity.addProperty("type", fieldDecl.getVariable(0).getType().asString());
            fieldEntity.addProperty("filePath", filePath);
            entities.put(id, fieldEntity);
        });
    }

    private void buildRelations(CompilationUnit cu, Map<String, Entity> entities) {
        // 构建类级别的关系（implements）
        if (extractionConfig.isRelationEnabled("implements") && 
            extractionConfig.isEntityEnabled("ClassOrInterface")) {
            buildImplementsRelations(cu, entities);
        }
        
        // 只有Method实体存在时才构建方法级别的关系
        if (!extractionConfig.isEntityEnabled("Method")) {
            return;
        }
        
        // 构建calls关系
        if (extractionConfig.isRelationEnabled("calls")) {
            buildCallsRelations(cu, entities);
        }
        
        // 构建accesses关系
        if (extractionConfig.isRelationEnabled("accesses") && 
            extractionConfig.isEntityEnabled("Field")) {
            buildAccessesRelations(cu, entities);
        }
        
        // 构建overrides关系
        if (extractionConfig.isRelationEnabled("overrides")) {
            buildOverridesRelations(cu, entities);
        }
    }
    
    /**
     * 构建calls关系
     */
    private void buildCallsRelations(CompilationUnit cu, Map<String, Entity> entities) {
        cu.findAll(MethodDeclaration.class).forEach(methodDecl -> {
            String methodName = methodDecl.getNameAsString();
            String className = methodDecl.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse("Unknown");
            
            // 生成当前方法的签名
            String callerSignature = methodDecl.getParameters().stream()
                .map(p -> p.getType().asString())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
            String methodId = "method_" + className + "_" + methodName + "(" + callerSignature + ")";
            Entity methodEntity = entities.get(methodId);
            if (methodEntity == null) return;

            methodDecl.findAll(MethodCallExpr.class).forEach(callExpr -> {
                // 优先使用AST分析（不需要JAR包）
                if (tryASTBasedCallAnalysis(methodEntity, callExpr, cu, className, entities)) {
                    return;  // AST分析成功，直接返回
                }
                
                // 降级到符号解析器（需要JAR包）
                try {
                    ResolvedMethodDeclaration resolved = callExpr.resolve();
                    String declaringClass = resolved.declaringType().getQualifiedName();
                    String calleeMethod = resolved.getName();
                    
                    // 判断是否为项目代码（包括同类调用）
                    if (isProjectCode(declaringClass)) {
                        // 项目内调用：建立完整关系
                        String calleeClass = resolved.declaringType().getClassName();
                        
                        // 生成被调用方法的签名（使用简单类名以匹配extractMethods）
                        String calleeSignature = "";
                        if (resolved.getNumberOfParams() > 0) {
                            // 提取简单类名而非全限定名
                            calleeSignature = getSimpleTypeName(resolved.getParam(0).describeType());
                            for (int i = 1; i < resolved.getNumberOfParams(); i++) {
                                calleeSignature += "," + getSimpleTypeName(resolved.getParam(i).describeType());
                            }
                        }
                        String calleeId = "method_" + calleeClass + "_" + calleeMethod + "(" + calleeSignature + ")";
                        
                        if (entities.containsKey(calleeId)) {
                            methodEntity.addRelation("calls", calleeId);
                        }
                    } else {
                        // 第三方库调用：根据策略处理
                        handleThirdPartyCall(methodEntity, declaringClass, calleeMethod, entities);
                    }
                } catch (Exception e) {
                    // 符号解析也失败，最后尝试简单匹配（仅用于同类方法调用）
                    trySimpleMethodMatch(methodEntity, callExpr, className, entities);
                }
            });
        });
    }
    
    /**
     * 处理符号解析失败
     */
    private void handleResolutionFailure(String relationType, String entityId, Exception e) {
        String strategy = extractionConfig.getOnResolutionFailure();
        
        switch (strategy) {
            case "ignore":
                // 静默忽略
                break;
            case "warn":
                // 输出警告（仅在调试时）
                // System.err.println("[警告] " + relationType + " 解析失败: " + entityId + " - " + e.getMessage());
                break;
            case "error":
                // 抛出错误
                throw new RuntimeException(relationType + " 解析失败: " + entityId, e);
            default:
                // 默认忽略
                break;
        }
    }
    
    /**
     * 判断是否为项目代码
     */
    private boolean isProjectCode(String fullyQualifiedName) {
        if (projectPackages.isEmpty()) {
            return true;  // 没有配置项目包名，默认都算项目代码
        }
        return projectPackages.stream()
            .anyMatch(pkg -> fullyQualifiedName.startsWith(pkg));
    }
    
    /**
     * 处理第三方库调用
     */
    private void handleThirdPartyCall(Entity methodEntity, String declaringClass, String methodName, Map<String, Entity> entities) {
        String strategy = extractionConfig.getThirdPartyCallStrategy();
        
        if ("ignore".equals(strategy)) {
            // 完全忽略
            return;
        } else if ("mark".equals(strategy)) {
            // 标记为外部依赖
            String externalCall = declaringClass + "." + methodName;
            // 添加到属性中
            String existingDeps = methodEntity.getProperties().get("external_dependencies");
            if (existingDeps == null) {
                methodEntity.addProperty("external_dependencies", externalCall);
            } else {
                methodEntity.addProperty("external_dependencies", existingDeps + ", " + externalCall);
            }
        } else if ("full".equals(strategy)) {
            // 创建第三方方法实体并建立调用关系
            String simpleClassName = getSimpleClassName(declaringClass);
            String thirdPartyMethodId = "method_" + simpleClassName + "_" + methodName + "()";
            
            // 如果第三方方法实体不存在，创建它
            if (!entities.containsKey(thirdPartyMethodId)) {
                Entity thirdPartyMethod = new Entity(thirdPartyMethodId, "Method");
                thirdPartyMethod.addProperty("name", methodName);
                thirdPartyMethod.addProperty("owner", simpleClassName);
                thirdPartyMethod.addProperty("signature", methodName + "()");
                thirdPartyMethod.addProperty("is_external", "true");
                thirdPartyMethod.addProperty("full_class_name", declaringClass);
                entities.put(thirdPartyMethodId, thirdPartyMethod);
            }
            
            // 建立调用关系
            methodEntity.addRelation("calls", thirdPartyMethodId);
        }
    }
    
    /**
     * 构建implements关系
     */
    private void buildImplementsRelations(CompilationUnit cu, Map<String, Entity> entities) {
        for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (classDecl.isInterface()) {
                continue;  // 接口不能实现其他接口，跳过
            }
            
            String className = classDecl.getNameAsString();
            String classId = "class_" + className;
            Entity classEntity = entities.get(classId);
            
            if (classEntity == null) {
                continue;
            }
            
            // 提取实现的接口
            for (com.github.javaparser.ast.type.ClassOrInterfaceType implementedType : classDecl.getImplementedTypes()) {
                String interfaceName = implementedType.getNameAsString();
                String interfaceId = "iface_" + interfaceName;
                
                // 如果接口实体存在，建立implements关系
                if (entities.containsKey(interfaceId)) {
                    classEntity.addRelation("implements", interfaceId);
                }
            }
        }
    }
    
    /**
     * 构建accesses关系（字段访问）
     */
    private void buildAccessesRelations(CompilationUnit cu, Map<String, Entity> entities) {
        cu.findAll(MethodDeclaration.class).forEach(methodDecl -> {
            String methodName = methodDecl.getNameAsString();
            String className = methodDecl.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse("Unknown");
            
            // 生成方法签名
            String signature = methodDecl.getParameters().stream()
                .map(p -> p.getType().asString())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
            String methodId = "method_" + className + "_" + methodName + "(" + signature + ")";
            Entity methodEntity = entities.get(methodId);
            if (methodEntity == null) return;
            
            // 查找方法体中的字段访问
            methodDecl.findAll(com.github.javaparser.ast.expr.FieldAccessExpr.class).forEach(fieldAccess -> {
                try {
                    String fieldName = fieldAccess.getNameAsString();
                    // 尝试简单匹配：假设访问的是当前类的字段
                    String fieldId = "field_" + className + "_" + fieldName;
                    
                    if (entities.containsKey(fieldId)) {
                        methodEntity.addRelation("accesses", fieldId);
                    }
                } catch (Exception e) {
                    handleResolutionFailure("accesses", methodId, e);
                }
            });
            
            // 也查找简单的字段名引用（this.field或直接field）
            methodDecl.findAll(com.github.javaparser.ast.expr.NameExpr.class).forEach(nameExpr -> {
                String name = nameExpr.getNameAsString();
                String fieldId = "field_" + className + "_" + name;
                
                // 检查是否存在对应的字段实体
                if (entities.containsKey(fieldId)) {
                    methodEntity.addRelation("accesses", fieldId);
                }
            });
        });
    }
    
    /**
     * 构建overrides关系（方法重写）
     */
    private void buildOverridesRelations(CompilationUnit cu, Map<String, Entity> entities) {
        cu.findAll(MethodDeclaration.class).forEach(methodDecl -> {
            String methodName = methodDecl.getNameAsString();
            String className = methodDecl.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse("Unknown");
            
            // 生成当前方法签名
            String signature = methodDecl.getParameters().stream()
                .map(p -> p.getType().asString())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
            String methodId = "method_" + className + "_" + methodName + "(" + signature + ")";
            Entity methodEntity = entities.get(methodId);
            if (methodEntity == null) return;
            
            // 策略1: 检查是否有@Override注解（快速但不完整）
            boolean hasOverrideAnnotation = methodDecl.getAnnotationByName("Override").isPresent();
            
            if (hasOverrideAnnotation) {
                // 有@Override注解，说明肯定是重写方法
                // 尝试找到父类/接口中的同名方法
                ClassOrInterfaceDeclaration currentClass = methodDecl.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
                if (currentClass == null) return;
                
                // 遍历实现的接口和继承的类
                currentClass.getImplementedTypes().forEach(implementedType -> {
                    String interfaceName = implementedType.getNameAsString();
                    // 接口方法使用相同的签名
                    String parentMethodId = "method_" + interfaceName + "_" + methodName + "(" + signature + ")";
                    
                    if (entities.containsKey(parentMethodId)) {
                        methodEntity.addRelation("overrides", parentMethodId);
                    }
                });
                
                currentClass.getExtendedTypes().forEach(extendedType -> {
                    String parentClassName = extendedType.getNameAsString();
                    // 父类方法使用相同的签名
                    String parentMethodId = "method_" + parentClassName + "_" + methodName + "(" + signature + ")";
                    
                    if (entities.containsKey(parentMethodId)) {
                        methodEntity.addRelation("overrides", parentMethodId);
                    }
                });
            }
            
            // 策略2: 使用符号解析器（完整但耗时，容易失败）
            // 注意：这部分代码会显著增加耗时，建议谨慎使用
            /*
            try {
                ResolvedMethodDeclaration resolved = methodDecl.resolve();
                ResolvedReferenceTypeDeclaration declaringType = resolved.declaringType();
                
                // 获取所有祖先类型
                for (ResolvedReferenceType ancestor : declaringType.getAllAncestors()) {
                    for (ResolvedMethodDeclaration ancestorMethod : ancestor.getAllMethods()) {
                        // 检查方法签名是否匹配
                        if (ancestorMethod.getName().equals(methodName) && 
                            ancestorMethod.getNumberOfParams() == resolved.getNumberOfParams()) {
                            
                            String ancestorClassName = ancestor.getTypeDeclaration().get().getClassName();
                            String parentMethodId = "method_" + ancestorClassName + "_" + methodName;
                            
                            if (entities.containsKey(parentMethodId)) {
                                methodEntity.addRelation("overrides", parentMethodId);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                handleResolutionFailure("overrides", methodId, e);
            }
            */
        });
    }

    /**
     * 从全限定类名中提取简单类名
     * 例如: "de.codecentric.boot.admin.client.registration.Application" -> "Application"
     *       "List<String>" -> "List<String>"
     */
    private String getSimpleTypeName(String fullTypeName) {
        // 处理泛型类型
        if (fullTypeName.contains("<")) {
            int genericStart = fullTypeName.indexOf('<');
            String baseType = fullTypeName.substring(0, genericStart);
            String genericPart = fullTypeName.substring(genericStart);
            return getSimpleClassName(baseType) + genericPart;
        }
        return getSimpleClassName(fullTypeName);
    }
    
    /**
     * 提取简单类名
     */
    private String getSimpleClassName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }
    
    /**
     * 基于AST的方法调用分析（不依赖符号解析器和JAR包）
     * 返回true表示成功处理，false表示需要降级到其他方案
     */
    private boolean tryASTBasedCallAnalysis(Entity methodEntity, MethodCallExpr callExpr,
                                           CompilationUnit cu, String currentClassName,
                                           Map<String, Entity> entities) {
        try {
            String calledMethodName = callExpr.getNameAsString();
            Optional<Expression> scope = callExpr.getScope();
            
            // 情况1：有scope的调用，如 obj.method() 或 ClassName.method()
            if (scope.isPresent()) {
                String scopeStr = scope.get().toString();
                
                // 检查是否是super调用
                if (scopeStr.equals("super")) {
                    // super调用不处理（父类方法，无法在当前项目找到）
                    return true;  // 返回true表示已处理（选择忽略）
                }
                
                // 检查是否是this调用
                if (scopeStr.equals("this")) {
                    // this调用视为同类调用
                    return matchSameClassMethod(methodEntity, calledMethodName, currentClassName, entities);
                }
                
                // 尝试推断调用对象的类型
                String calleeClassName = inferTypeFromScope(scope.get(), cu, currentClassName);
                
                if (calleeClassName != null) {
                    // 判断是否为项目代码
                    if (isProjectCode(calleeClassName)) {
                        // 项目内调用：尝试匹配方法
                        String simpleClassName = getSimpleClassName(calleeClassName);
                        return matchProjectMethod(methodEntity, simpleClassName, calledMethodName, entities);
                    } else {
                        // 第三方库调用：记录到external_dependencies
                        handleThirdPartyCall(methodEntity, calleeClassName, calledMethodName, entities);
                        return true;
                    }
                }
            } else {
                // 情况2：无scope的调用，如 method()，可能是同类方法或静态导入
                return matchSameClassMethod(methodEntity, calledMethodName, currentClassName, entities);
            }
            
            return false;  // 无法处理，降级到符号解析器
            
        } catch (Exception e) {
            return false;  // 分析失败，降级到符号解析器
        }
    }
    
    /**
     * 推断表达式的类型
     */
    private String inferTypeFromScope(Expression scope, CompilationUnit cu, String currentClassName) {
        if (scope instanceof NameExpr) {
            // 变量名，查找字段或局部变量声明
            String varName = ((NameExpr) scope).getNameAsString();
            return findVariableType(varName, cu, currentClassName);
            
        } else if (scope instanceof FieldAccessExpr) {
            // 字段访问，如 this.field 或 obj.field
            FieldAccessExpr fieldAccess = (FieldAccessExpr) scope;
            // 递归推断scope的类型
            String scopeType = inferTypeFromScope(fieldAccess.getScope(), cu, currentClassName);
            if (scopeType != null) {
                // TODO: 这里可以进一步查找scopeType类中的字段类型
                return null;  // 暂时返回null，降级到符号解析器
            }
        }
        
        return null;
    }
    
    /**
     * 查找变量的类型（字段或局部变量）
     */
    private String findVariableType(String varName, CompilationUnit cu, String currentClassName) {
        // 1. 在当前类中查找字段声明
        for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (classDecl.getNameAsString().equals(currentClassName)) {
                for (FieldDeclaration field : classDecl.getFields()) {
                    for (VariableDeclarator variable : field.getVariables()) {
                        if (variable.getNameAsString().equals(varName)) {
                            Type fieldType = field.getCommonType();
                            return resolveTypeFromImports(fieldType.asString(), cu);
                        }
                    }
                }
            }
        }
        
        // 2. TODO: 查找局部变量声明（更复杂，需要scope分析）
        
        return null;
    }
    
    /**
     * 从import语句解析类型的全限定名
     */
    private String resolveTypeFromImports(String simpleTypeName, CompilationUnit cu) {
        // 去除泛型参数
        String baseType = simpleTypeName.split("<")[0].trim();
        
        // 检查是否已经是全限定名
        if (baseType.contains(".")) {
            return baseType;
        }
        
        // 1. 检查java.lang包（默认导入）
        if (isJavaLangClass(baseType)) {
            return "java.lang." + baseType;
        }
        
        // 2. 在import语句中查找
        for (ImportDeclaration importDecl : cu.getImports()) {
            String importName = importDecl.getNameAsString();
            
            if (importDecl.isAsterisk()) {
                // 通配符导入，无法确定具体类
                continue;
            }
            
            if (importName.endsWith("." + baseType)) {
                return importName;
            }
        }
        
        // 3. 检查是否是同包的类
        Optional<String> packageName = cu.getPackageDeclaration()
            .map(pd -> pd.getNameAsString());
        if (packageName.isPresent()) {
            return packageName.get() + "." + baseType;
        }
        
        return null;
    }
    
    /**
     * 检查是否是java.lang包中的类
     */
    private boolean isJavaLangClass(String className) {
        Set<String> javaLangClasses = new HashSet<>(Arrays.asList(
            "String", "Object", "Class", "Integer", "Long", "Double", "Float",
            "Boolean", "Byte", "Short", "Character", "System", "Math",
            "Thread", "Runnable", "Exception", "RuntimeException"
        ));
        return javaLangClasses.contains(className);
    }
    
    /**
     * 匹配同类方法调用
     */
    private boolean matchSameClassMethod(Entity methodEntity, String calledMethodName,
                                        String currentClassName, Map<String, Entity> entities) {
        String prefix = "method_" + currentClassName + "_" + calledMethodName + "(";
        
        for (String entityId : entities.keySet()) {
            if (entityId.startsWith(prefix) && !entityId.equals(methodEntity.getId())) {
                // 找到匹配且不是自己
                methodEntity.addRelation("calls", entityId);
                return true;
            }
        }
        
        return false;  // 未找到匹配
    }
    
    /**
     * 匹配项目内方法调用
     */
    private boolean matchProjectMethod(Entity methodEntity, String className,
                                      String methodName, Map<String, Entity> entities) {
        String prefix = "method_" + className + "_" + methodName + "(";
        
        for (String entityId : entities.keySet()) {
            if (entityId.startsWith(prefix)) {
                methodEntity.addRelation("calls", entityId);
                return true;
            }
        }
        
        return false;  // 未找到匹配（可能是重载方法无法精确匹配）
    }
    
    
    /**
     * 当符号解析失败时，尝试简单匹配同类方法调用
     * 这对于同类方法调用特别有用，因为有时符号解析可能失败
     */
    private void trySimpleMethodMatch(Entity methodEntity, MethodCallExpr callExpr, 
                                     String currentClassName, Map<String, Entity> entities) {
        try {
            String calledMethodName = callExpr.getNameAsString();
            
            // 尝试匹配同类的所有方法（通过方法名）
            // 格式: method_ClassName_methodName(...)
            String prefix = "method_" + currentClassName + "_" + calledMethodName + "(";
            
            for (String entityId : entities.keySet()) {
                if (entityId.startsWith(prefix) && !entityId.equals(methodEntity.getId())) {
                    // 找到可能的匹配，建立调用关系（排除自己）
                    methodEntity.addRelation("calls", entityId);
                    // 注意：这里可能会匹配到多个重载方法，但通常只有一个是真正被调用的
                    // 在无法精确解析的情况下，保守地建立关系
                    break;  // 只建立第一个匹配
                }
            }
        } catch (Exception e) {
            // 简单匹配也失败，忽略
        }
    }

    private String extractJavadoc(Optional<Javadoc> javadocOpt) {
        return javadocOpt.map(jc -> jc.toText().trim()).orElse("无描述");
    }
    
    /**
     * 从方法注解推断框架回调关系
     * 例如：@EventListener → Spring会调用此方法
     */
    private void inferFrameworkCallbacksFromAnnotations(Entity methodEntity, MethodDeclaration methodDecl) {
        for (com.github.javaparser.ast.expr.AnnotationExpr annotation : methodDecl.getAnnotations()) {
            String annotationName = annotation.getNameAsString();
            String frameworkCaller = getFrameworkCallerFromAnnotation(annotationName);
            
            if (frameworkCaller != null) {
                // 记录框架回调信息到属性中
                String existingCallbacks = methodEntity.getProperties().get("framework_callbacks");
                if (existingCallbacks == null) {
                    methodEntity.addProperty("framework_callbacks", frameworkCaller + " (@" + annotationName + ")");
                } else {
                    methodEntity.addProperty("framework_callbacks", 
                        existingCallbacks + ", " + frameworkCaller + " (@" + annotationName + ")");
                }
            }
        }
    }
    
    /**
     * 根据注解名推断调用的框架
     */
    private String getFrameworkCallerFromAnnotation(String annotationName) {
        // Spring框架注解
        if (annotationName.equals("EventListener")) {
            return "Spring EventPublisher";
        }
        if (annotationName.equals("PostConstruct")) {
            return "Spring Container (initialization)";
        }
        if (annotationName.equals("PreDestroy")) {
            return "Spring Container (destruction)";
        }
        if (annotationName.equals("Scheduled")) {
            return "Spring Task Scheduler";
        }
        if (annotationName.equals("Async")) {
            return "Spring Async Executor";
        }
        if (annotationName.equals("Transactional")) {
            return "Spring Transaction Manager";
        }
        if (annotationName.contains("Mapping")) {  // GetMapping, PostMapping等
            return "Spring MVC DispatcherServlet";
        }
        if (annotationName.equals("JmsListener")) {
            return "Spring JMS Container";
        }
        if (annotationName.equals("KafkaListener")) {
            return "Spring Kafka Container";
        }
        if (annotationName.equals("RabbitListener")) {
            return "Spring AMQP Container";
        }
        
        // JPA回调注解
        if (annotationName.equals("PrePersist")) {
            return "JPA EntityManager (before persist)";
        }
        if (annotationName.equals("PostPersist")) {
            return "JPA EntityManager (after persist)";
        }
        if (annotationName.equals("PreUpdate")) {
            return "JPA EntityManager (before update)";
        }
        if (annotationName.equals("PostUpdate")) {
            return "JPA EntityManager (after update)";
        }
        if (annotationName.equals("PreRemove")) {
            return "JPA EntityManager (before remove)";
        }
        if (annotationName.equals("PostRemove")) {
            return "JPA EntityManager (after remove)";
        }
        if (annotationName.equals("PostLoad")) {
            return "JPA EntityManager (after load)";
        }
        
        // Servlet注解
        if (annotationName.equals("WebServlet")) {
            return "Servlet Container";
        }
        if (annotationName.equals("WebFilter")) {
            return "Servlet Container (filter chain)";
        }
        if (annotationName.equals("WebListener")) {
            return "Servlet Container (event)";
        }
        
        return null;  // 不是框架回调注解
    }
}