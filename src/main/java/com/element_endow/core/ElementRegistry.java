package com.element_endow.core;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ElementRegistry {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, "element_endow");

    private final Map<String, RegistryObject<Attribute>> registeredAttributes;
    private final Map<String, AttributeData> attributeData;

    public ElementRegistry() {
        this.registeredAttributes = new ConcurrentHashMap<>();
        this.attributeData = new ConcurrentHashMap<>();
    }

    public boolean registerElement(String elementId, String displayName,
                                   double defaultValue, double minValue, double maxValue) {
        if (registeredAttributes.containsKey(elementId)) {
            return false;
        }

        try {
            String attributeName = extractAttributeName(elementId);
            RegistryObject<Attribute> attribute = ATTRIBUTES.register(
                    attributeName,
                    () -> new RangedAttribute(
                            "attribute.element_endow." + attributeName,
                            defaultValue, minValue, maxValue
                    ).setSyncable(true)
            );

            registeredAttributes.put(elementId, attribute);
            attributeData.put(elementId, new AttributeData(displayName, defaultValue, minValue, maxValue));

            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to register element: {}", elementId, e);
            return false;
        }
    }

    public boolean unregisterElement(String elementId) {
        return registeredAttributes.remove(elementId) != null && attributeData.remove(elementId) != null;
    }

    public Optional<Attribute> getElementAttribute(String elementId) {
        RegistryObject<Attribute> registryObject = registeredAttributes.get(elementId);
        return registryObject != null ? Optional.of(registryObject.get()) : Optional.empty();
    }

    public Collection<String> getRegisteredElementIds() {
        return Collections.unmodifiableCollection(registeredAttributes.keySet());
    }

    public AttributeData getAttributeData(String elementId) {
        return attributeData.get(elementId);
    }

    private String extractAttributeName(String elementId) {
        String[] parts = elementId.split(":");
        return parts.length == 2 ? parts[1].toLowerCase() : elementId.replace(':', '_').toLowerCase();
    }

    public static class AttributeData {
        public final String displayName;
        public final double defaultValue;
        public final double minValue;
        public final double maxValue;

        public AttributeData(String displayName, double defaultValue, double minValue, double maxValue) {
            this.displayName = displayName;
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }
    }
}