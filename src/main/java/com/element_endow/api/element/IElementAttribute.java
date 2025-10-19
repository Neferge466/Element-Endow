package com.element_endow.api.element;

import net.minecraft.world.entity.ai.attributes.Attribute;

//元素属性接口
public interface IElementAttribute {
    String getElementId();
    String getDisplayName();
    Attribute getMinecraftAttribute();
    double getDefaultValue();
    double getMinValue();
    double getMaxValue();
}