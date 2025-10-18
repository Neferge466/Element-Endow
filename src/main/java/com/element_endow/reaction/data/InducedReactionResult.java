package com.element_endow.reaction.data;

public class InducedReactionResult {
    private final double damageMultiplier;
    private final double defenseMultiplier;
    private final ReactionEffect effect;
    private final String reactionKey;

    public InducedReactionResult(double damageMultiplier, double defenseMultiplier,
                                 ReactionEffect effect, String reactionKey) {
        this.damageMultiplier = damageMultiplier;
        this.defenseMultiplier = defenseMultiplier;
        this.effect = effect;
        this.reactionKey = reactionKey;
    }

    public double getDamageMultiplier() { return damageMultiplier; }
    public double getDefenseMultiplier() { return defenseMultiplier; }
    public ReactionEffect getEffect() { return effect; }
    public String getReactionKey() { return reactionKey; }
}