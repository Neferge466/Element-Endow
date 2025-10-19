package com.element_endow.config;

import com.element_endow.ElementEndow;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementRegistry {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ElementEndow.MODID);

    private static final Map<String, RegistryObject<Attribute>> REGISTERED_ELEMENTS = new HashMap<>();

    //静态初始化，类加载时注册元素
    static {
        registerAllElements();
    }

    private static void registerAllElements() {
        ElementEndow.LOGGER.info("Starting registration of element attributes");

        //从配置文件获取元素列表
        List<String> elements = SimpleElementConfig.getElements();

        if (elements.isEmpty()) {
            ElementEndow.LOGGER.warn("No elements configured. The mod will not function until elements are added to the config file.");
            return;
        }

        for (String elementId : elements) {
            registerElement(elementId);
        }

        ElementEndow.LOGGER.info("Completed registration of {} element attributes", REGISTERED_ELEMENTS.size());

        for (String elementId : REGISTERED_ELEMENTS.keySet()) {
            ElementEndow.LOGGER.debug("Registered: {}", elementId);
        }
    }

    private static void registerElement(String elementId) {
        try {
            String[] parts = elementId.split(":");
            if (parts.length == 2) {
                String elementName = parts[1];
                String attributeName = elementName.toLowerCase();

                RegistryObject<Attribute> attribute = ATTRIBUTES.register(
                        attributeName,
                        () -> new RangedAttribute(
                                "attribute.element_endow." + elementName,
                                0.0, 0.0, 1024.0
                        ).setSyncable(true)
                );

                REGISTERED_ELEMENTS.put(elementId, attribute);
                ElementEndow.LOGGER.debug("Registered element: {}", elementId);
            } else {
                ElementEndow.LOGGER.error("Invalid element format: {}", elementId);
            }
        } catch (Exception e) {
            ElementEndow.LOGGER.error("Failed to register element {}: {}", elementId, e.getMessage());
        }
    }

    public static Map<String, RegistryObject<Attribute>> getRegisteredElements() {
        return new HashMap<>(REGISTERED_ELEMENTS);
    }

    public static RegistryObject<Attribute> getElementAttribute(String elementId) {
        return REGISTERED_ELEMENTS.get(elementId);
    }
}