package com.element_endow.core;

import com.element_endow.core.config.ElementConfigManager;
import com.element_endow.data.source.DataType;
import com.element_endow.data.UnifiedDataManager;
import com.element_endow.data.ElementDataService;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 元素注册表
 * 负责在Forge注册事件之前注册所有元素属性
 * 与统一数据管理系统集成
 */
public class ElementRegistry {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, "element_endow");

    private final Map<String, RegistryObject<Attribute>> registeredAttributes;
    private final Map<String, AttributeData> attributeData;
    private final UnifiedDataManager dataManager;
    private final ElementDataService dataService;
    private final ElementConfigManager configManager;

    public ElementRegistry(UnifiedDataManager dataManager, ElementDataService dataService, ElementConfigManager configManager) {
        this.registeredAttributes = new ConcurrentHashMap<>();
        this.attributeData = new ConcurrentHashMap<>();
        this.dataManager = dataManager;
        this.dataService = dataService;
        this.configManager = configManager;

        //立即注册配置文件中定义的元素
        registerElementsFromConfig();
    }

    /**
     * 从配置管理器注册元素（早加载阶段）
     */
    private void registerElementsFromConfig() {
        LOGGER.info("Starting early registration of elements from config manager...");

        //从配置管理器获取元素定义
        for (ElementConfigManager.ElementDefinition definition : configManager.getEarlyLoadElementDefinitions()) {
            if (registerElementInternal(definition.elementId, definition.displayName,
                    definition.defaultValue, definition.minValue, definition.maxValue)) {
                LOGGER.debug("Registered element from config: {}", definition.elementId);
            }
        }

        LOGGER.info("Early registration completed: {} elements registered", registeredAttributes.size());
    }

    /**
     * 内部注册方法
     */
    private boolean registerElementInternal(String elementId, String displayName,
                                            double defaultValue, double minValue, double maxValue) {
        if (registeredAttributes.containsKey(elementId)) {
            LOGGER.warn("Element already registered: {}", elementId);
            return false;
        }

        try {
            String attributeName = extractAttributeName(elementId);
            RegistryObject<Attribute> attribute = ATTRIBUTES.register(
                    attributeName,
                    () -> new RangedAttribute(
                            "attribute.element_endow." + attributeName,
                            defaultValue, minValue, maxValue
                    ).setSyncable(true)
            );

            registeredAttributes.put(elementId, attribute);
            attributeData.put(elementId, new AttributeData(displayName, defaultValue, minValue, maxValue));

            LOGGER.debug("Registered element attribute: {}", elementId);
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to register element: {}", elementId, e);
            return false;
        }
    }

    /**
     * 动态注册元素（运行时）
     */
    public boolean registerElement(String elementId, String displayName,
                                   double defaultValue, double minValue, double maxValue) {
        boolean success = registerElementInternal(elementId, displayName, defaultValue, minValue, maxValue);
        if (success) {
            //通过数据服务注册元素定义
            ElementConfigManager.ElementDefinition definition =
                    new ElementConfigManager.ElementDefinition(elementId, displayName, defaultValue, minValue, maxValue);
            //需要将ElementDefinition添加到配置管理器
            configManager.putElementDefinition(definition);
        }
        return success;
    }

    /**
     * 从数据服务重新加载元素
     */
    public void reloadFromDataService() {
        LOGGER.info("Reloading elements from data service...");

        //获取当前所有已注册的元素
        Set<String> currentElements = new HashSet<>(registeredAttributes.keySet());

        //从数据服务获取最新定义
        Map<String, ElementConfigManager.ElementDefinition> latestDefs =
                dataManager.getDataMap(DataType.ELEMENTS);

        //注册新元素
        int addedCount = 0;
        for (ElementConfigManager.ElementDefinition definition : latestDefs.values()) {
            if (!currentElements.contains(definition.elementId)) {
                if (registerElementInternal(definition.elementId, definition.displayName,
                        definition.defaultValue, definition.minValue, definition.maxValue)) {
                    addedCount++;
                }
            }
        }

        LOGGER.info("Element reload completed: {} new elements added", addedCount);
    }

    public boolean isElementRegistered(String elementId) {
        return registeredAttributes.containsKey(elementId);
    }

    public Optional<Attribute> getElementAttribute(String elementId) {
        RegistryObject<Attribute> registryObject = registeredAttributes.get(elementId);
        return registryObject != null ? Optional.of(registryObject.get()) : Optional.empty();
    }

    public Collection<String> getRegisteredElementIds() {
        return Collections.unmodifiableCollection(registeredAttributes.keySet());
    }

    public AttributeData getAttributeData(String elementId) {
        return attributeData.get(elementId);
    }

    public int getRegisteredCount() {
        return registeredAttributes.size();
    }

    private String extractAttributeName(String elementId) {
        String[] parts = elementId.split(":");
        return parts.length == 2 ? parts[1].toLowerCase() : elementId.replace(':', '_').toLowerCase();
    }

    public static class AttributeData {
        public final String displayName;
        public final double defaultValue;
        public final double minValue;
        public final double maxValue;

        public AttributeData(String displayName, double defaultValue, double minValue, double maxValue) {
            this.displayName = displayName;
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }
    }
}