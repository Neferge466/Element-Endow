package com.element_endow.core.manager;

import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一实体数据管理器
 * 替代各系统中分散的实体数据存储
 */
public class EntityDataManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<LivingEntity, EntityData> entityDataMap = new ConcurrentHashMap<>();
    private final Object cleanupLock = new Object();
    private long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 30000; //30秒清理一次

    public static class EntityData {
        public final Map<String, List<com.element_endow.api.IElementMountSystem.MountData>> mounts = new HashMap<>();
        public final Map<String, com.element_endow.api.IElementMountSystem.AdvancedMountData> advancedMounts = new HashMap<>();
        public final Set<String> activeCombinations = new HashSet<>();
        public final Map<String, Object> customData = new HashMap<>();
        public long lastAccessTime = System.currentTimeMillis();

        public void updateAccessTime() {
            lastAccessTime = System.currentTimeMillis();
        }
    }

    public EntityData getOrCreateEntityData(LivingEntity entity) {
        EntityData data = entityDataMap.computeIfAbsent(entity, k -> new EntityData());
        data.updateAccessTime();

        //定期清理无效实体
        checkAndCleanup();

        return data;
    }

    public EntityData getEntityData(LivingEntity entity) {
        EntityData data = entityDataMap.get(entity);
        if (data != null) {
            data.updateAccessTime();
        }
        return data;
    }

    public void removeEntityData(LivingEntity entity) {
        entityDataMap.remove(entity);
    }

    private void checkAndCleanup() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime < CLEANUP_INTERVAL) {
            return;
        }

        synchronized (cleanupLock) {
            lastCleanupTime = currentTime;
            long cleanupThreshold = currentTime - 300000; // 5分钟未访问

            Iterator<Map.Entry<LivingEntity, EntityData>> iterator = entityDataMap.entrySet().iterator();
            int removedCount = 0;

            while (iterator.hasNext()) {
                Map.Entry<LivingEntity, EntityData> entry = iterator.next();
                LivingEntity entity = entry.getKey();
                EntityData data = entry.getValue();

                // 清理无效实体或长时间未访问的实体
                if (entity == null || !entity.isAlive() || data.lastAccessTime < cleanupThreshold) {
                    iterator.remove();
                    removedCount++;
                }
            }

            if (removedCount > 0) {
                LOGGER.debug("Cleaned up {} inactive entity data entries", removedCount);
            }
        }
    }

    public void clear() {
        entityDataMap.clear();
    }

    public int getManagedEntityCount() {
        return entityDataMap.size();
    }
}