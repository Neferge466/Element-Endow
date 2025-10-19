package com.element_endow.core.element;

import com.element_endow.api.element.IElementAttribute;
import com.element_endow.api.element.IElementSystem;
import com.element_endow.core.config.ElementConfigImpl;
import com.element_endow.api.config.IElementConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ElementSystemImpl implements IElementSystem {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ElementRegistry elementRegistry;
    private final IElementConfig config;
    private final List<ElementChangeListener> listeners;
    private final Map<LivingEntity, ElementHolderImpl> entityHolders;

    public ElementSystemImpl() {
        this.elementRegistry = new ElementRegistry();
        this.config = new com.element_endow.core.config.ElementConfigImpl();
        this.listeners = new CopyOnWriteArrayList<>();
        this.entityHolders = new ConcurrentHashMap<>();

        //从配置里初始化元素
        initializeFromConfig();
    }

    private void initializeFromConfig() {
        for (String elementId : config.getElements()) {
            if (!elementRegistry.isElementRegistered(elementId)) {
                registerElement(elementId, formatDisplayName(elementId), 0.0, 0.0, 1024.0);
            }
        }
        LOGGER.info("Initialized {} elements from config", elementRegistry.getRegisteredElementCount());
    }

    private String formatDisplayName(String elementId) {
        String[] parts = elementId.split(":");
        if (parts.length == 2) {
            return parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1).toLowerCase();
        }
        return elementId;
    }

    @Override
    public void registerElement(String elementId, String displayName, double defaultValue, double minValue, double maxValue) {
        boolean success = elementRegistry.registerElement(elementId, displayName, defaultValue, minValue, maxValue);
        if (success) {
            LOGGER.info("Registered element: {} ({})", elementId, displayName);
            //通知监听器
            MinecraftForge.EVENT_BUS.post(new com.element_endow.event.ElementAttributeEvent.ElementRegisteredEvent(elementId));
        }
    }

    @Override
    public boolean unregisterElement(String elementId) {
        boolean success = elementRegistry.unregisterElement(elementId);
        if (success) {
            LOGGER.info("Unregistered element: {}", elementId);
            MinecraftForge.EVENT_BUS.post(new com.element_endow.event.ElementAttributeEvent.ElementUnregisteredEvent(elementId));
        }
        return success;
    }

    @Override
    public Collection<String> getRegisteredElements() {
        return elementRegistry.getRegisteredElementIds();
    }

    @Override
    public Optional<IElementAttribute> getElementAttribute(String elementId) {
        return elementRegistry.getElementAttribute(elementId);
    }

    @Override
    public double getElementValue(LivingEntity entity, String elementId) {
        ElementHolderImpl holder = entityHolders.computeIfAbsent(entity, k -> new ElementHolderImpl(entity, this));
        return holder.getElementValue(elementId);
    }

    @Override
    public void setElementBaseValue(LivingEntity entity, String elementId, double value) {
        ElementHolderImpl holder = entityHolders.computeIfAbsent(entity, k -> new ElementHolderImpl(entity, this));
        double oldValue = holder.getElementValue(elementId);
        holder.setElementValue(elementId, value);

        //通知监听器
        for (ElementChangeListener listener : listeners) {
            listener.onElementChanged(entity, elementId, oldValue, value);
        }

        //发布事件
        MinecraftForge.EVENT_BUS.post(new com.element_endow.event.ElementAttributeEvent.ElementValueChangedEvent(entity, elementId, oldValue, value));
    }

    @Override
    public boolean hasElementAttribute(LivingEntity entity, String elementId) {
        return elementRegistry.isElementRegistered(elementId) &&
                entity.getAttribute(elementRegistry.getMinecraftAttribute(elementId)) != null;
    }

    @Override
    public IElementConfig getConfig() {
        return config;
    }

    @Override
    public void addElementChangeListener(ElementChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeElementChangeListener(ElementChangeListener listener) {
        listeners.remove(listener);
    }

    public ElementRegistry getElementRegistry() {
        return elementRegistry;
    }

    public void onEntityRemoved(LivingEntity entity) {
        entityHolders.remove(entity);
    }


    @Override
    public DeferredRegister<Attribute> getAttributeRegister() {
        return ElementRegistry.ATTRIBUTES;
    }




}