package com.element_endow;

import com.element_endow.core.ElementEndowCore;
import com.element_endow.core.element.ElementRegistry;
import com.element_endow.core.element.ElementSystemImpl;
import com.element_endow.services.ServiceManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ElementEndow.MODID)
public class ElementEndow {
    public static final String MODID = "element_endow";
    public static final Logger LOGGER = LogManager.getLogger();

    public ElementEndow() {
        LOGGER.info("Initializing ElementEndow Lib Mod");

        try {
            //初始化核心系统
            ElementEndowCore core = ElementEndowCore.getInstance();
            core.initialize();

            //通过ElementSystemImpl获取注册表
            ElementSystemImpl elementSystem = (ElementSystemImpl) core.getElementSystem();
            ElementRegistry.ATTRIBUTES.register(FMLJavaModLoadingContext.get().getModEventBus());

            //发现并注册服务
            ServiceManager.discoverAndRegisterServices();

            LOGGER.info("ElementEndow Lib Mod initialized successfully");

        } catch (Exception e) {
            LOGGER.error("Failed to initialize ElementEndow Lib Mod", e);
            throw new RuntimeException("ElementEndow initialization failed", e);
        }
    }
}