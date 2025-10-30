package com.element_endow.event;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.data.ElementDataManager;
import com.element_endow.data.entity_bindings.EntityElementBindingLoader;
import com.element_endow.util.ConditionChecker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 处理生物生成时的元素绑定
 */
@Mod.EventBusSubscriber(modid = "element_endow", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntitySpawnHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    //记录已经处理过的实体UUID及其绑定条件
    private static final Map<LivingEntity, EntityElementBindingLoader.EntityElementBinding> conditionalBindings = new WeakHashMap<>();

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        //只处理生物实体
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        LivingEntity livingEntity = (LivingEntity) entity;

        //检查是否为自然生成（避免从刷怪笼或其他情况重复生成时重复应用）
        if (!isNaturalSpawn(livingEntity)) {
            return;
        }

        applyEntityElementBindings(livingEntity);
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        LivingEntity entity = event.getEntity();

        //对于自然生成的生物，立即应用元素绑定
        if (isNaturalSpawn(entity)) {
            applyEntityElementBindings(entity);
        }
    }

    /**
     * 定期检查条件绑定（在ElementCombinationHandler中调用）
     */
    public static void checkConditionalBindings() {
        if (conditionalBindings.isEmpty()) {
            return;
        }

        //每5秒检查一次条件绑定
        long currentTime = System.currentTimeMillis();
        if (currentTime % 5000 > 100) {
            return;
        }

        conditionalBindings.entrySet().removeIf(entry -> {
            LivingEntity entity = entry.getKey();
            EntityElementBindingLoader.EntityElementBinding binding = entry.getValue();

            if (entity == null || !entity.isAlive()) {
                return true; //移除死亡或无效的实体
            }

            //检查条件是否仍然满足
            if (binding.hasConditions()) {
                boolean conditionsMet = ConditionChecker.checkConditions(binding.conditions, entity, entity.level());
                if (!conditionsMet) {
                    //条件不再满足，移除元素绑定
                    removeElementBinding(entity, binding);
                    LOGGER.debug("Removed conditional element binding from entity {}: conditions no longer met", entity);
                    return true;
                }
            }
            return false;
        });
    }

    private static boolean isNaturalSpawn(LivingEntity entity) {
        //检查生成类型
        var spawnData = entity.getPersistentData();
        return entity.level().isClientSide == false &&
                (spawnData.getLong("ElementEndowLastApplied") == 0);
    }

    private static void applyEntityElementBindings(LivingEntity entity) {
        try {
            var elementSystem = ElementSystemAPI.getElementSystem();
            var bindingLoader = ElementDataManager.getEntityBindingLoader();

            EntityElementBindingLoader.EntityElementBinding binding = bindingLoader.getBindingForEntity(entity.getType());

            if (binding != null) {
                boolean shouldApply = true;

                //检查条件
                if (binding.hasConditions()) {
                    shouldApply = ConditionChecker.checkConditions(binding.conditions, entity, entity.level());
                    if (shouldApply) {
                        //记录条件绑定，用于后续检查
                        conditionalBindings.put(entity, binding);
                        LOGGER.debug("Applied conditional element binding to entity: {} (Type: {})",
                                entity.getName().getString(), entity.getType());
                    } else {
                        LOGGER.debug("Conditions not met for entity binding: {} (Type: {})",
                                entity.getName().getString(), entity.getType());
                        return;
                    }
                }

                if (shouldApply) {
                    LOGGER.info("Applying element bindings for entity: {} (Type: {})",
                            entity.getName().getString(), entity.getType());

                    //应用元素绑定
                    for (Map.Entry<String, Double> elementEntry : binding.elements.entrySet()) {
                        String elementId = elementEntry.getKey();
                        double value = elementEntry.getValue();

                        //确保元素已注册
                        if (elementSystem.isElementRegistered(elementId)) {
                            elementSystem.setElementValue(entity, elementId, value);
                            LOGGER.debug("Applied element {} with value {} to entity {}",
                                    elementId, value, entity.getName().getString());
                        } else {
                            LOGGER.warn("Element {} is not registered, skipping for entity {}",
                                    elementId, entity.getName().getString());
                        }
                    }

                    //标记为已处理
                    entity.getPersistentData().putLong("ElementEndowLastApplied", System.currentTimeMillis());

                    LOGGER.info("Successfully applied {} element bindings to entity {}",
                            binding.elements.size(), entity.getName().getString());
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to apply element bindings for entity: {}", entity, e);
        }
    }

    /**
     * 移除元素的绑定
     */
    private static void removeElementBinding(LivingEntity entity, EntityElementBindingLoader.EntityElementBinding binding) {
        try {
            var elementSystem = ElementSystemAPI.getElementSystem();

            for (String elementId : binding.elements.keySet()) {
                if (elementSystem.isElementRegistered(elementId)) {
                    //将元素值重置为0
                    elementSystem.setElementValue(entity, elementId, 0.0);
                    LOGGER.debug("Removed element {} from entity {}", elementId, entity);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to remove element binding from entity: {}", entity, e);
        }
    }

    /**
     * 手动为实体应用元素绑定
     */
    public static void applyBindingsManually(LivingEntity entity) {
        applyEntityElementBindings(entity);
    }

    /**
     * 清除实体的处理记录（用于重新应用绑定）
     */
    public static void clearProcessedRecord(LivingEntity entity) {
        conditionalBindings.remove(entity);
        entity.getPersistentData().remove("ElementEndowLastApplied");
    }
}