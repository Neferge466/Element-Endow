package com.element_endow.services;

import com.element_endow.api.reaction.IReactionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

//反应处理器接口
//允许其他模组注册自定义反应逻辑
public interface IReactionHandler {

    //检查是否能处理内部反应
    boolean canHandleInternal(Player player, List<String> activeElements);

    //处理内部反应
    List<IReactionResult> processInternalReaction(Player player, List<String> activeElements);

    //检查是否能处理诱发反应
    boolean canHandleInduced(Player attacker, LivingEntity target, String attackingElement);

    //处理诱发反应
    List<IReactionResult> processInducedReaction(Player attacker, LivingEntity target, String attackingElement);

    //获取处理器优先级（数值越小优先级越高）
    default int getPriority() {
        return 10;
    }

    //获取处理器名称
    default String getName() {
        return getClass().getSimpleName();
    }
}