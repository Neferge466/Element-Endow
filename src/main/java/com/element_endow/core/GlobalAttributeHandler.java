package com.element_endow.core;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.api.element.IElementSystem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

//全局属性处理器，将元素属性添加到所有生物实体
@Mod.EventBusSubscriber(modid = "element_endow", bus = Mod.EventBusSubscriber.Bus.MOD)
public class GlobalAttributeHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        try {
            IElementSystem elementSystem = ElementSystemAPI.getElementSystem();
            Collection<String> registeredElements = elementSystem.getRegisteredElements();

            if (registeredElements.isEmpty()) {
                LOGGER.warn("No elements registered. Skipping attribute modification.");
                return;
            }

            int addedCount = 0;
            int entityCount = 0;

            //为所有LivingEntity添加元素属性
            for (EntityType<? extends LivingEntity> entityType : event.getTypes()) {
                if (DefaultAttributes.hasSupplier(entityType)) {
                    entityCount++;

                    for (String elementId : registeredElements) {
                        var attributeOpt = elementSystem.getElementAttribute(elementId);
                        if (attributeOpt.isPresent()) {
                            Attribute attribute = attributeOpt.get().getMinecraftAttribute();
                            if (!event.has(entityType, attribute)) {
                                event.add(entityType, attribute);
                                addedCount++;
                            }
                        }
                    }
                }
            }

            LOGGER.info("Added {} element attributes to {} entity types", addedCount, entityCount);

        } catch (Exception e) {
            LOGGER.error("Failed to modify entity attributes: {}", e.getMessage(), e);
        }
    }
}