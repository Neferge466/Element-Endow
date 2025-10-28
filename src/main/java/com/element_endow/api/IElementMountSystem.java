package com.element_endow.api;

import net.minecraft.world.entity.LivingEntity;
import java.util.Map;

public interface IElementMountSystem {

    /**
     * 应用挂载效果
     */
    void applyMount(LivingEntity target, String elementId, double amount, int duration, double probability, String stackBehavior);

    /**
     * 应用高级挂载效果
     */
    void applyAdvancedMount(LivingEntity target, AdvancedMountData mountData);

    /**
     * 移除挂载效果
     */
    void removeMount(LivingEntity entity, String elementId);

    /**
     * 获取实体当前挂载状态
     */
    Map<String, MountData> getActiveMounts(LivingEntity entity);

    /**
     * 获取实体的挂载堆叠信息
     */
    Map<String, MountStackInfo> getMountStacks(LivingEntity entity);

    /**
     * 定时清理过期挂载
     */
    void tick();

    public static class MountData {
        public final double amount;
        public final int startTime;
        public final int duration;
        public final String stackBehavior;

        public MountData(double amount, int startTime, int duration, String stackBehavior) {
            this.amount = amount;
            this.startTime = startTime;
            this.duration = duration;
            this.stackBehavior = stackBehavior;
        }

        public boolean isExpired(int currentTime) {
            return currentTime - startTime >= duration;
        }
    }

    public static class MountStackInfo {
        public final int currentStacks;
        public final int maxStacks;
        public final double totalAmount;
        public final String stackBehavior;

        public MountStackInfo(int currentStacks, int maxStacks, double totalAmount, String stackBehavior) {
            this.currentStacks = currentStacks;
            this.maxStacks = maxStacks;
            this.totalAmount = totalAmount;
            this.stackBehavior = stackBehavior;
        }
    }

    public static class AdvancedMountData {
        public String elementId;
        public double baseAmount;
        public int baseDuration;
        public double probability = 1.0;
        public String stackBehavior = "refresh";
        public int maxStacks = 1;
        public ScalingData scaling;
        public DecayData decay;

        public AdvancedMountData(String elementId, double baseAmount, int baseDuration) {
            this.elementId = elementId;
            this.baseAmount = baseAmount;
            this.baseDuration = baseDuration;
        }

        //无参构造函数用于json反序列化
        public AdvancedMountData() {
        }
    }

    public static class ScalingData {
        public String type = "linear";
        public double factor = 1.0;
        public String basedOn = "stacks";

        //无参构造函数...
        public ScalingData() {
        }
    }

    public static class DecayData {
        public String type = "none";
        public double rate = 0.0;
        public int decayStart = 0;

        //无参构造函数...
        public DecayData() {
        }
    }
}