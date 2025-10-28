package com.element_endow.core;

import com.element_endow.api.IElementSystem;
import com.element_endow.api.ReactionResult;
import com.element_endow.data.ElementDataManager;
import com.element_endow.data.ReactionLoader;
import com.element_endow.util.ConditionChecker;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ElementReactionSystem implements com.element_endow.api.IElementReactionSystem {
    private static final Logger LOGGER = LogManager.getLogger();

    private final IElementSystem elementSystem;
    private final ReactionLoader reactionLoader;

    public ElementReactionSystem(IElementSystem elementSystem) {
        this.elementSystem = elementSystem;
        this.reactionLoader = ElementDataManager.getReactionLoader();
        LOGGER.info("Elemental reaction system initialization complete");
    }

    @Override
    public ReactionResult processAttackReaction(LivingEntity attacker, LivingEntity target, double baseDamage) {
        ReactionResult result = new ReactionResult();

        try {
            //获取攻击者和目标的元素
            java.util.Collection<String> attackerElements = new java.util.ArrayList<>();
            java.util.Collection<String> targetElements = new java.util.ArrayList<>();

            for (String elementId : elementSystem.getEnabledElements()) {
                if (elementSystem.hasElement(attacker, elementId)) {
                    attackerElements.add(elementId);
                }
                if (elementSystem.hasElement(target, elementId)) {
                    targetElements.add(elementId);
                }
            }

            //使用反应加载器处理攻击反应逻辑
            reactionLoader.processAttackReactions(attackerElements, targetElements, result, attacker, target);

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
            java.util.Collection<String> attackerElements = new java.util.ArrayList<>();
            java.util.Collection<String> defenderElements = new java.util.ArrayList<>();

            for (String elementId : elementSystem.getEnabledElements()) {
                if (elementSystem.hasElement(attacker, elementId)) {
                    attackerElements.add(elementId);
                }
                if (elementSystem.hasElement(defender, elementId)) {
                    defenderElements.add(elementId);
                }
            }

            //使用反应加载器处理防御反应逻辑
            reactionLoader.processDefenseReactions(attackerElements, defenderElements, result, attacker, defender);

            LOGGER.debug("Processed defense reaction: {} attacker elements, {} defender elements, defense multiplier: {}",
                    attackerElements.size(), defenderElements.size(), result.defenseMultiplier);

        } catch (Exception e) {
            LOGGER.error("Error processing defense reaction", e);
        }

        return result;
    }

    @Override
    public ReactionLoader getReactionLoader() {
        return reactionLoader;
    }

    @Override
    public void reloadReactions() {
        reactionLoader.loadReactions();
        LOGGER.info("Element reactions reloaded");
    }
}