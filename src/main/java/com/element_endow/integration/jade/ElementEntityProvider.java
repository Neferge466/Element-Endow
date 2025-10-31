package com.element_endow.integration.jade;

import com.element_endow.api.ElementSystemAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Collection;

public enum ElementEntityProvider implements IEntityComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!config.get(ElementEndowJadePlugin.ELEMENT_INFO)) {
            return;
        }

        if (!(accessor.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        try {
            var elementSystem = ElementSystemAPI.getElementSystem();
            Collection<String> enabledElements = elementSystem.getEnabledElements();

            boolean hasElements = false;

            // 检查是否有激活的元素
            for (String elementId : enabledElements) {
                double value = elementSystem.getElementValue(entity, elementId);
                if (value > 0.01) { // 忽略很小的值
                    if (!hasElements) {
                        tooltip.add(Component.translatable("jade.element_endow.elements")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                        hasElements = true;
                    }

                    addElementToTooltip(tooltip, elementId, value);
                }
            }

        } catch (Exception e) {
            //Jade不存在时静默失败
        }
    }

    private void addElementToTooltip(ITooltip tooltip, String elementId, double value) {
        String displayName = getElementDisplayName(elementId);
        String formattedValue = String.format("%.1f", value);

        Component elementText = Component.literal(" • ")
                .append(Component.translatable(displayName))
                .append(Component.literal(": " + formattedValue))
                .withStyle(ChatFormatting.GRAY);

        tooltip.add(elementText);
    }

    private String getElementDisplayName(String elementId) {
        //从注册表获取显示名称
        var elementSystem = ElementSystemAPI.getElementSystem();
        if (elementSystem instanceof com.element_endow.core.ElementSystemImpl systemImpl) {
            var registry = systemImpl.getRegistry();
            var attributeData = registry.getAttributeData(elementId);
            if (attributeData != null) {
                return attributeData.displayName;
            }
        }

        //回退
        String[] parts = elementId.split(":");
        if (parts.length == 2) {
            return "element." + parts[0] + "." + parts[1];
        }
        return "element." + elementId;
    }

    @Override
    public ResourceLocation getUid() {
        return ElementEndowJadePlugin.ELEMENT_INFO;
    }
}