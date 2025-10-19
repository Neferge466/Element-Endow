package com.element_endow.core.config;

import com.element_endow.api.config.IElementConfig;
import com.element_endow.ElementEndow;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ElementConfigImpl implements IElementConfig {
    private final List<String> elements;
    private final File configFile;

    public ElementConfigImpl() {
        this.elements = new ArrayList<>();
        this.configFile = new File("config/element_endow_elements.properties");
        reload();
    }

    @Override
    public List<String> getElements() {
        return new ArrayList<>(elements);
    }

    @Override
    public void reload() {
        elements.clear();

        try {
            if (!configFile.exists()) {
                createDefaultConfig();
                ElementEndow.LOGGER.info("Created default element config file");
                return;
            }

            Properties props = new Properties();
            try (FileReader reader = new FileReader(configFile)) {
                props.load(reader);
            }

            String elementsStr = props.getProperty("elements", "");
            if (!elementsStr.isEmpty()) {
                String[] elementsArray = elementsStr.split(",");
                for (String element : elementsArray) {
                    String trimmed = element.trim();
                    if (!trimmed.isEmpty()) {
                        elements.add(trimmed);
                    }
                }
                ElementEndow.LOGGER.info("Loaded {} elements from config", elements.size());
            }
        } catch (Exception e) {
            ElementEndow.LOGGER.error("Failed to load element config: {}", e.getMessage());
        }
    }

    @Override
    public void addElement(String elementId) {
        if (!elements.contains(elementId)) {
            elements.add(elementId);
            saveConfig();
        }
    }

    @Override
    public void removeElement(String elementId) {
        if (elements.remove(elementId)) {
            saveConfig();
        }
    }

    @Override
    public boolean isElementEnabled(String elementId) {
        return elements.contains(elementId);
    }

    private void createDefaultConfig() {
        try {
            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("# Element Endow Configuration\n");
                writer.write("# Add elements in format: modid:element_name\n");
                writer.write("elements=element_endow:fire,element_endow:water,element_endow:earth\n");
            }
        } catch (Exception e) {
            ElementEndow.LOGGER.error("Failed to create default config: {}", e.getMessage());
        }
    }

    private void saveConfig() {
        try {
            StringBuilder sb = new StringBuilder();
            for (String element : elements) {
                if (sb.length() > 0) sb.append(",");
                sb.append(element);
            }

            Properties props = new Properties();
            props.setProperty("elements", sb.toString());

            try (FileWriter writer = new FileWriter(configFile)) {
                props.store(writer, "Element Endow Configuration");
            }
        } catch (Exception e) {
            ElementEndow.LOGGER.error("Failed to save element config: {}", e.getMessage());
        }
    }
}