package com.element_endow.event;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.api.element.IElementSystem;
import com.element_endow.api.reaction.IReactionResult;
import com.element_endow.core.reaction.data.InducedReactionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "element_endow")
public class ReactionEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Set<String> appliedReactions = new HashSet<>();
    private static long lastLogTime = 0;
    private static final long LOG_INTERVAL = 60000; //60秒内不重复日志

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();

        if (source.getDirectEntity() instanceof Player attacker) {
            //处理诱发反应
            String attackingElement = getAttackingElement(attacker);
            if (attackingElement != null) {
                var manager = ElementSystemAPI.getReactionManager();
                if (manager != null) {
                    List<IReactionResult> inducedResults = manager.processInducedReaction(attacker, target, attackingElement);
                    if (!inducedResults.isEmpty()) {
                        float modifiedDamage = event.getAmount();

                        for (IReactionResult result : inducedResults) {
                            if (result instanceof InducedReactionResult inducedResult) {
                                modifiedDamage = (float) (modifiedDamage * inducedResult.getMultiplier() / inducedResult.getDefenseMultiplier());
                                applyEffects(attacker, target, inducedResult.getEffect());

                                String reactionKey = "induced_" + inducedResult.getReactionKey();
                                if (shouldLogReaction(reactionKey)) {
                                    LOGGER.info("Induced reaction: {} Damage rate: {}/Defense rate: {}",
                                            inducedResult.getReactionKey(),
                                            inducedResult.getMultiplier(),
                                            inducedResult.getDefenseMultiplier());
                                }
                            }
                        }

                        event.setAmount(modifiedDamage);
                    }
                }
            }

            //处理内部反应的afflict效果
            var manager = ElementSystemAPI.getReactionManager();
            if (manager != null) {
                List<String> activeElements = getActiveElements(attacker);
                if (!activeElements.isEmpty()) {
                    List<IReactionResult> internalResults = manager.processInternalReaction(attacker, activeElements);
                    if (!internalResults.isEmpty()) {
                        for (IReactionResult result : internalResults) {
                            applyInternalAfflictEffects(target, result);
                            String reactionKey = "afflict_" + result.getReactionKey();
                            if (shouldLogReaction(reactionKey)) {
                                LOGGER.info("internal afflict: {}", result.getReactionKey());
                            }
                        }
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
                var manager = ElementSystemAPI.getReactionManager();
                if (manager != null) {
                    List<IReactionResult> results = manager.processInternalReaction(player, activeElements);
                    if (!results.isEmpty()) {
                        for (IReactionResult result : results) {
                            applyInternalEmpowerEffects(player, result);
                            String reactionKey = "empower_" + result.getReactionKey();
                            if (shouldLogReaction(reactionKey)) {
                                LOGGER.info("internal empower: {} rate: {}",
                                        result.getReactionKey(),
                                        String.format("%.2f", result.getMultiplier()));
                            }
                        }
                    }
                }
            }
        }
    }

    //检查是否该记录反应
    private static boolean shouldLogReaction(String reactionKey) {
        long currentTime = System.currentTimeMillis();
        //时间间隔内，不重复记录已出现的log
        if (appliedReactions.contains(reactionKey) && (currentTime - lastLogTime) < LOG_INTERVAL) {
            return false;
        }
        //更新记录
        appliedReactions.add(reactionKey);
        lastLogTime = currentTime;
        //清理过期的记录
        if (appliedReactions.size() > 100) {
            appliedReactions.clear();
        }

        return true;
    }

    private static String getAttackingElement(Player player) {
        IElementSystem elementSystem = ElementSystemAPI.getElementSystem();
        String maxElement = null;
        double maxValue = 0;

        for (String elementId : elementSystem.getRegisteredElements()) {
            double value = elementSystem.getElementValue(player, elementId);
            if (value > maxValue) {
                maxValue = value;
                maxElement = elementId;
            }
        }

        return maxElement;
    }

    private static List<String> getActiveElements(Player player) {
        IElementSystem elementSystem = ElementSystemAPI.getElementSystem();
        List<String> activeElements = new ArrayList<>();

        for (String elementId : elementSystem.getRegisteredElements()) {
            if (elementSystem.getElementValue(player, elementId) > 0) {
                activeElements.add(elementId);
            }
        }

        return activeElements;
    }

    private static void applyEffects(Player attacker, LivingEntity target, com.element_endow.api.reaction.IReactionEffect effect) {
        if (effect == null) return;

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

    private static void applyInternalEmpowerEffects(Player player, IReactionResult result) {
        if (result.getEffect() == null) return;

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

    private static void applyInternalAfflictEffects(LivingEntity target, IReactionResult result) {
        if (result.getEffect() == null) return;

        for (MobEffectInstance effectInstance : result.getEffect().getAfflictEffects()) {
            target.addEffect(new MobEffectInstance(
                    effectInstance.getEffect(),
                    effectInstance.getDuration(),
                    effectInstance.getAmplifier(),
                    effectInstance.isAmbient(),
                    effectInstance.isVisible()
            ));
        }
    }
}