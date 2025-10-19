package com.element_endow.core.element;

import com.element_endow.api.element.IElementHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.HashMap;
import java.util.Map;

public class ElementHolderImpl implements IElementHolder {
    private final LivingEntity entity;
    private final ElementSystemImpl elementSystem;

    public ElementHolderImpl(LivingEntity entity, ElementSystemImpl elementSystem) {
        this.entity = entity;
        this.elementSystem = elementSystem;
    }

    @Override
    public LivingEntity getEntity() {
        return entity;
    }

    @Override
    public double getElementValue(String elementId) {
        var attribute = elementSystem.getElementRegistry().getMinecraftAttribute(elementId);
        if (attribute != null && entity.getAttribute(attribute) != null) {
            return entity.getAttributeValue(attribute);
        }
        return 0.0;
    }

    @Override
    public void setElementValue(String elementId, double value) {
        var attribute = elementSystem.getElementRegistry().getMinecraftAttribute(elementId);
        if (attribute != null) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) {
                //从注册表获取
                double minValue = 0.0;
                double maxValue = 1024.0;

                var elementAttr = elementSystem.getElementRegistry().getElementAttribute(elementId);
                if (elementAttr.isPresent()) {
                    minValue = elementAttr.get().getMinValue();
                    maxValue = elementAttr.get().getMaxValue();
                }

                double clampedValue = Math.max(minValue, Math.min(maxValue, value));
                instance.setBaseValue(clampedValue);
            }
        }
    }

    @Override
    public Map<String, Double> getAllElementValues() {
        Map<String, Double> values = new HashMap<>();
        for (String elementId : elementSystem.getRegisteredElements()) {
            values.put(elementId, getElementValue(elementId));
        }
        return values;
    }

    @Override
    public boolean hasElement(String elementId) {
        return getElementValue(elementId) > 0;
    }
}