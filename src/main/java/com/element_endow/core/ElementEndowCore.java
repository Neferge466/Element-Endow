package com.element_endow.core;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.api.element.IElementSystem;
import com.element_endow.api.reaction.IReactionManager;
import com.element_endow.core.element.ElementSystemImpl;
import com.element_endow.core.reaction.ReactionManagerImpl;
import com.element_endow.event.EventHandlers;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//初始化
public class ElementEndowCore {
    private static final Logger LOGGER = LogManager.getLogger();

    private static ElementEndowCore instance;
    private IElementSystem elementSystem;
    private IReactionManager reactionManager;
    private boolean initialized = false;

    public static ElementEndowCore getInstance() {
        if (instance == null) {
            instance = new ElementEndowCore();
        }
        return instance;
    }

    public void initialize() {
        if (initialized) {
            LOGGER.warn("ElementEndowCore has already been initialized");
            return;
        }

        try {
            //初始化系统组件
            this.elementSystem = new ElementSystemImpl();
            this.reactionManager = new ReactionManagerImpl();

            //注册API
            ElementSystemAPI.initialize(elementSystem, reactionManager);

            //注册事件
            registerEvents();

            initialized = true;
            LOGGER.info("ElementEndow Core initialized successfully");

        } catch (Exception e) {
            LOGGER.error("Failed to initialize ElementEndow Core", e);
            throw new RuntimeException("ElementEndow Core initialization failed", e);
        }
    }

    private void registerEvents() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        //注册事件处理器
        forgeBus.addListener(this::onAddReloadListeners);
        forgeBus.register(new EventHandlers());

        LOGGER.debug("ElementEndow events registered");
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        //注册数据包重载监听器
        if (reactionManager instanceof ReactionManagerImpl) {
            event.addListener((ReactionManagerImpl) reactionManager);
        }
        LOGGER.info("ElementEndow reload listeners registered");
    }
    public IElementSystem getElementSystem() {
        return elementSystem;
    }

    public IReactionManager getReactionManager() {
        return reactionManager;
    }

    public boolean isInitialized() {
        return initialized;
    }
}