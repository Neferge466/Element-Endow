package com.element_endow.core;

import com.element_endow.api.IElementCombinationSystem;
import com.element_endow.api.IElementSystem;
import com.element_endow.data.CombinationLoader;
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
    private final Map<LivingEntity, Set<String>> activeCombinations;
    private final Map<LivingEntity, Map<String, UUID>> activeModifiers;
    private final Map<String, Integer> combinationDurations = new HashMap<>();

    public ElementCombinationSystem(IElementSystem elementSystem) {
        this.elementSystem = elementSystem;
        this.combinationLoader = new CombinationLoader();
        this.combinationLoader.loadCombinations();
        this.activeCombinations = new WeakHashMap<>();
        this.activeModifiers = new WeakHashMap<>();
        this.combinationDurations.put("default", 200);
    }

    @Override
    public void checkAndApplyCombinations(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return;
        }

        Set<String> previousCombinations = activeCombinations.getOrDefault(entity, Collections.emptySet());
        Set<String> newCombinations = new HashSet<>();

        //获取实体当前的所有元素
        Set<String> entityElements = new HashSet<>();
        for (String elementId : elementSystem.getEnabledElements()) {
            if (elementSystem.hasElement(entity, elementId)) {
                entityElements.add(elementId);
            }
        }

        //检查所有可能的组合
        for (CombinationLoader.ElementCombination combination : combinationLoader.getCombinations().values()) {
            if (matchesCombination(entity, entityElements, combination)) {
                newCombinations.add(combination.id);
                if (!previousCombinations.contains(combination.id)) {
                    applyCombinationEffects(entity, combination);
                }
            }
        }

        //移除不再激活的组合效果
        for (String oldCombination : previousCombinations) {
            if (!newCombinations.contains(oldCombination)) {
                removeCombinationEffects(entity, oldCombination);
            }
        }

        activeCombinations.put(entity, newCombinations);
    }

    private boolean matchesCombination(LivingEntity entity,
                                       Set<String> entityElements,
                                       CombinationLoader.ElementCombination combination) {
        //检查必需元素
        if (!entityElements.containsAll(combination.requiredElements)) {
            return false;
        }

        //检查禁止元素
        for (String forbidden : combination.forbiddenElements) {
            if (entityElements.contains(forbidden)) {
                return false;
            }
        }

        //检查最小值要求
        for (Map.Entry<String, Double> entry : combination.minValues.entrySet()) {
            double currentValue = elementSystem.getElementValue(entity, entry.getKey());
            if (currentValue < entry.getValue()) {
                return false;
            }
        }

        return true;
    }

    private void applyCombinationEffects(LivingEntity entity, CombinationLoader.ElementCombination combination) {
        Map<String, UUID> entityModifiers = activeModifiers.computeIfAbsent(entity, k -> new HashMap<>());

        for (CombinationLoader.CombinationEffect effect : combination.effects) {
            applyCombinationEffect(entity, effect, combination.id, entityModifiers);
        }
    }

    private void applyCombinationEffect(LivingEntity entity, CombinationLoader.CombinationEffect effect,
                                        String combinationId, Map<String, UUID> entityModifiers) {
        try {
            ResourceLocation attributeId = ResourceLocation.tryParse(effect.attribute);
            if (attributeId == null) {
                LOGGER.warn("Invalid attribute ID format: {}", effect.attribute);
                return;
            }

            String modifierKey = combinationId + ":" + effect.attribute;
            UUID modifierId = UUID.nameUUIDFromBytes(modifierKey.getBytes());

            AttributeModifier modifier = createAttributeModifier(modifierId, effect);
            if (modifier != null) {
                int duration = combinationDurations.getOrDefault(combinationId, combinationDurations.get("default"));

                boolean success = elementSystem.applyTimedAttributeModifier(entity, attributeId, modifier, duration);

                if (success) {
                    entityModifiers.put(modifierKey, modifierId);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to apply combination effect", e);
        }
    }

    private AttributeModifier createAttributeModifier(UUID modifierId, CombinationLoader.CombinationEffect effect) {
        try {
            AttributeModifier.Operation operation;

            switch (effect.operation.toLowerCase()) {
                case "add":
                    operation = AttributeModifier.Operation.ADDITION;
                    break;
                case "multiply_base":
                    operation = AttributeModifier.Operation.MULTIPLY_BASE;
                    break;
                case "multiply_total":
                    operation = AttributeModifier.Operation.MULTIPLY_TOTAL;
                    break;
                default:
                    operation = AttributeModifier.Operation.ADDITION;
                    break;
            }

            return new AttributeModifier(
                    modifierId,
                    "element_endow.combination." + effect.operation,
                    effect.value,
                    operation
            );

        } catch (Exception e) {
            LOGGER.error("Failed to create attribute modifier", e);
            return null;
        }
    }

    @Override
    public void removeCombinationEffects(LivingEntity entity, String combinationId) {
        Map<String, UUID> entityModifiers = activeModifiers.get(entity);
        if (entityModifiers == null) {
            return;
        }

        List<String> toRemove = new ArrayList<>();
        for (String modifierKey : entityModifiers.keySet()) {
            if (modifierKey.startsWith(combinationId + ":")) {
                String attributeId = modifierKey.substring(combinationId.length() + 1);
                removeModifier(entity, attributeId, entityModifiers.get(modifierKey));
                toRemove.add(modifierKey);
            }
        }

        toRemove.forEach(entityModifiers::remove);
        if (entityModifiers.isEmpty()) {
            activeModifiers.remove(entity);
        }
    }

    private void removeModifier(LivingEntity entity, String attributeId, UUID modifierId) {
        elementSystem.getAttributeById(attributeId).ifPresent(attribute -> {
            var instance = entity.getAttribute(attribute);
            if (instance != null && instance.getModifier(modifierId) != null) {
                instance.removeModifier(modifierId);
            }
        });
    }

    @Override
    public Collection<String> getActiveCombinations(LivingEntity entity) {
        return Collections.unmodifiableSet(activeCombinations.getOrDefault(entity, Collections.emptySet()));
    }

    @Override
    public CombinationLoader getCombinationLoader() {
        return combinationLoader;
    }

    @Override
    public void reloadCombinations() {
        combinationLoader.loadCombinations();

        for (LivingEntity entity : activeCombinations.keySet()) {
            if (entity != null && entity.isAlive()) {
                checkAndApplyCombinations(entity);
            }
        }
    }

    public void setCombinationDuration(String combinationId, int durationTicks) {
        combinationDurations.put(combinationId, durationTicks);
    }
}