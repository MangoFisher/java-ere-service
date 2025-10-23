package com.java.extractor.diff;

import java.util.HashMap;
import java.util.Map;

/**
 * 变更检测器
 * 检测变更类型：ADD / DELETE（Field 只支持 ADD/DELETE，Method 支持 ADD/MODIFY/DELETE）
 */
public class ChangeDetector {

    /**
     * 检测方法变更类型
     *
     * @param addedMethods key: methodName(signature), value: method info map
     * @param removedMethods key: methodName(signature), value: method info map
     * @return Map of method changes: key=signature, value=changeType (ADD/MODIFY/DELETE)
     */
    public Map<String, String> detectMethodChanges(
        Map<String, Map<String, String>> addedMethods,
        Map<String, Map<String, String>> removedMethods
    ) {
        Map<String, String> changes = new HashMap<>();

        // 检测新增和修改
        for (String signature : addedMethods.keySet()) {
            if (removedMethods.containsKey(signature)) {
                // 签名相同但内容可能不同 -> 修改
                changes.put(signature, "MODIFY");
                removedMethods.remove(signature); // 避免重复处理
            } else {
                // 纯新增
                changes.put(signature, "ADD");
            }
        }

        // 剩余的都是删除
        for (String signature : removedMethods.keySet()) {
            changes.put(signature, "DELETE");
        }

        return changes;
    }

    /**
     * 检测字段变更类型
     * 注意：字段只支持 ADD 和 DELETE 两种类型，不支持 MODIFY
     * 字段的修改会被拆分为 DELETE（旧字段）+ ADD（新字段）
     *
     * @param addedFields key: fieldName, value: field info map
     * @param removedFields key: fieldName, value: field info map
     * @return Map of field changes: key=fieldName (如果同名字段有修改，key为"fieldName#DELETE"和"fieldName#ADD"), value=changeType (ADD/DELETE)
     */
    public Map<String, String> detectFieldChanges(
        Map<String, Map<String, Object>> addedFields,
        Map<String, Map<String, Object>> removedFields
    ) {
        Map<String, String> changes = new HashMap<>();

        // 处理新增的字段
        for (String fieldName : addedFields.keySet()) {
            if (removedFields.containsKey(fieldName)) {
                // 同名字段既有添加又有删除，拆分为两条记录
                changes.put(fieldName + "#DELETE", "DELETE");
                changes.put(fieldName + "#ADD", "ADD");
            } else {
                // 纯新增
                changes.put(fieldName, "ADD");
            }
        }

        // 处理删除的字段（排除已经处理过的同名字段）
        for (String fieldName : removedFields.keySet()) {
            if (!addedFields.containsKey(fieldName)) {
                // 纯删除
                changes.put(fieldName, "DELETE");
            }
        }

        return changes;
    }
}
