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

        //检查组合的频率，每40tick检查一次（2秒）
        if (entity.tickCount % 40 == 0) {
            try {
                var combinationSystem = ElementSystemAPI.getElementSystem().getCombinationSystem();
                combinationSystem.checkAndApplyCombinations(entity);
            } catch (Exception e) {
                LOGGER.error("Error checking entity combinations", e);
            }
        }

        //检查条件绑定的频率，每100tick检查一次（5秒）
        if (entity.tickCount % 100 == 0) {
            try {
                EntitySpawnHandler.checkConditionalBindings();
            } catch (Exception e) {
                LOGGER.error("Error checking conditional bindings", e);
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