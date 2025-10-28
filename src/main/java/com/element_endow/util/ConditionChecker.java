package com.element_endow.util;

import com.element_endow.api.ElementSystemAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConditionChecker {
    private static final Logger LOGGER = LogManager.getLogger();

    public static boolean checkConditions(Map<String, Object> conditions,
                                          LivingEntity entity,
                                          Level level) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        try {
            BlockPos pos = entity.blockPosition();

            if (conditions.containsKey("biome")) {
                if (!checkBiomeCondition(conditions.get("biome"), level, pos)) {
                    return false;
                }
            }

            if (conditions.containsKey("dimension")) {
                if (!checkDimensionCondition(conditions.get("dimension"), level)) {
                    return false;
                }
            }

            if (conditions.containsKey("weather")) {
                if (!checkWeatherCondition(conditions.get("weather"), level)) {
                    return false;
                }
            }

            if (conditions.containsKey("time")) {
                if (!checkTimeCondition(conditions.get("time"), level)) {
                    return false;
                }
            }

            if (conditions.containsKey("moon_phase")) {
                if (!checkMoonPhaseCondition(conditions.get("moon_phase"), level)) {
                    return false;
                }
            }

            if (conditions.containsKey("elements")) {
                if (!checkElementConditions((List<?>) conditions.get("elements"), entity)) {
                    return false;
                }
            }

            if (conditions.containsKey("health")) {
                if (!checkHealthCondition((Map<?, ?>) conditions.get("health"), entity)) {
                    return false;
                }
            }

            if (conditions.containsKey("difficulty")) {
                if (!checkDifficultyCondition(conditions.get("difficulty"), level)) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            LOGGER.error("Error checking conditions for entity {}", entity, e);
            return false;
        }
    }

    private static boolean checkBiomeCondition(Object biomeCondition, Level level, BlockPos pos) {
        if (biomeCondition instanceof String) {
            String requiredBiome = (String) biomeCondition;
            Biome currentBiome = level.getBiome(pos).value();
            ResourceLocation currentBiomeId = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getKey(currentBiome);
            return currentBiomeId != null && currentBiomeId.toString().equals(requiredBiome);

        } else if (biomeCondition instanceof List) {
            List<String> allowedBiomes = (List<String>) biomeCondition;
            Biome currentBiome = level.getBiome(pos).value();
            ResourceLocation currentBiomeId = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getKey(currentBiome);
            return currentBiomeId != null && allowedBiomes.contains(currentBiomeId.toString());
        }

        return true;
    }

    private static boolean checkDimensionCondition(Object dimensionCondition, Level level) {
        ResourceLocation dimensionId = level.dimension().location();

        if (dimensionCondition instanceof String) {
            return dimensionId.toString().equals(dimensionCondition);

        } else if (dimensionCondition instanceof List) {
            List<String> allowedDimensions = (List<String>) dimensionCondition;
            return allowedDimensions.contains(dimensionId.toString());
        }

        return true;
    }

    private static boolean checkWeatherCondition(Object weatherCondition, Level level) {
        if (weatherCondition instanceof String) {
            String weather = (String) weatherCondition;
            switch (weather.toLowerCase()) {
                case "clear": return !level.isRaining() && !level.isThundering();
                case "rain": return level.isRaining() && !level.isThundering();
                case "thunder": return level.isThundering();
                default: return true;
            }

        } else if (weatherCondition instanceof Map) {
            Map<?, ?> weatherMap = (Map<?, ?>) weatherCondition;
            if (weatherMap.containsKey("raining")) {
                boolean shouldRain = (Boolean) weatherMap.get("raining");
                if (level.isRaining() != shouldRain) return false;
            }
            if (weatherMap.containsKey("thundering")) {
                boolean shouldThunder = (Boolean) weatherMap.get("thundering");
                if (level.isThundering() != shouldThunder) return false;
            }
        }

        return true;
    }

    private static boolean checkTimeCondition(Object timeCondition, Level level) {
        long dayTime = level.getDayTime() % 24000;

        if (timeCondition instanceof String) {
            String timeOfDay = (String) timeCondition;
            switch (timeOfDay.toLowerCase()) {
                case "day": return dayTime >= 0 && dayTime < 12000;
                case "night": return dayTime >= 12000 && dayTime < 24000;
                case "sunrise": return dayTime >= 0 && dayTime < 2000;
                case "sunset": return dayTime >= 12000 && dayTime < 14000;
                default: return true;
            }

        } else if (timeCondition instanceof Map) {
            Map<?, ?> timeMap = (Map<?, ?>) timeCondition;
            if (timeMap.containsKey("min")) {
                long minTime = ((Number) timeMap.get("min")).longValue();
                if (dayTime < minTime) return false;
            }
            if (timeMap.containsKey("max")) {
                long maxTime = ((Number) timeMap.get("max")).longValue();
                if (dayTime > maxTime) return false;
            }
        }

        return true;
    }

    private static boolean checkMoonPhaseCondition(Object moonCondition, Level level) {
        int moonPhase = (int) (level.getDayTime() / 24000 % 8);

        if (moonCondition instanceof Number) {
            int requiredPhase = ((Number) moonCondition).intValue();
            return moonPhase == requiredPhase;

        } else if (moonCondition instanceof List) {
            List<Number> allowedPhases = (List<Number>) moonCondition;
            return allowedPhases.stream().anyMatch(phase -> phase.intValue() == moonPhase);

        } else if (moonCondition instanceof Map) {
            Map<?, ?> moonMap = (Map<?, ?>) moonCondition;
            if (moonMap.containsKey("full_moon") && (Boolean) moonMap.get("full_moon")) {
                return moonPhase == 0;
            }
            if (moonMap.containsKey("new_moon") && (Boolean) moonMap.get("new_moon")) {
                return moonPhase == 4;
            }
        }

        return true;
    }

    private static boolean checkElementConditions(List<?> elementConditions, LivingEntity entity) {
        for (Object conditionObj : elementConditions) {
            if (conditionObj instanceof Map) {
                Map<?, ?> condition = (Map<?, ?>) conditionObj;
                String elementId = (String) condition.get("element");
                boolean required = !condition.containsKey("required") || (Boolean) condition.get("required");

                if (elementId != null) {
                    boolean hasElement = ElementSystemAPI.getElementSystem().hasElement(entity, elementId);

                    if (required && !hasElement) {
                        return false;
                    }

                    if (condition.containsKey("min_value")) {
                        double minValue = ((Number) condition.get("min_value")).doubleValue();
                        double currentValue = ElementSystemAPI.getElementSystem().getElementValue(entity, elementId);
                        if (currentValue < minValue) return false;
                    }

                    if (condition.containsKey("max_value")) {
                        double maxValue = ((Number) condition.get("max_value")).doubleValue();
                        double currentValue = ElementSystemAPI.getElementSystem().getElementValue(entity, elementId);
                        if (currentValue > maxValue) return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean checkHealthCondition(Map<?, ?> healthCondition, LivingEntity entity) {
        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();

        if (healthCondition.containsKey("min")) {
            float minHealth = ((Number) healthCondition.get("min")).floatValue();
            if (health < minHealth) return false;
        }

        if (healthCondition.containsKey("max")) {
            float maxHealthLimit = ((Number) healthCondition.get("max")).floatValue();
            if (health > maxHealthLimit) return false;
        }

        if (healthCondition.containsKey("percentage")) {
            float percentage = ((Number) healthCondition.get("percentage")).floatValue();
            float requiredHealth = maxHealth * percentage;
            if (health < requiredHealth) return false;
        }

        return true;
    }

    private static boolean checkDifficultyCondition(Object difficultyCondition, Level level) {
        net.minecraft.world.Difficulty difficulty = level.getDifficulty();

        if (difficultyCondition instanceof String) {
            String requiredDifficulty = (String) difficultyCondition;
            return difficulty.name().equalsIgnoreCase(requiredDifficulty);
        }

        return true;
    }

    public static Component getConditionDescription(Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return Component.translatable("gui.element_endow.conditions.none");
        }

        List<Component> conditionTexts = new ArrayList<>();

        try {
            if (conditions.containsKey("biome")) {
                Object biomeCond = conditions.get("biome");
                if (biomeCond instanceof String) {
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.biome", biomeCond));
                } else if (biomeCond instanceof List) {
                    String biomes = String.join(", ", (List<String>) biomeCond);
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.biomes", biomes));
                }
            }

            if (conditions.containsKey("dimension")) {
                Object dimCond = conditions.get("dimension");
                if (dimCond instanceof String) {
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.dimension", dimCond));
                } else if (dimCond instanceof List) {
                    String dimensions = String.join(", ", (List<String>) dimCond);
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.dimensions", dimensions));
                }
            }

            if (conditions.containsKey("weather")) {
                Object weatherCond = conditions.get("weather");
                if (weatherCond instanceof String) {
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.weather", weatherCond));
                } else if (weatherCond instanceof Map) {
                    Map<?, ?> weatherMap = (Map<?, ?>) weatherCond;
                    List<String> weatherParts = new ArrayList<>();
                    if (weatherMap.containsKey("raining")) {
                        weatherParts.add((Boolean) weatherMap.get("raining") ? "raining" : "clear");
                    }
                    if (weatherMap.containsKey("thundering")) {
                        weatherParts.add((Boolean) weatherMap.get("thundering") ? "thundering" : "not_thundering");
                    }
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.weather", String.join(", ", weatherParts)));
                }
            }

            if (conditions.containsKey("time")) {
                Object timeCond = conditions.get("time");
                if (timeCond instanceof String) {
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.time", timeCond));
                } else if (timeCond instanceof Map) {
                    Map<?, ?> timeMap = (Map<?, ?>) timeCond;
                    List<String> timeParts = new ArrayList<>();
                    if (timeMap.containsKey("min")) {
                        timeParts.add("min: " + timeMap.get("min"));
                    }
                    if (timeMap.containsKey("max")) {
                        timeParts.add("max: " + timeMap.get("max"));
                    }
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.time_range", String.join(", ", timeParts)));
                }
            }

            if (conditions.containsKey("moon_phase")) {
                Object moonCond = conditions.get("moon_phase");
                if (moonCond instanceof Number) {
                    int phase = ((Number) moonCond).intValue();
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.moon_phase", getMoonPhaseName(phase)));
                } else if (moonCond instanceof Map) {
                    Map<?, ?> moonMap = (Map<?, ?>) moonCond;
                    List<String> moonParts = new ArrayList<>();
                    if (moonMap.containsKey("full_moon") && (Boolean) moonMap.get("full_moon")) {
                        moonParts.add("full_moon");
                    }
                    if (moonMap.containsKey("new_moon") && (Boolean) moonMap.get("new_moon")) {
                        moonParts.add("new_moon");
                    }
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.moon_phases", String.join(", ", moonParts)));
                }
            }

            if (conditions.containsKey("health")) {
                Object healthCond = conditions.get("health");
                if (healthCond instanceof Map) {
                    Map<?, ?> healthMap = (Map<?, ?>) healthCond;
                    List<String> healthParts = new ArrayList<>();
                    if (healthMap.containsKey("min")) {
                        healthParts.add("min: " + healthMap.get("min"));
                    }
                    if (healthMap.containsKey("max")) {
                        healthParts.add("max: " + healthMap.get("max"));
                    }
                    if (healthMap.containsKey("percentage")) {
                        healthParts.add("percentage: " + healthMap.get("percentage"));
                    }
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.health", String.join(", ", healthParts)));
                }
            }

            if (conditions.containsKey("difficulty")) {
                Object diffCond = conditions.get("difficulty");
                if (diffCond instanceof String) {
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.difficulty", diffCond));
                }
            }

            if (conditions.containsKey("elements")) {
                List<?> elementConds = (List<?>) conditions.get("elements");
                if (!elementConds.isEmpty()) {
                    conditionTexts.add(Component.translatable("gui.element_endow.conditions.elements", elementConds.size()));
                    for (Object condObj : elementConds) {
                        if (condObj instanceof Map) {
                            Map<?, ?> elementCond = (Map<?, ?>) condObj;
                            String elementId = (String) elementCond.get("element");
                            if (elementId != null) {
                                List<String> elementParts = new ArrayList<>();
                                elementParts.add(elementId);
                                if (elementCond.containsKey("min_value")) {
                                    elementParts.add("min: " + elementCond.get("min_value"));
                                }
                                if (elementCond.containsKey("max_value")) {
                                    elementParts.add("max: " + elementCond.get("max_value"));
                                }
                                conditionTexts.add(Component.literal("    - " + String.join(", ", elementParts)));
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            conditionTexts.add(Component.translatable("gui.element_endow.conditions.error"));
        }

        if (conditionTexts.isEmpty()) {
            return Component.translatable("gui.element_endow.conditions.none");
        }

        MutableComponent result = Component.empty();
        for (int i = 0; i < conditionTexts.size(); i++) {
            if (i > 0) {
                result.append(Component.literal(" | "));
            }
            result.append(conditionTexts.get(i));
        }

        return result;
    }

    private static String getMoonPhaseName(int phase) {
        switch (phase) {
            case 0: return "Full Moon";
            case 1: return "Waning Gibbous";
            case 2: return "Last Quarter";
            case 3: return "Waning Crescent";
            case 4: return "New Moon";
            case 5: return "Waxing Crescent";
            case 6: return "First Quarter";
            case 7: return "Waxing Gibbous";
            default: return "Unknown";
        }
    }
}