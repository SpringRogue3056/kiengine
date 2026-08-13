package com.ki.engine.script;

/**
 * 脚本管理器 - 融合 Pandora 的 Nashorn JS 引擎
 * 支持自定义事件/条件/动作的 JS 脚本
 */
public interface ScriptManager {
    void loadScript(String name, String code);
    Object execute(String scriptName, java.util.Map<String, Object> context);
    boolean evaluateCondition(String condition, java.util.Map<String, Object> context);
    void reload();
}
