package com.element_endow.api;

/**
 * ElementEndow 系统 API 入口点
 * 提供全局访问元素系统的静态方法，采用单例模式设计。
 */
public class ElementSystemAPI {
    private static IElementSystem elementSystem;

    /**
     * 初始化元素系统
     */
    public static void initialize(IElementSystem system) {
        if (system == null) {
            throw new IllegalArgumentException("ElementSystem cannot be null");
        }
        elementSystem = system;
    }

    /**
     * 获取元素系统实例
     */
    public static IElementSystem getElementSystem() {
        if (elementSystem == null) {
            throw new IllegalStateException("ElementSystem not initialized. Call ElementSystemAPI.initialize() first.");
        }
        return elementSystem;
    }

    /**
     * 获取元素反应系统实例
     */
    public static IElementReactionSystem getReactionSystem() {
        return getElementSystem().getReactionSystem();
    }

    /**
     * 获取元素组合系统实例
     */
    public static IElementCombinationSystem getCombinationSystem() {
        return getElementSystem().getCombinationSystem();
    }

    /**
     * 获取元素挂载系统实例
     */
    public static IElementMountSystem getMountSystem() {
        return getElementSystem().getMountSystem();
    }

    /**
     * 重新加载所有数据
     */
    public static void reloadAllData() {
        getElementSystem().reloadData();
    }
}