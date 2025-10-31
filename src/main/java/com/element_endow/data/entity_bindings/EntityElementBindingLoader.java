package com.element_endow.data.entity_bindings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生物元素绑定加载器
 * 从数据包加载生物初始元素配置
 */
public class EntityElementBindingLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private final Map<EntityType<?>, EntityElementBinding> entityBindings = new HashMap<>();
    private final Map<EntityType<?>, EntityElementBinding> runtimeBindings = new ConcurrentHashMap<>();
    private final Map<String, EntityElementBinding> pendingRuntimeBindings = new ConcurrentHashMap<>();

    public void loadBindings() {
        entityBindings.clear();
    }

    public void loadFromResources(Map<ResourceLocation, JsonElement> resources) {
        entityBindings.clear();

        int loadedCount = 0;
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();

            if (!location.getNamespace().equals("element_endow")) {
                continue;
            }

            try {
                EntityElementBinding binding = GSON.fromJson(entry.getValue(), EntityElementBinding.class);

                if (validateBinding(binding)) {
                    ResourceLocation entityTypeId = ResourceLocation.tryParse(binding.entityType);
                    if (entityTypeId != null) {
                        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityTypeId);
                        if (entityType != null) {
                            entityBindings.put(entityType, binding);
                            loadedCount++;
                        } else {
                            LOGGER.warn("Unknown entity type: {}", binding.entityType);
                            errorCount++;
                        }
                    } else {
                        LOGGER.warn("Invalid entity type format: {}", binding.entityType);
                        errorCount++;
                    }
                } else {
                    LOGGER.warn("Invalid entity binding data: {}", location);
                    errorCount++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load entity binding {}: {}", location, e.getMessage());
                errorCount++;
            }
        }
    }

    private boolean validateBinding(EntityElementBinding binding) {
        if (binding.entityType == null || binding.entityType.isEmpty()) {
            LOGGER.error("Entity binding missing entity type");
            return false;
        }

        if (binding.elements == null) {
            binding.elements = new HashMap<>();
        }

        //确保条件有效
        if (binding.conditions != null && binding.conditions.isEmpty()) {
            binding.conditions = null;//空条件视为无条件
        }

        return true;
    }

    /**
     * 在运行时为实体类型添加元素绑定
     */
    public void addRuntimeBinding(EntityType<?> entityType, EntityElementBinding binding) {
        if (entityType == null || binding == null) {
            LOGGER.warn("Attempted to add null entity type or binding");
            return;
        }

        //验证绑定数据
        if (!validateBinding(binding)) {
            LOGGER.error("Invalid runtime binding for entity type: {}", entityType);
            return;
        }

        runtimeBindings.put(entityType, binding);
    }

    /**
     * 通过实体类型ID添加运行时绑定（延迟解析）
     */
    public void addRuntimeBinding(String entityTypeId, EntityElementBinding binding) {
        if (entityTypeId == null || entityTypeId.isEmpty()) {
            LOGGER.warn("Attempted to add binding with null entity type ID");
            return;
        }

        pendingRuntimeBindings.put(entityTypeId, binding);
    }

    /**
     * 解析所有待处理的运行时绑定
     */
    public void resolvePendingBindings() {
        if (pendingRuntimeBindings.isEmpty()) {
            return;
        }

        int resolved = 0;
        int failed = 0;

        for (Map.Entry<String, EntityElementBinding> entry : pendingRuntimeBindings.entrySet()) {
            try {
                ResourceLocation entityId = ResourceLocation.tryParse(entry.getKey());
                if (entityId != null) {
                    // 使用 ForgeRegistries 替代弃用的 BuiltInRegistries.ENTITY_TYPE
                    EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
                    if (entityType != null) {
                        runtimeBindings.put(entityType, entry.getValue());
                        resolved++;
                    } else {
                        LOGGER.warn("Unknown entity type for pending binding: {}", entityId);
                        failed++;
                    }
                } else {
                    LOGGER.warn("Invalid entity type ID format: {}", entry.getKey());
                    failed++;
                }
            } catch (Exception e) {
                LOGGER.error("Error resolving pending binding for: {}", entry.getKey(), e);
                failed++;
            }
        }

        pendingRuntimeBindings.clear();
    }

    /**
     * 移除实体类型的运行时绑定
     */
    public boolean removeRuntimeBinding(EntityType<?> entityType) {
        return runtimeBindings.remove(entityType) != null;
    }

    public Map<EntityType<?>, EntityElementBinding> getEntityBindings() {
        return new HashMap<>(entityBindings);
    }

    /**
     * 获取实体类型的绑定（合并数据包绑定和运行时绑定）
     */
    public EntityElementBinding getBindingForEntity(EntityType<?> entityType) {
        //优先返回运行时绑定
        EntityElementBinding runtimeBinding = runtimeBindings.get(entityType);
        if (runtimeBinding != null) {
            return runtimeBinding;
        }

        //返回数据包绑定
        return entityBindings.get(entityType);
    }

    /**
     * 检查实体类型是否有任何绑定
     */
    public boolean hasBindingForEntity(EntityType<?> entityType) {
        return runtimeBindings.containsKey(entityType) || entityBindings.containsKey(entityType);
    }

    /**
     * 获取所有绑定（包括运行时和数据包）
     */
    public Map<EntityType<?>, EntityElementBinding> getAllBindings() {
        Map<EntityType<?>, EntityElementBinding> allBindings = new HashMap<>();
        allBindings.putAll(entityBindings); // 数据包绑定
        allBindings.putAll(runtimeBindings); // 运行时绑定（覆盖数据包绑定）
        return allBindings;
    }

    /**
     * 清空所有运行时绑定
     */
    public void clearRuntimeBindings() {
        runtimeBindings.clear();
        pendingRuntimeBindings.clear();
    }

    /**
     * 获取运行时绑定统计信息
     */
    public RuntimeBindingStats getRuntimeBindingStats() {
        return new RuntimeBindingStats(
                runtimeBindings.size(),
                pendingRuntimeBindings.size(),
                entityBindings.size()
        );
    }

    public static class RuntimeBindingStats {
        public final int activeRuntimeBindings;
        public final int pendingRuntimeBindings;
        public final int dataPackBindings;

        public RuntimeBindingStats(int activeRuntimeBindings, int pendingRuntimeBindings, int dataPackBindings) {
            this.activeRuntimeBindings = activeRuntimeBindings;
            this.pendingRuntimeBindings = pendingRuntimeBindings;
            this.dataPackBindings = dataPackBindings;
        }
    }

    public static class EntityElementBinding {
        public String entityType;
        public Map<String, Double> elements = new HashMap<>();
        public boolean persistent = true;//是否持久化
        public int priority = 0;//优先级，数值高覆盖数值低的
        public Map<String, Object> conditions;//应用条件，null表示无条件

        public EntityElementBinding() {}

        public EntityElementBinding(String entityType, Map<String, Double> elements) {
            this.entityType = entityType;
            this.elements = elements;
        }

        /**
         * 检查是否有条件
         */
        public boolean hasConditions() {
            return conditions != null && !conditions.isEmpty();
        }
    }
}