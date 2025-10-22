package com.java.extractor.diff;

import java.util.HashMap;
import java.util.Map;

/**
 * 变更检测器
 * 检测变更类型：ADD / MODIFY / DELETE
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
            Map<String, Map<String, String>> removedMethods) {

        Map<String, String> changes = new HashMap<>();

        // 检测新增和修改
        for (String signature : addedMethods.keySet()) {
            if (removedMethods.containsKey(signature)) {
                // 签名相同但内容可能不同 -> 修改
                changes.put(signature, "MODIFY");
                removedMethods.remove(signature);  // 避免重复处理
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
     *
     * @param addedFields key: fieldName, value: field info map
     * @param removedFields key: fieldName, value: field info map
     * @return Map of field changes: key=fieldName, value=changeType (ADD/MODIFY/DELETE)
     */
    public Map<String, String> detectFieldChanges(
            Map<String, Map<String, Object>> addedFields,
            Map<String, Map<String, Object>> removedFields) {

        Map<String, String> changes = new HashMap<>();

        // 检测新增和修改
        for (String fieldName : addedFields.keySet()) {
            if (removedFields.containsKey(fieldName)) {
                // 同名字段存在 -> 可能是修改
                Map<String, Object> added = addedFields.get(fieldName);
                Map<String, Object> removed = removedFields.get(fieldName);

                if (isFieldModified(added, removed)) {
                    changes.put(fieldName, "MODIFY");
                } else {
                    // 没有实质性修改，跳过
                }
                removedFields.remove(fieldName);  // 避免重复处理
            } else {
                // 纯新增
                changes.put(fieldName, "ADD");
            }
        }

        // 剩余的都是删除
        for (String fieldName : removedFields.keySet()) {
            changes.put(fieldName, "DELETE");
        }

        return changes;
    }

    /**
     * 判断字段是否被修改（不只是格式变化）
     */
    private boolean isFieldModified(Map<String, Object> newField, Map<String, Object> oldField) {
        // 比较类型
        String newType = (String) newField.get("type");
        String oldType = (String) oldField.get("type");
        if (newType != null && !newType.equals(oldType)) {
            return true;
        }

        // 比较是否为常量（修饰符变化）
        Boolean newIsConstant = (Boolean) newField.get("isConstant");
        Boolean oldIsConstant = (Boolean) oldField.get("isConstant");
        if (newIsConstant != null && !newIsConstant.equals(oldIsConstant)) {
            return true;
        }

        // 比较初始化值（字段值变化）
        String newValue = (String) newField.get("value");
        String oldValue = (String) oldField.get("value");

        // 如果两个值都存在，比较它们
        if (newValue != null && oldValue != null) {
            // 去除空白后比较，避免格式差异
            if (!newValue.trim().equals(oldValue.trim())) {
                return true;
            }
        } else if (newValue != null || oldValue != null) {
            // 一个有值一个没有值，也算修改
            return true;
        }

        // 没有实质性差异
        return false;
    }
}
