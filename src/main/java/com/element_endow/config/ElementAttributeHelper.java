package com.element_endow.config;

import com.element_endow.ElementEndow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.element_endow.ElementEndow.MODID)
public class ElementAttributeHelper {

    public static double getElementValue(LivingEntity entity, String elementId) {
        var attribute = ElementRegistry.getElementAttribute(elementId);
        if (attribute != null && entity.getAttribute(attribute.get()) != null) {
            return entity.getAttributeValue(attribute.get());
        }
        return 0.0;
    }

    public static void setElementBaseValue(LivingEntity entity, String elementId, double value) {
        var attribute = ElementRegistry.getElementAttribute(elementId);
        if (attribute != null) {
            AttributeInstance instance = entity.getAttribute(attribute.get());
            if (instance != null) {
                instance.setBaseValue(Math.max(0.0, Math.min(1024.0, value)));
            } else {
                ElementEndow.LOGGER.warn("Attribute {} not found on entity", attribute.get().getDescriptionId());
            }
        }
    }

    public static boolean hasElementAttribute(LivingEntity entity, String elementId) {
        var attribute = ElementRegistry.getElementAttribute(elementId);
        return attribute != null && entity.getAttribute(attribute.get()) != null;
    }

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Player player) {
            //验证属性是否存在
            for (var entry : ElementRegistry.getRegisteredElements().entrySet()) {
                var attribute = entry.getValue().get();
                if (player.getAttribute(attribute) == null) {
                    ElementEndow.LOGGER.warn("Player is missing attribute: {}", attribute.getDescriptionId());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        for (var entry : ElementRegistry.getRegisteredElements().entrySet()) {
            double value = getElementValue(original, entry.getKey());
            setElementBaseValue(newPlayer, entry.getKey(), value);
        }
    }
}