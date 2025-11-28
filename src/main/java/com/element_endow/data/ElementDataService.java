package com.element_endow.data;

import com.element_endow.core.config.ElementConfigManager;
import com.element_endow.data.source.DataType;
import com.element_endow.api.ReactionResult;
import com.element_endow.data.source.impl.CodeRegistrationSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Supplier;

/**
 * 元素数据服务
 * 提供类型安全的数据访问方法
 */
public class ElementDataService {
    private static final Logger LOGGER = LogManager.getLogger();

    private final UnifiedDataManager dataManager;
    private final Map<DataType, CodeRegistrationSource<Object>> codeSources = new EnumMap<>(DataType.class);

    public ElementDataService(UnifiedDataManager dataManager) {
        this.dataManager = dataManager;
        initializeCodeSources();
    }

    /**
     * ========== 元素定义数据访问 ==========
     */
    public Optional<ElementConfigManager.ElementDefinition> getElementDefinition(String elementId) {
        return dataManager.getData(DataType.ELEMENTS, elementId);
    }

    public Map<String, ElementConfigManager.ElementDefinition> getAllElementDefinitions() {
        return dataManager.getDataMap(DataType.ELEMENTS);
    }

//    public void registerElementDefinition(String elementId, ElementConfigManager.ElementDefinition definition) {
//        getCodeSource(DataType.ELEMENTS).register(elementId, definition);
//        LOGGER.info("Registered element definition via code: {}", elementId);
//    }
//
//    public void unregisterElementDefinition(String elementId) {
//        getCodeSource(DataType.ELEMENTS).unregister(elementId);
//        LOGGER.info("Unregistered element definition: {}", elementId);
//    }


    /**
     * ========== 反应数据访问 ==========
     */

    public Optional<com.element_endow.data.ReactionLoader.ElementReaction> getReaction(String id) {
        return dataManager.getData(DataType.REACTIONS, id);
    }

    public Map<String, com.element_endow.data.ReactionLoader.ElementReaction> getAllReactions() {
        return dataManager.getDataMap(DataType.REACTIONS);
    }

    public void registerReaction(String id, com.element_endow.data.ReactionLoader.ElementReaction reaction) {
        getCodeSource(DataType.REACTIONS).register(id, reaction);
        LOGGER.info("Registered reaction via code: {}", id);
    }

    public void unregisterReaction(String id) {
        getCodeSource(DataType.REACTIONS).unregister(id);
        LOGGER.info("Unregistered reaction: {}", id);
    }

    /**
     * ========== 组合数据访问 ==========
     */

    public Optional<com.element_endow.data.CombinationLoader.ElementCombination> getCombination(String id) {
        return dataManager.getData(DataType.COMBINATIONS, id);
    }

    public Map<String, com.element_endow.data.CombinationLoader.ElementCombination> getAllCombinations() {
        return dataManager.getDataMap(DataType.COMBINATIONS);
    }

    public void registerCombination(String id, com.element_endow.data.CombinationLoader.ElementCombination combination) {
        getCodeSource(DataType.COMBINATIONS).register(id, combination);
        LOGGER.info("Registered combination via code: {}", id);
    }

    public void unregisterCombination(String id) {
        getCodeSource(DataType.COMBINATIONS).unregister(id);
        LOGGER.info("Unregistered combination: {}", id);
    }

    /**
     * ========== 实体绑定数据访问 ==========
     */

    /**
     * 通过实体类型ID获取绑定
     */
    public Optional<com.element_endow.data.entity_bindings.EntityElementBindingLoader.EntityElementBinding>
    getEntityBinding(String entityTypeId) {
        return dataManager.getData(DataType.ENTITY_BINDINGS, entityTypeId);
    }

    /**
     * 通过实体类型实例获取绑定
     */
    public Optional<com.element_endow.data.entity_bindings.EntityElementBindingLoader.EntityElementBinding>
    getEntityBinding(net.minecraft.world.entity.EntityType<?> entityType) {
        net.minecraft.resources.ResourceLocation entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        return entityId != null ? getEntityBinding(entityId.toString()) : Optional.empty();
    }

    /**
     * 获取所有实体绑定
     */
    public Map<String, com.element_endow.data.entity_bindings.EntityElementBindingLoader.EntityElementBinding>
    getAllEntityBindings() {
        return dataManager.getDataMap(DataType.ENTITY_BINDINGS);
    }

    /**
     * 注册实体绑定
     */
    public void registerEntityBinding(String entityTypeId,
                                      com.element_endow.data.entity_bindings.EntityElementBindingLoader.EntityElementBinding binding) {
        getCodeSource(DataType.ENTITY_BINDINGS).register(entityTypeId, binding);
        LOGGER.info("Registered entity binding via code: {} -> {}", entityTypeId, binding.elements);
    }

    /**
     * 注销实体绑定
     */
    public void unregisterEntityBinding(String entityTypeId) {
        getCodeSource(DataType.ENTITY_BINDINGS).unregister(entityTypeId);
        LOGGER.info("Unregistered entity binding: {}", entityTypeId);
    }

    /**
     * ========== 批量操作 ==========
     */

    public void batchRegisterReactions(Map<String, com.element_endow.data.ReactionLoader.ElementReaction> reactions) {
        CodeRegistrationSource<Object> source = getCodeSource(DataType.REACTIONS);
        reactions.forEach(source::register);
        LOGGER.info("Batch registered {} reactions", reactions.size());
    }

    public void batchRegisterCombinations(Map<String, com.element_endow.data.CombinationLoader.ElementCombination> combinations) {
        CodeRegistrationSource<Object> source = getCodeSource(DataType.COMBINATIONS);
        combinations.forEach(source::register);
        LOGGER.info("Batch registered {} combinations", combinations.size());
    }

    public void batchRegisterEntityBindings(
            Map<String, com.element_endow.data.entity_bindings.EntityElementBindingLoader.EntityElementBinding> bindings) {
        CodeRegistrationSource<Object> source = getCodeSource(DataType.ENTITY_BINDINGS);
        bindings.forEach(source::register);
        LOGGER.info("Batch registered {} entity bindings", bindings.size());
    }

    /**
     * ========== 数据统计 ==========
     */
    public DataStats getDataStats() {
        Map<DataType, Integer> counts = new EnumMap<>(DataType.class);
        Map<DataType, Integer> sources = dataManager.getDataSourceStats();

        for (DataType type : DataType.values()) {
            counts.put(type, dataManager.getDataMap(type).size());
        }

        return new DataStats(counts, sources);
    }

    /**
     * ========== 数据重载 ==========
     */

    public void reloadReactions() {
        dataManager.reloadData(DataType.REACTIONS);
    }

    public void reloadCombinations() {
        dataManager.reloadData(DataType.COMBINATIONS);
    }

    public void reloadAll() {
        dataManager.reloadAllData();
    }

    /**
     * ========== 工具方法 ==========
     */

    @SuppressWarnings("unchecked")
    private CodeRegistrationSource<Object> getCodeSource(DataType type) {
        return (CodeRegistrationSource<Object>) codeSources.computeIfAbsent(type,
                t -> new CodeRegistrationSource<>(t, 1000) // 代码源具有高优先级
        );
    }

    private void initializeCodeSources() {
        // 注册所有代码数据源
        for (DataType type : DataType.values()) {
            CodeRegistrationSource<Object> source = new CodeRegistrationSource<>(type, 1000);
            dataManager.registerDataSource(type, source);
            codeSources.put(type, source);
        }
        LOGGER.info("Initialized code registration sources for all data types");
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
    }
}