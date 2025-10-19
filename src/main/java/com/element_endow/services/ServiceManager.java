package com.element_endow.services;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.api.reaction.IReactionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

//服务管理器，自动发现和加载服务
public class ServiceManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final CopyOnWriteArrayList<IReactionHandler> discoveredHandlers = new CopyOnWriteArrayList<>();

    public static void discoverAndRegisterServices() {
        LOGGER.info("Discovering reaction handlers...");

        ServiceLoader<IReactionHandler> loader = ServiceLoader.load(IReactionHandler.class);
        for (IReactionHandler handler : loader) {
            discoveredHandlers.add(handler);
            IReactionManager reactionManager = ElementSystemAPI.getReactionManager();
            reactionManager.registerReactionHandler(handler);

            LOGGER.info("Registered reaction handler: {} (priority: {})",
                    handler.getName(), handler.getPriority());
        }

        LOGGER.info("Discovered {} reaction handlers", discoveredHandlers.size());
    }

    public static void unregisterAllServices() {
        IReactionManager reactionManager = ElementSystemAPI.getReactionManager();
        for (IReactionHandler handler : discoveredHandlers) {
            reactionManager.unregisterReactionHandler(handler);
        }
        discoveredHandlers.clear();

        LOGGER.info("Unregistered all reaction handlers");
    }
}