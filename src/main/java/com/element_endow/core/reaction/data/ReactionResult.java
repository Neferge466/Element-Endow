package com.element_endow.core.reaction.data;

import com.element_endow.api.reaction.IReactionContext;
import com.element_endow.api.reaction.IReactionEffect;
import com.element_endow.api.reaction.IReactionResult;

public class ReactionResult implements IReactionResult {
    private final double multiplier;
    private final IReactionEffect effect;
    private final String reactionKey;
    private final IReactionContext context;

    public ReactionResult(double multiplier, IReactionEffect effect, String reactionKey, IReactionContext context) {
        this.multiplier = multiplier;
        this.effect = effect;
        this.reactionKey = reactionKey;
        this.context = context;
    }

    @Override
    public double getMultiplier() {
        return multiplier;
    }

    @Override
    public IReactionEffect getEffect() {
        return effect;
    }

    @Override
    public String getReactionKey() {
        return reactionKey;
    }

    @Override
    public IReactionContext getContext() {
        return context;
    }
}