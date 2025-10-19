package com.element_endow.config;

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
                ElementEndow.LOGGER.info("Created empty element config file. No elements will be registered until configured.");
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
                ElementEndow.LOGGER.info("Loaded {} elements from config file", ELEMENTS.size());
            } else {
                ElementEndow.LOGGER.info("No elements configured in config file. Add elements to enable the mod's functionality.");
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
                writer.write("# \n");
                writer.write("# This file defines the elements that will be registered as attributes.\n");
                writer.write("# \n");
                writer.write("# Format: modid:element_name\n");
                writer.write("# \n");
                writer.write("# Examples:\n");
                writer.write("#   element_endow:fire\n");
                writer.write("#   element_endow:water\n");
                writer.write("#   element_endow:earth\n");
                writer.write("#   element_endow:air\n");
                writer.write("#   element_endow:lightning\n");
                writer.write("#   element_endow:ice\n");
                writer.write("#   element_endow:nature\n");
                writer.write("#   element_endow:metal\n");
                writer.write("#   element_endow:light\n");
                writer.write("#   element_endow:dark\n");
                writer.write("#   element_endow:chaos\n");
                writer.write("#   element_endow:order\n");
                writer.write("#   element_endow:time\n");
                writer.write("#   element_endow:space\n");
                writer.write("#   element_endow:life\n");
                writer.write("#   element_endow:death\n");
                writer.write("# \n");
                writer.write("# Each element will be registered as an attribute with the name:\n");
                writer.write("#   attribute.element_endow.{element_name}\n");
                writer.write("# \n");
                writer.write("# The attribute will have a default value of 0.0 and a range of 0.0 to 1024.0\n");
                writer.write("# \n");
                writer.write("# Separate multiple elements with commas (no spaces around commas)\n");
                writer.write("# \n");
                writer.write("# To enable elements, add them to the line below in the format shown above:\n");
                writer.write("# elements=element_endow:fire,element_endow:water,element_endow:earth\n");
                writer.write("# \n");
                writer.write("# If no elements are specified below, no attributes will be registered.\n");
                writer.write("# This means the mod will not function until elements are added to this file.\n");
                writer.write("\n");
                writer.write("elements=\n");
            }

            ElementEndow.LOGGER.info("Created empty element config file with detailed instructions");
        } catch (Exception e) {
            ElementEndow.LOGGER.error("Failed to create empty config: {}", e.getMessage());
        }
    }

    public static List<String> getElements() {
        return new ArrayList<>(ELEMENTS);
    }
}