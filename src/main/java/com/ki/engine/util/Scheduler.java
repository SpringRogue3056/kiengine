package com.ki.engine.util;

import com.ki.engine.core.KiEnginePlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

/**
 * 统一调度器 - 替代三个插件各自的定时任务系统
 */
public class Scheduler {
    private final KiEnginePlugin plugin;

    public Scheduler(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    public BukkitTask run(Runnable task) {
        return Bukkit.getScheduler().runTask(plugin, task);
    }

    public BukkitTask runLater(Runnable task, long ticks) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }

    public BukkitTask runTimer(Runnable task, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
    }

    public BukkitTask runAsync(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public BukkitTask runAsyncTimer(Runnable task, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
    }

    public void cancelAll() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
