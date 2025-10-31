package com.element_endow.integration.jade;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.api.IElementMountSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Map;

public enum MountEntityProvider implements IEntityComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!config.get(ElementEndowJadePlugin.MOUNT_INFO)) {
            return;
        }

        if (!(accessor.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        try {
            var mountSystem = ElementSystemAPI.getElementSystem().getMountSystem();
            Map<String, IElementMountSystem.MountStackInfo> mountStacks = mountSystem.getMountStacks(entity);

            if (!mountStacks.isEmpty()) {
                tooltip.add(Component.translatable("jade.element_endow.mounts")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

                for (Map.Entry<String, IElementMountSystem.MountStackInfo> entry : mountStacks.entrySet()) {
                    String elementId = entry.getKey();
                    IElementMountSystem.MountStackInfo stackInfo = entry.getValue();

                    Component mountText = Component.literal(" • ")
                            .append(getElementDisplayName(elementId))
                            .append(Component.literal(" [" + stackInfo.currentStacks + "/" + stackInfo.maxStacks + "]"))
                            .append(Component.literal(" (" + String.format("%.1f", stackInfo.totalAmount) + ")"))
                            .withStyle(ChatFormatting.LIGHT_PURPLE);

                    tooltip.add(mountText);
                }
            }

        } catch (Exception e) {
            //静默失败
        }
    }

    private Component getElementDisplayName(String elementId) {
        //复用元素显示名称逻辑
        String[] parts = elementId.split(":");
        if (parts.length == 2) {
            String translationKey = "element." + parts[0] + "." + parts[1];
            Component translated = Component.translatable(translationKey);
            if (!translated.getString().equals(translationKey)) {
                return translated;
            }
        }
        return Component.literal(elementId);
    }

    @Override
    public ResourceLocation getUid() {
        return ElementEndowJadePlugin.MOUNT_INFO;
    }
}