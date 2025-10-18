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
                    manager.processInducedReaction(attacker, target, attackingElement).ifPresent(result -> {
                        float originalDamage = event.getAmount();
                        float modifiedDamage = (float) (originalDamage * result.getDamageMultiplier() / result.getDefenseMultiplier());
                        event.setAmount(modifiedDamage);
                        applyEffects(attacker, target, result.getEffect());
                    });
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
                    manager.processInternalReaction(player, activeElements).ifPresent(result -> {
                        applyInternalEffects(player, result);
                    });
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
        // 对目标施加负面效果
        for (MobEffectInstance effectInstance : effect.getAfflictEffects()) {
            target.addEffect(new MobEffectInstance(
                    effectInstance.getEffect(),
                    effectInstance.getDuration(),
                    effectInstance.getAmplifier(),
                    effectInstance.isAmbient(),
                    effectInstance.isVisible()
            ));
        }

        // 对攻击者施加增益效果
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