package com.element_endow.core;

import com.element_endow.api.*;
import com.element_endow.data.registry.DataRegistry;
import com.element_endow.data.UnifiedDataManager;
import com.element_endow.data.ElementDataService;
import com.google.gson.JsonElement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 元素系统实现
 * 与统一数据管理系统集成
 */
public class ElementSystemImpl implements IElementSystem {
    private static final Logger LOGGER = LogManager.getLogger();

    //核心组件
    private final DataRegistry dataRegistry;
    private final ElementRegistry elementRegistry;

    //子系统
    private final ElementReactionSystem reactionSystem;
    private final ElementCombinationSystem combinationSystem;
    private final ElementMountSystem mountSystem;

    //时效性修饰符管理
    private final Map<LivingEntity, Map<UUID, TimedModifierInfo>> timedModifiers = new WeakHashMap<>();

    //元素状态管理
    private final Set<String> disabledElements = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ElementSystemImpl() {
        LOGGER.info("Initializing Element System with unified data management...");

        //初始化数据注册中心（立即加载配置文件）
        this.dataRegistry = new DataRegistry();

        //初始化元素注册表（在Forge注册之前）
        this.elementRegistry = new ElementRegistry(
                dataRegistry.getDataManager(),
                dataRegistry.getDataService(),
                dataRegistry.getConfigManager()
        );

        //加载系统配置
        dataRegistry.loadSystemConfiguration();

        //初始化子系统
        this.reactionSystem = new ElementReactionSystem(this, dataRegistry.getDataService());
        this.combinationSystem = new ElementCombinationSystem(this, dataRegistry.getDataService());
        this.mountSystem = new ElementMountSystem(this);

        LOGGER.info("Element System initialization completed");
    }

    /**
     * 完成系统初始化（在数据包加载后调用）
     */
    public void finalizeInitialization() {
        dataRegistry.finalizeDataSystem();
        LOGGER.info("Element System fully initialized with all data sources");
    }

    /**
     * 注册数据包资源（由资源监听器调用）
     */
    public void registerDataPackResources(ResourceManager resourceManager,
                                          Map<ResourceLocation, JsonElement> resources) {
        dataRegistry.registerDataPackSources(resourceManager, resources);
    }

    // ========== IElementSystem 接口实现 ==========

    @Override
    public void registerElement(String elementId, String displayName,
                                double defaultValue, double minValue, double maxValue) {
        elementRegistry.registerElement(elementId, displayName, defaultValue, minValue, maxValue);
        // 新注册的元素默认是启用的
        disabledElements.remove(elementId);
    }

    @Override
    public boolean disableElement(String elementId) {
        if (!elementRegistry.isElementRegistered(elementId)) {
            LOGGER.warn("Cannot disable unregistered element: {}", elementId);
            return false;
        }

        boolean wasEnabled = !disabledElements.contains(elementId);
        disabledElements.add(elementId);

        if (wasEnabled) {
            LOGGER.info("Element disabled: {}", elementId);
            //可加更多禁用逻辑
        }

        return wasEnabled;
    }

    @Override
    public boolean enableElement(String elementId) {
        if (!elementRegistry.isElementRegistered(elementId)) {
            LOGGER.warn("Cannot enable unregistered element: {}", elementId);
            return false;
        }

        boolean wasDisabled = disabledElements.contains(elementId);
        disabledElements.remove(elementId);

        if (wasDisabled) {
            LOGGER.info("Element enabled: {}", elementId);
            //可加启用逻辑
        }

        return wasDisabled;
    }

    @Override
    public Collection<String> getRegisteredElements() {
        return elementRegistry.getRegisteredElementIds();
    }

    @Override
    public Collection<String> getEnabledElements() {
        Collection<String> registered = elementRegistry.getRegisteredElementIds();
        if (disabledElements.isEmpty()) {
            return registered;
        }

        // 过滤掉禁用的元素
        List<String> enabledElements = new ArrayList<>();
        for (String elementId : registered) {
            if (!disabledElements.contains(elementId)) {
                enabledElements.add(elementId);
            }
        }
        return enabledElements;
    }

    @Override
    public boolean isElementManaged(String elementId) {
        return elementRegistry.isElementRegistered(elementId);
    }

    @Override
    public boolean isElementRegistered(String elementId) {
        return elementRegistry.isElementRegistered(elementId);
    }

    @Override
    public double getElementValue(LivingEntity entity, String elementId) {
        // 如果元素被禁用，返回0
        if (disabledElements.contains(elementId)) {
            return 0.0;
        }

        Optional<Attribute> attribute = elementRegistry.getElementAttribute(elementId);
        if (attribute.isPresent()) {
            AttributeInstance instance = entity.getAttribute(attribute.get());
            return instance != null ? instance.getValue() : 0.0;
        }
        return 0.0;
    }

    @Override
    public void setElementValue(LivingEntity entity, String elementId, double value) {
        // 如果元素被禁用，不设置值
        if (disabledElements.contains(elementId)) {
            LOGGER.warn("Attempted to set value for disabled element: {}", elementId);
            return;
        }

        Optional<Attribute> attribute = elementRegistry.getElementAttribute(elementId);
        if (attribute.isPresent()) {
            AttributeInstance instance = entity.getAttribute(attribute.get());
            if (instance != null) {
                ElementRegistry.AttributeData data = elementRegistry.getAttributeData(elementId);
                if (data != null) {
                    double oldValue = instance.getBaseValue();
                    double clampedValue = Math.max(data.minValue, Math.min(data.maxValue, value));
                    instance.setBaseValue(clampedValue);

                    // 如果值发生变化，使组合缓存失效
                    if (Math.abs(oldValue - clampedValue) > 0.001) {
                        combinationSystem.invalidateEntityCache(entity);
                    }
                }
            }
        }
    }

    @Override
    public boolean hasElement(LivingEntity entity, String elementId) {
        // 如果元素被禁用，返回false
        if (disabledElements.contains(elementId)) {
            return false;
        }
        return getElementValue(entity, elementId) > 0;
    }

    @Override
    public DeferredRegister<Attribute> getAttributeRegister() {
        return ElementRegistry.ATTRIBUTES;
    }

    @Override
    public Optional<Attribute> getElementAttribute(String elementId) {
        // 如果元素被禁用，返回空
        if (disabledElements.contains(elementId)) {
            return Optional.empty();
        }
        return elementRegistry.getElementAttribute(elementId);
    }

    @Override
    public Optional<Attribute> getAttributeById(ResourceLocation attributeId) {
        if (attributeId == null) {
            return Optional.empty();
        }

        try {
            // 从全局注册表获取
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
            if (attribute != null) {
                return Optional.of(attribute);
            }

            // 从自定义注册表获取
            return getElementAttribute(attributeId.toString());

        } catch (Exception e) {
            LOGGER.error("Error getting attribute: {}", attributeId, e);
            return Optional.empty();
        }
    }

    @Override
    public double getAttributeValue(LivingEntity entity, ResourceLocation attributeId) {
        return getAttributeById(attributeId)
                .map(attribute -> {
                    AttributeInstance instance = entity.getAttribute(attribute);
                    return instance != null ? instance.getValue() : 0.0;
                })
                .orElse(0.0);
    }

    @Override
    public void setAttributeBaseValue(LivingEntity entity, ResourceLocation attributeId, double value) {
        getAttributeById(attributeId).ifPresent(attribute -> {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) {
                instance.setBaseValue(value);
            }
        });
    }

    @Override
    public boolean hasAttribute(LivingEntity entity, ResourceLocation attributeId) {
        return getAttributeById(attributeId)
                .map(attribute -> entity.getAttribute(attribute) != null)
                .orElse(false);
    }

    @Override
    public boolean applyAttributeModifier(LivingEntity entity,
                                          ResourceLocation attributeId,
                                          AttributeModifier modifier) {
        // 调用时效性方法，但设置持续时间为0
        return applyTimedAttributeModifier(entity, attributeId, modifier, 0);
    }

    @Override
    public boolean removeAttributeModifier(LivingEntity entity, ResourceLocation attributeId, UUID modifierId) {
        Optional<Attribute> attributeOpt = getAttributeById(attributeId);
        if (attributeOpt.isPresent()) {
            AttributeInstance instance = entity.getAttribute(attributeOpt.get());
            if (instance != null && instance.getModifier(modifierId) != null) {
                instance.removeModifier(modifierId);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean applyTimedAttributeModifier(LivingEntity entity, ResourceLocation attributeId,
                                               AttributeModifier modifier, int durationTicks) {
        try {
            Optional<Attribute> attributeOpt = getAttributeById(attributeId);
            if (attributeOpt.isEmpty()) {
                LOGGER.warn("Attribute not registered: {}", attributeId);
                return false;
            }

            AttributeInstance instance = entity.getAttribute(attributeOpt.get());
            if (instance == null) {
                LOGGER.warn("The entity has no attribute instances: {}, attribute: {}", entity, attributeId);
                return false;
            }

            // 记录应用前的值
            double beforeValue = instance.getValue();

            // 移除可能存在的相同UUID的旧修饰符
            if (instance.getModifier(modifier.getId()) != null) {
                instance.removeModifier(modifier.getId());
            }

            // 应用新修饰符
            instance.addTransientModifier(modifier);

            // 记录时效
            long currentTime = entity.level().getGameTime();
            TimedModifierInfo modifierInfo = new TimedModifierInfo(
                    attributeId,
                    modifier.getId(),
                    currentTime,
                    durationTicks,
                    modifier.getAmount()
            );

            Map<UUID, TimedModifierInfo> entityModifiers = timedModifiers.computeIfAbsent(entity, k -> new HashMap<>());
            entityModifiers.put(modifier.getId(), modifierInfo);

            // 验证修饰符应用
            boolean success = instance.getModifier(modifier.getId()) != null;
            double afterValue = instance.getValue();

            if (success) {
                LOGGER.debug("Timed attribute modifier applied: entity={}, attribute={}, value: {} -> {}, duration={} ticks",
                        entity, attributeId, beforeValue, afterValue, durationTicks);
            } else {
                LOGGER.error("Timed attribute modifier application failed: entity={}, attribute={}", entity, attributeId);
            }

            return success;

        } catch (Exception e) {
            LOGGER.error("Error applying timed attribute modifier: {}", attributeId, e);
            return false;
        }
    }

    @Override
    public void checkAndRemoveExpiredModifiers(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return;
        }

        Map<UUID, TimedModifierInfo> entityModifiers = timedModifiers.get(entity);
        if (entityModifiers == null || entityModifiers.isEmpty()) {
            return;
        }

        long currentTime = entity.level().getGameTime();
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, TimedModifierInfo> entry : entityModifiers.entrySet()) {
            TimedModifierInfo info = entry.getValue();

            if (info.isExpired(currentTime)) {
                // 移除过期的修饰符
                removeAttributeModifier(entity, info.attributeId, info.modifierId);
                toRemove.add(entry.getKey());

                LOGGER.debug("Removed expired attribute modifier: entity={}, attribute={}, value={}",
                        entity, info.attributeId, info.value);
            }
        }

        // 清理记录
        for (UUID modifierId : toRemove) {
            entityModifiers.remove(modifierId);
        }

        if (entityModifiers.isEmpty()) {
            timedModifiers.remove(entity);
        }
    }

    @Override
    public Map<String, TimedModifierInfo> getActiveModifierInfo(LivingEntity entity) {
        Map<String, TimedModifierInfo> result = new HashMap<>();
        Map<UUID, TimedModifierInfo> entityModifiers = timedModifiers.get(entity);

        if (entityModifiers != null) {
            long currentTime = entity.level().getGameTime();
            for (TimedModifierInfo info : entityModifiers.values()) {
                String key = info.attributeId + ":" + info.modifierId;
                result.put(key, info);
            }
        }

        return result;
    }

    @Override
    public IElementReactionSystem getReactionSystem() {
        return reactionSystem;
    }

    @Override
    public IElementCombinationSystem getCombinationSystem() {
        return combinationSystem;
    }

    @Override
    public IElementMountSystem getMountSystem() {
        return mountSystem;
    }

    @Override
    public void reloadData() {
        //重新加载反应数据
        reactionSystem.reloadReactions();

        //重新加载组合数据
        combinationSystem.reloadCombinations();

        //清除禁用状态（可选）
        //disabledElements.clear();

        LOGGER.info("Element system data reloaded");
    }

    /**
     * 获取禁用元素的数量（用于调试）
     */
    public int getDisabledElementCount() {
        return disabledElements.size();
    }

    /**
     * 检查元素是否被禁用
     */
    public boolean isElementDisabled(String elementId) {
        return disabledElements.contains(elementId);
    }

    // 数据服务访问器
    public ElementDataService getDataService() {
        return dataRegistry.getDataService();
    }

    public UnifiedDataManager getDataManager() {
        return dataRegistry.getDataManager();
    }

    public DataRegistry getDataRegistry() {
        return dataRegistry;
    }

    public ElementRegistry getElementRegistry() {
        return elementRegistry;
    }

    //保持向后兼容的方法
    public ElementRegistry getRegistry() {
        return this.elementRegistry;
    }

    private String formatDisplayName(String elementId) {
        String[] parts = elementId.split(":");
        if (parts.length == 2) {
            return parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1).toLowerCase();
        }
        return elementId;
    }
}