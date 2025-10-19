package com.element_endow.api.config;

//反应配置接口
public interface IReactionConfig {
    void reload();
    String getConfigPath();
    void setConfigPath(String path);
}