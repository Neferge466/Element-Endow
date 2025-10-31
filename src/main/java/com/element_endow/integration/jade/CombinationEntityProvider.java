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

public enum CombinationEntityProvider implements IEntityComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!config.get(ElementEndowJadePlugin.COMBINATION_INFO)) {
            return;
        }

        if (!(accessor.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        try {
            var combinationSystem = ElementSystemAPI.getElementSystem().getCombinationSystem();
            Collection<String> activeCombinations = combinationSystem.getActiveCombinations(entity);

            if (!activeCombinations.isEmpty()) {
                tooltip.add(Component.translatable("jade.element_endow.combinations")
                        .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));

                for (String combinationId : activeCombinations) {
                    tooltip.add(Component.literal(" • ")
                            .append(getCombinationDisplayName(combinationId))
                            .withStyle(ChatFormatting.AQUA));
                }
            }

        } catch (Exception e) {
            //静默失败
        }
    }

    private Component getCombinationDisplayName(String combinationId) {
        //从组合加载器获取显示名称
        try {
            var combinationSystem = ElementSystemAPI.getElementSystem().getCombinationSystem();
            var combinationLoader = combinationSystem.getCombinationLoader();
            var combination = combinationLoader.getCombinations().get(combinationId);

            if (combination != null) {
                return Component.literal(combinationId);
            }
        } catch (Exception e) {
            //忽略
        }

        //回退到翻译键原始ID
        String translationKey = "combination." + combinationId.replace(':', '.');
        Component translated = Component.translatable(translationKey);
        if (!translated.getString().equals(translationKey)) {
            return translated;
        }

        return Component.literal(combinationId);
    }

    @Override
    public ResourceLocation getUid() {
        return ElementEndowJadePlugin.COMBINATION_INFO;
    }
}