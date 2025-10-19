package com.element_endow.api;

import com.element_endow.api.element.IElementSystem;
import com.element_endow.api.reaction.IReactionManager;


//ElementEndow 主 API 入口
//提供获取系统实例的静态方法
public class ElementSystemAPI {
    private static IElementSystem elementSystem;
    private static IReactionManager reactionManager;

    public static void initialize(IElementSystem elementSystem, IReactionManager reactionManager) {
        ElementSystemAPI.elementSystem = elementSystem;
        ElementSystemAPI.reactionManager = reactionManager;
    }

    public static IElementSystem getElementSystem() {
        if (elementSystem == null) {
            throw new IllegalStateException("ElementSystem has not been initialized. Make sure ElementEndow mod is loaded.");
        }
        return elementSystem;
    }

    public static IReactionManager getReactionManager() {
        if (reactionManager == null) {
            throw new IllegalStateException("ReactionManager has not been initialized. Make sure ElementEndow mod is loaded.");
        }
        return reactionManager;
    }
}