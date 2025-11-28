package com.element_endow.data;

import com.element_endow.data.source.IElementDataSource;
import com.element_endow.data.source.DataType;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 统一数据管理器
 * 负责协调多个数据源，提供统一的数据访问接口
 */
public class UnifiedDataManager {
    private static final Logger LOGGER = LogManager.getLogger();

    //数据源注册表：数据类型 -> 数据源列表（按优先级排序）
    private final ListMultimap<DataType, DataSourceWrapper> dataSources = ArrayListMultimap.create();

    //数据缓存：数据类型 -> 合并后的数据映射
    private final Map<DataType, Map<String, Object>> dataCache = new ConcurrentHashMap<>();

    //数据变更监听器
    private final List<DataChangeListener> listeners = new ArrayList<>();

    //冲突解决策略
    private final ConflictResolutionStrategy conflictStrategy = ConflictResolutionStrategy.PRIORITY_HIGHEST;

    public UnifiedDataManager() {
        LOGGER.info("Initializing Unified Data Manager");
        initializeDataCache();
    }

    /**
     * 注册数据源
     */
    public <T> void registerDataSource(DataType type, IElementDataSource<T> source) {
        synchronized (dataSources) {
            DataSourceWrapper wrapper = new DataSourceWrapper(source);
            List<DataSourceWrapper> sources = dataSources.get(type);

            // 按优先级插入排序
            int index = 0;
            for (; index < sources.size(); index++) {
                if (sources.get(index).getPriority() < wrapper.getPriority()) {
                    break;
                }
            }
            sources.add(index, wrapper);

            LOGGER.debug("Registered data source for {}: {}", type, source.getDescription());
            invalidateCache(type);
        }
    }

    /**
     * 注销数据源
     */
    public boolean unregisterDataSource(DataType type, String sourceType) {
        synchronized (dataSources) {
            boolean removed = dataSources.get(type).removeIf(
                    wrapper -> wrapper.getSourceType().equals(sourceType)
            );

            if (removed) {
                LOGGER.debug("Unregistered data source for {}: {}", type, sourceType);
                invalidateCache(type);
            }
            return removed;
        }
    }

    /**
     * 获取数据（泛型版本）
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getData(DataType type, String id) {
        Map<String, Object> typeData = getDataMap(type);
        return Optional.ofNullable((T) typeData.get(id));
    }

    /**
     * 获取所有数据
     */
    @SuppressWarnings("unchecked")
    public <T> Collection<T> getAllData(DataType type) {
        Map<String, Object> typeData = getDataMap(type);
        return (Collection<T>) typeData.values();
    }

    /**
     * 获取数据映射
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getDataMap(DataType type) {
        return (Map<String, T>) dataCache.computeIfAbsent(type, this::loadAndMergeData);
    }

    /**
     * 触发数据重载
     */
    public void reloadData(DataType type) {
        LOGGER.info("Reloading data for type: {}", type);
        invalidateCache(type);
        notifyDataChanged(type);
    }

    /**
     * 重载所有数据
     */
    public void reloadAllData() {
        LOGGER.info("Reloading all data");
        for (DataType type : DataType.values()) {
            invalidateCache(type);
        }
        notifyAllDataChanged();
    }

    /**
     * 添加数据变更监听器
     */
    public void addDataChangeListener(DataChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * 移除数据变更监听器
     */
    public void removeDataChangeListener(DataChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * 获取数据源统计信息
     */
    public Map<DataType, Integer> getDataSourceStats() {
        Map<DataType, Integer> stats = new EnumMap<>(DataType.class);
        for (DataType type : DataType.values()) {
            stats.put(type, dataSources.get(type).size());
        }
        return stats;
    }

    // ========== 私有方法 ==========

    private void initializeDataCache() {
        for (DataType type : DataType.values()) {
            dataCache.put(type, new ConcurrentHashMap<>());
        }
    }

    private void invalidateCache(DataType type) {
        dataCache.remove(type);
    }

    private Map<String, Object> loadAndMergeData(DataType type) {
        Map<String, Object> mergedData = new LinkedHashMap<>();
        List<DataSourceWrapper> sources = dataSources.get(type);

        if (sources.isEmpty()) {
            LOGGER.warn("No data sources registered for type: {}", type);
            return mergedData;
        }

        LOGGER.debug("Loading data for {} from {} sources", type, sources.size());

        // 按优先级从高到低合并数据
        for (DataSourceWrapper wrapper : sources) {
            if (!wrapper.isAvailable()) {
                LOGGER.debug("Skipping unavailable data source: {}", wrapper.getSourceType());
                continue;
            }

            try {
                Map<String, Object> sourceData = wrapper.loadData();
                LOGGER.debug("Loaded {} entries from {}", sourceData.size(), wrapper.getSourceType());

                // 应用冲突解决策略
                switch (conflictStrategy) {
                    case PRIORITY_HIGHEST:
                        // 高优先级覆盖低优先级
                        for (Map.Entry<String, Object> entry : sourceData.entrySet()) {
                            if (!mergedData.containsKey(entry.getKey())) {
                                mergedData.put(entry.getKey(), entry.getValue());
                            }
                        }
                        break;

                    case MERGE_STRATEGY:
                        // 合并策略（需要数据类型支持合并）
                        mergedData.putAll(sourceData);
                        break;

                    case SOURCE_WHITELIST:
                        // 只使用白名单源（当前实现为优先级最高）
                        if (wrapper.getPriority() >= getHighestPriority(sources)) {
                            mergedData.putAll(sourceData);
                        }
                        break;
                }

            } catch (Exception e) {
                LOGGER.error("Failed to load data from {} for type {}",
                        wrapper.getSourceType(), type, e);
            }
        }

        LOGGER.info("Merged {} entries for type {}", mergedData.size(), type);
        return mergedData;
    }

    private int getHighestPriority(List<DataSourceWrapper> sources) {
        return sources.stream()
                .mapToInt(DataSourceWrapper::getPriority)
                .max()
                .orElse(0);
    }

    private void notifyDataChanged(DataType type) {
        for (DataChangeListener listener : listeners) {
            try {
                listener.onDataChanged(type);
            } catch (Exception e) {
                LOGGER.error("Error in data change listener", e);
            }
        }
    }

    private void notifyAllDataChanged() {
        for (DataChangeListener listener : listeners) {
            try {
                listener.onAllDataChanged();
            } catch (Exception e) {
                LOGGER.error("Error in data change listener", e);
            }
        }
    }

    // ========== 内部类 ==========

    /**
     * 数据源包装器
     */
    private static class DataSourceWrapper {
        private final IElementDataSource<?> source;

        public DataSourceWrapper(IElementDataSource<?> source) {
            this.source = source;
        }

        public String getSourceType() {
            return source.getSourceType();
        }

        public int getPriority() {
            return source.getPriority();
        }

        public boolean isAvailable() {
            return source.isAvailable();
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> loadData() {
            return (Map<String, Object>) source.loadData();
        }
    }

    /**
     * 数据变更监听器接口
     */
    public interface DataChangeListener {
        void onDataChanged(DataType type);
        void onAllDataChanged();
    }

    /**
     * 冲突解决策略
     */
    public enum ConflictResolutionStrategy {
        PRIORITY_HIGHEST,   // 高优先级覆盖
        MERGE_STRATEGY,     // 合并策略
        SOURCE_WHITELIST    // 源白名单
    }

    /**
     * 数据统计类
     */
    public static class DataStats {
        public final Map<DataType, Integer> dataCounts;
        public final Map<DataType, Integer> sourceCounts;

        public DataStats(Map<DataType, Integer> dataCounts, Map<DataType, Integer> sourceCounts) {
            this.dataCounts = Collections.unmodifiableMap(new EnumMap<>(dataCounts));
            this.sourceCounts = Collections.unmodifiableMap(new EnumMap<>(sourceCounts));
        }

        public int getTotalDataCount() {
            return dataCounts.values().stream().mapToInt(Integer::intValue).sum();
        }

        public int getTotalSourceCount() {
            return sourceCounts.values().stream().mapToInt(Integer::intValue).sum();
        }

        @Override
        public String toString() {
            return String.format("DataStats{totalData=%d, totalSources=%d}",
                    getTotalDataCount(), getTotalSourceCount());
        }
    }

    /**
     * 获取数据统计信息
     */
    public DataStats getDataStats() {
        Map<DataType, Integer> dataCounts = new EnumMap<>(DataType.class);
        Map<DataType, Integer> sourceCounts = getDataSourceStats();

        for (DataType type : DataType.values()) {
            dataCounts.put(type, getDataMap(type).size());
        }

        return new DataStats(dataCounts, sourceCounts);
    }

}