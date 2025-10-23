package com.element_endow.api;

public class ElementSystemAPI {
    private static com.element_endow.api.IElementSystem elementSystem;

    public static void initialize(com.element_endow.api.IElementSystem system) {
        elementSystem = system;
    }

    public static com.element_endow.api.IElementSystem getElementSystem() {
        if (elementSystem == null) {
            throw new IllegalStateException("ElementSystem not initialized");
        }
        return elementSystem;
    }
}