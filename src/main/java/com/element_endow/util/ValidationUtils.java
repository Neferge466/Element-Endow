package com.element_endow.util;

import com.element_endow.ElementEndow;
import org.apache.logging.log4j.Logger;

public class ValidationUtils {
    private static final Logger LOGGER = ElementEndow.LOGGER;

    public static boolean isValidElementId(String elementId) {
        if (elementId == null || elementId.trim().isEmpty()) {
            LOGGER.warn("Element ID cannot be null or empty");
            return false;
        }

        String[] parts = elementId.split(":");
        if (parts.length != 2) {
            LOGGER.warn("Invalid element ID format: {}. Expected format: modid:element_name", elementId);
            return false;
        }

        if (parts[0].isEmpty() || parts[1].isEmpty()) {
            LOGGER.warn("Invalid element ID: {}. Both modid and element_name must be non-empty", elementId);
            return false;
        }

        return true;
    }

    public static boolean isValidAttributeValue(double value, double min, double max) {
        if (value < min || value > max) {
            LOGGER.warn("Value {} is outside valid range [{}, {}]", value, min, max);
            return false;
        }
        return true;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static String sanitizeElementId(String elementId) {
        if (elementId == null) return null;
        return elementId.trim().toLowerCase();
    }
}