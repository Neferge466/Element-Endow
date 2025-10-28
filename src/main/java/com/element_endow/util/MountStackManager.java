package com.element_endow.util;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.api.IElementMountSystem;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class MountStackManager {
    private static final Logger LOGGER = LogManager.getLogger();

    public static IElementMountSystem.MountData applyStackedMount(
            LivingEntity entity,
            String elementId,
            double amount,
            int duration,
            String stackBehavior,
            int maxStacks,
            Map<String, List<IElementMountSystem.MountData>> entityMounts) {

        int currentTime = (int) entity.level().getGameTime();
        List<IElementMountSystem.MountData> mounts = entityMounts.computeIfAbsent(elementId, k -> new ArrayList<>());

        IElementMountSystem.MountData newMount = new IElementMountSystem.MountData(amount, currentTime, duration, stackBehavior);

        switch (stackBehavior) {
            case "refresh":
                mounts.clear();
                mounts.add(newMount);
                break;

            case "add":
                if (mounts.size() >= maxStacks) {
                    mounts.remove(0);
                }
                mounts.add(newMount);
                break;

            case "independent":
                if (mounts.size() >= 10) {
                    mounts.remove(0);
                }
                mounts.add(newMount);
                break;

            case "max":
                if (mounts.isEmpty() || amount > mounts.get(0).amount) {
                    mounts.clear();
                    mounts.add(newMount);
                }
                break;

            default:
                mounts.clear();
                mounts.add(newMount);
                break;
        }

        return newMount;
    }

    public static double calculateStackedAmount(List<IElementMountSystem.MountData> mounts, String stackBehavior) {
        if (mounts.isEmpty()) {
            return 0.0;
        }

        switch (stackBehavior) {
            case "refresh":
            case "max":
                return mounts.get(mounts.size() - 1).amount;

            case "add":
                return mounts.stream().mapToDouble(mount -> mount.amount).sum();

            case "independent":
                return mounts.stream().mapToDouble(mount -> mount.amount).sum();

            default:
                return mounts.get(mounts.size() - 1).amount;
        }
    }

    public static void cleanupExpiredMounts(Map<String, List<IElementMountSystem.MountData>> entityMounts, int currentTime) {
        Iterator<Map.Entry<String, List<IElementMountSystem.MountData>>> iterator = entityMounts.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, List<IElementMountSystem.MountData>> entry = iterator.next();
            List<IElementMountSystem.MountData> mounts = entry.getValue();

            mounts.removeIf(mount -> mount.isExpired(currentTime));

            if (mounts.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public static IElementMountSystem.MountStackInfo getStackInfo(List<IElementMountSystem.MountData> mounts, String stackBehavior, int maxStacks) {
        if (mounts == null || mounts.isEmpty()) {
            return new IElementMountSystem.MountStackInfo(0, maxStacks, 0.0, stackBehavior);
        }

        double totalAmount = calculateStackedAmount(mounts, stackBehavior);
        return new IElementMountSystem.MountStackInfo(mounts.size(), maxStacks, totalAmount, stackBehavior);
    }

    public static double applyDecay(IElementMountSystem.MountData mount, IElementMountSystem.DecayData decay, int currentTime) {
        if (decay == null || "none".equals(decay.type)) {
            return mount.amount;
        }

        int elapsed = currentTime - mount.startTime;
        if (elapsed < decay.decayStart) {
            return mount.amount;
        }

        int decayTime = elapsed - decay.decayStart;
        double decayFactor;

        switch (decay.type) {
            case "linear":
                decayFactor = Math.max(0, 1.0 - (decay.rate * decayTime));
                break;

            case "exponential":
                decayFactor = Math.exp(-decay.rate * decayTime);
                break;

            default:
                decayFactor = 1.0;
                break;
        }

        return mount.amount * decayFactor;
    }

    public static double applyScaling(double baseAmount, IElementMountSystem.ScalingData scaling, int stacks, int currentTime, LivingEntity entity, String elementId) {
        if (scaling == null) {
            return baseAmount;
        }

        double scaleFactor = 1.0;

        switch (scaling.basedOn) {
            case "stacks":
                scaleFactor = calculateStackScaling(stacks, scaling);
                break;

            case "time":
                scaleFactor = calculateTimeScaling(currentTime, scaling);
                break;

            case "element_value":
                scaleFactor = calculateElementScaling(entity, elementId, scaling);
                break;

            default:
                scaleFactor = 1.0;
                break;
        }

        return baseAmount * scaleFactor;
    }

    private static double calculateStackScaling(int stacks, IElementMountSystem.ScalingData scaling) {
        switch (scaling.type) {
            case "linear":
                return 1.0 + (scaling.factor * (stacks - 1));

            case "exponential":
                return Math.pow(1.0 + scaling.factor, stacks - 1);

            case "logarithmic":
                return 1.0 + scaling.factor * Math.log1p(stacks - 1);

            default:
                return 1.0;
        }
    }

    private static double calculateTimeScaling(int currentTime, IElementMountSystem.ScalingData scaling) {
        return 1.0 + (scaling.factor * (currentTime % 24000) / 24000.0);
    }

    private static double calculateElementScaling(LivingEntity entity, String elementId, IElementMountSystem.ScalingData scaling) {
        double elementValue = ElementSystemAPI.getElementSystem().getElementValue(entity, elementId);
        return 1.0 + (scaling.factor * elementValue / 100.0);
    }
}