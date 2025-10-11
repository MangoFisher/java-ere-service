package com.java.ere; // 包路径更新

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CodeParser {
    public void init(String projectPath) {
        TypeSolver typeSolver = new CombinedTypeSolver(
            new JavaParserTypeSolver(new File(projectPath)),
            new ReflectionTypeSolver()
        );
        ParserConfiguration config = new ParserConfiguration()
            .setSymbolResolver(new JavaSymbolSolver(typeSolver));
        StaticJavaParser.setConfiguration(config);
    }

    public Map<String, Entity> parseFile(File javaFile) throws Exception {
        Map<String, Entity> entities = new HashMap<>();
        CompilationUnit cu = StaticJavaParser.parse(javaFile);

        extractClassesAndInterfaces(cu, entities);
        extractMethods(cu, entities);
        extractFields(cu, entities);
        buildRelations(cu, entities);

        return entities;
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
            String id = "method_" + className + "_" + methodName;
            Entity methodEntity = new Entity(id, "Method");
            methodEntity.addProperty("name", methodName);
            methodEntity.addProperty("owner", className);
            methodEntity.addProperty("business_role", extractJavadoc(methodDecl.getJavadoc()));
            entities.put(id, methodEntity);

            methodDecl.getParameters().forEach(param -> {
                String paramName = param.getNameAsString();
                String paramId = "param_" + className + "_" + methodName + "_" + paramName;
                Entity paramEntity = new Entity(paramId, "Parameter");
                paramEntity.addProperty("name", paramName);
                paramEntity.addProperty("type", param.getType().asString());
                entities.put(paramId, paramEntity);
                methodEntity.addRelation("has_parameter", paramId);
            });

            String returnId = "return_" + className + "_" + methodName;
            Entity returnEntity = new Entity(returnId, "Return");
            returnEntity.addProperty("type", methodDecl.getType().asString());
            entities.put(returnId, returnEntity);
            methodEntity.addRelation("returns", returnId);
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
        cu.findAll(MethodDeclaration.class).forEach(methodDecl -> {
            String methodName = methodDecl.getNameAsString();
            String className = methodDecl.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(ClassOrInterfaceDeclaration::getNameAsString)
                .orElse("Unknown");
            String methodId = "method_" + className + "_" + methodName;
            Entity methodEntity = entities.get(methodId);
            if (methodEntity == null) return;

            methodDecl.findAll(MethodCallExpr.class).forEach(callExpr -> {
                try {
                    ResolvedMethodDeclaration resolved = callExpr.resolve();
                    String calleeClass = resolved.declaringType().getClassName();
                    String calleeMethod = resolved.getName();
                    String calleeId = "method_" + calleeClass + "_" + calleeMethod;
                    if (entities.containsKey(calleeId)) {
                        methodEntity.addRelation("calls", calleeId);
                    }
                } catch (Exception e) { /* 忽略无法解析的调用 */ }
            });
        });
    }

    private String extractJavadoc(Optional<Javadoc> javadocOpt) {
        return javadocOpt.map(jc -> jc.toText().trim()).orElse("无描述");
    }
}