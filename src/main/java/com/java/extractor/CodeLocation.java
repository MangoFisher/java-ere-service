package com.java.extractor;

import java.util.Objects;

/**
 * 代码位置 - 用于标识类中的方法
 */
public class CodeLocation {
    private final String className;
    private final String methodName;
    private final String signature; // 可选：用于区分重载方法
    
    public CodeLocation(String className, String methodName) {
        this(className, methodName, null);
    }
    
    public CodeLocation(String className, String methodName, String signature) {
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
    }
    
    public String getClassName() {
        return className;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public String getSignature() {
        return signature;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeLocation that = (CodeLocation) o;
        return Objects.equals(className, that.className) &&
               Objects.equals(methodName, that.methodName) &&
               Objects.equals(signature, that.signature);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, signature);
    }
    
    @Override
    public String toString() {
        if (signature != null) {
            return className + "." + methodName + signature;
        }
        return className + "." + methodName;
    }
}
