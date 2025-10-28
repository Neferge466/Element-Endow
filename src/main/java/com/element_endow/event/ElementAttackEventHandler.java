package com.element_endow.event;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.api.ReactionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = "element_endow", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ElementAttackEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

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
            var mountSystem = elementSystem.getMountSystem();

            ReactionResult attackResult = reactionSystem.processAttackReaction(attacker, target, event.getAmount());
            ReactionResult defenseResult = reactionSystem.processDefenseReaction(attacker, target, event.getAmount());
            ReactionResult combinedResult = combineResults(attackResult, defenseResult);

            applyReactionResult(event, combinedResult, attacker, target);
            applyMountEffects(attacker, target, combinedResult, mountSystem);

        } catch (Exception e) {
            LOGGER.error("Error processing element reactions", e);
        }
    }

    private static ReactionResult combineResults(ReactionResult attackResult, ReactionResult defenseResult) {
        ReactionResult combined = new ReactionResult();

        combined.damageMultiplier = attackResult.damageMultiplier * defenseResult.defenseMultiplier;
        combined.defenseMultiplier = defenseResult.defenseMultiplier;
        combined.extraDamage = attackResult.extraDamage;
        combined.damageReduction = attackResult.damageReduction + defenseResult.damageReduction;

        combined.targetEffects.addAll(attackResult.targetEffects);
        combined.targetEffects.addAll(defenseResult.targetEffects);
        combined.selfEffects.addAll(attackResult.selfEffects);
        combined.selfEffects.addAll(defenseResult.selfEffects);

        combined.targetAttributeModifiers.addAll(attackResult.targetAttributeModifiers);
        combined.targetAttributeModifiers.addAll(defenseResult.targetAttributeModifiers);
        combined.selfAttributeModifiers.addAll(attackResult.selfAttributeModifiers);
        combined.selfAttributeModifiers.addAll(defenseResult.selfAttributeModifiers);

        return combined;
    }

    private static void applyReactionResult(LivingHurtEvent event, ReactionResult result, LivingEntity attacker, LivingEntity target) {
        float originalDamage = event.getAmount();
        float finalDamage = (float) ((originalDamage * result.damageMultiplier) + result.extraDamage);
        finalDamage = (float) Math.max(0, finalDamage - result.damageReduction);
        event.setAmount(finalDamage);

        for (var effect : result.targetEffects) {
            if (effect != null) {
                target.addEffect(effect);
            }
        }

        for (var effect : result.selfEffects) {
            if (effect != null) {
                attacker.addEffect(effect);
            }
        }

        applyAttributeModifiers(result, attacker, target);
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.tickCount % 5 == 0) {
            try {
                var elementSystem = ElementSystemAPI.getElementSystem();
                elementSystem.checkAndRemoveExpiredModifiers(entity);
            } catch (Exception e) {
                LOGGER.error("Error checking expired modifiers", e);
            }
        }
    }

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
            }
        }
    }

    private static void applyMountEffects(LivingEntity attacker, LivingEntity target, ReactionResult result,
                                          com.element_endow.api.IElementMountSystem mountSystem) {
        for (ReactionResult.MountApplication mount : result.mountApplications) {
            if (target.level().random.nextDouble() < mount.probability) {
                mountSystem.applyMount(target,
                        mount.elementId,
                        mount.amount,
                        mount.duration,
                        mount.probability, "refresh");
            }
        }

        for (com.element_endow.api.IElementMountSystem.AdvancedMountData advancedMount : result.advancedMountApplications) {
            mountSystem.applyAdvancedMount(target, advancedMount);
        }
    }
}