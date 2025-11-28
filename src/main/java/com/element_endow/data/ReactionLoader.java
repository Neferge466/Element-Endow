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
        LOGGER.info("ReactionLoader initialized with empty reactions (will be populated by DataService)");
    }

    public void loadFromResources(Map<ResourceLocation, JsonElement> resources) {
        reactions.clear();

        int loadedCount = 0;
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();

            if (!location.getNamespace().equals("element_endow")) {
                continue;
            }

            try {
                ElementReaction reaction = GSON.fromJson(entry.getValue(), ElementReaction.class);

                if (validateReaction(reaction)) {
                    reactions.put(reaction.id, reaction);
                    loadedCount++;
                    LOGGER.debug("Loaded reaction from resources: {}", reaction.id);
                } else {
                    errorCount++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load reaction {}: {}", location, e.getMessage());
                errorCount++;
            }
        }

        LOGGER.info("Loaded {} element reactions from resources ({} errors)", loadedCount, errorCount);
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

        // 初始化空列表避免NPE
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
            LOGGER.debug("Triggered {} attack reactions from legacy loader", triggeredReactions);
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
            LOGGER.debug("Triggered {} defense reactions from legacy loader", triggeredReactions);
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
        if (entry.targetAttributeModifiers != null && target != null) {
            for (AttributeModifierData modifierData : entry.targetAttributeModifiers) {
                ReactionResult.AttributeModifierApplication app = modifierData.toAttributeModifierApplication();
                if (app != null) {
                    result.targetAttributeModifiers.add(app);
                }
            }
        }

        if (entry.selfAttributeModifiers != null && source != null) {
            for (AttributeModifierData modifierData : entry.selfAttributeModifiers) {
                ReactionResult.AttributeModifierApplication app = modifierData.toAttributeModifierApplication();
                if (app != null) {
                    result.selfAttributeModifiers.add(app);
                }
            }
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
                }
            }
        }

        if (entry.selfEffects != null && source != null) {
            for (ReactionEffect effect : entry.selfEffects) {
                MobEffectInstance effectInstance = createEffectInstance(effect);
                if (effectInstance != null) {
                    source.addEffect(effectInstance);
                }
            }
        }
    }

    private MobEffectInstance createEffectInstance(ReactionEffect effect) {
        try {
            ResourceLocation effectId = new ResourceLocation(effect.effect);
            MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(effectId);

            if (mobEffect == null) {
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
            LOGGER.error("Failed to create effect instance: {}", effect.effect, e);
            return null;
        }
    }

    // 数据类定义
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

        /**
         * 转换为 AttributeModifierApplication 类型
         */
        public ReactionResult.AttributeModifierApplication toAttributeModifierApplication() {
            AttributeModifier modifier = createAttributeModifier();
            if (modifier == null) {
                return null;
            }
            return new ReactionResult.AttributeModifierApplication(
                    this.attribute,
                    modifier,
                    this.permanent,
                    this.duration
            );
        }

        private AttributeModifier createAttributeModifier() {
            try {
                AttributeModifier.Operation operation = getOperation();

                UUID modifierId;
                if (uuid != null && !uuid.trim().isEmpty()) {
                    try {
                        modifierId = UUID.fromString(uuid);
                    } catch (IllegalArgumentException e) {
                        modifierId = UUID.randomUUID();
                    }
                } else {
                    modifierId = UUID.randomUUID();
                }

                String modifierName = name != null ? name : "element_endow.reaction." + operation;

                return new AttributeModifier(modifierId, modifierName, value, operation);

            } catch (Exception e) {
                LOGGER.error("Failed to create attribute modifier for reaction", e);
                return null;
            }
        }

        private AttributeModifier.Operation getOperation() {
            switch (operation.toLowerCase()) {
                case "add": return AttributeModifier.Operation.ADDITION;
                case "multiply_base": return AttributeModifier.Operation.MULTIPLY_BASE;
                case "multiply_total": return AttributeModifier.Operation.MULTIPLY_TOTAL;
                default: return AttributeModifier.Operation.ADDITION;
            }
        }
    }

    public Map<String, ElementReaction> getReactions() {
        return new HashMap<>(reactions);
    }

    public int getReactionCount() {
        return reactions.size();
    }
}