package com.element_endow.data;

import com.element_endow.api.IElementMountSystem;
import com.element_endow.api.ReactionResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class CombinationLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private final Map<String, ElementCombination> combinations = new HashMap<>();

    public void loadCombinations() {
        combinations.clear();
    }

    public void loadFromResources(Map<ResourceLocation, JsonElement> resources) {
        combinations.clear();

        int loadedCount = 0;
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();

            if (!location.getNamespace().equals("element_endow")) {
                continue;
            }

            try {
                ElementCombination combination = GSON.fromJson(entry.getValue(), ElementCombination.class);

                if (validateCombination(combination)) {
                    combinations.put(combination.id, combination);
                    loadedCount++;
                } else {
                    errorCount++;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load combination {}: {}", location, e.getMessage());
                errorCount++;
            }
        }

        if (loadedCount > 0 || errorCount > 0) {
            LOGGER.info("Loaded {} combinations ({} errors)", loadedCount, errorCount);
        }
    }

    private boolean validateCombination(ElementCombination combination) {
        if (combination.id == null || combination.id.isEmpty()) {
            return false;
        }

        //初始化空集合避免NPE
        if (combination.requiredElements == null) combination.requiredElements = new HashSet<>();
        if (combination.forbiddenElements == null) combination.forbiddenElements = new HashSet<>();
        if (combination.minValues == null) combination.minValues = new HashMap<>();
        if (combination.attributeEffects == null) combination.attributeEffects = new ArrayList<>();
        if (combination.statusEffects == null) combination.statusEffects = new ArrayList<>();
        if (combination.mountTable == null) combination.mountTable = new HashMap<>();
        if (combination.conditions == null) combination.conditions = new HashMap<>();

        //初始化触发效果
        if (combination.attackTrigger == null) combination.attackTrigger = new TriggerEffect();
        if (combination.defenseTrigger == null) combination.defenseTrigger = new TriggerEffect();

        return true;
    }

    //数据类定义
    public static class ElementCombination {
        public String id;
        public Set<String> requiredElements = new HashSet<>();
        public Set<String> forbiddenElements = new HashSet<>();
        public Map<String, Double> minValues = new HashMap<>();
        public Map<String, Object> conditions = new HashMap<>();

        //常态效果，组合激活时持续生效
        public List<AttributeEffect> attributeEffects = new ArrayList<>();//属性效果
        public List<StatusEffect> statusEffects = new ArrayList<>();//状态效果
        public Map<String, MountData> mountTable = new HashMap<>();

        //触发效果，特定事件时触发
        public TriggerEffect attackTrigger = new TriggerEffect();
        public TriggerEffect defenseTrigger = new TriggerEffect();
    }

    //属性效果
    public static class AttributeEffect {
        public String attribute;//属性ID
        public double value;
        public String operation;//操作类型: add, multiply_base, multiply_total

        public AttributeEffect() {}

        public AttributeEffect(String attribute, double value, String operation) {
            this.attribute = attribute;
            this.value = value;
            this.operation = operation;
        }
    }

    //状态效果
    public static class StatusEffect {
        public String effect;     // 状态效果ID
        public int duration;      // 持续时间（tick）
        public int amplifier;     // 效果等级
        public boolean showParticles = true; // 是否显示粒子

        public StatusEffect() {}

        public StatusEffect(String effect, int duration, int amplifier, boolean showParticles) {
            this.effect = effect;
            this.duration = duration;
            this.amplifier = amplifier;
            this.showParticles = showParticles;
        }
    }

    public static class MountData {
        public int duration;
        public double amount;

        public MountData() {}

        public MountData(int duration, double amount) {
            this.duration = duration;
            this.amount = amount;
        }
    }

    //组合触发效果
    public static class TriggerEffect {
        public double damageMultiplier = 1.0;
        public double defenseMultiplier = 1.0;
        public double extraDamage = 0.0;
        public double damageReduction = 0.0;
        public double probability = 1.0;

        public List<EffectData> targetEffects = new ArrayList<>();
        public List<EffectData> selfEffects = new ArrayList<>();
        public List<AttributeModifierData> targetAttributeModifiers = new ArrayList<>();
        public List<AttributeModifierData> selfAttributeModifiers = new ArrayList<>();
        public List<MountApplication> mountApplications = new ArrayList<>();
        public List<IElementMountSystem.AdvancedMountData> advancedMountApplications = new ArrayList<>();

        public Map<String, Object> triggerConditions = new HashMap<>();
    }

    //效果数据（用于触发）
    public static class EffectData {
        public String effect;
        public int duration;
        public int amplifier;
        public boolean showParticles = true;

        public EffectData() {}

        public EffectData(String effect, int duration, int amplifier, boolean showParticles) {
            this.effect = effect;
            this.duration = duration;
            this.amplifier = amplifier;
            this.showParticles = showParticles;
        }
    }

    public static class MountApplication {
        public String elementId;
        public double amount;
        public int duration;
        public double probability = 1.0;

        public MountApplication() {}

        public MountApplication(String elementId, double amount, int duration, double probability) {
            this.elementId = elementId;
            this.amount = amount;
            this.duration = duration;
            this.probability = probability;
        }
    }

    public static class AttributeModifierData {
        public String attribute;
        public String operation;
        public double value;
        public String name;
        public String uuid;
        public boolean permanent = false;
        public int duration = 100;

        public AttributeModifierData() {}

        public AttributeModifierData(String attribute, String operation, double value, String name, String uuid, boolean permanent, int duration) {
            this.attribute = attribute;
            this.operation = operation;
            this.value = value;
            this.name = name;
            this.uuid = uuid;
            this.permanent = permanent;
            this.duration = duration;
        }

        /**
         * 转换为 AttributeModifierApplication 类型
         */
        public ReactionResult.AttributeModifierApplication toAttributeModifierApplication() {
            AttributeModifier modifier = createAttributeModifier();
            if (modifier == null) {
                return null;
            }
            return new ReactionResult.AttributeModifierApplication(
                    this.attribute,
                    modifier,
                    this.permanent,
                    this.duration
            );
        }

        private AttributeModifier createAttributeModifier() {
            try {
                AttributeModifier.Operation operation = getOperation();

                UUID modifierId;
                if (uuid != null && !uuid.trim().isEmpty()) {
                    try {
                        modifierId = UUID.fromString(uuid);
                    } catch (IllegalArgumentException e) {
                        modifierId = UUID.randomUUID();
                    }
                } else {
                    modifierId = UUID.randomUUID();
                }

                String modifierName = name != null ? name : "element_endow.combination." + operation;

                return new AttributeModifier(modifierId, modifierName, value, operation);

            } catch (Exception e) {
                return null;
            }
        }

        private AttributeModifier.Operation getOperation() {
            switch (operation.toLowerCase()) {
                case "add": return AttributeModifier.Operation.ADDITION;
                case "multiply_base": return AttributeModifier.Operation.MULTIPLY_BASE;
                case "multiply_total": return AttributeModifier.Operation.MULTIPLY_TOTAL;
                default: return AttributeModifier.Operation.ADDITION;
            }
        }
    }

    public Map<String, ElementCombination> getCombinations() {
        return new HashMap<>(combinations);
    }
}