package com.element_endow.core.reaction;

import com.element_endow.api.config.IReactionConfig;
import com.element_endow.api.reaction.*;
import com.element_endow.api.element.IElementSystem;
import com.element_endow.core.ElementEndowCore;
import com.element_endow.core.reaction.data.ReactionEntry;
import com.element_endow.services.IReactionHandler;
import com.element_endow.services.ReactionHandlerRegistry;
import com.element_endow.core.reaction.data.ReactionConfig;
import com.element_endow.core.reaction.data.ReactionResult;
import com.element_endow.core.reaction.data.InducedReactionResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReactionManagerImpl extends SimpleJsonResourceReloadListener implements IReactionManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private final Map<String, IReactionContext> reactions;
    private final List<IReactionHandler> customHandlers;

    public ReactionManagerImpl() {
        super(GSON, "element_reactions");
        this.reactions = new ConcurrentHashMap<>();
        ReactionHandlerRegistry handlerRegistry = new ReactionHandlerRegistry();
        this.customHandlers = new CopyOnWriteArrayList<>();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        reactions.clear();

        int loadedCount = 0;
        int reactionEntryCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            try {
                ReactionConfig config = new ReactionConfig(entry.getValue().getAsJsonObject());

                //为每个反应条目创建独立的上下文
                List<ReactionEntry> reactionEntries = config.getReactions();
                for (int i = 0; i < reactionEntries.size(); i++) {
                    ReactionEntry reactionEntry = reactionEntries.get(i);
                    ReactionContextImpl context = new ReactionContextImpl(config.getKey(), config.getType(), reactionEntry, i);
                    reactions.put(context.getReactionKey(), context);
                    reactionEntryCount++;
                }
                loadedCount++;

            } catch (Exception e) {
                LOGGER.error("Failed to load reaction config {}: {}", entry.getKey(), e.getMessage());
            }
        }

        //只在加载完成时输出一次总结性日志
        LOGGER.info("Loaded {} reaction entries from {} configuration files", reactionEntryCount, loadedCount);
    }

    @Override
    public List<IReactionResult> processInternalReaction(Player player, List<String> activeElements) {
        List<IReactionResult> results = new ArrayList<>();

        //调用自定义处理器
        for (IReactionHandler handler : customHandlers) {
            if (handler.canHandleInternal(player, activeElements)) {
                results.addAll(handler.processInternalReaction(player, activeElements));
            }
        }

        //处理内置反应
        for (IReactionContext context : reactions.values()) {
            if (context.getType() == ReactionType.INTERNAL) {
                List<String> requiredElements = context.getRequiredElements();

                if (matchesElements(activeElements, requiredElements)) {
                    double average = calculateAverageAttributeValue(player, requiredElements);
                    double multiplier = average * context.getRate();

                    ReactionResult result = new ReactionResult(
                            multiplier, context.getEffect(), context.getReactionKey(), context
                    );
                    results.add(result);
                }
            }
        }

        return results;
    }

    @Override
    public List<IReactionResult> processInducedReaction(Player attacker, LivingEntity target, String attackingElement) {
        List<IReactionResult> results = new ArrayList<>();

        //调用自定义处理
        for (IReactionHandler handler : customHandlers) {
            if (handler.canHandleInduced(attacker, target, attackingElement)) {
                results.addAll(handler.processInducedReaction(attacker, target, attackingElement));
            }
        }

        //处理内置诱发反应
        IElementSystem elementSystem = ElementEndowCore.getInstance().getElementSystem();

        for (IReactionContext context : reactions.values()) {
            if (context.getType() == ReactionType.INDUCED) {
                List<String> requiredElements = context.getRequiredElements();

                if (requiredElements.size() == 2) {
                    String attackerElement = requiredElements.get(0);
                    String targetElement = requiredElements.get(1);

                    if (attackerElement.equals(attackingElement) &&
                            elementSystem.hasElementAttribute(target, targetElement) &&
                            elementSystem.getElementValue(target, targetElement) > 0) {

                        double[] rates = context.getRateArray();
                        if (rates != null && rates.length == 2) {
                            InducedReactionResult result = new InducedReactionResult(
                                    rates[0], rates[1], context.getEffect(), context.getReactionKey(), context
                            );
                            results.add(result);
                        }
                    }
                }
            }
        }

        return results;
    }

    @Override
    public boolean registerReaction(String reactionKey, IReactionContext context) {
        if (reactions.containsKey(reactionKey)) {
            LOGGER.warn("Reaction already registered: {}", reactionKey);
            return false;
        }

        reactions.put(reactionKey, context);
        LOGGER.info("Registered custom reaction: {}", reactionKey);
        return true;
    }

    @Override
    public boolean unregisterReaction(String reactionKey) {
        if (!reactions.containsKey(reactionKey)) {
            LOGGER.warn("Reaction not found for unregistration: {}", reactionKey);
            return false;
        }

        reactions.remove(reactionKey);
        LOGGER.info("Unregistered reaction: {}", reactionKey);
        return true;
    }

    @Override
    public IReactionConfig getConfig() {
        return new com.element_endow.core.config.ReactionConfigImpl();
    }

    @Override
    public void registerReactionHandler(IReactionHandler handler) {
        if (handler != null && !customHandlers.contains(handler)) {
            customHandlers.add(handler);
            LOGGER.debug("Registered reaction handler: {}", handler.getClass().getSimpleName());
        }
    }

    @Override
    public void unregisterReactionHandler(IReactionHandler handler) {
        customHandlers.remove(handler);
        LOGGER.debug("Unregistered reaction handler: {}", handler.getClass().getSimpleName());
    }

    private boolean matchesElements(List<String> activeElements, List<String> requiredElements) {
        for (String required : requiredElements) {
            if (!activeElements.contains(required)) {
                return false;
            }
        }
        return true;
    }

    private double calculateAverageAttributeValue(Player player, List<String> elements) {
        IElementSystem elementSystem = ElementEndowCore.getInstance().getElementSystem();
        double sum = 0.0;
        int count = 0;

        for (String element : elements) {
            double value = elementSystem.getElementValue(player, element);
            if (value > 0) {
                sum += value;
                count++;
            }
        }

        return count > 0 ? sum / count : 0.0;
    }

    public Map<String, IReactionContext> getReactions() {
        return Collections.unmodifiableMap(reactions);
    }
}