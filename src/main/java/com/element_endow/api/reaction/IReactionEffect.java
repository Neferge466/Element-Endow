package com.element_endow.api.reaction;

import net.minecraft.world.effect.MobEffectInstance;
import java.util.List;

//反应效果接口
public interface IReactionEffect {
    List<MobEffectInstance> getAfflictEffects();
    List<MobEffectInstance> getEmpowerEffects();

    //创建效果实例的副本
    IReactionEffect copy();

    //合并
    IReactionEffect merge(IReactionEffect other);
}