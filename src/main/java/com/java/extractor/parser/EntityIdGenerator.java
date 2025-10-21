package com.java.extractor.parser;

/**
 * 实体ID生成器
 * 与ChangeInfo.toEntityId()保持一致的ID格式
 */
public class EntityIdGenerator {
    
    /**
     * 生成方法实体ID
     * 格式: method_ClassName_methodName(ParamType1,ParamType2)
     */
    public static String generateMethodId(String className, String methodName, String signature) {
        if (signature == null) {
            signature = "";
        }
        return "method_" + className + "_" + methodName + "(" + signature + ")";
    }
    
    /**
     * 生成字段实体ID
     * 格式: field_ClassName_fieldName
     */
    public static String generateFieldId(String className, String fieldName) {
        return "field_" + className + "_" + fieldName;
    }
    
    /**
     * 生成类实体ID
     * 格式: class_ClassName
     */
    public static String generateClassId(String className) {
        return "class_" + className;
    }
    
    /**
     * 生成接口实体ID
     * 格式: iface_InterfaceName
     */
    public static String generateInterfaceId(String interfaceName) {
        return "iface_" + interfaceName;
    }
}
