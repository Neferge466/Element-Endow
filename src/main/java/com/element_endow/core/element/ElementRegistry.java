package com.element_endow.core.element;

import com.element_endow.api.element.IElementAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ElementRegistry {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, "element_endow");

    private final Map<String, ElementAttributeImpl> registeredElements;
    private final Map<String, RegistryObject<Attribute>> attributeRegistry;

    public ElementRegistry() {
        this.registeredElements = new ConcurrentHashMap<>();
        this.attributeRegistry = new ConcurrentHashMap<>();
    }

    public boolean registerElement(String elementId, String displayName, double defaultValue, double minValue, double maxValue) {
        if (registeredElements.containsKey(elementId)) {
            LOGGER.warn("Element already registered: {}", elementId);
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

            ElementAttributeImpl elementAttribute = new ElementAttributeImpl(
                    elementId, displayName, attribute, defaultValue, minValue, maxValue
            );

            attributeRegistry.put(elementId, attribute);
            registeredElements.put(elementId, elementAttribute);

            LOGGER.debug("Successfully registered element: {}", elementId);
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to register element {}: {}", elementId, e.getMessage());
            return false;
        }
    }

    public boolean unregisterElement(String elementId) {
        if (!registeredElements.containsKey(elementId)) {
            LOGGER.warn("Element not found for unregistration: {}", elementId);
            return false;
        }

        //从管理映射中移除
        registeredElements.remove(elementId);
        attributeRegistry.remove(elementId);

        LOGGER.info("Unregistered element from management: {}", elementId);
        return true;
    }

    private String extractAttributeName(String elementId) {
        String[] parts = elementId.split(":");
        if (parts.length == 2) {
            return parts[1].toLowerCase();
        }
        return elementId.replace(':', '_').toLowerCase();
    }

    public Optional<IElementAttribute> getElementAttribute(String elementId) {
        return Optional.ofNullable(registeredElements.get(elementId));
    }

    public boolean isElementRegistered(String elementId) {
        return registeredElements.containsKey(elementId);
    }

    public Collection<String> getRegisteredElementIds() {
        return Collections.unmodifiableCollection(registeredElements.keySet());
    }

    public int getRegisteredElementCount() {
        return registeredElements.size();
    }

    public Attribute getMinecraftAttribute(String elementId) {
        RegistryObject<Attribute> registryObject = attributeRegistry.get(elementId);
        return registryObject != null ? registryObject.get() : null;
    }

    //获取所有已注册的Minecraft属性实例
    public Collection<Attribute> getAllMinecraftAttributes() {
        return attributeRegistry.values().stream()
                .map(RegistryObject::get)
                .collect(Collectors.toList());
    }

    //获取属性注册器
    public DeferredRegister<Attribute> getAttributeRegister() {
        return ATTRIBUTES;
    }


}