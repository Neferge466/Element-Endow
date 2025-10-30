package com.element_endow.api;

import com.element_endow.data.CombinationLoader;
import net.minecraft.world.entity.LivingEntity;
import java.util.Collection;

public interface IElementCombinationSystem {

    /**
     * 检查并应用实体组合效果（持续效果）
     */
    void checkAndApplyCombinations(LivingEntity entity);

    /**
     * 处理攻击时的组合触发效果
     */
    CombinationTriggerResult processAttackTrigger(LivingEntity attacker, LivingEntity target);

    /**
     * 处理防御时的组合触发效果
     */
    CombinationTriggerResult processDefenseTrigger(LivingEntity attacker, LivingEntity defender);

    /**
     * 移除实体的组合效果
     */
    void removeCombinationEffects(LivingEntity entity, String combinationId);

    /**
     * 获取实体当前激活的组合
     */
    Collection<String> getActiveCombinations(LivingEntity entity);

    /**
     * 重新加载组合数据
     */
    void reloadCombinations();

    /**
     * 获取组合加载器
     */
    CombinationLoader getCombinationLoader();

    /**
     * 组合触发结果类
     */
    class CombinationTriggerResult {
        public double damageMultiplier = 1.0;
        public double defenseMultiplier = 1.0;
        public double extraDamage = 0.0;
        public double damageReduction = 0.0;
        public final java.util.List<ReactionResult.MountApplication> mountApplications = new java.util.ArrayList<>();
        public final java.util.List<IElementMountSystem.AdvancedMountData> advancedMountApplications = new java.util.ArrayList<>();
        public final java.util.List<ReactionResult.AttributeModifierApplication> targetAttributeModifiers = new java.util.ArrayList<>();
        public final java.util.List<ReactionResult.AttributeModifierApplication> selfAttributeModifiers = new java.util.ArrayList<>();
        public final java.util.List<net.minecraft.world.effect.MobEffectInstance> targetEffects = new java.util.ArrayList<>();
        public final java.util.List<net.minecraft.world.effect.MobEffectInstance> selfEffects = new java.util.ArrayList<>();

        public CombinationTriggerResult() {}
    }
}