package com.element_endow.data.entity_bindings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 生物元素绑定加载器
 * 从数据包加载生物初始元素配置
 */
public class EntityElementBindingLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private final Map<EntityType<?>, EntityElementBinding> entityBindings = new HashMap<>();

    public void loadBindings() {
        entityBindings.clear();
        LOGGER.info("Entity element binding system initialized");
    }

    public void loadFromResources(Map<ResourceLocation, JsonElement> resources) {
        entityBindings.clear();

        int loadedCount = 0;
        int errorCount = 0;

        LOGGER.info("Processing {} entity element binding resources", resources.size());

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();

            if (!location.getNamespace().equals("element_endow")) {
                continue;
            }

            LOGGER.info("Loading entity binding file: {}", location);
            try {
                EntityElementBinding binding = GSON.fromJson(entry.getValue(), EntityElementBinding.class);

                if (validateBinding(binding)) {
                    EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(binding.entityType));
                    if (entityType != null) {
                        entityBindings.put(entityType, binding);
                        loadedCount++;
                        LOGGER.info("Successfully loaded entity binding for: {}", binding.entityType);
                    } else {
                        LOGGER.warn("Unknown entity type: {}", binding.entityType);
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

        LOGGER.info("Loaded {} entity element bindings from resources ({} errors)", loadedCount, errorCount);
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

    public Map<EntityType<?>, EntityElementBinding> getEntityBindings() {
        return new HashMap<>(entityBindings);
    }

    public EntityElementBinding getBindingForEntity(EntityType<?> entityType) {
        return entityBindings.get(entityType);
    }

    public boolean hasBindingForEntity(EntityType<?> entityType) {
        return entityBindings.containsKey(entityType);
    }

    public static class EntityElementBinding {
        public String entityType;
        public Map<String, Double> elements = new HashMap<>();
        public boolean persistent = true;//是否持久化（死亡后重新生成时是否重新应用）
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