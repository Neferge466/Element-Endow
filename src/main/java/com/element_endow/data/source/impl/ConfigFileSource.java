package com.element_endow.data.source.impl;

import com.element_endow.core.config.ElementConfigManager;
import com.element_endow.data.source.IElementDataSource;
import com.element_endow.data.source.DataType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置文件数据源
 * 使用统一的配置管理器
 */
public class ConfigFileSource implements IElementDataSource<ElementConfigManager.ElementDefinition> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ElementConfigManager configManager;

    public ConfigFileSource(ElementConfigManager configManager) {
        this.configManager = configManager;
        LOGGER.info("Config file source initialized with config manager");
    }

    @Override
    public String getSourceType() {
        return "config_file";
    }

    @Override
    public int getPriority() {
        return configManager.getSystemConfig().configFilePriority;
    }

    @Override
    public Map<String, ElementConfigManager.ElementDefinition> loadData() {
        Map<String, ElementConfigManager.ElementDefinition> result = new HashMap<>();

        // 从配置管理器获取所有元素定义
        for (ElementConfigManager.ElementDefinition definition : configManager.getEarlyLoadElementDefinitions()) {
            result.put(definition.elementId, definition);
        }

        LOGGER.debug("Loaded {} element definitions from config manager", result.size());
        return result;
    }

    @Override
    public boolean supportsHotReload() {
        return true;
    }

    @Override
    public void onReload() {
        LOGGER.debug("Config file source reloaded");
        // 配置管理器会处理重载逻辑
    }
}