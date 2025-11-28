package com.element_endow.data.source.impl;

import com.element_endow.data.source.IElementDataSource;
import com.element_endow.data.source.DataType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代码注册数据源
 * 允许通过代码动态注册数据
 */
public class CodeRegistrationSource<T> implements IElementDataSource<T> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final DataType dataType;
    private final int priority;
    private final Map<String, T> registeredData = new ConcurrentHashMap<>();

    public CodeRegistrationSource(DataType dataType, int priority) {
        this.dataType = dataType;
        this.priority = priority;
        LOGGER.debug("Created code registration source for {} with priority {}", dataType, priority);
    }

    @Override
    public String getSourceType() {
        return "code";
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public Map<String, T> loadData() {
        LOGGER.debug("Loading {} entries from code registration for {}",
                registeredData.size(), dataType);
        return new ConcurrentHashMap<>(registeredData);
    }

    @Override
    public boolean supportsHotReload() {
        return true;
    }

    @Override
    public void onReload() {
        // 代码注册的数据不需要重新加载
        LOGGER.debug("Code registration source reloaded (no action needed)");
    }

    /**
     * 注册新数据
     */
    public void register(String id, T data) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Data ID cannot be null or empty");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }

        registeredData.put(id, data);
        LOGGER.debug("Registered {}: {}", dataType, id);
    }

    /**
     * 注销数据
     */
    public void unregister(String id) {
        T removed = registeredData.remove(id);
        if (removed != null) {
            LOGGER.debug("Unregistered {}: {}", dataType, id);
        }
    }

    /**
     * 检查是否包含指定ID的数据
     */
    public boolean contains(String id) {
        return registeredData.containsKey(id);
    }

    /**
     * 获取注册的数据数量
     */
    public int getRegisteredCount() {
        return registeredData.size();
    }

    /**
     * 清空所有注册数据
     */
    public void clear() {
        int count = registeredData.size();
        registeredData.clear();
        LOGGER.debug("Cleared all {} registered data entries for {}", count, dataType);
    }

    /**
     * 批量注册数据
     */
    public void registerAll(Map<String, T> dataMap) {
        registeredData.putAll(dataMap);
        LOGGER.debug("Batch registered {} entries for {}", dataMap.size(), dataType);
    }
}