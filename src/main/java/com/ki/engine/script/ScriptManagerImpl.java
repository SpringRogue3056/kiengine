package com.ki.engine.script;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.Bukkit;

import javax.script.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nashorn JS 脚本引擎实现。
 * 附属插件可通过 JS 脚本自定义事件/条件/动作逻辑。
 */
public class ScriptManagerImpl implements ScriptManager {

    private final KiEnginePlugin plugin;
    private final ScriptEngine engine;
    private final Map<String, String> scripts = new ConcurrentHashMap<>();
    private final Bindings globalBindings;

    public ScriptManagerImpl(KiEnginePlugin plugin) {
        this.plugin = plugin;
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("nashorn");
        if (this.engine == null) {
            plugin.getLogger().warning("[ScriptManager] Nashorn engine not available (Java 15+). JS scripts disabled.");
            this.globalBindings = null;
        } else {
            this.globalBindings = engine.createBindings();
            setupGlobals();
        }
    }

    private void setupGlobals() {
        globalBindings.put("plugin", plugin);
        globalBindings.put("server", plugin.getServer());
        globalBindings.put("logger", plugin.getLogger());
        globalBindings.put("Bukkit", Bukkit.class);
        globalBindings.put("KiEngineAPI", com.ki.engine.api.KiEngineAPI.class);
    }

    @Override
    public void loadScript(String name, String code) {
        scripts.put(name, code);
        if (engine != null) {
            try {
                engine.eval(code, globalBindings);
                plugin.getLogger().info("[ScriptManager] Loaded script: " + name);
            } catch (ScriptException e) {
                plugin.getLogger().warning("[ScriptManager] Script error in " + name + ": " + e.getMessage());
            }
        }
    }

    @Override
    public Object execute(String scriptName, Map<String, Object> context) {
        if (engine == null) return null;
        String code = scripts.get(scriptName);
        if (code == null) return null;

        try {
            Bindings local = engine.createBindings();
            local.putAll(globalBindings);
            if (context != null) local.putAll(context);
            return engine.eval(code, local);
        } catch (ScriptException e) {
            plugin.getLogger().warning("[ScriptManager] Execute error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean evaluateCondition(String condition, Map<String, Object> context) {
        if (engine == null) return false;
        try {
            Bindings local = engine.createBindings();
            local.putAll(globalBindings);
            if (context != null) local.putAll(context);
            Object result = engine.eval(condition, local);
            return Boolean.TRUE.equals(result);
        } catch (ScriptException e) {
            return false;
        }
    }

    @Override
    public void reload() {
        if (engine == null) return;
        scripts.clear();
        plugin.getLogger().info("[ScriptManager] Cleared all scripts");
    }
}
