package com.element_endow.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class CombinationLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private final Map<String, ElementCombination> combinations = new HashMap<>();

    public void loadCombinations() {
        combinations.clear();
    }

    public void loadFromResources(Map<ResourceLocation, JsonElement> resources) {
        combinations.clear();

        int loadedCount = 0;
        int errorCount = 0;

        LOGGER.info("Processing {} combination resources", resources.size());

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();

            if (!location.getNamespace().equals("element_endow")) {
                continue;
            }

            LOGGER.info("Loading combination file: {}", location);
            try {
                ElementCombination combination = GSON.fromJson(entry.getValue(), ElementCombination.class);

                if (validateCombination(combination)) {
                    combinations.put(combination.id, combination);
                    loadedCount++;
                    LOGGER.info("Successfully loaded combination: {}", combination.id);
                } else {
                    LOGGER.warn("Invalid combination data: {}", location);
                    errorCount++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load combination {}: {}", location, e.getMessage());
                errorCount++;
            }
        }

        LOGGER.info("Loaded {} element combinations from resources ({} errors)", loadedCount, errorCount);
    }

    private boolean validateCombination(ElementCombination combination) {
        if (combination.id == null || combination.id.isEmpty()) {
            LOGGER.error("Combination missing ID");
            return false;
        }

        if (combination.requiredElements == null) {
            combination.requiredElements = new HashSet<>();
        }
        if (combination.forbiddenElements == null) {
            combination.forbiddenElements = new HashSet<>();
        }
        if (combination.minValues == null) {
            combination.minValues = new HashMap<>();
        }
        if (combination.effects == null) {
            combination.effects = new ArrayList<>();
        }
        if (combination.mountTable == null) {
            combination.mountTable = new HashMap<>();
        }
        if (combination.conditions == null) {
            combination.conditions = new HashMap<>();
        }

        return true;
    }

    public Map<String, ElementCombination> getCombinations() {
        return new HashMap<>(combinations);
    }

    public static class ElementCombination {
        public String id;
        public Set<String> requiredElements = new HashSet<>();
        public Set<String> forbiddenElements = new HashSet<>();
        public Map<String, Double> minValues = new HashMap<>();
        public List<CombinationEffect> effects = new ArrayList<>();
        public Map<String, MountData> mountTable = new HashMap<>();
        public Map<String, Object> conditions = new HashMap<>();
    }

    public static class CombinationEffect {
        public String attribute;
        public double value;
        public String operation;
    }

    public static class MountData {
        public int duration;
        public double amount;
    }
}