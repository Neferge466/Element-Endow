package com.element_endow.data.source.impl;

import com.element_endow.data.source.IElementDataSource;
import com.element_endow.data.source.DataType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 数据包数据源
 * 从Minecraft数据包加载JSON数据
 */
public class DataPackSource<T> implements IElementDataSource<T> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private final DataType dataType;
    private final Class<T> dataClass;
    private final Function<JsonElement, T> deserializer;
    private final ResourceManager resourceManager;

    private Map<ResourceLocation, JsonElement> resources = new HashMap<>();

    public DataPackSource(DataType dataType, Class<T> dataClass,
                          ResourceManager resourceManager) {
        this(dataType, dataClass, json -> GSON.fromJson(json, dataClass), resourceManager);
    }

    public DataPackSource(DataType dataType, Class<T> dataClass,
                          Function<JsonElement, T> deserializer,
                          ResourceManager resourceManager) {
        this.dataType = dataType;
        this.dataClass = dataClass;
        this.deserializer = deserializer;
        this.resourceManager = resourceManager;
    }

    @Override
    public String getSourceType() {
        return "datapack";
    }

    @Override
    public int getPriority() {
        return 500; // 中等优先级，可被代码源覆盖
    }

    @Override
    public Map<String, T> loadData() {
        Map<String, T> result = new HashMap<>();

        if (resources.isEmpty()) {
            LOGGER.warn("No resources available for data type: {}", dataType);
            return result;
        }

        int successCount = 0;
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();

            // 只处理当前mod的数据
            if (!location.getNamespace().equals("element_endow")) {
                continue;
            }

            try {
                T data = deserializer.apply(entry.getValue());
                if (validateData(data)) {
                    String id = extractId(location, data);
                    result.put(id, data);
                    successCount++;
                    LOGGER.debug("Loaded {}: {}", dataType, id);
                } else {
                    LOGGER.warn("Invalid data for {}: {}", dataType, location);
                    errorCount++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load {} from {}: {}", dataType, location, e.getMessage());
                errorCount++;
            }
        }

        LOGGER.info("Loaded {} {} entries ({} errors)", successCount, dataType, errorCount);
        return result;
    }

    @Override
    public boolean supportsHotReload() {
        return true;
    }

    @Override
    public void onReload() {
        LOGGER.debug("Data pack source reloaded for type: {}", dataType);
        // 资源会在外部更新
    }

    /**
     * 更新资源数据（由资源监听器调用）
     */
    public void updateResources(Map<ResourceLocation, JsonElement> newResources) {
        this.resources = new HashMap<>(newResources);
        LOGGER.debug("Updated resources for {}: {} entries", dataType, resources.size());
    }

    /**
     * 验证数据有效性
     */
    protected boolean validateData(T data) {
        // 子类可以重写此方法进行具体验证
        return data != null;
    }

    /**
     * 从资源位置和数据中提取ID
     */
    protected String extractId(ResourceLocation location, T data) {
        // 默认使用文件名作为ID
        return location.getPath().substring(location.getPath().lastIndexOf('/') + 1)
                .replace(".json", "");
    }

    /**
     * 获取资源数量
     */
    public int getResourceCount() {
        return resources.size();
    }

    /**
     * 获取数据类
     */
    public Class<T> getDataClass() {
        return dataClass;
    }
}