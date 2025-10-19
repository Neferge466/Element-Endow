package com.element_endow.core.config;

import com.element_endow.ElementEndow;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SimpleElementConfig {
    private static final List<String> ELEMENTS = new ArrayList<>();
    private static final File CONFIG_FILE = new File("config/element_endow_elements.properties");

    static {
        loadElements();
    }

    private static void loadElements() {
        try {
            if (!CONFIG_FILE.exists()) {
                createEmptyConfig();
                return;
            }

            Properties props = new Properties();
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                props.load(reader);
            }

            String elementsStr = props.getProperty("elements", "");
            if (!elementsStr.isEmpty()) {
                ELEMENTS.clear();
                String[] elementsArray = elementsStr.split(",");
                for (String element : elementsArray) {
                    String trimmed = element.trim();
                    if (!trimmed.isEmpty()) {
                        ELEMENTS.add(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            ElementEndow.LOGGER.error("Failed to load element config: {}", e.getMessage());
        }
    }

    private static void createEmptyConfig() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                writer.write("# Element Endow Configuration\n");
                writer.write("elements=\n");
            }
        } catch (Exception e) {
            ElementEndow.LOGGER.error("Failed to create empty config: {}", e.getMessage());
        }
    }

    public static List<String> getElements() {
        return new ArrayList<>(ELEMENTS);
    }
}