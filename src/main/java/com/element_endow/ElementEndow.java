package com.element_endow;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.core.ElementSystemImpl;
import com.element_endow.core.ElementRegistry;
import com.element_endow.event.ElementAttackEventHandler;
import com.element_endow.event.ElementCombinationHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ElementEndow.MODID)
public class ElementEndow {
    public static final String MODID = "element_endow";
    public static final Logger LOGGER = LogManager.getLogger();

    public ElementEndow() {
        LOGGER.info("Initialize the ElementEndow system");

        try {
            //创建元素系统核心实例
            ElementSystemImpl elementSystem = new ElementSystemImpl();
            //初始化API层
            ElementSystemAPI.initialize(elementSystem);
            //注册属性到Forge系统
            ElementRegistry.ATTRIBUTES.register(FMLJavaModLoadingContext.get().getModEventBus());
            //注册事件处理器
            IEventBus eventBus = MinecraftForge.EVENT_BUS;
            eventBus.register(new ElementAttackEventHandler());
            eventBus.register(new ElementCombinationHandler());

            LOGGER.info("Element Endow system initialization complete");

        } catch (Exception e) {
            LOGGER.error("ElementEndow initialization failed", e);
            throw new RuntimeException("ElementEndow initialization failed", e);
        }
    }
}