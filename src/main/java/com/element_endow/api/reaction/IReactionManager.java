package com.element_endow.api.reaction;

import com.element_endow.api.config.IReactionConfig;
import com.element_endow.services.IReactionHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.List;

//反应系统管理器接口
public interface IReactionManager {
    //反应处理
    List<IReactionResult> processInternalReaction(Player player, List<String> activeElements);
    List<IReactionResult> processInducedReaction(Player attacker, LivingEntity target, String attackingElement);

    //注册反应
    boolean registerReaction(String reactionKey, IReactionContext context);
    boolean unregisterReaction(String reactionKey);

    //配置
    IReactionConfig getConfig();

    //服务
    void registerReactionHandler(IReactionHandler handler);
    void unregisterReactionHandler(IReactionHandler handler);
}