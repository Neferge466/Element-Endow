package com.element_endow.data.registry;

import com.element_endow.core.config.ElementConfigManager;
import com.element_endow.data.UnifiedDataManager;
import com.element_endow.data.ElementDataService;
import com.element_endow.data.source.DataType;
import com.element_endow.data.source.impl.ConfigFileSource;
import com.element_endow.data.source.impl.DataPackSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;
import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * 数据注册中心
 * 现在使用统一的配置管理器来处理时序
 */
public class DataRegistry {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ElementConfigManager configManager;
    private final UnifiedDataManager dataManager;
    private final ElementDataService dataService;

    public DataRegistry() {
        // 阶段1：立即初始化配置管理器（最早）
        this.configManager = new ElementConfigManager();
        this.configManager.loadElementDefinitionsEarly(); // 早加载元素定义

        // 阶段2：初始化数据管理器
        this.dataManager = new UnifiedDataManager();
        this.dataService = new ElementDataService(dataManager);

        // 阶段3：注册配置数据源
        registerConfigDataSources();

        LOGGER.info("Data Registry initialized with unified config management");
    }

    /**
     * 注册配置文件数据源（在模组构造时调用）
     */
    private void registerConfigDataSources() {
        // 注册元素配置数据源
        ConfigFileSource configSource = new ConfigFileSource(configManager);
        dataManager.registerDataSource(DataType.ELEMENTS, configSource);
        LOGGER.info("Registered config file data source for elements (priority: {})",
                configManager.getSystemConfig().configFilePriority);
    }

    /**
     * 加载系统配置（在系统初始化时调用）
     */
    public void loadSystemConfiguration() {
        configManager.loadSystemConfig();
        LOGGER.info("System configuration loaded");
    }

    /**
     * 注册数据包数据源（在资源加载时调用）
     */
    public void registerDataPackSources(ResourceManager resourceManager,
                                        Map<ResourceLocation, JsonElement> resources) {
        LOGGER.info("Registering data pack sources...");

        //注册反应数据源
        DataPackSource<com.element_endow.data.ReactionLoader.ElementReaction> reactionSource =
                new DataPackSource<>(
                        DataType.REACTIONS,
                        com.element_endow.data.ReactionLoader.ElementReaction.class,
                        resourceManager
                );
        reactionSource.updateResources(resources);
        dataManager.registerDataSource(DataType.REACTIONS, reactionSource);

        //注册组合数据源
        DataPackSource<com.element_endow.data.CombinationLoader.ElementCombination> combinationSource =
                new DataPackSource<>(
                        DataType.COMBINATIONS,
                        com.element_endow.data.CombinationLoader.ElementCombination.class,
                        resourceManager
                );
        combinationSource.updateResources(resources);
        dataManager.registerDataSource(DataType.COMBINATIONS, combinationSource);

        //注册实体绑定数据源
        DataPackSource<com.element_endow.data.entity_bindings.EntityElementBindingLoader.EntityElementBinding> bindingSource =
                new DataPackSource<>(
                        DataType.ENTITY_BINDINGS,
                        com.element_endow.data.entity_bindings.EntityElementBindingLoader.EntityElementBinding.class,
                        resourceManager
                );
        bindingSource.updateResources(resources);
        dataManager.registerDataSource(DataType.ENTITY_BINDINGS, bindingSource);

        LOGGER.info("Registered data pack sources with priority: {}",
                configManager.getSystemConfig().dataPackPriority);
    }

    /**
     * 完成数据系统初始化（在所有数据源注册后调用）
     */
    public void finalizeDataSystem() {
        // 触发初始数据加载
        dataManager.reloadAllData();

        // 完成配置加载
        configManager.finalizeConfigLoading();

        // 打印数据统计
        UnifiedDataManager.DataStats stats = dataManager.getDataStats();
        LOGGER.info("Data system finalized. Total data entries: {}", stats.getTotalDataCount());

        for (DataType type : DataType.values()) {
            int count = stats.dataCounts.get(type);
            int sources = stats.sourceCounts.get(type);
            LOGGER.info("  {}: {} entries from {} sources", type.getDisplayName(), count, sources);
        }
    }

    // ========== 访问器方法 ==========

    public ElementConfigManager getConfigManager() {
        return configManager;
    }

    public UnifiedDataManager getDataManager() {
        return dataManager;
    }

    public ElementDataService getDataService() {
        return dataService;
    }

    /**
     * 获取早加载的元素ID列表（用于元素注册）
     */
    public java.util.List<String> getEarlyLoadElementIds() {
        return configManager.getEarlyLoadElementDefinitions().stream()
                .map(def -> def.elementId)
                .toList();
    }
}