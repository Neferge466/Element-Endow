package com.element_endow.core;

import com.element_endow.api.IElementSystem;
import com.element_endow.api.ReactionResult;
import com.element_endow.data.ElementDataService;
import com.element_endow.data.ReactionLoader;
import com.element_endow.util.ConditionChecker;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ElementReactionSystem implements com.element_endow.api.IElementReactionSystem {
    private static final Logger LOGGER = LogManager.getLogger();

    private final IElementSystem elementSystem;
    private final ReactionLoader reactionLoader;
    private final ElementDataService dataService;

    public ElementReactionSystem(IElementSystem elementSystem, ElementDataService dataService) {
        this.elementSystem = elementSystem;
        this.dataService = dataService;
        this.reactionLoader = new ReactionLoader();
        this.reactionLoader.loadReactions();

        LOGGER.info("Elemental reaction system initialization complete with DataService integration");
    }

    @Override
    public ReactionResult processAttackReaction(LivingEntity attacker, LivingEntity target, double baseDamage) {
        ReactionResult result = new ReactionResult();

        try {
            //获取攻击者和目标的元素
            Collection<String> attackerElements = new ArrayList<>();
            Collection<String> targetElements = new ArrayList<>();

            for (String elementId : elementSystem.getEnabledElements()) {
                if (elementSystem.hasElement(attacker, elementId)) {
                    attackerElements.add(elementId);
                }
                if (elementSystem.hasElement(target, elementId)) {
                    targetElements.add(elementId);
                }
            }

            //使用反应加载器处理攻击反应逻辑
            if (dataService != null) {
                processReactionsWithDataService(attackerElements, targetElements, result, attacker, target, "attack");
            } else {
                reactionLoader.processAttackReactions(attackerElements, targetElements, result, attacker, target);
            }

            LOGGER.debug("Processed attack reaction: {} attacker elements, {} target elements, damage multiplier: {}",
                    attackerElements.size(), targetElements.size(), result.damageMultiplier);

        } catch (Exception e) {
            LOGGER.error("Error processing attack reaction", e);
        }

        return result;
    }

    @Override
    public ReactionResult processDefenseReaction(LivingEntity attacker, LivingEntity defender, double incomingDamage) {
        ReactionResult result = new ReactionResult();

        try {
            //获取攻击者和防御者的元素
            Collection<String> attackerElements = new ArrayList<>();
            Collection<String> defenderElements = new ArrayList<>();

            for (String elementId : elementSystem.getEnabledElements()) {
                if (elementSystem.hasElement(attacker, elementId)) {
                    attackerElements.add(elementId);
                }
                if (elementSystem.hasElement(defender, elementId)) {
                    defenderElements.add(elementId);
                }
            }

            //使用反应加载器处理防御反应逻辑
            if (dataService != null) {
                processReactionsWithDataService(attackerElements, defenderElements, result, attacker, defender, "defense");
            } else {
                reactionLoader.processDefenseReactions(attackerElements, defenderElements, result, attacker, defender);
            }

            LOGGER.debug("Processed defense reaction: {} attacker elements, {} defender elements, defense multiplier: {}",
                    attackerElements.size(), defenderElements.size(), result.defenseMultiplier);

        } catch (Exception e) {
            LOGGER.error("Error processing defense reaction", e);
        }

        return result;
    }

    /**
     * 使用数据服务处理反应
     */
    private void processReactionsWithDataService(Collection<String> sourceElements,
                                                 Collection<String> targetElements,
                                                 ReactionResult result,
                                                 LivingEntity sourceEntity,
                                                 LivingEntity targetEntity,
                                                 String reactionType) {
        try {
            //从数据服务获取所有反应
            Map<String, ReactionLoader.ElementReaction> reactions = dataService.getAllReactions();
            int triggeredReactions = 0;

            for (ReactionLoader.ElementReaction reaction : reactions.values()) {
                boolean hasElementA = sourceElements.contains(reaction.elementA) && targetElements.contains(reaction.elementB);
                boolean hasElementB = sourceElements.contains(reaction.elementB) && targetElements.contains(reaction.elementA);

                if ((hasElementA || hasElementB) && checkReactionConditions(reaction, sourceEntity, targetEntity)) {
                    if ("attack".equals(reactionType)) {
                        applyAttackReaction(reaction, result, sourceEntity, targetEntity);
                    } else {
                        applyDefenseReaction(reaction, result, sourceEntity, targetEntity);
                    }
                    triggeredReactions++;
                }
            }

            if (triggeredReactions > 0) {
                LOGGER.debug("Triggered {} {} reactions using DataService", triggeredReactions, reactionType);
            }

        } catch (Exception e) {
            LOGGER.error("Error processing reactions with DataService, falling back to legacy loader", e);
            // 回退到传统加载器
            if ("attack".equals(reactionType)) {
                reactionLoader.processAttackReactions(sourceElements, targetElements, result, sourceEntity, targetEntity);
            } else {
                reactionLoader.processDefenseReactions(sourceElements, targetElements, result, sourceEntity, targetEntity);
            }
        }
    }

    /**
     * 检查反应条件
     */
    private boolean checkReactionConditions(ReactionLoader.ElementReaction reaction,
                                            LivingEntity entity1, LivingEntity entity2) {
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

    /**
     * 应用攻击反应
     */
    private void applyAttackReaction(ReactionLoader.ElementReaction reaction, ReactionResult result,
                                     LivingEntity attacker, LivingEntity target) {
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

    /**
     * 应用防御反应
     */
    private void applyDefenseReaction(ReactionLoader.ElementReaction reaction, ReactionResult result,
                                      LivingEntity attacker, LivingEntity defender) {
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

    /**
     * 应用反应条目
     */
    private void applyReactionEntry(ReactionLoader.ReactionEntry entry, ReactionResult result,
                                    LivingEntity target, LivingEntity source) {
        result.damageMultiplier *= entry.damageMultiplier;
        result.defenseMultiplier *= entry.defenseMultiplier;
        result.extraDamage += entry.extraDamage;
        result.damageReduction += entry.damageReduction;

        if (entry.targetEffects != null && target != null) {
            for (ReactionLoader.ReactionEffect effect : entry.targetEffects) {
                net.minecraft.world.effect.MobEffectInstance effectInstance = createEffectInstance(effect);
                if (effectInstance != null) {
                    target.addEffect(effectInstance);
                }
            }
        }

        if (entry.selfEffects != null && source != null) {
            for (ReactionLoader.ReactionEffect effect : entry.selfEffects) {
                net.minecraft.world.effect.MobEffectInstance effectInstance = createEffectInstance(effect);
                if (effectInstance != null) {
                    source.addEffect(effectInstance);
                }
            }
        }
    }

    /**
     * 应用属性修饰符
     */
    private void applyAttributeModifiers(ReactionLoader.ReactionEntry entry, ReactionResult result,
                                         LivingEntity target, LivingEntity source) {
        if (entry.targetAttributeModifiers != null && target != null) {
            for (ReactionLoader.AttributeModifierData modifierData : entry.targetAttributeModifiers) {
                ReactionResult.AttributeModifierApplication app = modifierData.toAttributeModifierApplication();
                if (app != null) {
                    result.targetAttributeModifiers.add(app);
                }
            }
        }

        if (entry.selfAttributeModifiers != null && source != null) {
            for (ReactionLoader.AttributeModifierData modifierData : entry.selfAttributeModifiers) {
                ReactionResult.AttributeModifierApplication app = modifierData.toAttributeModifierApplication();
                if (app != null) {
                    result.selfAttributeModifiers.add(app);
                }
            }
        }
    }

    /**
     * 创建效果实例
     */
    private net.minecraft.world.effect.MobEffectInstance createEffectInstance(ReactionLoader.ReactionEffect effect) {
        try {
            net.minecraft.resources.ResourceLocation effectId = new net.minecraft.resources.ResourceLocation(effect.effect);
            net.minecraft.world.effect.MobEffect mobEffect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(effectId);

            if (mobEffect == null) {
                return null;
            }

            return new net.minecraft.world.effect.MobEffectInstance(
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

    @Override
    public ReactionLoader getReactionLoader() {
        return reactionLoader;
    }

    @Override
    public void reloadReactions() {
        //重新加载反应数据
        if (dataService != null) {
            dataService.reloadReactions();
        } else {
            reactionLoader.loadReactions();
        }

        LOGGER.info("Element reactions reloaded with DataService integration");
    }

    /**
     * 获取数据服务（用于调试和测试）
     */
    public ElementDataService getDataService() {
        return dataService;
    }
}