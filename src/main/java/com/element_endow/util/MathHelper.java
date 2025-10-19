package com.element_endow.util;

import java.util.List;

public class MathHelper {

    public static double calculateMultiplier(double base, double rate) {
        return base * rate;
    }

    public static double calculateWeightedAverage(List<Double> values, List<Double> weights) {
        if (values.isEmpty()) return 0.0;
        if (values.size() != weights.size()) {
            throw new IllegalArgumentException("Values and weights must have the same size");
        }

        double sum = 0.0;
        double weightSum = 0.0;

        for (int i = 0; i < values.size(); i++) {
            sum += values.get(i) * weights.get(i);
            weightSum += weights.get(i);
        }

        return weightSum > 0 ? sum / weightSum : 0.0;
    }

    public static double calculateStandardDeviation(List<Double> values) {
        if (values.isEmpty()) return 0.0;

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }

    public static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    public static double smoothstep(double edge0, double edge1, double x) {
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3 - 2 * t);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}