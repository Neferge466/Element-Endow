package com.element_endow.core.reaction.data;

import com.element_endow.api.reaction.IReactionContext;
import com.element_endow.api.reaction.IReactionEffect;
import com.element_endow.api.reaction.IReactionResult;

public class InducedReactionResult implements IReactionResult {
    private final double damageMultiplier;
    private final double defenseMultiplier;
    private final IReactionEffect effect;
    private final String reactionKey;
    private final IReactionContext context;

    public InducedReactionResult(double damageMultiplier, double defenseMultiplier,
                                 IReactionEffect effect, String reactionKey, IReactionContext context) {
        this.damageMultiplier = damageMultiplier;
        this.defenseMultiplier = defenseMultiplier;
        this.effect = effect;
        this.reactionKey = reactionKey;
        this.context = context;
    }

    @Override
    public double getMultiplier() {
        return damageMultiplier;
    }

    public double getDefenseMultiplier() {
        return defenseMultiplier;
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