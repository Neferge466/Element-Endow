package com.element_endow.util;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.api.IElementMountSystem;
import com.element_endow.data.ElementDataManager;
import com.element_endow.data.entity_bindings.EntityElementBindingLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

import static com.element_endow.ElementEndow.LOGGER;

/**
 * 为其他模组开发者提供的便捷工具类
 * 简化元素系统的常见操作
 */
public class ElementHelper {

    /**
     * 为单个生物添加元素值
     */
    public static void addElementToEntity(LivingEntity entity, String elementId, double value) {
        var elementSystem = ElementSystemAPI.getElementSystem();
        if (!elementSystem.isElementRegistered(elementId)) {
            // 自动注册元素（可选）
            elementSystem.registerElement(elementId, formatDisplayName(elementId), 0.0, 0.0, 1024.0);
        }
        elementSystem.setElementValue(entity, elementId, value);
    }

    /**
     * 为生物类型添加默认元素绑定
     * 用于快速绑定
     */
    public static void addElementToEntityType(EntityType<? extends LivingEntity> entityType,
                                              String elementId, double value) {
        addElementToEntityType(entityType, elementId, value, true, 0, null);
    }


    /**
     * 为生物类型添加元素绑定（完整参数）
     * 支持所有参数配置
     */
    public static void addElementToEntityType(EntityType<? extends LivingEntity> entityType,
                                              String elementId, double value,
                                              boolean persistent, int priority,
                                              Map<String, Object> conditions) {
        try {
            var elementSystem = ElementSystemAPI.getElementSystem();
            var bindingLoader = ElementDataManager.getEntityBindingLoader();

            //确保元素已注册
            if (!elementSystem.isElementRegistered(elementId)) {
                LOGGER.warn("Element {} is not registered, cannot add binding for entity type {}. Please register the element first.",
                        elementId, entityType);
                return;
            }

            //获取实体类型的注册表名称
            ResourceLocation entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
            if (entityTypeId == null) {
                LOGGER.error("Failed to get entity type ID for: {}", entityType);
                return;
            }

            // 创建绑定配置
            var binding = new EntityElementBindingLoader.EntityElementBinding();
            binding.entityType = entityTypeId.toString();
            binding.elements = new HashMap<>();
            binding.elements.put(elementId, value);
            binding.persistent = persistent;
            binding.priority = priority;
            binding.conditions = conditions;

            //添加到运行时绑定
            bindingLoader.addRuntimeBinding(entityType, binding);

            LOGGER.info("Added element binding for entity type {}: {} = {}",
                    entityTypeId, elementId, value);

        } catch (Exception e) {
            LOGGER.error("Failed to add element binding for entity type: {}", entityType, e);
        }
    }




    /**
     * 为生物类型添加多个元素绑定
     * 快速绑定多个元素
     */
    public static void addElementsToEntityType(EntityType<? extends LivingEntity> entityType,
                                               Map<String, Double> elements) {
        addElementsToEntityType(entityType, elements, true, 0, null);
    }

    /**
     * 为生物类型添加多个元素绑定（完整参数）
     * 支持所有参数配置
     */
    public static void addElementsToEntityType(EntityType<? extends LivingEntity> entityType,
                                               Map<String, Double> elements,
                                               boolean persistent, int priority,
                                               Map<String, Object> conditions) {
        try {
            var bindingLoader = ElementDataManager.getEntityBindingLoader();

            //获取实体类型的注册表名称
            ResourceLocation entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
            if (entityTypeId == null) {
                LOGGER.error("Failed to get entity type ID for: {}", entityType);
                return;
            }

            //创建绑定配置
            var binding = new EntityElementBindingLoader.EntityElementBinding();
            binding.entityType = entityTypeId.toString();
            binding.elements = new HashMap<>(elements);
            binding.persistent = persistent;
            binding.priority = priority;
            binding.conditions = conditions;
            //添加到运行时绑定
            bindingLoader.addRuntimeBinding(entityType, binding);

            LOGGER.info("Added {} element bindings for entity type {}",
                    elements.size(), entityTypeId);
        } catch (Exception e) {
            LOGGER.error("Failed to add element bindings for entity type: {}", entityType, e);
        }
    }

    /**
     * 移除生物类型的元素绑定
     */
    public static boolean removeElementBinding(EntityType<? extends LivingEntity> entityType) {
        try {
            var bindingLoader = ElementDataManager.getEntityBindingLoader();
            return bindingLoader.removeRuntimeBinding(entityType);
        } catch (Exception e) {
            LOGGER.error("Failed to remove element binding for entity type: {}", entityType, e);
            return false;
        }
    }

    /**
     * 检查生物类型是否有元素绑定
     */
    public static boolean hasElementBinding(EntityType<? extends LivingEntity> entityType) {
        try {
            var bindingLoader = ElementDataManager.getEntityBindingLoader();
            return bindingLoader.hasBindingForEntity(entityType);
        } catch (Exception e) {
            LOGGER.error("Failed to check element binding for entity type: {}", entityType, e);
            return false;
        }
    }

    /**
     * 获取绑定统计信息
     */
    public static EntityElementBindingLoader.RuntimeBindingStats getBindingStats() {
        try {
            var bindingLoader = ElementDataManager.getEntityBindingLoader();
            return bindingLoader.getRuntimeBindingStats();
        } catch (Exception e) {
            LOGGER.error("Failed to get binding stats", e);
            return new EntityElementBindingLoader.RuntimeBindingStats(0, 0, 0);
        }
    }

    /**
     * 为生物添加挂载效果
     */
    public static void applyMountEffect(LivingEntity target, String elementId,
                                        double amount, int duration) {
        var mountSystem = ElementSystemAPI.getMountSystem();
        mountSystem.applyMount(target, elementId, amount, duration, 1.0, "refresh");
    }

    /**
     * 批量为多个生物添加元素
     * 这个方法用于为已存在的多个实体批量添加元素
     */
    public static void addElementToEntities(Iterable<LivingEntity> entities,
                                            String elementId, double value) {
        for (LivingEntity entity : entities) {
            addElementToEntity(entity, elementId, value);
        }
    }

    /**
     * 检查生物是否拥有特定元素
     */
    public static boolean hasElement(LivingEntity entity, String elementId) {
        return ElementSystemAPI.getElementSystem().hasElement(entity, elementId);
    }

    /**
     * 获取生物的元素值
     */
    public static double getElementValue(LivingEntity entity, String elementId) {
        return ElementSystemAPI.getElementSystem().getElementValue(entity, elementId);
    }

    /**
     * 创建高级挂载数据（Builder模式）
     */
    public static AdvancedMountBuilder createAdvancedMount(String elementId, double amount, int duration) {
        return new AdvancedMountBuilder(elementId, amount, duration);
    }

    //Builder类用于创建高级挂载配置
    public static class AdvancedMountBuilder {
        private final IElementMountSystem.AdvancedMountData mountData;

        public AdvancedMountBuilder(String elementId, double baseAmount, int baseDuration) {
            this.mountData = new IElementMountSystem.AdvancedMountData(elementId, baseAmount, baseDuration);
        }

        public AdvancedMountBuilder withProbability(double probability) {
            mountData.probability = probability;
            return this;
        }

        public AdvancedMountBuilder withStackBehavior(String stackBehavior) {
            mountData.stackBehavior = stackBehavior;
            return this;
        }

        public AdvancedMountBuilder withMaxStacks(int maxStacks) {
            mountData.maxStacks = maxStacks;
            return this;
        }

        public IElementMountSystem.AdvancedMountData build() {
            return mountData;
        }
    }

    private static String formatDisplayName(String elementId) {
        //简单的显示名称格式化
        String[] parts = elementId.split(":");
        if (parts.length == 2) {
            return parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1);
        }
        return elementId;
    }
}