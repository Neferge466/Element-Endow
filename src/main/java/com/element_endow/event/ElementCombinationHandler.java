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
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide()) {
            return; // 只在服务端处理
        }

        // 检查组合的频率，每40tick检查一次（2秒）
        if (entity.tickCount % 40 == 0) {
            try {
                var combinationSystem = ElementSystemAPI.getElementSystem().getCombinationSystem();
                combinationSystem.checkAndApplyCombinations(entity);

                // 调试日志（每100次检查记录一次）
                if (entity.tickCount % 4000 == 0) {
                    var activeCombinations = combinationSystem.getActiveCombinations(entity);
                    if (!activeCombinations.isEmpty()) {
                        LOGGER.debug("Entity {} has active combinations: {}", entity, activeCombinations.size());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error checking entity combinations for {}", entity, e);
            }
        }

        // 检查条件绑定的频率，每100tick检查一次（5秒）
        if (entity.tickCount % 100 == 0) {
            try {
                EntitySpawnHandler.checkConditionalBindings();
            } catch (Exception e) {
                LOGGER.error("Error checking conditional bindings", e);
            }
        }

        // 挂载系统每tick更新
        try {
            var mountSystem = ElementSystemAPI.getElementSystem().getMountSystem();
            mountSystem.tick();
        } catch (Exception e) {
            LOGGER.error("Error updating mount system for {}", entity, e);
        }

        // 全局定时任务（每20tick执行一次）
        tickCounter++;
        if (tickCounter % 20 == 0) {
            performGlobalMaintenance();
        }
    }

    /**
     * 执行全局维护任务
     */
    private static void performGlobalMaintenance() {
        try {
            var elementSystem = ElementSystemAPI.getElementSystem();
            // 检查并移除过期的属性修饰符
            // 注意：这里无法遍历所有实体，所以这个功能在实体tick中处理更合适
            // 保持这个结构以便未来可能的全局维护任务
        } catch (Exception e) {
            LOGGER.error("Error performing global maintenance", e);
        }
    }

    /**
     * 强制重新检查所有实体的组合（用于调试和命令）
     */
    public static void forceRecheckAllCombinations() {
        LOGGER.info("Forcing recheck of all combinations");
        // 这个功能需要遍历所有实体，在实际游戏中可能性能开销较大
        // 建议在调试时使用，或通过命令针对单个实体执行
    }
}