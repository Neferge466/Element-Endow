package com.element_endow.core;

import com.element_endow.api.IElementMountSystem;
import com.element_endow.api.IElementSystem;
import com.element_endow.util.MountStackManager;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ElementMountSystem implements IElementMountSystem {
    private static final Logger LOGGER = LogManager.getLogger();

    private final IElementSystem elementSystem;
    private final Map<LivingEntity, Map<String, List<MountData>>> entityMounts;
    private final Map<LivingEntity, Map<String, AdvancedMountData>> entityAdvancedMounts;
    private int tickCounter = 0;

    public ElementMountSystem(IElementSystem elementSystem) {
        this.elementSystem = elementSystem;
        this.entityMounts = new WeakHashMap<>();
        this.entityAdvancedMounts = new WeakHashMap<>();
    }

    @Override
    public void applyMount(LivingEntity target, String elementId,
                           double amount, int duration, double probability,
                           String stackBehavior) {

        if (target.level().random.nextDouble() >= probability) {
            return;
        }

        if (!elementSystem.isElementRegistered(elementId)) {
            LOGGER.warn("Attempted to apply mount for unregistered element: {}", elementId);
            return;
        }

        Map<String, List<MountData>> entityMountsMap = entityMounts.computeIfAbsent(target, k -> new HashMap<>());
        MountStackManager.applyStackedMount(target,
                elementId,
                amount,
                duration,
                stackBehavior,
                5,
                entityMountsMap);
        applyMountEffect(target, elementId);

        LOGGER.debug("Applied mount: {} to entity {}, amount: {}, duration: {}", elementId, target, amount, duration);
    }

    @Override
    public void applyAdvancedMount(LivingEntity target, AdvancedMountData mountData) {
        if (target.level().random.nextDouble() >= mountData.probability) {
            return;
        }

        if (!elementSystem.isElementRegistered(mountData.elementId)) {
            LOGGER.warn("Attempted to apply advanced mount for unregistered element: {}", mountData.elementId);
            return;
        }

        int currentTime = (int) target.level().getGameTime();
        double scaledAmount = MountStackManager.applyScaling(
                mountData.baseAmount,
                mountData.scaling,
                getCurrentStacks(target, mountData.elementId),
                currentTime,
                target,
                mountData.elementId
        );

        applyMount(target, mountData.elementId, scaledAmount, mountData.baseDuration, 1.0, mountData.stackBehavior);

        Map<String, AdvancedMountData> advancedMounts = entityAdvancedMounts.computeIfAbsent(target, k -> new HashMap<>());
        advancedMounts.put(mountData.elementId, mountData);

        LOGGER.debug("Applied advanced mount: {} to entity {}, scaled amount: {}", mountData.elementId, target, scaledAmount);
    }

    @Override
    public void removeMount(LivingEntity entity, String elementId) {
        Map<String, List<MountData>> mountsMap = entityMounts.get(entity);
        if (mountsMap != null) {
            List<MountData> removed = mountsMap.remove(elementId);
            if (removed != null) {
                removeMountEffect(entity, elementId);
                if (mountsMap.isEmpty()) {
                    entityMounts.remove(entity);
                }
            }
        }

        Map<String, AdvancedMountData> advancedMounts = entityAdvancedMounts.get(entity);
        if (advancedMounts != null) {
            advancedMounts.remove(elementId);
            if (advancedMounts.isEmpty()) {
                entityAdvancedMounts.remove(entity);
            }
        }

        LOGGER.debug("Removed mount: {} from entity {}", elementId, entity);
    }

    @Override
    public Map<String, MountData> getActiveMounts(LivingEntity entity) {
        Map<String, MountData> result = new HashMap<>();
        Map<String, List<MountData>> entityMountsMap = entityMounts.get(entity);

        if (entityMountsMap != null) {
            for (Map.Entry<String, List<MountData>> entry : entityMountsMap.entrySet()) {
                List<MountData> mounts = entry.getValue();
                if (!mounts.isEmpty()) {
                    result.put(entry.getKey(), mounts.get(mounts.size() - 1));
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, MountStackInfo> getMountStacks(LivingEntity entity) {
        Map<String, MountStackInfo> result = new HashMap<>();
        Map<String, List<MountData>> entityMountsMap = entityMounts.get(entity);

        if (entityMountsMap != null) {
            for (Map.Entry<String, List<MountData>> entry : entityMountsMap.entrySet()) {
                List<MountData> mounts = entry.getValue();
                if (!mounts.isEmpty()) {
                    String stackBehavior = mounts.get(0).stackBehavior;
                    MountStackInfo stackInfo = MountStackManager.getStackInfo(mounts, stackBehavior, 5);
                    result.put(entry.getKey(), stackInfo);
                }
            }
        }

        return result;
    }

    @Override
    public void tick() {
        tickCounter++;
        updateMountEffects();

        if (tickCounter % 10 == 0) {
            cleanupExpiredMounts();
        }
    }

    private void updateMountEffects() {
        int currentTime = -1;

        for (Map.Entry<LivingEntity, Map<String, List<MountData>>> entityEntry : entityMounts.entrySet()) {
            LivingEntity entity = entityEntry.getKey();
            Map<String, List<MountData>> mountsMap = entityEntry.getValue();

            if (entity == null || !entity.isAlive()) {
                continue;
            }

            if (currentTime == -1 && entity.level() != null) {
                currentTime = (int) entity.level().getGameTime();
            }

            if (currentTime == -1) continue;

            for (Map.Entry<String, List<MountData>> mountEntry : mountsMap.entrySet()) {
                String elementId = mountEntry.getKey();
                List<MountData> mounts = mountEntry.getValue();

                int finalCurrentTime = currentTime;
                mounts.removeIf(mount -> mount.isExpired(finalCurrentTime));

                if (!mounts.isEmpty()) {
                    applyMountEffect(entity, elementId);
                }
            }
        }
    }

    private void applyMountEffect(LivingEntity entity, String elementId) {
        Map<String, List<MountData>> mountsMap = entityMounts.get(entity);
        Map<String, AdvancedMountData> advancedMounts = entityAdvancedMounts.get(entity);

        if (mountsMap == null || !mountsMap.containsKey(elementId)) {
            return;
        }

        List<MountData> mounts = mountsMap.get(elementId);
        int currentTime = (int) entity.level().getGameTime();

        double totalAmount = 0.0;

        for (MountData mount : mounts) {
            double mountAmount = mount.amount;

            if (advancedMounts != null && advancedMounts.containsKey(elementId)) {
                AdvancedMountData advancedMount = advancedMounts.get(elementId);
                mountAmount = MountStackManager.applyDecay(mount, advancedMount.decay, currentTime);
            }

            totalAmount += mountAmount;
        }

        double currentBaseValue = getBaseElementValue(entity, elementId);
        elementSystem.setElementValue(entity, elementId, currentBaseValue + totalAmount);
    }

    private void removeMountEffect(LivingEntity entity, String elementId) {
        applyMountEffect(entity, elementId);
    }

    private double getBaseElementValue(LivingEntity entity, String elementId) {
        double currentValue = elementSystem.getElementValue(entity, elementId);
        Map<String, List<MountData>> mountsMap = entityMounts.get(entity);

        if (mountsMap != null && mountsMap.containsKey(elementId)) {
            List<MountData> mounts = mountsMap.get(elementId);
            double mountAmount = MountStackManager.calculateStackedAmount(mounts,
                    mounts.isEmpty() ? "refresh" : mounts.get(0).stackBehavior);
            return currentValue - mountAmount;
        }

        return currentValue;
    }

    private void cleanupExpiredMounts() {
        int currentTime = -1;
        List<LivingEntity> toRemove = new ArrayList<>();

        for (Map.Entry<LivingEntity, Map<String, List<MountData>>> entityEntry : entityMounts.entrySet()) {
            LivingEntity entity = entityEntry.getKey();
            Map<String, List<MountData>> mountsMap = entityEntry.getValue();

            if (entity == null || !entity.isAlive()) {
                toRemove.add(entity);
                continue;
            }

            if (currentTime == -1 && entity.level() != null) {
                currentTime = (int) entity.level().getGameTime();
            }

            if (currentTime == -1) continue;

            MountStackManager.cleanupExpiredMounts(mountsMap, currentTime);

            if (mountsMap.isEmpty()) {
                toRemove.add(entity);
            }
        }

        for (LivingEntity entity : toRemove) {
            entityMounts.remove(entity);
            entityAdvancedMounts.remove(entity);
        }
    }

    private int getCurrentStacks(LivingEntity entity, String elementId) {
        Map<String, List<MountData>> mountsMap = entityMounts.get(entity);
        if (mountsMap != null && mountsMap.containsKey(elementId)) {
            return mountsMap.get(elementId).size();
        }
        return 0;
    }
}