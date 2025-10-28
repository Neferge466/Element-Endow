package com.element_endow.data;

import com.element_endow.api.IElementMountSystem;
import com.element_endow.api.ReactionResult;
import com.element_endow.util.ConditionChecker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ReactionLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private final Map<String, ElementReaction> reactions = new HashMap<>();

    public void loadReactions() {
        reactions.clear();
        LOGGER.info("Loaded {} element reactions", reactions.size());
    }

    public void loadFromResources(Map<ResourceLocation, JsonElement> resources) {
        reactions.clear();

        int loadedCount = 0;
        int errorCount = 0;

        LOGGER.info("Processing {} reaction resources", resources.size());

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();

            if (!location.getNamespace().equals("element_endow")) {
                continue;
            }

            LOGGER.info("Loading reaction file: {}", location);
            try {
                ElementReaction reaction = GSON.fromJson(entry.getValue(), ElementReaction.class);

                if (validateReaction(reaction)) {
                    reactions.put(reaction.id, reaction);
                    loadedCount++;
                    LOGGER.info("Successfully loaded reaction: {}", reaction.id);
                } else {
                    LOGGER.warn("Invalid reaction data: {}", location);
                    errorCount++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load reaction {}: {}", location, e.getMessage());
                errorCount++;
            }
        }

        LOGGER.info("Loaded {} element reactions from resources ({} errors)", loadedCount, errorCount);
        validateReactionEffects();
    }

    private MobEffectInstance createEffectInstance(ReactionEffect effect) {
        try {
            ResourceLocation effectId = new ResourceLocation(effect.effect);
            MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(effectId);

            if (mobEffect == null) {
                LOGGER.error("Effect not found: {}. Please check if the effect ID is correct.", effect.effect);
                return null;
            }

            return new MobEffectInstance(
                    mobEffect,
                    effect.duration,
                    effect.amplifier,
                    false,
                    effect.showParticles,
                    true
            );

        } catch (Exception e) {
            LOGGER.error("Failed to create effect instance for: {}. Error: {}", effect.effect, e.getMessage());
            return null;
        }
    }

    private boolean validateEffect(String effectId) {
        try {
            ResourceLocation location = new ResourceLocation(effectId);
            return BuiltInRegistries.MOB_EFFECT.containsKey(location);
        } catch (Exception e) {
            LOGGER.warn("Invalid effect ID format: {}", effectId);
            return false;
        }
    }

    public void validateReactionEffects() {
        LOGGER.info("Validating reaction effects...");

        int validEffects = 0;
        int invalidEffects = 0;

        for (ElementReaction reaction : reactions.values()) {
            if (reaction.attackEntry != null) {
                validEffects += validateEffectList(reaction.attackEntry.targetEffects, reaction.id, "attack target");
                validEffects += validateEffectList(reaction.attackEntry.selfEffects, reaction.id, "attack self");
            }

            if (reaction.defenseEntry != null) {
                validEffects += validateEffectList(reaction.defenseEntry.targetEffects, reaction.id, "defense target");
                validEffects += validateEffectList(reaction.defenseEntry.selfEffects, reaction.id, "defense self");
            }
        }

        LOGGER.info("Effect validation complete: {} valid effects found", validEffects);
        if (invalidEffects > 0) {
            LOGGER.warn("{} invalid effects were found and will be ignored", invalidEffects);
        }
    }

    private int validateEffectList(List<ReactionEffect> effects, String reactionId, String effectType) {
        if (effects == null) return 0;

        int validCount = 0;
        for (ReactionEffect effect : effects) {
            if (validateEffect(effect.effect)) {
                validCount++;
            } else {
                LOGGER.warn("Invalid {} effect in reaction {}: {}", effectType, reactionId, effect.effect);
            }
        }
        return validCount;
    }

    public void processAttackReactions(Collection<String> attackerElements, Collection<String> targetElements,
                                       ReactionResult result, LivingEntity attacker, LivingEntity target) {
        int triggeredReactions = 0;

        for (ElementReaction reaction : reactions.values()) {
            boolean hasElementA = attackerElements.contains(reaction.elementA) && targetElements.contains(reaction.elementB);
            boolean hasElementB = attackerElements.contains(reaction.elementB) && targetElements.contains(reaction.elementA);

            if ((hasElementA || hasElementB) && checkReactionConditions(reaction, attacker, target)) {
                applyAttackReaction(reaction, result, attacker, target);
                triggeredReactions++;
            }
        }

        if (triggeredReactions > 0) {
            LOGGER.debug("Triggered {} attack reactions", triggeredReactions);
        }
    }

    public void processDefenseReactions(Collection<String> attackerElements, Collection<String> defenderElements,
                                        ReactionResult result, LivingEntity attacker, LivingEntity defender) {
        int triggeredReactions = 0;

        for (ElementReaction reaction : reactions.values()) {
            boolean hasElementA = attackerElements.contains(reaction.elementA) && defenderElements.contains(reaction.elementB);
            boolean hasElementB = attackerElements.contains(reaction.elementB) && defenderElements.contains(reaction.elementA);

            if ((hasElementA || hasElementB) && checkReactionConditions(reaction, attacker, defender)) {
                applyDefenseReaction(reaction, result, attacker, defender);
                triggeredReactions++;
            }
        }

        if (triggeredReactions > 0) {
            LOGGER.debug("Triggered {} defense reactions", triggeredReactions);
        }
    }

    private boolean checkReactionConditions(ElementReaction reaction, LivingEntity entity1, LivingEntity entity2) {
        if (reaction.conditions == null) {
            return true;
        }

        if (reaction.conditions.attackerConditions != null) {
            if (!ConditionChecker.checkConditions(reaction.conditions.attackerConditions, entity1, entity1.level())) {
                return false;
            }
        }

        if (reaction.conditions.targetConditions != null) {
            if (!ConditionChecker.checkConditions(reaction.conditions.targetConditions, entity2, entity2.level())) {
                return false;
            }
        }

        if (reaction.conditions.worldConditions != null) {
            if (!ConditionChecker.checkConditions(reaction.conditions.worldConditions, entity1, entity1.level())) {
                return false;
            }
        }

        return true;
    }

    private void applyAttackReaction(ElementReaction reaction, ReactionResult result, LivingEntity attacker, LivingEntity target) {
        LOGGER.info("Applying attack reaction: {}", reaction.id);

        if (reaction.attackEntry != null) {
            applyReactionEntry(reaction.attackEntry, result, target, attacker);
            applyAttributeModifiers(reaction.attackEntry, result, target, attacker);
        }

        if (reaction.mountData != null) {
            result.mountApplications.add(new ReactionResult.MountApplication(
                    reaction.mountData.elementId,
                    reaction.mountData.amount,
                    reaction.mountData.duration,
                    reaction.mountData.probability
            ));
        }

        if (reaction.advancedMountData != null) {
            result.advancedMountApplications.add(reaction.advancedMountData);
        }
    }

    private void applyDefenseReaction(ElementReaction reaction, ReactionResult result, LivingEntity attacker, LivingEntity defender) {
        LOGGER.info("Applying defense reaction: {}", reaction.id);

        if (reaction.defenseEntry != null) {
            applyReactionEntry(reaction.defenseEntry, result, defender, attacker);
            applyAttributeModifiers(reaction.defenseEntry, result, defender, attacker);
        }

        if (reaction.mountData != null) {
            result.mountApplications.add(new ReactionResult.MountApplication(
                    reaction.mountData.elementId,
                    reaction.mountData.amount,
                    reaction.mountData.duration,
                    reaction.mountData.probability
            ));
        }

        if (reaction.advancedMountData != null) {
            result.advancedMountApplications.add(reaction.advancedMountData);
        }
    }

    private void applyAttributeModifiers(ReactionEntry entry, ReactionResult result, LivingEntity target, LivingEntity source) {
        LOGGER.info("Processing attribute modifiers - target: {}, self: {}", target, source);

        if (entry.targetAttributeModifiers != null && target != null) {
            LOGGER.info("Found {} target attribute modifiers", entry.targetAttributeModifiers.size());
            for (AttributeModifierData modifierData : entry.targetAttributeModifiers) {
                AttributeModifier modifier = createAttributeModifier(modifierData);
                if (modifier != null) {
                    int duration = modifierData.duration > 0 ? modifierData.duration : 100;
                    result.targetAttributeModifiers.add(new ReactionResult.AttributeModifierApplication(
                            modifierData.attribute, modifier, modifierData.permanent, duration
                    ));
                    LOGGER.info("Added target attribute modifier: {}, value: {}, duration: {} ticks",
                            modifierData.attribute, modifierData.value, duration);
                }
            }
        }

        if (entry.selfAttributeModifiers != null && source != null) {
            LOGGER.info("Found {} self attribute modifiers", entry.selfAttributeModifiers.size());
            for (AttributeModifierData modifierData : entry.selfAttributeModifiers) {
                AttributeModifier modifier = createAttributeModifier(modifierData);
                if (modifier != null) {
                    int duration = modifierData.duration > 0 ? modifierData.duration : 100;
                    result.selfAttributeModifiers.add(new ReactionResult.AttributeModifierApplication(
                            modifierData.attribute, modifier, modifierData.permanent, duration
                    ));
                    LOGGER.info("Added self attribute modifier: {}, value: {}, duration: {} ticks",
                            modifierData.attribute, modifierData.value, duration);
                }
            }
        }
    }

    private AttributeModifier createAttributeModifier(AttributeModifierData modifierData) {
        try {
            AttributeModifier.Operation operation;

            switch (modifierData.operation.toLowerCase()) {
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
                    LOGGER.error("Unknown attribute operation type: {}, using default ADDITION", modifierData.operation);
                    operation = AttributeModifier.Operation.ADDITION;
                    break;
            }

            UUID modifierId;
            if (modifierData.uuid != null && !modifierData.uuid.trim().isEmpty()) {
                try {
                    modifierId = UUID.fromString(modifierData.uuid);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid UUID format: {}, generating random UUID", modifierData.uuid);
                    modifierId = UUID.randomUUID();
                }
            } else {
                modifierId = UUID.randomUUID();
            }

            String modifierName = modifierData.name != null ?
                    modifierData.name : "element_endow.reaction." + modifierData.operation;

            LOGGER.info("Creating attribute modifier: ID={}, name={}, value={}, operation={}",
                    modifierId, modifierName, modifierData.value, operation);

            return new AttributeModifier(
                    modifierId,
                    modifierName,
                    modifierData.value,
                    operation
            );

        } catch (Exception e) {
            LOGGER.error("Failed to create attribute modifier: {}", modifierData.attribute, e);
            return null;
        }
    }

    private void applyReactionEntry(ReactionEntry entry, ReactionResult result, LivingEntity target, LivingEntity source) {
        result.damageMultiplier *= entry.damageMultiplier;
        result.defenseMultiplier *= entry.defenseMultiplier;
        result.extraDamage += entry.extraDamage;
        result.damageReduction += entry.damageReduction;

        if (entry.targetEffects != null && target != null) {
            for (ReactionEffect effect : entry.targetEffects) {
                MobEffectInstance effectInstance = createEffectInstance(effect);
                if (effectInstance != null) {
                    target.addEffect(effectInstance);
                    LOGGER.debug("Applied effect {} to target {}", effect.effect, target);
                }
            }
        }

        if (entry.selfEffects != null && source != null) {
            for (ReactionEffect effect : entry.selfEffects) {
                MobEffectInstance effectInstance = createEffectInstance(effect);
                if (effectInstance != null) {
                    source.addEffect(effectInstance);
                    LOGGER.debug("Applied effect {} to self {}", effect.effect, source);
                }
            }
        }
    }

    private boolean validateReaction(ElementReaction reaction) {
        if (reaction.id == null || reaction.id.isEmpty()) {
            LOGGER.error("Reaction missing ID");
            return false;
        }

        if (reaction.elementA == null || reaction.elementA.isEmpty()) {
            LOGGER.error("Reaction {} missing elementA", reaction.id);
            return false;
        }

        if (reaction.elementB == null || reaction.elementB.isEmpty()) {
            LOGGER.error("Reaction {} missing elementB", reaction.id);
            return false;
        }

        if (reaction.attackEntry != null) {
            if (reaction.attackEntry.targetEffects == null) reaction.attackEntry.targetEffects = new ArrayList<>();
            if (reaction.attackEntry.selfEffects == null) reaction.attackEntry.selfEffects = new ArrayList<>();
            if (reaction.attackEntry.targetAttributeModifiers == null) reaction.attackEntry.targetAttributeModifiers = new ArrayList<>();
            if (reaction.attackEntry.selfAttributeModifiers == null) reaction.attackEntry.selfAttributeModifiers = new ArrayList<>();
        }

        if (reaction.defenseEntry != null) {
            if (reaction.defenseEntry.targetEffects == null) reaction.defenseEntry.targetEffects = new ArrayList<>();
            if (reaction.defenseEntry.selfEffects == null) reaction.defenseEntry.selfEffects = new ArrayList<>();
            if (reaction.defenseEntry.targetAttributeModifiers == null) reaction.defenseEntry.targetAttributeModifiers = new ArrayList<>();
            if (reaction.defenseEntry.selfAttributeModifiers == null) reaction.defenseEntry.selfAttributeModifiers = new ArrayList<>();
        }

        return true;
    }

    public static class ElementReaction {
        public String id;
        public String elementA;
        public String elementB;
        public int priority;
        public ReactionConditions conditions;
        public ReactionEntry attackEntry;
        public ReactionEntry defenseEntry;
        public MountData mountData;
        public IElementMountSystem.AdvancedMountData advancedMountData;
    }

    public static class ReactionConditions {
        public Map<String, Object> attackerConditions;
        public Map<String, Object> targetConditions;
        public Map<String, Object> worldConditions;
    }

    public static class ReactionEntry {
        public double damageMultiplier = 1.0;
        public double defenseMultiplier = 1.0;
        public double extraDamage = 0.0;
        public double damageReduction = 0.0;
        public List<ReactionEffect> targetEffects;
        public List<ReactionEffect> selfEffects;
        public List<AttributeModifierData> targetAttributeModifiers;
        public List<AttributeModifierData> selfAttributeModifiers;
    }

    public static class ReactionEffect {
        public String effect;
        public int duration;
        public int amplifier;
        public boolean showParticles = true;
    }

    public static class MountData {
        public String elementId;
        public double amount;
        public int duration;
        public double probability;
    }

    public static class AttributeModifierData {
        public String attribute;
        public String operation;
        public double value;
        public String name;
        public String uuid;
        public boolean permanent = false;
        public int duration = 100;
    }

    public Map<String, ElementReaction> getReactions() {
        return new HashMap<>(reactions);
    }

    public int getReactionCount() {
        return reactions.size();
    }

    public boolean hasReaction(String reactionId) {
        return reactions.containsKey(reactionId);
    }

    public ElementReaction getReaction(String reactionId) {
        return reactions.get(reactionId);
    }
}