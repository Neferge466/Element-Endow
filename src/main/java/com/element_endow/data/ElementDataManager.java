package com.element_endow.data;

import com.element_endow.ElementEndow;
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

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        LOGGER.info("Registering element system data loaders");

        event.addListener(new ReactionDataLoader());
        LOGGER.info("Reaction data loader registered");

        event.addListener(new CombinationDataLoader());
        LOGGER.info("Combination data loader registered");

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

            int elementEndowCombinations = 0;
            for (ResourceLocation location : resources.keySet()) {
                if (location.getNamespace().equals(ElementEndow.MODID)) {
                    LOGGER.info("Found element combination resource: {}", location);
                    elementEndowCombinations++;
                } else {
                    LOGGER.debug("Skipping non-mod combination resource: {}", location);
                }
            }

            LOGGER.info("Found {} element combination resources", elementEndowCombinations);
            combinationLoader.loadFromResources(resources);
            LOGGER.info("Combination data loading completed");
        }
    }

    public static ReactionLoader getReactionLoader() {
        return reactionLoader;
    }

    public static CombinationLoader getCombinationLoader() {
        return combinationLoader;
    }
}