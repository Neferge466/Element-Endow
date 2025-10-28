package com.element_endow.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "element_endow", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {
    private static final DebugOverlay DEBUG_OVERLAY = new DebugOverlay(Minecraft.getInstance());
    private static LivingEntity lastTargetedEntity = null;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.DEBUG_OVERLAY);
        event.register(KeyBindings.ATTRIBUTE_SCREEN);
        event.register(KeyBindings.ENTITY_INSPECT);
        event.register(KeyBindings.REACTION_SCREEN);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (DEBUG_OVERLAY.isVisible()) {
            DEBUG_OVERLAY.render(event.getGuiGraphics());
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        //检测调试界面按键
        if (KeyBindings.DEBUG_OVERLAY.consumeClick()) {
            DEBUG_OVERLAY.toggleVisibility();
        }

        //属性屏幕
        if (KeyBindings.ATTRIBUTE_SCREEN.consumeClick()) {
            DEBUG_OVERLAY.toggleAttributeScreen();
        }

        //实体检查
        if (KeyBindings.ENTITY_INSPECT.consumeClick()) {
            DEBUG_OVERLAY.toggleEntityInspect();
        }

        //反应屏幕
        if (KeyBindings.REACTION_SCREEN.consumeClick()) {
            DEBUG_OVERLAY.toggleReactionScreen();
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        if (DEBUG_OVERLAY.isVisible()) {
            double scrollDelta = event.getScrollDelta();
            DEBUG_OVERLAY.handleScroll(scrollDelta);
            event.setCanceled(true);
        }
    }

    //更新目标实体
    public static void updateTargetedEntity() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        HitResult hitResult = minecraft.hitResult;
        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            if (entityHit.getEntity() instanceof LivingEntity livingEntity) {
                lastTargetedEntity = livingEntity;
                DEBUG_OVERLAY.setTargetEntity(livingEntity);
                return;
            }
        }
        lastTargetedEntity = null;
        DEBUG_OVERLAY.setTargetEntity(null);
    }

    public static LivingEntity getTargetedEntity() {
        return lastTargetedEntity;
    }
}