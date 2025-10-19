package com.element_endow.api.element;

import com.element_endow.api.config.IElementConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.Optional;

//元素系统主接口
public interface IElementSystem {
    //元素管理
    void registerElement(String elementId, String displayName, double defaultValue, double minValue, double maxValue);
    boolean unregisterElement(String elementId);
    Collection<String> getRegisteredElements();
    Optional<IElementAttribute> getElementAttribute(String elementId);

    //实体元素操作
    double getElementValue(LivingEntity entity, String elementId);
    void setElementBaseValue(LivingEntity entity, String elementId, double value);
    boolean hasElementAttribute(LivingEntity entity, String elementId);

    //配置
    IElementConfig getConfig();

    //事件
    void addElementChangeListener(ElementChangeListener listener);
    void removeElementChangeListener(ElementChangeListener listener);

    DeferredRegister<Attribute> getAttributeRegister();

    @FunctionalInterface
    interface ElementChangeListener {
        void onElementChanged(LivingEntity entity, String elementId, double oldValue, double newValue);
    }
}