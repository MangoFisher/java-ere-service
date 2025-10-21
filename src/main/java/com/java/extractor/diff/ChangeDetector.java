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
        
        // 其他差异也认为是修改
        Boolean newIsConstant = (Boolean) newField.get("isConstant");
        Boolean oldIsConstant = (Boolean) oldField.get("isConstant");
        return (newIsConstant != null && !newIsConstant.equals(oldIsConstant));
    }
}
