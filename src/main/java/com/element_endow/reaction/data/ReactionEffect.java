package com.element_endow.reaction.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.List;

public class ReactionEffect {
    private final List<MobEffectInstance> afflictEffects;
    private final List<MobEffectInstance> empowerEffects;

    public ReactionEffect(JsonElement afflictElement, JsonElement empowerElement) {
        this.afflictEffects = parseEffects(afflictElement);
        this.empowerEffects = parseEffects(empowerElement);
    }

    private List<MobEffectInstance> parseEffects(JsonElement element) {
        List<MobEffectInstance> effects = new ArrayList<>();

        if (element != null && element.isJsonArray()) {
            JsonArray effectArray = element.getAsJsonArray();
            for (JsonElement effectElement : effectArray) {
                if (effectElement.isJsonArray()) {
                    JsonArray effectData = effectElement.getAsJsonArray();
                    if (effectData.size() >= 4) {
                        String effectId = effectData.get(0).getAsString();
                        int duration = effectData.get(1).getAsInt();
                        int amplifier = effectData.get(2).getAsInt();
                        boolean showParticles = effectData.get(3).getAsBoolean();

                        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effectId));
                        if (effect != null) {
                            effects.add(new MobEffectInstance(effect, duration, amplifier, false, showParticles));
                        }
                    }
                }
            }
        }

        return effects;
    }

    public List<MobEffectInstance> getAfflictEffects() { return afflictEffects; }
    public List<MobEffectInstance> getEmpowerEffects() { return empowerEffects; }
}