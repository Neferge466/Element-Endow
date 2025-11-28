package com.element_endow.event;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.api.IElementCombinationSystem;
import com.element_endow.api.ReactionResult;
import com.element_endow.data.CombinationLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

@Mod.EventBusSubscriber(modid = "element_endow", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ElementAttackEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity attacker = (LivingEntity) event.getSource().getEntity();
        LivingEntity target = (LivingEntity) event.getEntity();

        try {
            var elementSystem = ElementSystemAPI.getElementSystem();
            var reactionSystem = elementSystem.getReactionSystem();
            var combinationSystem = elementSystem.getCombinationSystem();
            var mountSystem = elementSystem.getMountSystem();

            // 检查攻击者和目标是否有有效的元素
            if (!hasValidElements(attacker) && !hasValidElements(target)) {
                return;
            }

            //处理元素反应和组合触发
            ReactionResult attackResult = reactionSystem.processAttackReaction(attacker, target, event.getAmount());
            ReactionResult defenseResult = reactionSystem.processDefenseReaction(attacker, target, event.getAmount());
            var combinationAttackResult = processCombinationAttackTriggers(attacker, target);
            var combinationDefenseResult = processCombinationDefenseTriggers(attacker, target);

            //合并所有结果
            ReactionResult combinedResult = combineResults(attackResult, defenseResult,
                    combinationAttackResult, combinationDefenseResult);

            applyReactionResult(event, combinedResult, attacker, target);
            applyMountEffects(attacker, target, combinedResult, mountSystem);
            processCombinationEffects(attacker, target, combinationAttackResult);

        } catch (Exception e) {
            LOGGER.error("Error processing element reactions and combinations", e);
        }
    }

    /**
     * 检查实体是否有有效的元素
     */
    private static boolean hasValidElements(LivingEntity entity) {
        var elementSystem = ElementSystemAPI.getElementSystem();
        for (String elementId : elementSystem.getEnabledElements()) {
            if (elementSystem.hasElement(entity, elementId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理组合攻击触发效果
     */
    private static IElementCombinationSystem.CombinationTriggerResult processCombinationAttackTriggers(LivingEntity attacker, LivingEntity target) {
        IElementCombinationSystem.CombinationTriggerResult result = new IElementCombinationSystem.CombinationTriggerResult();

        try {
            var combinationSystem = ElementSystemAPI.getElementSystem().getCombinationSystem();
            var activeCombinations = combinationSystem.getActiveCombinations(attacker);

            if (activeCombinations.isEmpty()) {
                return result;
            }

            // 使用数据服务获取组合数据
            var combinations = combinationSystem.getCombinationLoader().getCombinations();

            for (String combinationId : activeCombinations) {
                CombinationLoader.ElementCombination combination = combinations.get(combinationId);
                if (combination == null || combination.attackTrigger == null) {
                    continue;
                }

                CombinationLoader.TriggerEffect trigger = combination.attackTrigger;

                //检查触发概率
                if (RANDOM.nextDouble() >= trigger.probability) {
                    continue;
                }

                //应用伤害效果
                result.damageMultiplier *= trigger.damageMultiplier;
                result.extraDamage += trigger.extraDamage;

                //应用效果
                if (trigger.targetEffects != null) {
                    for (CombinationLoader.EffectData effectData : trigger.targetEffects) {
                        MobEffectInstance effectInstance = createEffectInstance(effectData);
                        if (effectInstance != null) {
                            result.targetEffects.add(effectInstance);
                        }
                    }
                }

                if (trigger.selfEffects != null) {
                    for (CombinationLoader.EffectData effectData : trigger.selfEffects) {
                        MobEffectInstance effectInstance = createEffectInstance(effectData);
                        if (effectInstance != null) {
                            result.selfEffects.add(effectInstance);
                        }
                    }
                }

                //应用属性修饰符
                if (trigger.targetAttributeModifiers != null && !trigger.targetAttributeModifiers.isEmpty()) {
                    for (CombinationLoader.AttributeModifierData modifierData : trigger.targetAttributeModifiers) {
                        ReactionResult.AttributeModifierApplication app = modifierData.toAttributeModifierApplication();
                        if (app != null) {
                            result.targetAttributeModifiers.add(app);
                        }
                    }
                }

                if (trigger.selfAttributeModifiers != null && !trigger.selfAttributeModifiers.isEmpty()) {
                    for (CombinationLoader.AttributeModifierData modifierData : trigger.selfAttributeModifiers) {
                        ReactionResult.AttributeModifierApplication app = modifierData.toAttributeModifierApplication();
                        if (app != null) {
                            result.selfAttributeModifiers.add(app);
                        }
                    }
                }

                // 应用挂载效果
                if (trigger.mountApplications != null) {
                    for (var mountApp : trigger.mountApplications) {
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

        } catch (Exception e) {
            LOGGER.error("Error processing combination attack triggers", e);
        }

        return result;
    }

    /**
     * 处理组合防御触发效果
     */
    private static IElementCombinationSystem.CombinationTriggerResult processCombinationDefenseTriggers(LivingEntity attacker, LivingEntity defender) {
        IElementCombinationSystem.CombinationTriggerResult result = new IElementCombinationSystem.CombinationTriggerResult();

        try {
            var combinationSystem = ElementSystemAPI.getElementSystem().getCombinationSystem();
            var activeCombinations = combinationSystem.getActiveCombinations(defender);

            if (activeCombinations.isEmpty()) {
                return result;
            }

            // 使用数据服务获取组合数据
            var combinations = combinationSystem.getCombinationLoader().getCombinations();

            for (String combinationId : activeCombinations) {
                CombinationLoader.ElementCombination combination = combinations.get(combinationId);
                if (combination == null || combination.defenseTrigger == null) {
                    continue;
                }

                CombinationLoader.TriggerEffect trigger = combination.defenseTrigger;

                if (RANDOM.nextDouble() >= trigger.probability) {
                    continue;
                }

                //应用防御效果
                result.defenseMultiplier *= trigger.defenseMultiplier;
                result.damageReduction += trigger.damageReduction;

                //应用效果
                if (trigger.targetEffects != null) {
                    for (CombinationLoader.EffectData effectData : trigger.targetEffects) {
                        MobEffectInstance effectInstance = createEffectInstance(effectData);
                        if (effectInstance != null) {
                            result.targetEffects.add(effectInstance);
                        }
                    }
                }

                if (trigger.selfEffects != null) {
                    for (CombinationLoader.EffectData effectData : trigger.selfEffects) {
                        MobEffectInstance effectInstance = createEffectInstance(effectData);
                        if (effectInstance != null) {
                            result.selfEffects.add(effectInstance);
                        }
                    }
                }

                //应用属性修饰符
                if (trigger.targetAttributeModifiers != null && !trigger.targetAttributeModifiers.isEmpty()) {
                    for (CombinationLoader.AttributeModifierData modifierData : trigger.targetAttributeModifiers) {
                        ReactionResult.AttributeModifierApplication app = modifierData.toAttributeModifierApplication();
                        if (app != null) {
                            result.targetAttributeModifiers.add(app);
                        }
                    }
                }

                if (trigger.selfAttributeModifiers != null && !trigger.selfAttributeModifiers.isEmpty()) {
                    for (CombinationLoader.AttributeModifierData modifierData : trigger.selfAttributeModifiers) {
                        ReactionResult.AttributeModifierApplication app = modifierData.toAttributeModifierApplication();
                        if (app != null) {
                            result.selfAttributeModifiers.add(app);
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error processing combination defense triggers", e);
        }

        return result;
    }

    /**
     * 创建效果实例 - 使用组合系统的 EffectData
     */
    private static MobEffectInstance createEffectInstance(CombinationLoader.EffectData effectData) {
        try {
            ResourceLocation effectId = new ResourceLocation(effectData.effect);
            MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(effectId);

            if (mobEffect == null) {
                LOGGER.warn("Effect not found: {}", effectData.effect);
                return null;
            }

            return new MobEffectInstance(
                    mobEffect,
                    effectData.duration,
                    effectData.amplifier,
                    false,
                    effectData.showParticles,
                    true
            );

        } catch (Exception e) {
            LOGGER.error("Failed to create effect instance: {}", effectData.effect, e);
            return null;
        }
    }

    /**
     * 合并结果
     */
    private static ReactionResult combineResults(ReactionResult attackResult, ReactionResult defenseResult,
                                                 IElementCombinationSystem.CombinationTriggerResult combinationAttack,
                                                 IElementCombinationSystem.CombinationTriggerResult combinationDefense) {
        ReactionResult combined = new ReactionResult();

        //合并乘数
        combined.damageMultiplier = attackResult.damageMultiplier * defenseResult.defenseMultiplier
                * combinationAttack.damageMultiplier * combinationDefense.damageMultiplier;
        combined.defenseMultiplier = defenseResult.defenseMultiplier * combinationDefense.defenseMultiplier;
        combined.extraDamage = attackResult.extraDamage + defenseResult.extraDamage
                + combinationAttack.extraDamage + combinationDefense.extraDamage;
        combined.damageReduction = attackResult.damageReduction + defenseResult.damageReduction
                + combinationAttack.damageReduction + combinationDefense.damageReduction;

        //合并效果
        combined.targetEffects.addAll(attackResult.targetEffects);
        combined.targetEffects.addAll(defenseResult.targetEffects);
        combined.selfEffects.addAll(attackResult.selfEffects);
        combined.selfEffects.addAll(defenseResult.selfEffects);

        //合并属性修饰符
        combined.targetAttributeModifiers.addAll(attackResult.targetAttributeModifiers);
        combined.targetAttributeModifiers.addAll(defenseResult.targetAttributeModifiers);
        combined.selfAttributeModifiers.addAll(attackResult.selfAttributeModifiers);
        combined.selfAttributeModifiers.addAll(defenseResult.selfAttributeModifiers);

        //合并组合触发效果
        combined.targetEffects.addAll(combinationAttack.targetEffects);
        combined.targetEffects.addAll(combinationDefense.targetEffects);
        combined.selfEffects.addAll(combinationAttack.selfEffects);
        combined.selfEffects.addAll(combinationDefense.selfEffects);
        combined.targetAttributeModifiers.addAll(combinationAttack.targetAttributeModifiers);
        combined.targetAttributeModifiers.addAll(combinationDefense.targetAttributeModifiers);
        combined.selfAttributeModifiers.addAll(combinationAttack.selfAttributeModifiers);
        combined.selfAttributeModifiers.addAll(combinationDefense.selfAttributeModifiers);
        combined.mountApplications.addAll(combinationAttack.mountApplications);
        combined.mountApplications.addAll(combinationDefense.mountApplications);
        combined.advancedMountApplications.addAll(combinationAttack.advancedMountApplications);
        combined.advancedMountApplications.addAll(combinationDefense.advancedMountApplications);

        return combined;
    }

    /**
     * 应用反应结果
     */
    private static void applyReactionResult(LivingHurtEvent event, ReactionResult result, LivingEntity attacker, LivingEntity target) {
        float originalDamage = event.getAmount();
        float finalDamage = (float) ((originalDamage * result.damageMultiplier) + result.extraDamage);
        finalDamage = (float) Math.max(0, finalDamage - result.damageReduction);
        event.setAmount(finalDamage);

        LOGGER.debug("Damage calculation: original={}, multiplier={}, extra={}, reduction={}, final={}",
                originalDamage, result.damageMultiplier, result.extraDamage, result.damageReduction, finalDamage);

        //应用目标效果
        for (var effect : result.targetEffects) {
            if (effect != null) {
                target.addEffect(effect);
                LOGGER.debug("Applied target effect: {} to {}", effect.getEffect().getDisplayName().getString(), target);
            }
        }

        //应用自身效果
        for (var effect : result.selfEffects) {
            if (effect != null) {
                attacker.addEffect(effect);
                LOGGER.debug("Applied self effect: {} to {}", effect.getEffect().getDisplayName().getString(), attacker);
            }
        }

        applyAttributeModifiers(result, attacker, target);
    }

    /**
     * 单独处理组合效果
     */
    private static void processCombinationEffects(LivingEntity attacker, LivingEntity target,
                                                  IElementCombinationSystem.CombinationTriggerResult combinationResult) {
        //确保组合的目标效果被应用
        for (var effect : combinationResult.targetEffects) {
            if (effect != null && target != null) {
                target.addEffect(effect);
                LOGGER.debug("Applied combination target effect: {} to {}",
                        effect.getEffect().getDisplayName().getString(), target);
            }
        }

        //确保组合的自身效果被应用
        for (var effect : combinationResult.selfEffects) {
            if (effect != null && attacker != null) {
                attacker.addEffect(effect);
                LOGGER.debug("Applied combination self effect: {} to {}",
                        effect.getEffect().getDisplayName().getString(), attacker);
            }
        }
    }

    /**
     * 应用属性修饰符
     */
    private static void applyAttributeModifiers(ReactionResult result, LivingEntity attacker, LivingEntity target) {
        var elementSystem = ElementSystemAPI.getElementSystem();

        for (ReactionResult.AttributeModifierApplication modifierApp : result.targetAttributeModifiers) {
            if (target != null && modifierApp.attributeId != null && modifierApp.modifier != null) {
                boolean success;
                if (modifierApp.duration > 0) {
                    success = elementSystem.applyTimedAttributeModifier(target, modifierApp.attributeId,
                            modifierApp.modifier, modifierApp.duration);
                } else {
                    success = elementSystem.applyAttributeModifier(target, modifierApp.attributeId, modifierApp.modifier);
                }
                if (success) {
                    LOGGER.debug("Applied target attribute modifier: {} to {}", modifierApp.attributeId, target);
                }
            }
        }

        for (ReactionResult.AttributeModifierApplication modifierApp : result.selfAttributeModifiers) {
            if (attacker != null && modifierApp.attributeId != null && modifierApp.modifier != null) {
                boolean success;
                if (modifierApp.duration > 0) {
                    success = elementSystem.applyTimedAttributeModifier(attacker, modifierApp.attributeId,
                            modifierApp.modifier, modifierApp.duration);
                } else {
                    success = elementSystem.applyAttributeModifier(attacker, modifierApp.attributeId, modifierApp.modifier);
                }
                if (success) {
                    LOGGER.debug("Applied self attribute modifier: {} to {}", modifierApp.attributeId, attacker);
                }
            }
        }
    }

    /**
     * 应用挂载效果
     */
    private static void applyMountEffects(LivingEntity attacker, LivingEntity target, ReactionResult result,
                                          com.element_endow.api.IElementMountSystem mountSystem) {
        for (ReactionResult.MountApplication mount : result.mountApplications) {
            if (target.level().random.nextDouble() < mount.probability) {
                mountSystem.applyMount(target,
                        mount.elementId,
                        mount.amount,
                        mount.duration,
                        mount.probability, "refresh");
                LOGGER.debug("Applied mount effect: {} to {}", mount.elementId, target);
            }
        }

        for (com.element_endow.api.IElementMountSystem.AdvancedMountData advancedMount : result.advancedMountApplications) {
            mountSystem.applyAdvancedMount(target, advancedMount);
            LOGGER.debug("Applied advanced mount effect: {} to {}", advancedMount.elementId, target);
        }
    }
}