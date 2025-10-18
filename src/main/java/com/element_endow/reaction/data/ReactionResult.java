package com.element_endow.reaction.data;

public class ReactionResult {
    private final double multiplier;
    private final ReactionEffect effect;
    private final String reactionKey;

    public ReactionResult(double multiplier, ReactionEffect effect, String reactionKey) {
        this.multiplier = multiplier;
        this.effect = effect;
        this.reactionKey = reactionKey;
    }

    public double getMultiplier() { return multiplier; }
    public ReactionEffect getEffect() { return effect; }
    public String getReactionKey() { return reactionKey; }
}