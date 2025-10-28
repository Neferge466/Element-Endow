package com.element_endow.core;

import com.element_endow.ElementEndow;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ElementConfig {
    private final List<String> elements;
    private final File configFile;

    public ElementConfig() {
        this.elements = new ArrayList<>();
        this.configFile = new File("config/element_endow_elements.properties");
        load();
    }

    public List<String> getElements() {
        return new ArrayList<>(elements);
    }

    public void load() {
        elements.clear();
        try {
            if (!configFile.exists()) {
                createDefaultConfig();
                return;
            }

            Properties props = new Properties();
            try (FileReader reader = new FileReader(configFile)) {
                props.load(reader);
            }

            String elementsStr = props.getProperty("elements", "");
            if (!elementsStr.isEmpty()) {
                for (String element : elementsStr.split(",")) {
                    String trimmed = element.trim();
                    if (!trimmed.isEmpty()) {
                        elements.add(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            ElementEndow.LOGGER.error("Failed to load element config", e);
        }
    }

    public void save() {
        try {
            StringBuilder sb = new StringBuilder();
            for (String element : elements) {
                if (!sb.isEmpty()) sb.append(",");
                sb.append(element);
            }

            Properties props = new Properties();
            props.setProperty("elements", sb.toString());

            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                props.store(writer, "Element Endow Configuration");
            }
        } catch (Exception e) {
            ElementEndow.LOGGER.error("Failed to save element config", e);
        }
    }

    public void addElement(String elementId) {
        if (!elements.contains(elementId)) {
            elements.add(elementId);
            save();
        }
    }

    public void removeElement(String elementId) {
        if (elements.remove(elementId)) {
            save();
        }
    }

    private void createDefaultConfig() {
        try {
            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("# Element Endow Configuration\n");
                writer.write("# Format: modid:element_name\n");
                writer.write("elements=\n");
            }
        } catch (Exception e) {
            ElementEndow.LOGGER.error("Failed to create default config", e);
        }
    }
}