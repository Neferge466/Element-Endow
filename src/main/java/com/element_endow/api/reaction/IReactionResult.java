package com.element_endow.api.reaction;

//反应结果接口
public interface IReactionResult {
    double getMultiplier();
    IReactionEffect getEffect(); // 添加这一行
    String getReactionKey();
    IReactionContext getContext();
}