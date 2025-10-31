package com.element_endow.data;

import com.element_endow.ElementEndow;
import com.element_endow.api.ElementSystemAPI;
import com.element_endow.data.entity_bindings.EntityElementBindingLoader;
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

@Mod.EventBusSubscriber(modid = ElementEndow.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ElementDataManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final ReactionLoader reactionLoader = new ReactionLoader();
    private static final CombinationLoader combinationLoader = new CombinationLoader();
    private static final EntityElementBindingLoader entityBindingLoader = new EntityElementBindingLoader();

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ReactionDataLoader());
        event.addListener(new CombinationDataLoader());
        event.addListener(new EntityBindingDataLoader());

        LOGGER.info("Element system data loaders registration completed");
    }

    private static class ReactionDataLoader extends SimpleJsonResourceReloadListener {
        public ReactionDataLoader() {
            super(GSON, "reactions");
            LOGGER.info("Created reaction data loader, directory: reactions");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources,
                             ResourceManager resourceManager, ProfilerFiller profiler) {
            LOGGER.info("Starting reaction data loading");
            LOGGER.info("Received resource count: {}", resources.size());

            int elementEndowReactions = 0;
            for (ResourceLocation location : resources.keySet()) {
                if (location.getNamespace().equals(ElementEndow.MODID)) {
                    LOGGER.info("Found element reaction resource: {}", location);
                    elementEndowReactions++;
                } else {
                    LOGGER.debug("Skipping non-mod reaction resource: {}", location);
                }
            }

            LOGGER.info("Found {} element reaction resources", elementEndowReactions);
            reactionLoader.loadFromResources(resources);
            LOGGER.info("Reaction data loading completed");
        }
    }

    private static class CombinationDataLoader extends SimpleJsonResourceReloadListener {
        public CombinationDataLoader() {
            super(GSON, "combinations");
            LOGGER.info("Created combination data loader, directory: combinations");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources,
                             ResourceManager resourceManager, ProfilerFiller profiler) {
            LOGGER.info("Starting combination data loading");
            LOGGER.info("Received resource count: {}", resources.size());

            try {
                //直接获取组合系统并加载数据
                var elementSystem = ElementSystemAPI.getElementSystem();
                var combinationSystem = elementSystem.getCombinationSystem();

                //组合系统接收到数据包数据
                if (combinationSystem instanceof com.element_endow.core.ElementCombinationSystem) {
                    ((com.element_endow.core.ElementCombinationSystem) combinationSystem).loadFromResources(resources);
                    LOGGER.info("Successfully loaded combinations into combination system");
                } else {
                    //备用
                    CombinationLoader combinationLoader = combinationSystem.getCombinationLoader();
                    combinationLoader.loadFromResources(resources);
                    LOGGER.info("Loaded combinations using backup method");
                }

                //记录加载的组合数
                int loadedCount = combinationSystem.getCombinationLoader().getCombinations().size();
                LOGGER.info("Total combinations now available: {}", loadedCount);

            } catch (Exception e) {
                LOGGER.error("Failed to load combinations into combination system", e);

                //或直接使用CombinationLoader
                try {
                    CombinationLoader combinationLoader = new CombinationLoader();
                    combinationLoader.loadFromResources(resources);
                    LOGGER.info("Loaded combinations using direct method");
                } catch (Exception ex) {
                    LOGGER.error("Complete failure in loading combinations", ex);
                }
            }

            LOGGER.info("Combination data loading completed");
        }
    }

    private static class EntityBindingDataLoader extends SimpleJsonResourceReloadListener {
        public EntityBindingDataLoader() {
            super(GSON, "entity_bindings");
            LOGGER.info("Created entity binding data loader, directory: entity_bindings");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources,
                             ResourceManager resourceManager, ProfilerFiller profiler) {
            LOGGER.info("Starting entity binding data loading");
            LOGGER.info("Received resource count: {}", resources.size());

            int elementEndowBindings = 0;
            for (ResourceLocation location : resources.keySet()) {
                if (location.getNamespace().equals(ElementEndow.MODID)) {
                    LOGGER.info("Found entity binding resource: {}", location);
                    elementEndowBindings++;
                } else {
                    LOGGER.debug("Skipping non-mod entity binding resource: {}", location);
                }
            }

            LOGGER.info("Found {} entity binding resources", elementEndowBindings);
            entityBindingLoader.loadFromResources(resources);
            LOGGER.info("Entity binding data loading completed");
        }
    }

    public static ReactionLoader getReactionLoader() {
        return reactionLoader;
    }

    public static CombinationLoader getCombinationLoader() {
        return combinationLoader;
    }

    public static EntityElementBindingLoader getEntityBindingLoader() {
        return entityBindingLoader;
    }
}