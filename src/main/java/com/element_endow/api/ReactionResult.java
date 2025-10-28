package com.element_endow.api;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReactionResult {
    public double damageMultiplier;
    public double defenseMultiplier;
    public double extraDamage;
    public double damageReduction;
    public final List<MobEffectInstance> targetEffects;
    public final List<MobEffectInstance> selfEffects;
    public final List<MountApplication> mountApplications;
    public final List<IElementMountSystem.AdvancedMountData> advancedMountApplications;

    //属性修饰符
    public final List<AttributeModifierApplication> targetAttributeModifiers;
    public final List<AttributeModifierApplication> selfAttributeModifiers;

    public ReactionResult() {
        this.damageMultiplier = 1.0;
        this.defenseMultiplier = 1.0;
        this.extraDamage = 0.0;
        this.damageReduction = 0.0;
        this.targetEffects = new ArrayList<>();
        this.selfEffects = new ArrayList<>();
        this.mountApplications = new ArrayList<>();
        this.advancedMountApplications = new ArrayList<>();
        this.targetAttributeModifiers = new ArrayList<>();
        this.selfAttributeModifiers = new ArrayList<>();
    }

    public static class MountApplication {
        public final String elementId;
        public final double amount;
        public final int duration;
        public final double probability;

        public MountApplication(String elementId, double amount, int duration, double probability) {
            this.elementId = elementId;
            this.amount = amount;
            this.duration = duration;
            this.probability = probability;
        }
    }

    //属性修饰符应用类
    public static class AttributeModifierApplication {
        public final ResourceLocation attributeId;
        public final AttributeModifier modifier;
        public final boolean permanent;//是否为永久
        public final int duration;//持续时间，0表示永久


        public AttributeModifierApplication(ResourceLocation attributeId, AttributeModifier modifier, boolean permanent, int duration) {
            this.attributeId = attributeId;
            this.modifier = modifier;
            this.permanent = permanent;
            this.duration = duration;
        }

        public AttributeModifierApplication(String attributeId, AttributeModifier modifier, boolean permanent, int duration) {
            this(ResourceLocation.tryParse(attributeId), modifier, permanent, duration);
        }
    }
}