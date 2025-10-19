package com.element_endow.event;

import com.element_endow.api.reaction.IReactionContext;
import com.element_endow.api.reaction.IReactionResult;
import com.element_endow.api.reaction.ReactionType; // 使用 API 的 ReactionType
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

public class ElementReactionEvent {

    //反应处理前触发，可以修改或取消反应
    public static class Pre extends Event {
        private final Player player;
        private final List<String> activeElements;
        private final ReactionType reactionType; // 使用 API 的 ReactionType
        private boolean canceled = false;

        public Pre(Player player, List<String> activeElements, ReactionType reactionType) {
            this.player = player;
            this.activeElements = activeElements;
            this.reactionType = reactionType;
        }

        public Player getPlayer() { return player; }
        public List<String> getActiveElements() { return activeElements; }
        public ReactionType getReactionType() { return reactionType; }
        public boolean isCanceled() { return canceled; }
        public void setCanceled(boolean canceled) { this.canceled = canceled; }
    }

    //反应处理后触发，可以查看反应结果
    public static class Post extends Event {
        private final Player player;
        private final List<String> activeElements;
        private final ReactionType reactionType; // 使用 API 的 ReactionType
        private final List<IReactionResult> results;

        public Post(Player player, List<String> activeElements, ReactionType reactionType, List<IReactionResult> results) {
            this.player = player;
            this.activeElements = activeElements;
            this.reactionType = reactionType;
            this.results = results;
        }

        public Player getPlayer() { return player; }
        public List<String> getActiveElements() { return activeElements; }
        public ReactionType getReactionType() { return reactionType; }
        public List<IReactionResult> getResults() { return results; }
    }

    //自定义反应触发事件
    public static class CustomReactionEvent extends Event {
        private final String reactionKey;
        private final Player player;
        private final LivingEntity target;
        private final IReactionContext context;
        private IReactionResult reactionResult; // 重命名避免冲突

        public CustomReactionEvent(String reactionKey, Player player, LivingEntity target, IReactionContext context) {
            this.reactionKey = reactionKey;
            this.player = player;
            this.target = target;
            this.context = context;
        }

        public String getReactionKey() { return reactionKey; }
        public Player getPlayer() { return player; }
        public LivingEntity getTarget() { return target; }
        public IReactionContext getContext() { return context; }
        public IReactionResult getReactionResult() { return reactionResult; }
        public void setReactionResult(IReactionResult reactionResult) { this.reactionResult = reactionResult; }
    }
}