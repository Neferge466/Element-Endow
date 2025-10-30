package com.element_endow.core.cache;

import net.minecraft.world.entity.LivingEntity;
import com.element_endow.util.ConditionChecker;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 条件检查缓存
 */
public class ConditionCache {
    private final Map<ConditionKey, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = TimeUnit.SECONDS.toMillis(5); // 5秒缓存

    private static class ConditionKey {
        final LivingEntity entity;
        final Map<String, Object> conditions;
        final int hashCode;

        ConditionKey(LivingEntity entity, Map<String, Object> conditions) {
            this.entity = entity;
            this.conditions = conditions;
            this.hashCode = Objects.hash(entity, conditions);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConditionKey)) return false;
            ConditionKey that = (ConditionKey) o;
            return Objects.equals(entity, that.entity) &&
                    Objects.equals(conditions, that.conditions);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    private static class CacheEntry {
        final boolean result;
        final long timestamp;

        CacheEntry(boolean result, long timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }
    }

    public boolean checkWithCache(LivingEntity entity, Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        ConditionKey key = new ConditionKey(entity, conditions);
        CacheEntry entry = cache.get(key);

        //使用有效缓存
        if (entry != null && !entry.isExpired()) {
            return entry.result;
        }

        //执行条件检查并缓存结果
        boolean result = ConditionChecker.checkConditions(conditions, entity, entity.level());
        cache.put(key, new CacheEntry(result, System.currentTimeMillis()));

        return result;
    }

    public void clear() {
        cache.clear();
    }

    /**
     * 清理过期条目
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}