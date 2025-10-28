package com.element_endow.event;

import com.element_endow.api.ElementSystemAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = "element_endow", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ElementCombinationHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.tickCount % 40 == 0) {
            try {
                var combinationSystem = ElementSystemAPI.getElementSystem().getCombinationSystem();
                combinationSystem.checkAndApplyCombinations(entity);
            } catch (Exception e) {
                LOGGER.error("Error checking entity combinations", e);
            }
        }

        try {
            var mountSystem = ElementSystemAPI.getElementSystem().getMountSystem();
            mountSystem.tick();
        } catch (Exception e) {
            LOGGER.error("Error updating mount system", e);
        }
    }
}