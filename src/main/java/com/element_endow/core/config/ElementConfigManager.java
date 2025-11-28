package com.element_endow.core.config;

import com.element_endow.ElementEndow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一配置管理器
 * 负责所有配置文件的加载、保存和时序管理
 */
public class ElementConfigManager {
    private static final Logger LOGGER = LogManager.getLogger();

    // 配置目录
    private final File configDir;

    // 配置文件
    private final File elementDefinitionsFile;
    private final File systemConfigFile;

    // 配置数据
    private final Map<String, ElementDefinition> elementDefinitions;
    private final SystemConfig systemConfig;

    // 配置状态
    private boolean elementsLoaded = false;
    private boolean systemConfigLoaded = false;

    public ElementConfigManager() {
        this.configDir = new File("config/element_endow");
        this.elementDefinitionsFile = new File(configDir, "element_definitions.properties");
        this.systemConfigFile = new File(configDir, "system_config.properties");

        this.elementDefinitions = new ConcurrentHashMap<>();
        this.systemConfig = new SystemConfig();

        // 立即创建配置目录和默认配置
        ensureConfigFiles();
    }

    /**
     * 阶段1：早加载元素定义（在Forge注册之前调用）
     */
    public void loadElementDefinitionsEarly() {
        if (elementsLoaded) {
            return;
        }

        LOGGER.info("Loading element definitions (early phase)...");
        elementDefinitions.clear();

        try {
            if (!elementDefinitionsFile.exists()) {
                createDefaultElementDefinitions();
                LOGGER.info("Created default element definitions file");
            }

            Properties props = new Properties();
            try (FileReader reader = new FileReader(elementDefinitionsFile)) {
                props.load(reader);
            }

            int loadedCount = 0;
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("element.")) {
                    String elementId = key.substring(8); // 移除"element."前缀
                    String value = props.getProperty(key);

                    try {
                        ElementDefinition definition = parseElementDefinition(elementId, value);
                        if (definition != null) {
                            elementDefinitions.put(elementId, definition);
                            loadedCount++;
                            LOGGER.debug("Loaded element definition: {}", elementId);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse element definition for {}: {}", elementId, e.getMessage());
                    }
                }
            }

            elementsLoaded = true;
            LOGGER.info("Early loading completed: {} element definitions loaded", loadedCount);

        } catch (Exception e) {
            LOGGER.error("Failed to load element definitions in early phase", e);
        }
    }

    /**
     * 阶段2：加载系统配置（在系统初始化时调用）
     */
    public void loadSystemConfig() {
        if (systemConfigLoaded) {
            return;
        }

        LOGGER.info("Loading system configuration...");

        try {
            if (!systemConfigFile.exists()) {
                createDefaultSystemConfig();
                LOGGER.info("Created default system configuration file");
            }

            Properties props = new Properties();
            try (FileReader reader = new FileReader(systemConfigFile)) {
                props.load(reader);
            }

            // 加载调试配置
            systemConfig.debugOverlayEnabled = Boolean.parseBoolean(
                    props.getProperty("debug.overlay.enabled", "true")
            );
            systemConfig.debugLogLevel = props.getProperty("debug.log.level", "INFO");
            systemConfig.enableExperimentalFeatures = Boolean.parseBoolean(
                    props.getProperty("experimental.features.enabled", "false")
            );

            // 加载性能配置
            systemConfig.cacheDuration = Integer.parseInt(
                    props.getProperty("cache.duration.ticks", "40")
            );
            systemConfig.maxCombinationChecksPerTick = Integer.parseInt(
                    props.getProperty("performance.max_combination_checks", "10")
            );
            systemConfig.enableAsyncLoading = Boolean.parseBoolean(
                    props.getProperty("performance.async_loading", "true")
            );

            // 加载数据源配置
            systemConfig.dataPackPriority = Integer.parseInt(
                    props.getProperty("datasource.datapack.priority", "500")
            );
            systemConfig.codeRegistrationPriority = Integer.parseInt(
                    props.getProperty("datasource.code.priority", "1000")
            );
            systemConfig.configFilePriority = Integer.parseInt(
                    props.getProperty("datasource.config.priority", "2000")
            );

            systemConfigLoaded = true;
            LOGGER.info("System configuration loaded successfully");

        } catch (Exception e) {
            LOGGER.error("Failed to load system configuration", e);
        }
    }

    /**
     * 阶段3：完成配置加载（在所有系统组件初始化后调用）
     */
    public void finalizeConfigLoading() {
        LOGGER.info("Finalizing configuration loading...");

        // 验证配置完整性
        validateConfigurations();

        // 记录最终配置状态
        LOGGER.info("Configuration loading finalized. Elements: {}, System config: {}",
                elementDefinitions.size(), systemConfigLoaded ? "loaded" : "failed");
    }

    /**
     * 获取早加载的元素定义（用于元素注册）
     */
    public Collection<ElementDefinition> getEarlyLoadElementDefinitions() {
        return Collections.unmodifiableCollection(elementDefinitions.values());
    }

    /**
     * 获取元素定义
     */
    public Optional<ElementDefinition> getElementDefinition(String elementId) {
        return Optional.ofNullable(elementDefinitions.get(elementId));
    }

    /**
     * 添加或更新元素定义
     */
    public void putElementDefinition(ElementDefinition definition) {
        elementDefinitions.put(definition.elementId, definition);
        saveElementDefinitions();
    }

    /**
     * 移除元素定义
     */
    public void removeElementDefinition(String elementId) {
        if (elementDefinitions.remove(elementId) != null) {
            saveElementDefinitions();
        }
    }

    /**
     * 获取系统配置
     */
    public SystemConfig getSystemConfig() {
        return systemConfig;
    }

    /**
     * 保存元素定义到文件
     */
    public void saveElementDefinitions() {
        try {
            Properties props = new Properties();

            for (ElementDefinition definition : elementDefinitions.values()) {
                String value = String.format("%s,%.2f,%.2f,%.2f",
                        definition.displayName, definition.defaultValue,
                        definition.minValue, definition.maxValue);
                props.setProperty("element." + definition.elementId, value);
            }

            configDir.mkdirs();
            try (FileWriter writer = new FileWriter(elementDefinitionsFile)) {
                props.store(writer, "Element Endow Element Definitions\nFormat: element.<id>=<displayName>,<defaultValue>,<minValue>,<maxValue>");
            }

            LOGGER.info("Saved {} element definitions to file", elementDefinitions.size());

        } catch (Exception e) {
            LOGGER.error("Failed to save element definitions", e);
        }
    }

    /**
     * 保存系统配置到文件
     */
    public void saveSystemConfig() {
        try {
            Properties props = new Properties();

            // 调试配置
            props.setProperty("debug.overlay.enabled", String.valueOf(systemConfig.debugOverlayEnabled));
            props.setProperty("debug.log.level", systemConfig.debugLogLevel);
            props.setProperty("experimental.features.enabled", String.valueOf(systemConfig.enableExperimentalFeatures));

            // 性能配置
            props.setProperty("cache.duration.ticks", String.valueOf(systemConfig.cacheDuration));
            props.setProperty("performance.max_combination_checks", String.valueOf(systemConfig.maxCombinationChecksPerTick));
            props.setProperty("performance.async_loading", String.valueOf(systemConfig.enableAsyncLoading));

            // 数据源配置
            props.setProperty("datasource.datapack.priority", String.valueOf(systemConfig.dataPackPriority));
            props.setProperty("datasource.code.priority", String.valueOf(systemConfig.codeRegistrationPriority));
            props.setProperty("datasource.config.priority", String.valueOf(systemConfig.configFilePriority));

            configDir.mkdirs();
            try (FileWriter writer = new FileWriter(systemConfigFile)) {
                props.store(writer, "Element Endow System Configuration");
            }

            LOGGER.info("System configuration saved to file");

        } catch (Exception e) {
            LOGGER.error("Failed to save system configuration", e);
        }
    }

    // ========== 私有方法 ==========

    private void ensureConfigFiles() {
        configDir.mkdirs();
        LOGGER.info("Configuration directory: {}", configDir.getAbsolutePath());
    }

    private void createDefaultElementDefinitions() {
        try {
            Properties props = new Properties();

            // 默认元素定义
            String[] defaultElements = {

            };

            for (String elementDef : defaultElements) {
                String[] parts = elementDef.split(":", 2);
                if (parts.length == 2) {
                    props.setProperty("element." + parts[0], parts[1]);

                    // 同时加载到内存
                    ElementDefinition definition = parseElementDefinition(parts[0], parts[1]);
                    if (definition != null) {
                        elementDefinitions.put(parts[0], definition);
                    }
                }
            }

            configDir.mkdirs();
            try (FileWriter writer = new FileWriter(elementDefinitionsFile)) {
                props.store(writer, "Element Endow Element Definitions\nFormat: element.<id>=<displayName>,<defaultValue>,<minValue>,<maxValue>");
            }

            LOGGER.info("Created default element definitions with {} elements", defaultElements.length);

        } catch (Exception e) {
            LOGGER.error("Failed to create default element definitions", e);
        }
    }

    private void createDefaultSystemConfig() {
        try {
            Properties props = new Properties();

            //默认系统配置
            props.setProperty("debug.overlay.enabled", "true");
            props.setProperty("debug.log.level", "INFO");
            props.setProperty("experimental.features.enabled", "false");

            props.setProperty("cache.duration.ticks", "40");
            props.setProperty("performance.max_combination_checks", "10");
            props.setProperty("performance.async_loading", "true");

            props.setProperty("datasource.datapack.priority", "500");
            props.setProperty("datasource.code.priority", "1000");
            props.setProperty("datasource.config.priority", "2000");

            configDir.mkdirs();
            try (FileWriter writer = new FileWriter(systemConfigFile)) {
                props.store(writer, "Element Endow System Configuration");
            }

            //加载默认配置到内存
            systemConfig.debugOverlayEnabled = true;
            systemConfig.debugLogLevel = "INFO";
            systemConfig.enableExperimentalFeatures = false;
            systemConfig.cacheDuration = 40;
            systemConfig.maxCombinationChecksPerTick = 10;
            systemConfig.enableAsyncLoading = true;
            systemConfig.dataPackPriority = 500;
            systemConfig.codeRegistrationPriority = 1000;
            systemConfig.configFilePriority = 2000;

            systemConfigLoaded = true;
            LOGGER.info("Created default system configuration");

        } catch (Exception e) {
            LOGGER.error("Failed to create default system configuration", e);
        }
    }

    private ElementDefinition parseElementDefinition(String elementId, String value) {
        String[] parts = value.split(",");
        if (parts.length < 4) {
            LOGGER.warn("Invalid element definition format for {}: {}", elementId, value);
            return null;
        }

        try {
            String displayName = parts[0].trim();
            double defaultValue = Double.parseDouble(parts[1].trim());
            double minValue = Double.parseDouble(parts[2].trim());
            double maxValue = Double.parseDouble(parts[3].trim());

            return new ElementDefinition(elementId, displayName, defaultValue, minValue, maxValue);
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid number format in element definition for {}: {}", elementId, value);
            return null;
        }
    }

    private void validateConfigurations() {
        //验证元素定义
        for (ElementDefinition definition : elementDefinitions.values()) {
            if (definition.minValue > definition.maxValue) {
                LOGGER.warn("Invalid element definition {}: minValue > maxValue", definition.elementId);
            }
            if (definition.defaultValue < definition.minValue || definition.defaultValue > definition.maxValue) {
                LOGGER.warn("Element {} defaultValue out of range", definition.elementId);
            }
        }

        //验证系统配置
        if (systemConfig.cacheDuration <= 0) {
            LOGGER.warn("Invalid cache duration: {}", systemConfig.cacheDuration);
            systemConfig.cacheDuration = 40;
        }

        LOGGER.info("Configuration validation completed");
    }

    // ========== 数据类 ==========

    /**
     * 元素定义数据类
     */
    public static class ElementDefinition {
        public final String elementId;
        public final String displayName;
        public final double defaultValue;
        public final double minValue;
        public final double maxValue;

        public ElementDefinition(String elementId, String displayName,
                                 double defaultValue, double minValue, double maxValue) {
            this.elementId = elementId;
            this.displayName = displayName;
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        @Override
        public String toString() {
            return String.format("ElementDefinition{id=%s, name=%s, default=%.1f, range=[%.1f, %.1f]}",
                    elementId, displayName, defaultValue, minValue, maxValue);
        }
    }

    /**
     * 系统配置数据类
     */
    public static class SystemConfig {
        //调试
        public boolean debugOverlayEnabled = true;
        public String debugLogLevel = "INFO";
        public boolean enableExperimentalFeatures = false;

        //性能
        public int cacheDuration = 40;
        public int maxCombinationChecksPerTick = 10;
        public boolean enableAsyncLoading = true;

        //数据源
        public int dataPackPriority = 500;
        public int codeRegistrationPriority = 1000;
        public int configFilePriority = 2000;

        @Override
        public String toString() {
            return String.format("SystemConfig{debugOverlay=%s, cacheDuration=%d, dataPackPriority=%d}",
                    debugOverlayEnabled, cacheDuration, dataPackPriority);
        }
    }
}