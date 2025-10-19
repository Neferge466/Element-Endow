package com.element_endow.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

public class ElementAttributeEvent {

    //元素注册事件
    public static class ElementRegisteredEvent extends Event {
        private final String elementId;

        public ElementRegisteredEvent(String elementId) {
            this.elementId = elementId;
        }

        public String getElementId() {
            return elementId;
        }
    }

    //取消注册
    public static class ElementUnregisteredEvent extends Event {
        private final String elementId;

        public ElementUnregisteredEvent(String elementId) {
            this.elementId = elementId;
        }

        public String getElementId() {
            return elementId;
        }
    }

     //元素值改变事件
    public static class ElementValueChangedEvent extends Event {
        private final LivingEntity entity;
        private final String elementId;
        private final double oldValue;
        private final double newValue;

        public ElementValueChangedEvent(LivingEntity entity, String elementId, double oldValue, double newValue) {
            this.entity = entity;
            this.elementId = elementId;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        //Getters
        public LivingEntity getEntity() { return entity; }
        public String getElementId() { return elementId; }
        public double getOldValue() { return oldValue; }
        public double getNewValue() { return newValue; }
    }
}