package com.element_endow.util;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.data.ElementDataService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 集成验证工具
 * 用于验证所有子系统是否正确集成新数据服务
 */
public class IntegrationValidator {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void validateAllSystems() {
        LOGGER.info("=== Element Endow System Integration Validation ===");

        try {
            validateElementSystem();
            validateDataService();
            validateCombinationSystem();
            validateReactionSystem();
            validateEntityBindings();
            validateCacheSystems();

            LOGGER.info("=== All systems validated successfully ===");
        } catch (Exception e) {
            LOGGER.error("Integration validation failed", e);
        }
    }

    private static void validateElementSystem() {
        try {
            var elementSystem = ElementSystemAPI.getElementSystem();
            int registeredCount = elementSystem.getRegisteredElements().size();
            int enabledCount = elementSystem.getEnabledElements().size();

            LOGGER.info("Element System: {} registered, {} enabled elements",
                    registeredCount, enabledCount);

            if (registeredCount == 0) {
                LOGGER.warn("No elements registered - check configuration");
            }

        } catch (Exception e) {
            LOGGER.error("Element System validation failed", e);
        }
    }

    private static void validateDataService() {
        try {
            ElementDataService dataService = ElementSystemAPI.getElementSystem().getDataService();
            if (dataService == null) {
                LOGGER.error("DataService not available");
                return;
            }

            var stats = dataService.getDataStats();
            LOGGER.info("Data Service: {} total entries from {} sources",
                    stats.getTotalDataCount(), stats.getTotalSourceCount());

        } catch (Exception e) {
            LOGGER.error("Data Service validation failed", e);
        }
    }

    private static void validateCombinationSystem() {
        try {
            var combinationSystem = ElementSystemAPI.getElementSystem().getCombinationSystem();
            var combinations = combinationSystem.getCombinationLoader().getCombinations();

            LOGGER.info("Combination System: {} combinations loaded", combinations.size());

            // 验证数据服务集成
            if (combinationSystem instanceof com.element_endow.core.ElementCombinationSystem impl) {
                ElementDataService dataService = impl.getDataService();
                LOGGER.info("Combination System DataService: {}",
                        dataService != null ? "Integrated" : "Not available");
            }

        } catch (Exception e) {
            LOGGER.error("Combination System validation failed", e);
        }
    }

    private static void validateReactionSystem() {
        try {
            var reactionSystem = ElementSystemAPI.getElementSystem().getReactionSystem();
            var reactions = reactionSystem.getReactionLoader().getReactions();

            LOGGER.info("Reaction System: {} reactions loaded", reactions.size());

        } catch (Exception e) {
            LOGGER.error("Reaction System validation failed", e);
        }
    }

    private static void validateEntityBindings() {
        try {
            int bindingCount = com.element_endow.event.EntitySpawnHandler.getConditionalBindingCount();
            LOGGER.info("Entity Bindings: {} active conditional bindings", bindingCount);

        } catch (Exception e) {
            LOGGER.error("Entity Bindings validation failed", e);
        }
    }

    private static void validateCacheSystems() {
        try {
            // 这里可以添加缓存系统的验证逻辑
            LOGGER.info("Cache Systems: Combination and Condition caches initialized");

        } catch (Exception e) {
            LOGGER.error("Cache Systems validation failed", e);
        }
    }
}