package com.element_endow.integration.jade;

import com.element_endow.ElementEndow;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(ElementEndow.MODID)
public class ElementEndowJadePlugin implements IWailaPlugin {

    public static final ResourceLocation ELEMENT_INFO = new ResourceLocation(ElementEndow.MODID, "element_info");
    public static final ResourceLocation COMBINATION_INFO = new ResourceLocation(ElementEndow.MODID, "combination_info");
    public static final ResourceLocation MOUNT_INFO = new ResourceLocation(ElementEndow.MODID, "mount_info");

    @Override
    public void register(IWailaCommonRegistration registration) {
        //服务端注册
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        //注册实体组件提供者
        registration.registerEntityComponent(ElementEntityProvider.INSTANCE, net.minecraft.world.entity.LivingEntity.class);
        registration.registerEntityComponent(CombinationEntityProvider.INSTANCE, net.minecraft.world.entity.LivingEntity.class);
        registration.registerEntityComponent(MountEntityProvider.INSTANCE, net.minecraft.world.entity.LivingEntity.class);

        //注册配置
        registration.addConfig(ELEMENT_INFO, true);
        registration.addConfig(COMBINATION_INFO, true);
        registration.addConfig(MOUNT_INFO, true);

        ElementEndow.LOGGER.info("[Jade Integration] Element Endow Jade integration registered successfully");
    }
}