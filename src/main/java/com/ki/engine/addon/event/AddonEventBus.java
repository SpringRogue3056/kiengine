package com.ki.engine.addon.event;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Addon 事件总线 - 允许附属插件监听 KiEngine 内部事件
 * 支持：同步/异步监听、优先级、条件过滤、一次性监听
 */
public class AddonEventBus implements Listener {

    private final KiEnginePlugin plugin;
    private final Map<Class<? extends Event>, List<RegisteredListener>> listeners = new ConcurrentHashMap<>();
    private final Map<String, List<RegisteredListener>> addonListeners = new ConcurrentHashMap<>();

    public AddonEventBus(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 注册事件监听
     * @param addonId 附属ID
     * @param eventClass 事件类型
     * @param priority 优先级
     * @param handler 处理函数
     */
    public <T extends Event> void subscribe(String addonId, Class<T> eventClass, EventPriority priority, EventHandler<T> handler) {
        RegisteredListener reg = new RegisteredListener(addonId, priority, handler, false);
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(reg);
        addonListeners.computeIfAbsent(addonId, k -> new ArrayList<>()).add(reg);
        sortListeners(eventClass);
    }

    /**
     * 注册一次性监听（触发后自动取消）
     */
    public <T extends Event> void subscribeOnce(String addonId, Class<T> eventClass, EventHandler<T> handler) {
        RegisteredListener reg = new RegisteredListener(addonId, EventPriority.NORMAL, handler, true);
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(reg);
        addonListeners.computeIfAbsent(addonId, k -> new ArrayList<>()).add(reg);
    }

    /**
     * 带条件过滤的监听
     */
    public <T extends Event> void subscribeFiltered(String addonId, Class<T> eventClass, EventPriority priority, EventFilter<T> filter, EventHandler<T> handler) {
        EventHandler<T> filteredHandler = (event) -> {
            if (filter.test(event)) {
                handler.handle(event);
            }
        };
        subscribe(addonId, eventClass, priority, filteredHandler);
    }

    /**
     * 发布事件到所有监听器
     */
    @SuppressWarnings("unchecked")
    public void publish(Event event) {
        List<RegisteredListener> regs = listeners.get(event.getClass());
        if (regs == null) return;

        List<RegisteredListener> toRemove = new ArrayList<>();
        for (RegisteredListener reg : regs) {
            try {
                reg.invoke(event);
                if (reg.once) toRemove.add(reg);
            } catch (Exception e) {
                plugin.getLogger().warning("[AddonEventBus] Handler error from " + reg.addonId + ": " + e.getMessage());
            }
        }

        // 清理一次性监听
        if (!toRemove.isEmpty()) {
            regs.removeAll(toRemove);
            for (RegisteredListener reg : toRemove) {
                List<RegisteredListener> addonList = addonListeners.get(reg.addonId);
                if (addonList != null) addonList.remove(reg);
            }
        }
    }

    /**
     * 取消附属的所有事件监听
     */
    public void unsubscribeAll(String addonId) {
        List<RegisteredListener> regs = addonListeners.remove(addonId);
        if (regs == null) return;
        for (List<RegisteredListener> list : listeners.values()) {
            list.removeAll(regs);
        }
    }

    private void sortListeners(Class<? extends Event> eventClass) {
        List<RegisteredListener> list = listeners.get(eventClass);
        if (list != null) {
            list.sort(Comparator.comparingInt(r -> r.priority.getSlot()));
        }
    }

    // ========== 便捷订阅方法 ==========

    /** 订阅物品使用事件 */
    public void onItemUse(String addonId, EventHandler<KiAddonItemUseEvent> handler) {
        subscribe(addonId, KiAddonItemUseEvent.class, EventPriority.NORMAL, handler);
    }

    /** 订阅方块交互事件 */
    public void onBlockInteract(String addonId, EventHandler<KiAddonBlockInteractEvent> handler) {
        subscribe(addonId, KiAddonBlockInteractEvent.class, EventPriority.NORMAL, handler);
    }

    /** 订阅生物生成事件 */
    public void onMobSpawn(String addonId, EventHandler<KiAddonMobSpawnEvent> handler) {
        subscribe(addonId, KiAddonMobSpawnEvent.class, EventPriority.NORMAL, handler);
    }

    /** 订阅技能施放事件 */
    public void onSkillCast(String addonId, EventHandler<KiAddonSkillCastEvent> handler) {
        subscribe(addonId, KiAddonSkillCastEvent.class, EventPriority.NORMAL, handler);
    }

    // ========== 内部类 ==========

    @FunctionalInterface
    public interface EventHandler<T extends Event> {
        void handle(T event);
    }

    @FunctionalInterface
    public interface EventFilter<T extends Event> {
        boolean test(T event);
    }

    private record RegisteredListener(String addonId, EventPriority priority, EventHandler<?> handler, boolean once) {
        @SuppressWarnings("unchecked")
        public <T extends Event> void invoke(T event) {
            ((EventHandler<T>) handler).handle(event);
        }
    }
}
