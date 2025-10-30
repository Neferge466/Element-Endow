package com.element_endow.client;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.api.IElementSystem;
import com.element_endow.data.CombinationLoader;
import com.element_endow.data.ReactionLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class DebugOverlay {
    private boolean visible = false;
    private boolean showAttributeScreen = false;
    private boolean showEntityInspect = false;
    private boolean showReactionScreen = false;
    private final Minecraft minecraft;
    private LivingEntity targetEntity = null;

    //滚动和缩放控制
    private float scrollOffset = 0;
    private float scale = 0.8f;
    private final float minScale = 0.6f;
    private final float maxScale = 1.2f;

    //各屏幕滚动
    private float attributeScrollOffset = 0;
    private float reactionScrollOffset = 0;

    public DebugOverlay(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void toggleVisibility() {
        this.visible = !this.visible;
        if (!this.visible) {
            this.showAttributeScreen = false;
            this.showEntityInspect = false;
            this.showReactionScreen = false;
        }
    }

    public void toggleAttributeScreen() {
        if (visible) {
            this.showAttributeScreen = !this.showAttributeScreen;
            this.showReactionScreen = false;
            this.attributeScrollOffset = 0;
        }
    }

    public void toggleEntityInspect() {
        if (visible) {
            this.showEntityInspect = !this.showEntityInspect;
            this.showReactionScreen = false;
        }
    }

    public void toggleReactionScreen() {
        if (visible) {
            this.showReactionScreen = !this.showReactionScreen;
            this.showAttributeScreen = false;
            this.reactionScrollOffset = 0;
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setTargetEntity(LivingEntity entity) {
        this.targetEntity = entity;
    }

    public void handleScroll(double delta) {
        if (showAttributeScreen) {
            attributeScrollOffset = (float) Math.max(0, attributeScrollOffset - delta * 10);
        } else if (showReactionScreen) {
            reactionScrollOffset = (float) Math.max(0, reactionScrollOffset - delta * 10);
        } else {
            scrollOffset = (float) Math.max(0, scrollOffset - delta * 10);
        }
    }

    public void handleScaleChange(float delta) {
        scale = Math.max(minScale, Math.min(maxScale, scale + delta * 0.1f));
    }

    public void render(GuiGraphics guiGraphics) {
        if (!visible || minecraft.player == null) return;

        //更新鼠标指向的实体
        ClientEventHandler.updateTargetedEntity();

        if (showAttributeScreen) {
            renderAttributeScreen(guiGraphics);
        } else if (showReactionScreen) {
            renderReactionScreen(guiGraphics);
        } else if (showEntityInspect && targetEntity != null) {
            renderEntityInspectScreen(guiGraphics, targetEntity);
        } else {
            renderMainScreen(guiGraphics);
        }
    }

    private void renderMainScreen(GuiGraphics guiGraphics) {
        Player player = minecraft.player;
        IElementSystem elementSystem = ElementSystemAPI.getElementSystem();

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        //应用缩放
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, scale);

        float inverseScale = 1.0f / scale;
        int scaledWidth = (int)(screenWidth * inverseScale);
        int scaledHeight = (int)(screenHeight * inverseScale);

        //计算面板尺寸
        int panelWidth = 350;
        int panelHeight = Math.min(450, scaledHeight - 40);
        int panelX = 20;
        int panelY = 20;

        //绘制背景
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x80000000);

        //标题
        drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.title"),
                panelX + 10, panelY + 10, 0xFFFFFF);

        int contentStartY = panelY + 30;
        int currentY = contentStartY - (int)scrollOffset;
        int maxContentHeight = panelHeight - 60;

        //显示玩家当前拥有的元素
        drawSectionTitle(guiGraphics, Component.translatable("gui.element_endow.debug.player_elements"),
                panelX + 10, currentY);
        currentY += 15;

        Collection<String> enabledElements = elementSystem.getEnabledElements();
        boolean hasActiveElements = false;
        for (String elementId : enabledElements) {
            double value = elementSystem.getElementValue(player, elementId);
            if (value > 0) {
                drawScaledString(guiGraphics,
                        Component.translatable("gui.element_endow.debug.element_value", elementId, String.format("%.2f", value)),
                        panelX + 20, currentY, 0xAAAAAA);
                currentY += 10;
                hasActiveElements = true;
            }

            if (currentY > contentStartY + maxContentHeight) break;
        }

        if (!hasActiveElements) {
            drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.none"),
                    panelX + 20, currentY, 0x666666);
            currentY += 10;
        }
        currentY += 5;

        //显示激活的组合
        drawSectionTitle(guiGraphics, Component.translatable("gui.element_endow.debug.active_combinations"),
                panelX + 10, currentY);
        currentY += 15;

        Collection<String> activeCombinations = elementSystem.getCombinationSystem().getActiveCombinations(player);
        if (activeCombinations.isEmpty()) {
            drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.none"),
                    panelX + 20, currentY, 0x666666);
            currentY += 10;
        } else {
            CombinationLoader combinationLoader = elementSystem.getCombinationSystem().getCombinationLoader();
            for (String combinationId : activeCombinations) {
                CombinationLoader.ElementCombination combo = combinationLoader.getCombinations().get(combinationId);
                if (combo != null) {
                    drawScaledString(guiGraphics, Component.literal("• " + combinationId),
                            panelX + 20, currentY, 0x00FF00);
                    currentY += 8;

                    //显示组合条件
                    if (combo.conditions != null && !combo.conditions.isEmpty()) {
                        Component conditionText = getConditionDescription(combo.conditions);
                        drawScaledString(guiGraphics,
                                Component.translatable("gui.element_endow.debug.conditions", conditionText),
                                panelX + 25, currentY, 0xFFA500);
                        currentY += 8;
                    }

                    //显示必需元素
                    if (!combo.requiredElements.isEmpty()) {
                        drawScaledString(guiGraphics,
                                Component.translatable("gui.element_endow.debug.requires_elements", String.join(", ", combo.requiredElements)),
                                panelX + 25, currentY, 0xAAAAAA);
                        currentY += 6;
                    }

                    //显示禁止元素
                    if (!combo.forbiddenElements.isEmpty()) {
                        drawScaledString(guiGraphics,
                                Component.translatable("gui.element_endow.debug.forbids_elements", String.join(", ", combo.forbiddenElements)),
                                panelX + 25, currentY, 0xFF5555);
                        currentY += 6;
                    }

                    //显示属性效果
                    if (combo.attributeEffects != null && !combo.attributeEffects.isEmpty()) {
                        drawScaledString(guiGraphics,
                                Component.translatable("gui.element_endow.debug.attribute_effects"),
                                panelX + 25, currentY, 0x55FFFF);
                        currentY += 6;

                        for (CombinationLoader.AttributeEffect effect : combo.attributeEffects) {
                            String effectText = String.format("%s %s %.2f", effect.attribute, effect.operation, effect.value);
                            drawScaledString(guiGraphics,
                                    Component.literal("  - " + effectText),
                                    panelX + 30, currentY, 0xAAAAAA);
                            currentY += 6;
                        }
                    }

                    //显示状态效果
                    if (combo.statusEffects != null && !combo.statusEffects.isEmpty()) {
                        drawScaledString(guiGraphics,
                                Component.translatable("gui.element_endow.debug.status_effects"),
                                panelX + 25, currentY, 0xFF55FF);
                        currentY += 6;

                        for (CombinationLoader.StatusEffect effect : combo.statusEffects) {
                            String effectText = String.format("%s (Lv%d, %ds)", effect.effect, effect.amplifier + 1, effect.duration / 20);
                            drawScaledString(guiGraphics,
                                    Component.literal("  - " + effectText),
                                    panelX + 30, currentY, 0xAAAAAA);
                            currentY += 6;
                        }
                    }

                    currentY += 2;
                }

                if (currentY > contentStartY + maxContentHeight) break;
            }
        }
        currentY += 5;

        //显示当前效果
        drawSectionTitle(guiGraphics, Component.translatable("gui.element_endow.debug.active_effects"),
                panelX + 10, currentY);
        currentY += 15;

        Collection<MobEffectInstance> effects = player.getActiveEffects();
        if (effects.isEmpty()) {
            drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.none"),
                    panelX + 20, currentY, 0x666666);
            currentY += 10;
        } else {
            for (MobEffectInstance effect : effects) {
                String effectName = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect()).toString();
                String duration = String.format("%.1fs", effect.getDuration() / 20.0f);
                drawScaledString(guiGraphics,
                        Component.translatable("gui.element_endow.debug.effect_entry", effectName, effect.getAmplifier() + 1, duration),
                        panelX + 20, currentY, 0xAAAAAA);
                currentY += 8;

                if (currentY > contentStartY + maxContentHeight) break;
            }
        }

        //显示系统统计
        drawSectionTitle(guiGraphics, Component.translatable("gui.element_endow.debug.system_stats"),
                panelX + 10, currentY);
        currentY += 15;

        ReactionLoader reactionLoader = elementSystem.getReactionSystem().getReactionLoader();
        CombinationLoader combinationLoader = elementSystem.getCombinationSystem().getCombinationLoader();

        drawScaledString(guiGraphics,
                Component.translatable("gui.element_endow.debug.registered_elements", elementSystem.getRegisteredElements().size()),
                panelX + 20, currentY, 0xAAAAAA);
        currentY += 8;

        drawScaledString(guiGraphics,
                Component.translatable("gui.element_endow.debug.reaction_count", reactionLoader.getReactionCount()),
                panelX + 20, currentY, 0xAAAAAA);
        currentY += 8;

        drawScaledString(guiGraphics,
                Component.translatable("gui.element_endow.debug.combination_count", combinationLoader.getCombinations().size()),
                panelX + 20, currentY, 0xAAAAAA);
        currentY += 8;

        //显示提示
        int helpY = panelY + panelHeight - 25;
        drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.help_main"),
                panelX + 10, helpY, 0x666666);

        //显示滚动提示
        if (scrollOffset > 0 || currentY > contentStartY + maxContentHeight) {
            drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.scroll_help"),
                    panelX + 10, helpY - 12, 0x666666);
        }

        guiGraphics.pose().popPose();
    }

    private void renderAttributeScreen(GuiGraphics guiGraphics) {
        Player player = minecraft.player;
        IElementSystem elementSystem = ElementSystemAPI.getElementSystem();

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        //应用缩放
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, scale);

        float inverseScale = 1.0f / scale;
        int scaledWidth = (int)(screenWidth * inverseScale);
        int scaledHeight = (int)(screenHeight * inverseScale);

        //计算面板尺寸
        int panelWidth = 400;
        int panelHeight = Math.min(500, scaledHeight - 40);
        int panelX = (scaledWidth - panelWidth) / 2;
        int panelY = 20;

        //绘制背景
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x80000000);

        //标题
        drawScaledString(guiGraphics, Component.translatable("gui.element_endow.attributes.title"),
                panelX + 10, panelY + 10, 0xFFFFFF);

        int contentStartY = panelY + 30;
        int currentY = contentStartY - (int)attributeScrollOffset;
        int maxContentHeight = panelHeight - 60;

        //收集所有属性
        List<Attribute> allAttributes = new ArrayList<>();
        ForgeRegistries.ATTRIBUTES.getValues().forEach(allAttributes::add);

        //显示元素属性
        drawSectionTitle(guiGraphics, Component.translatable("gui.element_endow.attributes.elements"),
                panelX + 10, currentY);
        currentY += 15;

        Collection<String> enabledElements = elementSystem.getEnabledElements();
        for (String elementId : enabledElements) {
            double value = elementSystem.getElementValue(player, elementId);
            drawScaledString(guiGraphics,
                    Component.translatable("gui.element_endow.debug.element_value", elementId, String.format("%.3f", value)),
                    panelX + 20, currentY, value > 0 ? 0x00FF00 : 0x666666);
            currentY += 9;

            if (currentY > contentStartY + maxContentHeight) break;
        }
        currentY += 5;

        //显示其他属性
        drawSectionTitle(guiGraphics, Component.translatable("gui.element_endow.attributes.other"),
                panelX + 10, currentY);
        currentY += 15;

        int attributeCount = 0;
        for (Attribute attribute : allAttributes) {
            if (player.getAttribute(attribute) != null) {
                double value = player.getAttributeValue(attribute);
                ResourceLocation attributeId = ForgeRegistries.ATTRIBUTES.getKey(attribute);
                if (attributeId != null && !isElementAttribute(attributeId)) {
                    String attributeName = getAttributeDisplayName(attributeId);
                    drawScaledString(guiGraphics,
                            Component.translatable("gui.element_endow.attributes.entry", attributeName, String.format("%.3f", value)),
                            panelX + 20, currentY, 0xAAAAAA);
                    currentY += 9;
                    attributeCount++;
                }
            }

            if (currentY > contentStartY + maxContentHeight) break;
        }

        if (attributeCount == 0) {
            drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.none"),
                    panelX + 20, currentY, 0x666666);
            currentY += 9;
        }

        //显示滚动提示
        if (attributeScrollOffset > 0 || currentY > contentStartY + maxContentHeight) {
            int scrollY = panelY + panelHeight - 15;
            drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.scroll_help"),
                    panelX + 10, scrollY, 0x666666);
        }

        //显示返回提示
        int helpY = panelY + panelHeight - 30;
        drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.help_attributes"),
                panelX + 10, helpY, 0x666666);

        guiGraphics.pose().popPose();
    }

    private void renderReactionScreen(GuiGraphics guiGraphics) {
        IElementSystem elementSystem = ElementSystemAPI.getElementSystem();
        ReactionLoader reactionLoader = elementSystem.getReactionSystem().getReactionLoader();

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        //应用缩放
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, scale);

        float inverseScale = 1.0f / scale;
        int scaledWidth = (int)(screenWidth * inverseScale);
        int scaledHeight = (int)(screenHeight * inverseScale);

        //计算面板尺寸
        int panelWidth = 450;
        int panelHeight = Math.min(500, scaledHeight - 40);
        int panelX = (scaledWidth - panelWidth) / 2;
        int panelY = 20;

        //绘制背景
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x80000000);

        //标题
        drawScaledString(guiGraphics, Component.translatable("gui.element_endow.reactions.title"),
                panelX + 10, panelY + 10, 0xFFFFFF);

        int contentStartY = panelY + 30;
        int currentY = contentStartY - (int)reactionScrollOffset;
        int maxContentHeight = panelHeight - 60;

        Map<String, ReactionLoader.ElementReaction> reactions = reactionLoader.getReactions();
        if (reactions.isEmpty()) {
            drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.none"),
                    panelX + 20, currentY, 0x666666);
            currentY += 10;
        } else {
            for (ReactionLoader.ElementReaction reaction : reactions.values()) {
                //显示反应ID和元素
                drawScaledString(guiGraphics,
                        Component.translatable("gui.element_endow.reactions.entry", reaction.id, reaction.elementA, reaction.elementB),
                        panelX + 15, currentY, 0x00FF00);
                currentY += 10;

                //显示条件
                if (reaction.conditions != null) {
                    Component conditionText = getReactionConditionDescription(reaction.conditions);
                    drawScaledString(guiGraphics,
                            Component.translatable("gui.element_endow.debug.conditions", conditionText),
                            panelX + 25, currentY, 0xFFA500);
                    currentY += 8;
                }

                //显示攻击效果
                if (reaction.attackEntry != null) {
                    drawScaledString(guiGraphics,
                            Component.translatable("gui.element_endow.reactions.attack_effects"),
                            panelX + 25, currentY, 0xFFFF00);
                    currentY += 8;

                    if (reaction.attackEntry.damageMultiplier != 1.0) {
                        drawScaledString(guiGraphics,
                                Component.translatable("gui.element_endow.reactions.damage_multiplier", String.format("%.2f", reaction.attackEntry.damageMultiplier)),
                                panelX + 30, currentY, 0xAAAAAA);
                        currentY += 6;
                    }

                    if (reaction.attackEntry.extraDamage != 0.0) {
                        drawScaledString(guiGraphics,
                                Component.translatable("gui.element_endow.reactions.extra_damage", String.format("%.2f", reaction.attackEntry.extraDamage)),
                                panelX + 30, currentY, 0xAAAAAA);
                        currentY += 6;
                    }

                    //显示目标效果
                    if (reaction.attackEntry.targetEffects != null && !reaction.attackEntry.targetEffects.isEmpty()) {
                        drawScaledString(guiGraphics,
                                Component.translatable("gui.element_endow.reactions.target_effects", reaction.attackEntry.targetEffects.size()),
                                panelX + 30, currentY, 0xAAAAAA);
                        currentY += 6;
                    }

                    //显示属性修饰符
                    if (reaction.attackEntry.targetAttributeModifiers != null && !reaction.attackEntry.targetAttributeModifiers.isEmpty()) {
                        drawScaledString(guiGraphics,
                                Component.translatable("gui.element_endow.reactions.target_modifiers", reaction.attackEntry.targetAttributeModifiers.size()),
                                panelX + 30, currentY, 0xAAAAAA);
                        currentY += 6;
                    }
                }

                //显示防御效果
                if (reaction.defenseEntry != null) {
                    drawScaledString(guiGraphics,
                            Component.translatable("gui.element_endow.reactions.defense_effects"),
                            panelX + 25, currentY, 0xFFFF00);
                    currentY += 8;

                    if (reaction.defenseEntry.defenseMultiplier != 1.0) {
                        drawScaledString(guiGraphics,
                                Component.translatable("gui.element_endow.reactions.defense_multiplier", String.format("%.2f", reaction.defenseEntry.defenseMultiplier)),
                                panelX + 30, currentY, 0xAAAAAA);
                        currentY += 6;
                    }

                    if (reaction.defenseEntry.damageReduction != 0.0) {
                        drawScaledString(guiGraphics,
                                Component.translatable("gui.element_endow.reactions.damage_reduction", String.format("%.2f", reaction.defenseEntry.damageReduction)),
                                panelX + 30, currentY, 0xAAAAAA);
                        currentY += 6;
                    }
                }

                //显示挂载
                if (reaction.mountData != null) {
                    drawScaledString(guiGraphics,
                            Component.translatable("gui.element_endow.reactions.mount_effect",
                                    reaction.mountData.elementId,
                                    String.format("%.2f", reaction.mountData.amount),
                                    reaction.mountData.duration),
                            panelX + 25, currentY, 0xAAAAAA);
                    currentY += 8;
                }

                currentY += 5;

                if (currentY > contentStartY + maxContentHeight) break;
            }
        }

        //显示滚动提示
        if (reactionScrollOffset > 0 || currentY > contentStartY + maxContentHeight) {
            int scrollY = panelY + panelHeight - 15;
            drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.scroll_help"),
                    panelX + 10, scrollY, 0x666666);
        }

        //显示返回提示
        int helpY = panelY + panelHeight - 30;
        drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.help_reactions"),
                panelX + 10, helpY, 0x666666);

        guiGraphics.pose().popPose();
    }

    private void renderEntityInspectScreen(GuiGraphics guiGraphics, LivingEntity entity) {
        IElementSystem elementSystem = ElementSystemAPI.getElementSystem();

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // 应用缩放
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, scale);

        float inverseScale = 1.0f / scale;
        int scaledWidth = (int)(screenWidth * inverseScale);
        int scaledHeight = (int)(screenHeight * inverseScale);

        // 计算面板尺寸
        int panelWidth = 350;
        int panelHeight = Math.min(450, scaledHeight - 40); // 增加高度以容纳新信息
        int panelX = (scaledWidth - panelWidth) / 2;
        int panelY = 20;

        // 绘制背景
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x80000000);

        // 显示实体名称
        String entityName = entity.getDisplayName().getString();
        String entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        drawScaledString(guiGraphics, Component.translatable("gui.element_endow.inspect.title", entityName, entityType),
                panelX + 10,
                panelY + 10,
                0xFFFFFF);

        int contentStartY = panelY + 30;
        int currentY = contentStartY - (int)scrollOffset;
        int maxContentHeight = panelHeight - 70; // 调整最大高度

        //显示实体维度信息
        drawSectionTitle(guiGraphics, Component.translatable("gui.element_endow.inspect.dimension"),
                panelX + 10,
                currentY);
        currentY += 15;

        String dimensionName = getDimensionName(entity.level());
        drawScaledString(guiGraphics,
                Component.translatable("gui.element_endow.inspect.dimension_value", dimensionName),
                panelX + 20,
                currentY,
                0xAAAAAA);
        currentY += 10;

        //显示实体位置
        drawScaledString(guiGraphics,
                Component.translatable("gui.element_endow.inspect.position",
                        entity.getBlockX(), entity.getBlockY(), entity.getBlockZ()),
                panelX + 20,
                currentY,
                0xAAAAAA);
        currentY += 10;
        currentY += 5;

        //原有的实体元素显示
        drawSectionTitle(guiGraphics, Component.translatable("gui.element_endow.debug.entity_elements"),
                panelX + 10,
                currentY);
        currentY += 15;

        Collection<String> enabledElements = elementSystem.getEnabledElements();
        boolean hasElements = false;
        for (String elementId : enabledElements) {
            if (elementSystem.hasElement(entity, elementId)) {
                double value = elementSystem.getElementValue(entity, elementId);
                drawScaledString(guiGraphics,
                        Component.translatable("gui.element_endow.debug.element_value", elementId, String.format("%.2f", value)),
                        panelX + 20,
                        currentY,
                        0xAAAAAA);
                currentY += 10;
                hasElements = true;
            }

            if (currentY > contentStartY + maxContentHeight) break;
        }

        if (!hasElements) {
            drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.none"),
                    panelX + 20,
                    currentY,
                    0x666666);
            currentY += 10;
        }
        currentY += 5;

        //原有的实体激活的组合
        drawSectionTitle(guiGraphics, Component.translatable("gui.element_endow.debug.active_combinations"),
                panelX + 10,
                currentY);
        currentY += 15;

        Collection<String> activeCombinations = elementSystem.getCombinationSystem().getActiveCombinations(entity);
        if (activeCombinations.isEmpty()) {
            drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.none"),
                    panelX + 20,
                    currentY,
                    0x666666);
            currentY += 10;
        } else {
            for (String combination : activeCombinations) {
                drawScaledString(guiGraphics, Component.literal("• " + combination),
                        panelX + 20,
                        currentY,
                        0x00FF00);
                currentY += 10;

                if (currentY > contentStartY + maxContentHeight) break;
            }
        }
        currentY += 5;

        //原有的实体效果
        drawSectionTitle(guiGraphics, Component.translatable("gui.element_endow.debug.active_effects"),
                panelX + 10, currentY);
        currentY += 15;

        Collection<MobEffectInstance> effects = entity.getActiveEffects();
        if (effects.isEmpty()) {
            drawScaledString(guiGraphics, Component.translatable("gui.element_endow.debug.none"),
                    panelX + 20,
                    currentY,
                    0x666666);
            currentY += 10;
        } else {
            for (MobEffectInstance effect : effects) {
                String effectName = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect()).toString();
                String duration = String.format("%.1fs", effect.getDuration() / 20.0f);
                drawScaledString(guiGraphics,
                        Component.translatable("gui.element_endow.debug.effect_entry",
                                effectName,
                                effect.getAmplifier() + 1, duration),
                        panelX + 20,
                        currentY,
                        0xAAAAAA);
                currentY += 8;

                if (currentY > contentStartY + maxContentHeight) break;
            }
        }
        currentY += 5;

        //原有的实体生命值
        drawSectionTitle(guiGraphics, Component.translatable("gui.element_endow.inspect.health"),
                panelX + 10,
                currentY);
        currentY += 15;

        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        drawScaledString(guiGraphics,
                Component.translatable("gui.element_endow.inspect.health_value",
                        String.format("%.1f", health),
                        String.format("%.1f", maxHealth)),
                panelX + 20,
                currentY, getHealthColor(health / maxHealth));
        currentY += 10;

        //显示返回提示
        int helpY = panelY + panelHeight - 20;
        drawScaledString(guiGraphics,
                Component.translatable("gui.element_endow.debug.help_inspect"),
                panelX + 10,
                helpY,
                0x666666);

        guiGraphics.pose().popPose();
    }

    private void drawSectionTitle(GuiGraphics guiGraphics, Component text, int x, int y) {
        drawScaledString(guiGraphics, text, x, y, 0x00FF00);
    }

    private void drawScaledString(GuiGraphics guiGraphics, Component text, int x, int y, int color) {
        guiGraphics.drawString(minecraft.font, text, x, y, color, false);
    }

    private boolean isElementAttribute(ResourceLocation attributeId) {
        return attributeId.getNamespace().equals("element_endow");
    }

    private String getAttributeDisplayName(ResourceLocation attributeId) {
        String path = attributeId.getPath();
        //简化属性名称显示
        if (path.contains(".")) {
            return path.substring(path.lastIndexOf(".") + 1);
        }
        return path;
    }

    private int getHealthColor(float healthRatio) {
        if (healthRatio > 0.7f) return 0x00FF00; //绿
        if (healthRatio > 0.3f) return 0xFFFF00; //黄
        return 0xFF0000; //红
    }

    private Component getConditionDescription(Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return Component.translatable("gui.element_endow.conditions.none");
        }

        // 使用 Component 列表来构建条件描述
        List<Component> conditionTexts = new ArrayList<>();

        try {
            if (conditions.containsKey("biome")) {
                Object biomeCond = conditions.get("biome");
                if (biomeCond instanceof String) {
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.biome", biomeCond));
                } else if (biomeCond instanceof List) {
                    String biomes = String.join(", ", (List<String>) biomeCond);
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.biomes", biomes));
                }
            }

            if (conditions.containsKey("dimension")) {
                Object dimCond = conditions.get("dimension");
                if (dimCond instanceof String) {
                    String dimensionName = getDimensionDisplayName((String) dimCond);
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.dimension", dimensionName));
                } else if (dimCond instanceof List) {
                    List<String> dimensions = (List<String>) dimCond;
                    List<String> dimensionNames = new ArrayList<>();
                    for (String dim : dimensions) {
                        dimensionNames.add(getDimensionDisplayName(dim));
                    }
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.dimensions", String.join(", ", dimensionNames)));
                }
            }

            if (conditions.containsKey("weather")) {
                Object weatherCond = conditions.get("weather");
                if (weatherCond instanceof String) {
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.weather", weatherCond));
                } else if (weatherCond instanceof Map) {
                    Map<?, ?> weatherMap = (Map<?, ?>) weatherCond;
                    List<String> weatherParts = new ArrayList<>();
                    if (weatherMap.containsKey("raining")) {
                        weatherParts.add((Boolean) weatherMap.get("raining") ? "raining" : "clear");
                    }
                    if (weatherMap.containsKey("thundering")) {
                        weatherParts.add((Boolean) weatherMap.get("thundering") ? "thundering" : "not_thundering");
                    }
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.weather", String.join(", ", weatherParts)));
                }
            }

            if (conditions.containsKey("time")) {
                Object timeCond = conditions.get("time");
                if (timeCond instanceof String) {
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.time", timeCond));
                } else if (timeCond instanceof Map) {
                    Map<?, ?> timeMap = (Map<?, ?>) timeCond;
                    List<String> timeParts = new ArrayList<>();
                    if (timeMap.containsKey("min")) {
                        timeParts.add("min: " + timeMap.get("min"));
                    }
                    if (timeMap.containsKey("max")) {
                        timeParts.add("max: " + timeMap.get("max"));
                    }
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.time_range", String.join(", ", timeParts)));
                }
            }

            if (conditions.containsKey("moon_phase")) {
                Object moonCond = conditions.get("moon_phase");
                if (moonCond instanceof Number) {
                    int phase = ((Number) moonCond).intValue();
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.moon_phase", getMoonPhaseName(phase)));
                } else if (moonCond instanceof Map) {
                    Map<?, ?> moonMap = (Map<?, ?>) moonCond;
                    List<String> moonParts = new ArrayList<>();
                    if (moonMap.containsKey("full_moon") && (Boolean) moonMap.get("full_moon")) {
                        moonParts.add("full_moon");
                    }
                    if (moonMap.containsKey("new_moon") && (Boolean) moonMap.get("new_moon")) {
                        moonParts.add("new_moon");
                    }
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.moon_phases", String.join(", ", moonParts)));
                }
            }

            if (conditions.containsKey("health")) {
                Object healthCond = conditions.get("health");
                if (healthCond instanceof Map) {
                    Map<?, ?> healthMap = (Map<?, ?>) healthCond;
                    List<String> healthParts = new ArrayList<>();
                    if (healthMap.containsKey("min")) {
                        healthParts.add("min: " + healthMap.get("min"));
                    }
                    if (healthMap.containsKey("max")) {
                        healthParts.add("max: " + healthMap.get("max"));
                    }
                    if (healthMap.containsKey("percentage")) {
                        healthParts.add("percentage: " + healthMap.get("percentage"));
                    }
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.health", String.join(", ", healthParts)));
                }
            }

            if (conditions.containsKey("difficulty")) {
                Object diffCond = conditions.get("difficulty");
                if (diffCond instanceof String) {
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.difficulty", diffCond));
                }
            }

            if (conditions.containsKey("elements")) {
                List<?> elementConds = (List<?>) conditions.get("elements");
                if (!elementConds.isEmpty()) {
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.elements", elementConds.size()));
                    for (Object condObj : elementConds) {
                        if (condObj instanceof Map) {
                            Map<?, ?> elementCond = (Map<?, ?>) condObj;
                            String elementId = (String) elementCond.get("element");
                            if (elementId != null) {
                                List<String> elementParts = new ArrayList<>();
                                elementParts.add(elementId);
                                if (elementCond.containsKey("min_value")) {
                                    elementParts.add("min: " + elementCond.get("min_value"));
                                }
                                if (elementCond.containsKey("max_value")) {
                                    elementParts.add("max: " + elementCond.get("max_value"));
                                }
                                conditionTexts.add(Component.literal("    - " + String.join(", ", elementParts)));
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            conditionTexts.add(Component.translatable("gui.element_endow.conditions.error"));
        }

        if (conditionTexts.isEmpty()) {
            return Component.translatable("gui.element_endow.conditions.none");
        }

        // 将条件列表组合成一个 Component
        MutableComponent result = Component.empty();
        for (int i = 0; i < conditionTexts.size(); i++) {
            if (i > 0) {
                result.append(Component.literal(" | "));
            }
            result.append(conditionTexts.get(i));
        }

        return result;
    }



    private static String getDimensionDisplayName(String dimensionId) {
        String dimensionKey = "dimension." + dimensionId.replace(':', '.');
        Component translated = Component.translatable(dimensionKey);
        if (!translated.getString().equals(dimensionKey)) {
            return translated.getString();
        }
        return dimensionId;
    }

    private String getDimensionName(net.minecraft.world.level.Level level) {
        if (level == null) return "Unknown";

        net.minecraft.resources.ResourceLocation dimensionId = level.dimension().location();
        String dimensionKey = "dimension." + dimensionId.getNamespace() + "." + dimensionId.getPath();

        // 尝试获取本地化的维度名称
        Component translated = Component.translatable(dimensionKey);
        if (!translated.getString().equals(dimensionKey)) {
            return translated.getString();
        }

        // 如果没有本地化，返回原始ID（美化显示）
        return dimensionId.getPath().substring(0, 1).toUpperCase() +
                dimensionId.getPath().substring(1).replace('_', ' ');
    }



    //ReactionConditions
    private Component getReactionConditionDescription(ReactionLoader.ReactionConditions conditions) {
        if (conditions == null) {
            return Component.translatable("gui.element_endow.conditions.none");
        }

        List<String> conditionParts = new ArrayList<>();

        //处理攻击者条件
        if (conditions.attackerConditions != null && !conditions.attackerConditions.isEmpty()) {
            conditionParts.add("attacker:" + getSimpleConditionDescription(conditions.attackerConditions));
        }

        //处理目标条件
        if (conditions.targetConditions != null && !conditions.targetConditions.isEmpty()) {
            conditionParts.add("target:" + getSimpleConditionDescription(conditions.targetConditions));
        }

        //处理世界条件
        if (conditions.worldConditions != null && !conditions.worldConditions.isEmpty()) {
            conditionParts.add("world:" + getSimpleConditionDescription(conditions.worldConditions));
        }

        if (conditionParts.isEmpty()) {
            return Component.translatable("gui.element_endow.conditions.none");
        }

        return Component.literal(String.join("; ", conditionParts));
    }


    private String getMoonPhaseName(int phase) {
        switch (phase) {
            case 0: return "Full Moon";
            case 1: return "Waning Gibbous";
            case 2: return "Last Quarter";
            case 3: return "Waning Crescent";
            case 4: return "New Moon";
            case 5: return "Waxing Crescent";
            case 6: return "First Quarter";
            case 7: return "Waxing Gibbous";
            default: return "Unknown";
        }
    }


    private String getSimpleConditionDescription(Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return "none";
        }

        List<String> parts = new ArrayList<>();
        if (conditions.containsKey("biome")) {
            parts.add("biome");
        }
        if (conditions.containsKey("dimension")) {
            parts.add("dimension");
        }
        if (conditions.containsKey("weather")) {
            parts.add("weather");
        }
        if (conditions.containsKey("time")) {
            parts.add("time");
        }
        if (conditions.containsKey("moon_phase")) {
            parts.add("moon");
        }
        if (conditions.containsKey("health")) {
            parts.add("health");
        }
        if (conditions.containsKey("difficulty")) {
            parts.add("difficulty");
        }
        if (conditions.containsKey("elements")) {
            parts.add("elements");
        }
        return String.join(",", parts);
    }
}