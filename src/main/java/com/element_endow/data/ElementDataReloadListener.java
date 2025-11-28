package com.element_endow.data;

import com.element_endow.api.ElementSystemAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

@Mod.EventBusSubscriber(modid = "element_endow", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ElementDataReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ReactionDataLoader());
        event.addListener(new CombinationDataLoader());
        event.addListener(new EntityBindingDataLoader());
        LOGGER.info("Element system data loaders registered with new architecture");
    }

    private static class ReactionDataLoader extends SimpleJsonResourceReloadListener {
        public ReactionDataLoader() {
            super(GSON, "reactions");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources,
                             ResourceManager resourceManager, ProfilerFiller profiler) {
            LOGGER.info("Loading reactions through new data system");
            // 数据现在通过DataPackSource自动处理
        }
    }

    private static class CombinationDataLoader extends SimpleJsonResourceReloadListener {
        public CombinationDataLoader() {
            super(GSON, "combinations");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources,
                             ResourceManager resourceManager, ProfilerFiller profiler) {
            LOGGER.info("Loading combinations through new data system");
            // 数据现在通过DataPackSource自动处理
        }
    }

    private static class EntityBindingDataLoader extends SimpleJsonResourceReloadListener {
        public EntityBindingDataLoader() {
            super(GSON, "entity_bindings");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources,
                             ResourceManager resourceManager, ProfilerFiller profiler) {
            LOGGER.info("Loading entity bindings through new data system");
            // 数据现在通过DataPackSource自动处理
        }
    }
}