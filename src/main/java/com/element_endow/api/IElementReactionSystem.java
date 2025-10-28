package com.element_endow.api;

import com.element_endow.data.ReactionLoader;
import net.minecraft.world.entity.LivingEntity;

public interface IElementReactionSystem {

    /**
     * 处理攻击时的元素反应
     */
    ReactionResult processAttackReaction(LivingEntity attacker, LivingEntity target, double baseDamage);

    /**
     * 处理防御时的元素反应
     */
    ReactionResult processDefenseReaction(LivingEntity attacker, LivingEntity defender, double incomingDamage);

    /**
     * 获取反应加载器
     */
    ReactionLoader getReactionLoader();

    /**
     * 重新加载反应数据
     */
    void reloadReactions();
}