package com.element_endow;

import com.element_endow.config.ElementRegistry;
import com.element_endow.reaction.ReactionManager;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ElementEndow.MODID)
public class ElementEndow {
    public static final String MODID = "element_endow";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static ElementEndow instance;
    private ReactionManager reactionManager;

    public ElementEndow() {
        instance = this;

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ElementRegistry.ATTRIBUTES.register(modBus);

        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);

        LOGGER.info("Element Endow mod initialized");
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        reactionManager = new ReactionManager();
        event.addListener(reactionManager);
        LOGGER.info("Reaction system reload listener registered");
    }

    public static ReactionManager getReactionManager() {
        return instance != null ? instance.reactionManager : null;
    }

    public static ElementEndow getInstance() {
        return instance;
    }
}