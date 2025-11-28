package com.element_endow.data.source;

/**
 * 数据类型枚举
 * 定义系统中所有可管理的数据类型
 */
public enum DataType {
    ELEMENTS("elements", "元素定义"),
    REACTIONS("reactions", "元素反应"),
    COMBINATIONS("combinations", "元素组合"),
    ENTITY_BINDINGS("entity_bindings", "实体绑定"),
    MOUNT_EFFECTS("mount_effects", "挂载效果"),
    ATTRIBUTE_MODIFIERS("attribute_modifiers", "属性修饰符"),
    DEBUG_CONFIGS("debug_configs", "调试配置");

    private final String id;
    private final String displayName;

    DataType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DataType fromId(String id) {
        for (DataType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown data type: " + id);
    }

    /**
     * 获取数据目录路径
     */
    public String getDataPath() {
        return id;
    }
}