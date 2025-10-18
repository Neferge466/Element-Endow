package com.element_endow.reaction;

import com.element_endow.ElementEndow;
import com.element_endow.config.ElementAttributeHelper;
import com.element_endow.config.ElementRegistry;
import com.element_endow.reaction.data.ReactionEffect;
import com.element_endow.reaction.data.ReactionResult;
import com.element_endow.reaction.data.InducedReactionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = ElementEndow.MODID)
public class ReactionEventHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();

        if (source.getDirectEntity() instanceof Player attacker) {
            String attackingElement = getAttackingElement(attacker);
            if (attackingElement != null) {
                ReactionManager manager = ElementEndow.getReactionManager();
                if (manager != null) {
                    List<InducedReactionResult> results = manager.processInducedReaction(attacker, target, attackingElement);
                    if (!results.isEmpty()) {
                        float originalDamage = event.getAmount();
                        float modifiedDamage = originalDamage;

                        //应用所有匹配的induced反应
                        for (InducedReactionResult result : results) {
                            modifiedDamage = (float) (modifiedDamage * result.getDamageMultiplier() / result.getDefenseMultiplier());
                            applyEffects(attacker, target, result.getEffect());
                            ElementEndow.LOGGER.info("Applied induced reaction: {}", result.getReactionKey());
                        }

                        event.setAmount(modifiedDamage);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player && player.tickCount % 20 == 0) {
            List<String> activeElements = getActiveElements(player);
            if (!activeElements.isEmpty()) {
                ReactionManager manager = ElementEndow.getReactionManager();
                if (manager != null) {
                    List<ReactionResult> results = manager.processInternalReaction(player, activeElements);
                    if (!results.isEmpty()) {
                        //应用所有匹配的internal反应
                        for (ReactionResult result : results) {
                            applyInternalEffects(player, result);
                            ElementEndow.LOGGER.info("Applied internal reaction: {} with multiplier: {}",
                                    result.getReactionKey(), result.getMultiplier());
                        }
                    }
                }
            }
        }
    }

    private static String getAttackingElement(Player player) {
        String maxElement = null;
        double maxValue = 0;

        for (var entry : ElementRegistry.getRegisteredElements().entrySet()) {
            double value = ElementAttributeHelper.getElementValue(player, entry.getKey());
            if (value > maxValue) {
                maxValue = value;
                maxElement = entry.getKey();
            }
        }

        return maxElement;
    }

    private static List<String> getActiveElements(Player player) {
        List<String> activeElements = new ArrayList<>();
        ElementRegistry.getRegisteredElements().forEach((elementId, attribute) -> {
            if (ElementAttributeHelper.getElementValue(player, elementId) > 0) {
                activeElements.add(elementId);
            }
        });
        return activeElements;
    }

    private static void applyEffects(Player attacker, LivingEntity target, ReactionEffect effect) {
        //对目标施加效果
        for (MobEffectInstance effectInstance : effect.getAfflictEffects()) {
            target.addEffect(new MobEffectInstance(
                    effectInstance.getEffect(),
                    effectInstance.getDuration(),
                    effectInstance.getAmplifier(),
                    effectInstance.isAmbient(),
                    effectInstance.isVisible()
            ));
        }

        //对攻击者施加效果
        for (MobEffectInstance effectInstance : effect.getEmpowerEffects()) {
            attacker.addEffect(new MobEffectInstance(
                    effectInstance.getEffect(),
                    effectInstance.getDuration(),
                    effectInstance.getAmplifier(),
                    effectInstance.isAmbient(),
                    effectInstance.isVisible()
            ));
        }
    }

    private static void applyInternalEffects(Player player, ReactionResult result) {
        for (MobEffectInstance effectInstance : result.getEffect().getEmpowerEffects()) {
            player.addEffect(new MobEffectInstance(
                    effectInstance.getEffect(),
                    effectInstance.getDuration(),
                    effectInstance.getAmplifier(),
                    effectInstance.isAmbient(),
                    effectInstance.isVisible()
            ));
        }
    }
}