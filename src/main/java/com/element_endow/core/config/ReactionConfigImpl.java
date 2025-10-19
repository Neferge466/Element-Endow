package com.element_endow.core.config;

import com.element_endow.api.config.IReactionConfig;

public class ReactionConfigImpl implements IReactionConfig {
    private String configPath = "element_reactions";

    @Override
    public void reload() {
        //
    }

    @Override
    public String getConfigPath() {
        return configPath;
    }

    @Override
    public void setConfigPath(String path) {
        this.configPath = path;
    }
}