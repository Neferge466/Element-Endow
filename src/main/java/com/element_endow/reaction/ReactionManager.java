package com.element_endow.reaction;

import com.element_endow.ElementEndow;
import com.element_endow.config.ElementAttributeHelper;
import com.element_endow.reaction.data.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class ReactionManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private final Map<String, ReactionConfig> reactions = new HashMap<>();

    public ReactionManager() {
        super(GSON, "element_reactions");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        reactions.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                ReactionConfig config = new ReactionConfig(entry.getValue().getAsJsonObject());
                reactions.put(config.getKey(), config);
                ElementEndow.LOGGER.info("Loaded reaction config: {}", config.getKey());
            } catch (Exception e) {
                ElementEndow.LOGGER.error("Failed to load reaction config {}: {}", entry.getKey(), e.getMessage());
            }
        }

        ElementEndow.LOGGER.info("Loaded {} reaction configurations", reactions.size());
    }

    public Optional<ReactionResult> processInternalReaction(Player player, List<String> activeElements) {
        for (ReactionConfig config : reactions.values()) {
            if (config.getType() == ReactionType.INTERNAL) {
                for (ReactionEntry entry : config.getReactions()) {
                    if (matchesElements(activeElements, entry.getMatchElements())) {
                        double average = calculateAverageAttributeValue(player, entry.getMatchElements());
                        double multiplier = average * entry.getRate();
                        return Optional.of(new ReactionResult(multiplier, entry.getEffect(), config.getKey()));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public Optional<InducedReactionResult> processInducedReaction(Player attacker, LivingEntity target, String attackingElement) {
        for (ReactionConfig config : reactions.values()) {
            if (config.getType() == ReactionType.INDUCED) {
                for (ReactionEntry entry : config.getReactions()) {
                    List<String> matchElements = entry.getMatchElements();
                    if (matchElements.size() == 2 && matchElements.get(0).equals(attackingElement)) {
                        String targetElement = matchElements.get(1);
                        if (ElementAttributeHelper.hasElementAttribute(target, targetElement) &&
                                ElementAttributeHelper.getElementValue(target, targetElement) > 0) {
                            double[] rates = entry.getRateArray();
                            if (rates != null && rates.length == 2) {
                                return Optional.of(new InducedReactionResult(
                                        rates[0], rates[1], entry.getEffect(), config.getKey()
                                ));
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private boolean matchesElements(List<String> activeElements, List<String> requiredElements) {
        return activeElements.containsAll(requiredElements);
    }

    private double calculateAverageAttributeValue(Player player, List<String> elements) {
        double sum = 0.0;
        int count = 0;

        for (String element : elements) {
            double value = ElementAttributeHelper.getElementValue(player, element);
            if (value > 0) {
                sum += value;
                count++;
            }
        }

        return count > 0 ? sum / count : 0.0;
    }

    public Map<String, ReactionConfig> getReactions() {
        return Collections.unmodifiableMap(reactions);
    }
}