package com.element_endow.api.config;

import java.util.List;

//元素配置接口
public interface IElementConfig {
    List<String> getElements();
    void reload();
    void addElement(String elementId);
    void removeElement(String elementId);
    boolean isElementEnabled(String elementId);
}