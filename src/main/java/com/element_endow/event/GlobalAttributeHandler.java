package com.element_endow.event;

import com.element_endow.api.ElementSystemAPI;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = "element_endow", bus = Mod.EventBusSubscriber.Bus.MOD)
public class GlobalAttributeHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        try {
            var elementSystem = ElementSystemAPI.getElementSystem();
            int addedCount = 0;
            int entityCount = 0;

            for (EntityType<? extends LivingEntity> entityType : event.getTypes()) {
                if (DefaultAttributes.hasSupplier(entityType)) {
                    entityCount++;
                    for (String elementId : elementSystem.getEnabledElements()) {
                        Optional<Attribute> attribute = elementSystem.getElementAttribute(elementId);
                        if (attribute.isPresent() && !event.has(entityType, attribute.get())) {
                            event.add(entityType, attribute.get());
                            addedCount++;
                        }
                    }
                }
            }

            LOGGER.info("Added {} element attributes to {} entity types", addedCount, entityCount);
        } catch (Exception e) {
            LOGGER.error("Failed to modify entity attributes", e);
        }
    }
}