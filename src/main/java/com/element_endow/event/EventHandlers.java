package com.element_endow.event;

import com.element_endow.core.ElementEndowCore;
import com.element_endow.core.element.ElementSystemImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = "element_endow")
public class EventHandlers {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Player player) {
            //验证玩家属性
            ElementSystemImpl elementSystem = (ElementSystemImpl) ElementEndowCore.getInstance().getElementSystem();
            for (String elementId : elementSystem.getRegisteredElements()) {
                if (!elementSystem.hasElementAttribute(player, elementId)) {
                    LOGGER.warn("Player is missing attribute: {}", elementId);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        ElementSystemImpl elementSystem = (ElementSystemImpl) ElementEndowCore.getInstance().getElementSystem();
        for (String elementId : elementSystem.getRegisteredElements()) {
            double value = elementSystem.getElementValue(original, elementId);
            elementSystem.setElementBaseValue(newPlayer, elementId, value);
        }
    }

    @SubscribeEvent
    public static void onElementValueChanged(ElementAttributeEvent.ElementValueChangedEvent event) {
        LOGGER.debug("Element {} changed for entity {}: {} -> {}",
                event.getElementId(), event.getEntity().getName().getString(),
                event.getOldValue(), event.getNewValue());
    }
}