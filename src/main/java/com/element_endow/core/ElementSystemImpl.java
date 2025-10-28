package com.element_endow.core;

import com.element_endow.api.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ElementSystemImpl implements IElementSystem {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ElementRegistry registry;
    private final ElementConfig config;
    private final Set<String> disabledElements;
    private final ElementReactionSystem reactionSystem;
    private final ElementCombinationSystem combinationSystem;
    private final ElementMountSystem mountSystem;

    //时效性修饰符管理
    private final Map<LivingEntity, Map<UUID, TimedModifierInfo>> timedModifiers = new WeakHashMap<>();


    public ElementSystemImpl() {
        this.registry = new ElementRegistry();
        this.config = new ElementConfig();
        this.disabledElements = ConcurrentHashMap.newKeySet();
        this.reactionSystem = new ElementReactionSystem(this);
        this.combinationSystem = new ElementCombinationSystem(this);
        this.mountSystem = new ElementMountSystem(this);

        initializeFromConfig();
    }

    private void initializeFromConfig() {
        for (String elementId : config.getElements()) {
            if (!registry.isElementRegistered(elementId)) {
                registerElement(elementId, formatDisplayName(elementId), 0.0, 0.0, 1024.0);
            }
        }
    }

    @Override
    public void registerElement(String elementId, String displayName,
                                double defaultValue, double minValue, double maxValue) {
        if (registry.isElementRegistered(elementId)) {
            return;
        }

        if (registry.registerElement(elementId, displayName, defaultValue, minValue, maxValue)) {
            config.addElement(elementId);
            disabledElements.remove(elementId);
        }
    }

    @Override
    public boolean disableElement(String elementId) {
        if (!registry.isElementRegistered(elementId)) {
            return false;
        }
        return disabledElements.add(elementId);
    }

    @Override
    public boolean enableElement(String elementId) {
        if (!registry.isElementRegistered(elementId)) {
            return false;
        }
        return disabledElements.remove(elementId);
    }

    @Override
    public Collection<String> getRegisteredElements() {
        return registry.getRegisteredElementIds();
    }

    @Override
    public Collection<String> getEnabledElements() {
        Set<String> enabled = new HashSet<>(registry.getRegisteredElementIds());
        enabled.removeAll(disabledElements);
        return enabled;
    }

    @Override
    public boolean isElementManaged(String elementId) {
        return registry.isElementRegistered(elementId) && !disabledElements.contains(elementId);
    }

    @Override
    public boolean isElementRegistered(String elementId) {
        return registry.isElementRegistered(elementId);
    }

    @Override
    public double getElementValue(LivingEntity entity, String elementId) {
        Optional<Attribute> attribute = registry.getElementAttribute(elementId);
        if (attribute.isPresent()) {
            AttributeInstance instance = entity.getAttribute(attribute.get());
            return instance != null ? instance.getValue() : 0.0;
        }
        return 0.0;
    }

    @Override
    public void setElementValue(LivingEntity entity, String elementId, double value) {
        Optional<Attribute> attribute = registry.getElementAttribute(elementId);
        if (attribute.isPresent()) {
            AttributeInstance instance = entity.getAttribute(attribute.get());
            if (instance != null) {
                ElementRegistry.AttributeData data = registry.getAttributeData(elementId);
                if (data != null) {
                    double clampedValue = Math.max(data.minValue, Math.min(data.maxValue, value));
                    instance.setBaseValue(clampedValue);
                }
            }
        }
    }

    @Override
    public boolean hasElement(LivingEntity entity, String elementId) {
        return getElementValue(entity, elementId) > 0;
    }

    @Override
    public DeferredRegister<Attribute> getAttributeRegister() {
        return ElementRegistry.ATTRIBUTES;
    }

    @Override
    public Optional<Attribute> getElementAttribute(String elementId) {
        return registry.getElementAttribute(elementId);
    }

    @Override
    public Optional<Attribute> getAttributeById(ResourceLocation attributeId) {
        if (attributeId == null) {
            return Optional.empty();
        }

        try {
            //从全局注册表获取
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
            if (attribute != null) {
                return Optional.of(attribute);
            }

            //从自定义注册表获取
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
        //调用时效性方法，但设置持续时间为0
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


    //时效性属性修饰符方法
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

            //记录应用前的值
            double beforeValue = instance.getValue();

            //移除可能存在的相同UUID的旧修饰符
            if (instance.getModifier(modifier.getId()) != null) {
                instance.removeModifier(modifier.getId());
            }

            //应用新修饰符
            instance.addTransientModifier(modifier);

            //记录时效
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

            //验证修饰符应用
            boolean success = instance.getModifier(modifier.getId()) != null;
            double afterValue = instance.getValue();

            if (success) {
                LOGGER.info("The timeliness attribute modifier was applied successfully: entity={}, attribute={}, value changed: {} -> {}, duration={}tick",
                        entity, attributeId, beforeValue, afterValue, durationTicks);
            } else {
                LOGGER.error("The timeliness attribute modifier application failed: entity={}, attribute={}", entity, attributeId);
            }

            return success;

        } catch (Exception e) {
            LOGGER.error("Error applying time-sensitivity attribute modifier: {}", attributeId, e);
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
                //移除过期的修饰符
                removeAttributeModifier(entity, info.attributeId, info.modifierId);
                toRemove.add(entry.getKey());

                LOGGER.debug("Remove deprecated attribute modifiers： entity={}, attribute={}, value={}",
                        entity, info.attributeId, info.value);
            }
        }

        //清理记录
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
        reactionSystem.reloadReactions();
        combinationSystem.reloadCombinations();
    }

    private String formatDisplayName(String elementId) {
        String[] parts = elementId.split(":");
        if (parts.length == 2) {
            return parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1).toLowerCase();
        }
        return elementId;
    }

    public ElementRegistry getRegistry() {
        return this.registry;
    }
}