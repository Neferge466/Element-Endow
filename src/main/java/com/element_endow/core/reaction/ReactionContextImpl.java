package com.element_endow.core.reaction;

import com.element_endow.api.reaction.IReactionContext;
import com.element_endow.api.reaction.IReactionEffect;
import com.element_endow.api.reaction.ReactionType;
import com.element_endow.core.reaction.data.ReactionEntry;
import com.element_endow.core.reaction.data.ReactionEffect;

import java.util.List;

public class ReactionContextImpl implements IReactionContext {
    private final String reactionKey;
    private final ReactionType type;
    private final ReactionEntry reactionEntry;

    public ReactionContextImpl(String baseKey, ReactionType type, ReactionEntry reactionEntry, int index) {
        this.reactionKey = baseKey + "_" + index;
        this.type = type;
        this.reactionEntry = reactionEntry;
    }

    @Override
    public String getReactionKey() {
        return reactionKey;
    }

    @Override
    public ReactionType getType() {
        return type;
    }

    @Override
    public List<String> getRequiredElements() {
        // ✅ 直接返回当前反应条目所需的元素
        return reactionEntry.getMatchElements();
    }

    @Override
    public double getRate() {
        return reactionEntry.getRate();
    }

    @Override
    public double[] getRateArray() {
        return reactionEntry.getRateArray();
    }

    @Override
    public IReactionEffect getEffect() {
        return new ReactionEffectWrapper(reactionEntry.getEffect());
    }

    private static class ReactionEffectWrapper implements IReactionEffect {
        private final ReactionEffect effect;

        public ReactionEffectWrapper(ReactionEffect effect) {
            this.effect = effect;
        }

        @Override
        public List<net.minecraft.world.effect.MobEffectInstance> getAfflictEffects() {
            return effect.getAfflictEffects();
        }

        @Override
        public List<net.minecraft.world.effect.MobEffectInstance> getEmpowerEffects() {
            return effect.getEmpowerEffects();
        }

        @Override
        public IReactionEffect copy() {
            return new ReactionEffectWrapper(effect);
        }

        @Override
        public IReactionEffect merge(IReactionEffect other) {
            return this;
        }
    }
}