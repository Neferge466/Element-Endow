package com.element_endow.core;

import com.element_endow.api.IElementCombinationSystem;
import com.element_endow.api.IElementSystem;
import com.element_endow.api.ReactionResult;
import com.element_endow.core.cache.CombinationCache;
import com.element_endow.core.cache.ConditionCache;
import com.element_endow.data.CombinationLoader;
import com.element_endow.data.ElementDataService;
import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ElementCombinationSystem implements IElementCombinationSystem {
    private static final Logger LOGGER = LogManager.getLogger();

    private final IElementSystem elementSystem;
    private final CombinationLoader combinationLoader;
    private final CombinationCache combinationCache;
    private final ConditionCache conditionCache;
    private final Map<LivingEntity, Map<String, UUID>> activeModifiers;
    private final ElementDataService dataService;

    public ElementCombinationSystem(IElementSystem elementSystem) {
        this.elementSystem = elementSystem;
        this.combinationLoader = new CombinationLoader();
        this.combinationLoader.loadCombinations();
        this.combinationCache = new CombinationCache();
        this.conditionCache = new ConditionCache();
        this.activeModifiers = new WeakHashMap<>();

        //通过elementSystem获取dataService
        if (elementSystem instanceof ElementSystemImpl) {
            this.dataService = ((ElementSystemImpl) elementSystem).getDataService();
            LOGGER.info("ElementCombinationSystem initialized with DataService");
        } else {
            this.dataService = null;
            LOGGER.warn("ElementCombinationSystem: Cannot access DataService, falling back to legacy loader");
        }
    }

    //备用构造函数
    public ElementCombinationSystem(IElementSystem elementSystem, ElementDataService dataService) {
        this.elementSystem = elementSystem;
        this.combinationLoader = new CombinationLoader();
        this.combinationLoader.loadCombinations();
        this.combinationCache = new CombinationCache();
        this.conditionCache = new ConditionCache();
        this.activeModifiers = new WeakHashMap<>();
        this.dataService = dataService;
        LOGGER.info("ElementCombinationSystem initialized with provided DataService");
    }

    @Override
    public void checkAndApplyCombinations(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return;
        }

        //检查缓存是否需要重新计算
        if (!combinationCache.shouldRecheck(entity, elementSystem, entity.tickCount)) {
            return;
        }

        Set<String> previousCombinations = combinationCache.getCachedCombinations(entity);
        Set<String> newCombinations = new HashSet<>();

        //获取实体当前元素值
        Map<String, Double> entityElementValues = getEntityElementValues(entity);

        //获取组合数据
        Map<String, CombinationLoader.ElementCombination> combinations = getCombinations();

        //检查所有组合
        for (CombinationLoader.ElementCombination combination : combinations.values()) {
            if (matchesCombination(entity, entityElementValues, combination)) {
                newCombinations.add(combination.id);
                if (!previousCombinations.contains(combination.id)) {
                    applyCombinationEffects(entity, combination);
                }
            } else if (previousCombinations.contains(combination.id)) {
                removeCombinationEffects(entity, combination.id);
            }
        }

        //更新缓存
        combinationCache.updateCache(entity, newCombinations, elementSystem, entity.tickCount);

        //定期清理条件缓存
        if (entity.tickCount % 200 == 0) {
            conditionCache.cleanup();
        }
    }

    /**
     * 获取组合数据（优先使用数据服务）
     */
    private Map<String, CombinationLoader.ElementCombination> getCombinations() {
        if (dataService != null) {
            try {
                return dataService.getAllCombinations();
            } catch (Exception e) {
                LOGGER.error("Failed to get combinations from DataService, falling back to legacy loader", e);
            }
        }
        return combinationLoader.getCombinations();
    }

    /**
     * 获取实体当前元素值
     */
    private Map<String, Double> getEntityElementValues(LivingEntity entity) {
        Map<String, Double> values = new HashMap<>();
        for (String elementId : elementSystem.getEnabledElements()) {
            double value = elementSystem.getElementValue(entity, elementId);
            if (value > 0) {
                values.put(elementId, value);
            }
        }
        return values;
    }

    private boolean matchesCombination(LivingEntity entity,
                                       Map<String, Double> entityElementValues,
                                       CombinationLoader.ElementCombination combination) {

        //检查必需元素
        for (String requiredElement : combination.requiredElements) {
            if (!entityElementValues.containsKey(requiredElement)) {
                return false;
            }
        }

        //检查禁止元素
        for (String forbidden : combination.forbiddenElements) {
            if (entityElementValues.containsKey(forbidden)) {
                return false;
            }
        }

        //检查最小值要求
        for (Map.Entry<String, Double> entry : combination.minValues.entrySet()) {
            Double currentValue = entityElementValues.get(entry.getKey());
            if (currentValue == null || currentValue < entry.getValue()) {
                return false;
            }
        }

        //检查组合条件
        if (combination.conditions != null && !combination.conditions.isEmpty()) {
            return conditionCache.checkWithCache(entity, combination.conditions);
        }

        return true;
    }

    /**
     * 应用组合效果
     */
    private void applyCombinationEffects(LivingEntity entity, CombinationLoader.ElementCombination combination) {
        Map<String, UUID> entityModifiers = activeModifiers.computeIfAbsent(entity, k -> new HashMap<>());

        //应用属性效果
        for (CombinationLoader.AttributeEffect effect : combination.attributeEffects) {
            applyAttributeEffect(entity, effect, combination.id, entityModifiers);
        }

        //应用状态效果
        for (CombinationLoader.StatusEffect effect : combination.statusEffects) {
            applyStatusEffect(entity, effect, combination.id);
        }
    }

    /**
     * 应用属性效果
     */
    private void applyAttributeEffect(LivingEntity entity, CombinationLoader.AttributeEffect effect,
                                      String combinationId, Map<String, UUID> entityModifiers) {
        try {
            ResourceLocation attributeId = ResourceLocation.tryParse(effect.attribute);
            if (attributeId == null) return;

            String modifierKey = combinationId + ":" + effect.attribute;
            UUID modifierId = UUID.nameUUIDFromBytes(modifierKey.getBytes());

            AttributeModifier modifier = createAttributeModifier(modifierId, effect);
            if (modifier != null) {
                boolean success = elementSystem.applyTimedAttributeModifier(entity, attributeId, modifier, 12000); // 10分钟
                if (success) {
                    entityModifiers.put(modifierKey, modifierId);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to apply attribute effect: {}", effect.attribute, e);
        }
    }

    /**
     * 应用状态效果
     */
    private void applyStatusEffect(LivingEntity entity, CombinationLoader.StatusEffect effect, String combinationId) {
        try {
            ResourceLocation effectId = new ResourceLocation(effect.effect);
            MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(effectId);

            if (mobEffect == null) {
                LOGGER.warn("Status effect not found: {}", effect.effect);
                return;
            }

            MobEffectInstance effectInstance = new MobEffectInstance(
                    mobEffect,
                    effect.duration,
                    effect.amplifier,
                    false,
                    effect.showParticles,
                    true
            );

            entity.addEffect(effectInstance);
        } catch (Exception e) {
            LOGGER.error("Failed to apply status effect: {}", effect.effect, e);
        }
    }

    private AttributeModifier createAttributeModifier(UUID modifierId, CombinationLoader.AttributeEffect effect) {
        try {
            AttributeModifier.Operation operation = getOperation(effect.operation);
            return new AttributeModifier(
                    modifierId,
                    "element_endow.combination." + effect.operation,
                    effect.value,
                    operation
            );
        } catch (Exception e) {
            return null;
        }
    }

    private AttributeModifier.Operation getOperation(String operation) {
        switch (operation.toLowerCase()) {
            case "add": return AttributeModifier.Operation.ADDITION;
            case "multiply_base": return AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total": return AttributeModifier.Operation.MULTIPLY_TOTAL;
            default: return AttributeModifier.Operation.ADDITION;
        }
    }

    /**
     * 处理触发效果
     */
    private void processTriggerEffect(CombinationLoader.TriggerEffect trigger,
                                      IElementCombinationSystem.CombinationTriggerResult result,
                                      LivingEntity self, LivingEntity target,
                                      String triggerType) {
        if (self.level().random.nextDouble() >= trigger.probability) {
            return;
        }

        //应用效果乘数
        result.damageMultiplier *= trigger.damageMultiplier;
        result.defenseMultiplier *= trigger.defenseMultiplier;
        result.extraDamage += trigger.extraDamage;
        result.damageReduction += trigger.damageReduction;

        //应用目标效果
        if (trigger.targetEffects != null) {
            for (CombinationLoader.EffectData effect : trigger.targetEffects) {
                MobEffectInstance effectInstance = createEffectInstance(effect);
                if (effectInstance != null) {
                    result.targetEffects.add(effectInstance);
                }
            }
        }

        //应用自身效果
        if (trigger.selfEffects != null) {
            for (CombinationLoader.EffectData effect : trigger.selfEffects) {
                MobEffectInstance effectInstance = createEffectInstance(effect);
                if (effectInstance != null) {
                    result.selfEffects.add(effectInstance);
                }
            }
        }

        //应用属性修饰符
        if (trigger.targetAttributeModifiers != null) {
            for (CombinationLoader.AttributeModifierData modifierData : trigger.targetAttributeModifiers) {
                ReactionResult.AttributeModifierApplication app = modifierData.toAttributeModifierApplication();
                if (app != null) {
                    result.targetAttributeModifiers.add(app);
                }
            }
        }

        if (trigger.selfAttributeModifiers != null) {
            for (CombinationLoader.AttributeModifierData modifierData : trigger.selfAttributeModifiers) {
                ReactionResult.AttributeModifierApplication app = modifierData.toAttributeModifierApplication();
                if (app != null) {
                    result.selfAttributeModifiers.add(app);
                }
            }
        }

        //应用挂载效果
        if (trigger.mountApplications != null) {
            for (CombinationLoader.MountApplication mountApp : trigger.mountApplications) {
                result.mountApplications.add(new ReactionResult.MountApplication(
                        mountApp.elementId,
                        mountApp.amount,
                        mountApp.duration,
                        mountApp.probability
                ));
            }
        }

        if (trigger.advancedMountApplications != null) {
            result.advancedMountApplications.addAll(trigger.advancedMountApplications);
        }
    }

    /**
     * 创建效果实例
     */
    private MobEffectInstance createEffectInstance(CombinationLoader.EffectData effect) {
        try {
            ResourceLocation effectId = new ResourceLocation(effect.effect);
            MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(effectId);
            return mobEffect != null ? new MobEffectInstance(
                    mobEffect,
                    effect.duration,
                    effect.amplifier,
                    false,
                    effect.showParticles,
                    true
            ) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public CombinationTriggerResult processAttackTrigger(LivingEntity attacker, LivingEntity target) {
        CombinationTriggerResult result = new CombinationTriggerResult();
        Map<String, CombinationLoader.ElementCombination> combinations = getCombinations();

        //获取攻击者的激活组合
        Set<String> activeCombinations = combinationCache.getCachedCombinations(attacker);

        for (String combinationId : activeCombinations) {
            CombinationLoader.ElementCombination combination = combinations.get(combinationId);
            if (combination != null && combination.attackTrigger != null) {
                processTriggerEffect(combination.attackTrigger, result, attacker, target, "attack");
            }
        }

        return result;
    }

    @Override
    public CombinationTriggerResult processDefenseTrigger(LivingEntity attacker, LivingEntity defender) {
        CombinationTriggerResult result = new CombinationTriggerResult();
        Map<String, CombinationLoader.ElementCombination> combinations = getCombinations();

        //获取防御者的激活组合
        Set<String> activeCombinations = combinationCache.getCachedCombinations(defender);

        for (String combinationId : activeCombinations) {
            CombinationLoader.ElementCombination combination = combinations.get(combinationId);
            if (combination != null && combination.defenseTrigger != null) {
                processTriggerEffect(combination.defenseTrigger, result, defender, attacker, "defense");
            }
        }

        return result;
    }

    @Override
    public void removeCombinationEffects(LivingEntity entity, String combinationId) {
        Map<String, UUID> entityModifiers = activeModifiers.get(entity);
        if (entityModifiers == null) return;

        List<String> toRemove = new ArrayList<>();
        for (String modifierKey : entityModifiers.keySet()) {
            if (modifierKey.startsWith(combinationId + ":")) {
                String attributeId = modifierKey.substring(combinationId.length() + 1);
                UUID modifierId = entityModifiers.get(modifierKey);
                elementSystem.removeAttributeModifier(entity, ResourceLocation.tryParse(attributeId), modifierId);
                toRemove.add(modifierKey);
            }
        }

        toRemove.forEach(entityModifiers::remove);
        if (entityModifiers.isEmpty()) {
            activeModifiers.remove(entity);
        }
    }

    @Override
    public Collection<String> getActiveCombinations(LivingEntity entity) {
        return combinationCache.getCachedCombinations(entity);
    }

    @Override
    public CombinationLoader getCombinationLoader() {
        return combinationLoader;
    }

    @Override
    public void reloadCombinations() {
        //重新加载组合数据
        if (dataService != null) {
            dataService.reloadCombinations();
        } else {
            combinationLoader.loadCombinations();
        }

        combinationCache.clear();
        conditionCache.clear();
        activeModifiers.clear();

        LOGGER.info("Combination data reloaded");
    }

    public void loadFromResources(Map<ResourceLocation, JsonElement> resources) {
        combinationLoader.loadFromResources(resources);
        combinationCache.clear();
        conditionCache.clear();
    }

    public void invalidateEntityCache(LivingEntity entity) {
        combinationCache.invalidate(entity);
    }

    public void onEntityRemoved(LivingEntity entity) {
        combinationCache.invalidate(entity);
        activeModifiers.remove(entity);
    }

    /**
     * 获取数据服务（用于调试和测试）
     */
    public ElementDataService getDataService() {
        return dataService;
    }
}