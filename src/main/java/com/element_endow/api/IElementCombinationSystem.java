package com.element_endow.api;

import com.element_endow.data.CombinationLoader;
import net.minecraft.world.entity.LivingEntity;
import java.util.Collection;

public interface IElementCombinationSystem {

    /**
     * 检查并应用实体组合效果
     */
    void checkAndApplyCombinations(LivingEntity entity);

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
    CombinationLoader getCombinationLoader(); // 新增方法
}