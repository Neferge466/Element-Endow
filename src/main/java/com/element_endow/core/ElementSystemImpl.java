package com.element_endow.core;

import com.element_endow.api.IElementSystem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Optional;

public class ElementSystemImpl implements IElementSystem {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ElementRegistry registry;
    private final ElementConfig config;

    public ElementSystemImpl() {
        this.registry = new ElementRegistry();
        this.config = new ElementConfig();
        initializeFromConfig();
    }

    private void initializeFromConfig() {
        for (String elementId : config.getElements()) {
            if (!registry.getRegisteredElementIds().contains(elementId)) {
                registerElement(elementId, formatDisplayName(elementId), 0.0, 0.0, 1024.0);
            }
        }
    }

    @Override
    public void registerElement(String elementId, String displayName,
                                double defaultValue, double minValue, double maxValue) {
        if (registry.registerElement(elementId, displayName, defaultValue, minValue, maxValue)) {
            LOGGER.info("Registered element: {}", elementId);
        }
    }

    @Override
    public boolean unregisterElement(String elementId) {
        return registry.unregisterElement(elementId);
    }

    @Override
    public Collection<String> getRegisteredElements() {
        return registry.getRegisteredElementIds();
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
        Optional<Attribute> attribute = registry.getElementAttribute(elementId);
        return attribute.isPresent() && entity.getAttribute(attribute.get()) != null;
    }

    @Override
    public DeferredRegister<Attribute> getAttributeRegister() {
        return ElementRegistry.ATTRIBUTES;
    }

    @Override
    public Optional<Attribute> getElementAttribute(String elementId) {
        return registry.getElementAttribute(elementId);
    }

    private String formatDisplayName(String elementId) {
        String[] parts = elementId.split(":");
        if (parts.length == 2) {
            return parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1).toLowerCase();
        }
        return elementId;
    }
}