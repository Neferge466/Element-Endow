package com.element_endow.services;

import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;

//反应处理器注册表
public class ReactionHandlerRegistry {
    private final CopyOnWriteArrayList<PrioritizedHandler> handlers;

    public ReactionHandlerRegistry() {
        this.handlers = new CopyOnWriteArrayList<>();
    }

    public void registerHandler(IReactionHandler handler) {
        PrioritizedHandler prioritized = new PrioritizedHandler(handler, handler.getPriority());
        handlers.add(prioritized);
        //按优先级排序
        handlers.sort(Comparator.comparingInt(PrioritizedHandler::getPriority));
    }

    public void unregisterHandler(IReactionHandler handler) {
        handlers.removeIf(h -> h.getHandler() == handler);
    }

    public CopyOnWriteArrayList<PrioritizedHandler> getHandlers() {
        return handlers;
    }

    public static class PrioritizedHandler {
        private final IReactionHandler handler;
        private final int priority;

        public PrioritizedHandler(IReactionHandler handler, int priority) {
            this.handler = handler;
            this.priority = priority;
        }

        public IReactionHandler getHandler() {
            return handler;
        }

        public int getPriority() {
            return priority;
        }
    }
}