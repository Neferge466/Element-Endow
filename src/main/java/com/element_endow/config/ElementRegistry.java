package com.element_endow.config;

import com.element_endow.ElementEndow;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public class ElementRegistry {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ElementEndow.MODID);

    private static final Map<String, RegistryObject<Attribute>> REGISTERED_ELEMENTS = new HashMap<>();

    static {
        registerAllElements();
    }

    private static void registerAllElements() {
        ElementEndow.LOGGER.info("Starting static registration of element attributes");

        String[] elements = {
                "element_endow:fire", "element_endow:water", "element_endow:earth",
                "element_endow:air", "element_endow:lightning", "element_endow:ice",
                "element_endow:nature", "element_endow:metal"
        };

        for (String elementId : elements) {
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
            }
        }

        ElementEndow.LOGGER.info("Completed registration of {} element attributes", REGISTERED_ELEMENTS.size());
    }

    public static Map<String, RegistryObject<Attribute>> getRegisteredElements() {
        return new HashMap<>(REGISTERED_ELEMENTS);
    }

    public static RegistryObject<Attribute> getElementAttribute(String elementId) {
        return REGISTERED_ELEMENTS.get(elementId);
    }
}