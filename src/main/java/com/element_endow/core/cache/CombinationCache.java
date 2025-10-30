package com.element_endow.core.cache;

import com.element_endow.api.IElementSystem;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * 组合状态缓存
 */
public class CombinationCache {
    private static final Logger LOGGER = LogManager.getLogger();

    //缓存：实体-激活的组合
    private final Map<LivingEntity, CacheEntry> cache = new WeakHashMap<>();

    //元素变化检测阈值
    private static final double ELEMENT_CHANGE_THRESHOLD = 0.01;
    //缓存有效期（ticks）
    private static final int CACHE_DURATION = 40; // 2秒

    private static class CacheEntry {
        public final Set<String> activeCombinations;
        public final Map<String, Double> elementSnapshot;
        public final long lastUpdateTime;
        public final int tickCount;

        public CacheEntry(Set<String> combinations, Map<String, Double> elements, long time, int ticks) {
            this.activeCombinations = combinations;
            this.elementSnapshot = elements;
            this.lastUpdateTime = time;
            this.tickCount = ticks;
        }
    }

    /**
     * 检查是否需要重新计算组合
     */
    public boolean shouldRecheck(LivingEntity entity, IElementSystem elementSystem, int currentTick) {
        CacheEntry entry = cache.get(entity);

        // 没有缓存，需要检查
        if (entry == null) {
            return true;
        }

        //缓存过期
        if (currentTick - entry.tickCount > CACHE_DURATION) {
            return true;
        }

        //检查元素值是否有显著变化
        return hasElementValuesChanged(entity, elementSystem, entry.elementSnapshot);
    }

    /**
     * 更新缓存
     */
    public void updateCache(LivingEntity entity, Set<String> combinations,
                            IElementSystem elementSystem, int currentTick) {
        //创建元素快照
        Map<String, Double> elementSnapshot = createElementSnapshot(entity, elementSystem);

        CacheEntry entry = new CacheEntry(
                new HashSet<>(combinations),
                elementSnapshot,
                System.currentTimeMillis(),
                currentTick
        );

        cache.put(entity, entry);
    }

    /**
     * 获取缓存的组合
     */
    public Set<String> getCachedCombinations(LivingEntity entity) {
        CacheEntry entry = cache.get(entity);
        return entry != null ? new HashSet<>(entry.activeCombinations) : Collections.emptySet();
    }

    /**
     * 使特定实体的缓存失效
     */
    public void invalidate(LivingEntity entity) {
        cache.remove(entity);
    }

    /**
     * 清除所有缓存
     */
    public void clear() {
        cache.clear();
    }

    /**
     * 创建元素值快照
     */
    private Map<String, Double> createElementSnapshot(LivingEntity entity, IElementSystem elementSystem) {
        Map<String, Double> snapshot = new HashMap<>();
        for (String elementId : elementSystem.getEnabledElements()) {
            double value = elementSystem.getElementValue(entity, elementId);
            if (value > 0) {
                snapshot.put(elementId, value);
            }
        }
        return snapshot;
    }

    /**
     * 检查元素值是否发生显著变化
     */
    private boolean hasElementValuesChanged(LivingEntity entity, IElementSystem elementSystem,
                                            Map<String, Double> snapshot) {
        //检查差异（与快照）
        for (String elementId : elementSystem.getEnabledElements()) {
            double currentValue = elementSystem.getElementValue(entity, elementId);
            Double snapshotValue = snapshot.get(elementId);

            //新增或移除元素
            if ((currentValue > 0) != (snapshotValue != null && snapshotValue > 0)) {
                return true;
            }

            //值变化超过阈值
            if (snapshotValue != null && Math.abs(currentValue - snapshotValue) > ELEMENT_CHANGE_THRESHOLD) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取缓存统计信息（用于调试）
     */
    public CacheStats getStats() {
        return new CacheStats(cache.size());
    }

    public static class CacheStats {
        public final int cachedEntities;

        public CacheStats(int cachedEntities) {
            this.cachedEntities = cachedEntities;
        }
    }
}