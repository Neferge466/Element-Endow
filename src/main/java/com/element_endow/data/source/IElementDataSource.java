package com.element_endow.data.source;

import java.util.Map;

/**
 * 元素数据源接口
 * 统一所有数据来源的访问方式
 */
public interface IElementDataSource<T> {

    /**
     * 获取数据源类型标识
     */
    String getSourceType();

    /**
     * 获取数据源优先级（数值越高优先级越高）
     */
    int getPriority();

    /**
     * 加载数据
     */
    Map<String, T> loadData();

    /**
     * 是否支持热重载
     */
    boolean supportsHotReload();

    /**
     * 重载回调
     */
    void onReload();

    /**
     * 获取数据源描述
     */
    default String getDescription() {
        return getSourceType() + " (Priority: " + getPriority() + ")";
    }

    /**
     * 检查数据源是否可用
     */
    default boolean isAvailable() {
        return true;
    }
}