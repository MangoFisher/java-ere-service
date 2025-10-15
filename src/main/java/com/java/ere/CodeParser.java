package com.java.ere;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.java.ere.config.ResolverConfig;
import com.java.ere.config.ExtractionConfig;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
                Map<String, Entity> entities = extractEntitiesFromFile(cu);
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
    private Map<String, Entity> extractEntitiesFromFile(CompilationUnit cu) {
        Map<String, Entity> entities = new HashMap<>();
        
        // 根据配置决定提取哪些实体
        if (extractionConfig.isEntityEnabled("ClassOrInterface")) {
            extractClassesAndInterfaces(cu, entities);
        }
        
        if (extractionConfig.isEntityEnabled("Method")) {
            extractMethods(cu, entities);
        }
        
        if (extractionConfig.isEntityEnabled("Field")) {
            extractFields(cu, entities);
        }

        return entities;
    }
    
    /**
     * 单个文件解析（用于调试）
     */
    public Map<String, Entity> parseFile(File javaFile) throws Exception {
        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        return extractEntitiesFromFile(cu);
    }

    private void extractClassesAndInterfaces(CompilationUnit cu, Map<String, Entity> entities) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String name = classDecl.getNameAsString();
            String prefix = classDecl.isInterface() ? "iface_" : "class_";
            Entity entity = new Entity(prefix + name, "ClassOrInterface");
            entity.addProperty("name", name);
            entity.addProperty("isInterface", String.valueOf(classDecl.isInterface()));
            entity.addProperty("purpose", extractJavadoc(classDecl.getJavadoc()));
            entities.put(entity.getId(), entity);
        });
    }

    private void extractMethods(CompilationUnit cu, Map<String, Entity> entities) {
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
            
            // 根据配置决定是否提取Javadoc
            if (extractionConfig.isIncludeJavadoc()) {
                methodEntity.addProperty("business_role", extractJavadoc(methodDecl.getJavadoc()));
            }
            
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

    private void extractFields(CompilationUnit cu, Map<String, Entity> entities) {
        cu.findAll(FieldDeclaration.class).forEach(fieldDecl -> {
            String fieldName = fieldDecl.getVariable(0).getNameAsString();
            String className = fieldDecl.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse("Unknown");
            String id = "field_" + className + "_" + fieldName;
            Entity fieldEntity = new Entity(id, "Field");
            fieldEntity.addProperty("name", fieldName);
            fieldEntity.addProperty("type", fieldDecl.getVariable(0).getType().asString());
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
                        handleThirdPartyCall(methodEntity, declaringClass, calleeMethod);
                    }
                } catch (Exception e) {
                    // 符号解析失败时，尝试简单匹配（用于同类方法调用）
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
    private void handleThirdPartyCall(Entity methodEntity, String declaringClass, String methodName) {
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
        }
        // "full" 策略暂不实现
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
                if (entityId.startsWith(prefix)) {
                    // 找到可能的匹配，建立调用关系
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
}