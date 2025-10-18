package com.element_endow.config;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.element_endow.ElementEndow.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GlobalAttributeHandler {

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        //获取所有注册的元素属性
        var elements = ElementRegistry.getRegisteredElements();
        //只为LivingEntity添加元素属性
        for (EntityType<? extends LivingEntity> entityType : event.getTypes()) {
            if (DefaultAttributes.hasSupplier(entityType)) {
                for (var entry : elements.entrySet()) {
                    Attribute attribute = entry.getValue().get();
                    event.add(entityType, attribute);
                }
            }
        }
    }
}