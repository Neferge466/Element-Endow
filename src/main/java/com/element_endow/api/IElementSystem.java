package com.element_endow.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IElementSystem {

    void registerElement(String elementId, String displayName,
                         double defaultValue, double minValue, double maxValue);
    boolean disableElement(String elementId);
    boolean enableElement(String elementId);
    Collection<String> getRegisteredElements();
    Collection<String> getEnabledElements();
    boolean isElementManaged(String elementId);
    boolean isElementRegistered(String elementId);
    double getElementValue(LivingEntity entity, String elementId);
    void setElementValue(LivingEntity entity, String elementId, double value);
    boolean hasElement(LivingEntity entity, String elementId);
    DeferredRegister<Attribute> getAttributeRegister();
    Optional<Attribute> getElementAttribute(String elementId);

    /**
     * 通过属性ID获取属性实例 (适用于任何已注册的属性)
     */
    Optional<Attribute> getAttributeById(ResourceLocation attributeId);

    /**
     * 通过字符串ID获取属性实例
     */
    default Optional<Attribute> getAttributeById(String attributeId) {
        return getAttributeById(ResourceLocation.tryParse(attributeId));
    }

    /**
     * 获取实体的某个属性值
     */
    double getAttributeValue(LivingEntity entity, ResourceLocation attributeId);

    /**
     * 设置实体的某个属性基础值
     */
    void setAttributeBaseValue(LivingEntity entity, ResourceLocation attributeId, double value);

    /**
     * 检查实体是否拥有某个属性
     */
    boolean hasAttribute(LivingEntity entity, ResourceLocation attributeId);

    /**
     * 对实体应用一个属性修饰符
     */
    boolean applyAttributeModifier(LivingEntity entity, ResourceLocation attributeId, AttributeModifier modifier);

    /**
     * 移除实体某个属性的修饰符
     */
    boolean removeAttributeModifier(LivingEntity entity, ResourceLocation attributeId, UUID modifierId);

    /**
     * 应用有时效的属性修饰符
     */
    boolean applyTimedAttributeModifier(LivingEntity entity, ResourceLocation attributeId,
                                        AttributeModifier modifier, int durationTicks);

    /**
     * 检查并移除过期的属性修饰符
     */
    void checkAndRemoveExpiredModifiers(LivingEntity entity);

    /**
     * 获取实体的所有活跃修饰符信息（用于调试）
     */
    java.util.Map<String, TimedModifierInfo> getActiveModifierInfo(LivingEntity entity);


    //时效修饰符信息类
    class TimedModifierInfo {
        public final ResourceLocation attributeId;
        public final UUID modifierId;
        public final long applyTime;
        public final int duration;
        public final double value;

        public TimedModifierInfo(ResourceLocation attributeId, UUID modifierId, long applyTime, int duration, double value) {
            this.attributeId = attributeId;
            this.modifierId = modifierId;
            this.applyTime = applyTime;
            this.duration = duration;
            this.value = value;
        }

        public boolean isExpired(long currentTime) {
            return currentTime - applyTime >= duration;
        }
    }

    //系统访问器
    IElementReactionSystem getReactionSystem();
    IElementCombinationSystem getCombinationSystem();
    IElementMountSystem getMountSystem();

    //数据重载
    void reloadData();
}