package com.element_endow.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.Optional;

public interface IElementSystem {
    // 元素管理
    void registerElement(String elementId, String displayName, double defaultValue, double minValue, double maxValue);
    boolean unregisterElement(String elementId);
    Collection<String> getRegisteredElements();

    // 实体元素操作
    double getElementValue(LivingEntity entity, String elementId);
    void setElementValue(LivingEntity entity, String elementId, double value);
    boolean hasElement(LivingEntity entity, String elementId);

    // 属性注册
    DeferredRegister<Attribute> getAttributeRegister();

    // 获取元素属性
    Optional<Attribute> getElementAttribute(String elementId);
}