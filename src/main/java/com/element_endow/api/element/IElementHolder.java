package com.element_endow.api.element;

import net.minecraft.world.entity.LivingEntity;
import java.util.Map;

//实体元素持有者接口
public interface IElementHolder {
    LivingEntity getEntity();
    double getElementValue(String elementId);
    void setElementValue(String elementId, double value);
    Map<String, Double> getAllElementValues();
    boolean hasElement(String elementId);
}