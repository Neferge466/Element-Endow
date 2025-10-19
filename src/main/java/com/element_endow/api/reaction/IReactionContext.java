package com.element_endow.api.reaction;

import java.util.List;

//反应上下文接口
public interface IReactionContext {
    String getReactionKey();
    ReactionType getType();
    List<String> getRequiredElements();
    double getRate();
    double[] getRateArray();
    IReactionEffect getEffect();
}