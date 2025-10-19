package com.element_endow.core.element;

import com.element_endow.api.element.IElementAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.RegistryObject;

public class ElementAttributeImpl implements IElementAttribute {
    private final String elementId;
    private final String displayName;
    private final RegistryObject<Attribute> attribute;
    private final double defaultValue;
    private final double minValue;
    private final double maxValue;

    public ElementAttributeImpl(String elementId, String displayName, RegistryObject<Attribute> attribute,
                                double defaultValue, double minValue, double maxValue) {
        this.elementId = elementId;
        this.displayName = displayName;
        this.attribute = attribute;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public String getElementId() {
        return elementId;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Attribute getMinecraftAttribute() {
        return attribute.get();
    }

    @Override
    public double getDefaultValue() {
        return defaultValue;
    }

    @Override
    public double getMinValue() {
        return minValue;
    }

    @Override
    public double getMaxValue() {
        return maxValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ElementAttributeImpl that = (ElementAttributeImpl) obj;
        return elementId.equals(that.elementId);
    }

    @Override
    public int hashCode() {
        return elementId.hashCode();
    }

    @Override
    public String toString() {
        return "ElementAttributeImpl{" +
                "elementId='" + elementId + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}