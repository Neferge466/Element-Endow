package com.element_endow;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.core.ElementSystemImpl;
import com.element_endow.core.ElementRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ElementEndow.MODID)
public class ElementEndow {
    public static final String MODID = "element_endow";
    public static final Logger LOGGER = LogManager.getLogger();

    public ElementEndow() {
        LOGGER.info("Initializing ElementEndow");

        try {
            ElementSystemImpl elementSystem = new ElementSystemImpl();
            ElementSystemAPI.initialize(elementSystem);
            ElementRegistry.ATTRIBUTES.register(FMLJavaModLoadingContext.get().getModEventBus());

            LOGGER.info("ElementEndow initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize ElementEndow", e);
            throw new RuntimeException("ElementEndow initialization failed", e);
        }
    }
}