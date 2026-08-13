package com.ki.engine.database;

import com.ki.engine.core.KiEnginePlugin;
import com.ki.engine.core.Manager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 数据库管理器 - 支持 SQLite 和 MySQL
 * 存储：玩家RPG数据、方块位置、自定义实体
 */
public class DatabaseManager implements Manager {

    private final KiEnginePlugin plugin;
    private HikariDataSource dataSource;
    private boolean useMySQL;

    public DatabaseManager(KiEnginePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        reload();
    }

    @Override
    public void reload() {
        close();
        ConfigurationSection dbConfig = plugin.getConfigManager().getConfig("database");
        if (dbConfig == null) dbConfig = createDefaultConfig();

        useMySQL = dbConfig.getBoolean("mysql.enabled", false);

        if (useMySQL) {
            initMySQL(dbConfig.getConfigurationSection("mysql"));
        } else {
            initSQLite();
        }
        createTables();
    }

    private ConfigurationSection createDefaultConfig() {
        // 返回默认空配置
        return new org.bukkit.configuration.MemoryConfiguration();
    }

    private void initMySQL(ConfigurationSection mysql) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + mysql.getString("host", "localhost") + ":" + mysql.getInt("port", 3306) + "/" + mysql.getString("database", "kiengine"));
        config.setUsername(mysql.getString("username", "root"));
        config.setPassword(mysql.getString("password", ""));
        config.setMaximumPoolSize(mysql.getInt("pool-size", 10));
        config.setConnectionTimeout(30000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        this.dataSource = new HikariDataSource(config);
        plugin.getLogger().info("[Database] Connected to MySQL");
    }

    private void initSQLite() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + new java.io.File(plugin.getDataFolder(), "kiengine.db").getAbsolutePath());
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(30000);
        this.dataSource = new HikariDataSource(config);
        plugin.getLogger().info("[Database] Connected to SQLite");
    }

    private void createTables() {
        executeSync("""
            CREATE TABLE IF NOT EXISTS player_rpg (
                uuid VARCHAR(36) PRIMARY KEY,
                skill VARCHAR(32) NOT NULL,
                level INT DEFAULT 0,
                exp INT DEFAULT 0,
                UNIQUE(uuid, skill)
            )
            """);

        executeSync("""
            CREATE TABLE IF NOT EXISTS custom_blocks (
                world VARCHAR(64) NOT NULL,
                x INT NOT NULL,
                y INT NOT NULL,
                z INT NOT NULL,
                block_id VARCHAR(64) NOT NULL,
                PRIMARY KEY(world, x, y, z)
            )
            """);

        executeSync("""
            CREATE TABLE IF NOT EXISTS custom_entities (
                uuid VARCHAR(36) PRIMARY KEY,
                mob_id VARCHAR(64) NOT NULL,
                world VARCHAR(64) NOT NULL,
                x DOUBLE NOT NULL,
                y DOUBLE NOT NULL,
                z DOUBLE NOT NULL
            )
            """);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void executeSync(String sql) {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().warning("[Database] SQL Error: " + e.getMessage());
        }
    }

    public CompletableFuture<Void> executeAsync(String sql) {
        return CompletableFuture.runAsync(() -> executeSync(sql));
    }

    // ===== RPG 数据操作 =====

    public void saveRpgData(UUID playerUuid, String skill, int level, int exp) {
        String sql = useMySQL
            ? "INSERT INTO player_rpg (uuid, skill, level, exp) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE level=?, exp=?"
            : "INSERT OR REPLACE INTO player_rpg (uuid, skill, level, exp) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, skill);
            ps.setInt(3, level);
            ps.setInt(4, exp);
            if (useMySQL) {
                ps.setInt(5, level);
                ps.setInt(6, exp);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[Database] Save RPG failed: " + e.getMessage());
        }
    }

    public int[] loadRpgData(UUID playerUuid, String skill) {
        String sql = "SELECT level, exp FROM player_rpg WHERE uuid=? AND skill=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, skill);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new int[]{rs.getInt("level"), rs.getInt("exp")};
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[Database] Load RPG failed: " + e.getMessage());
        }
        return new int[]{0, 0};
    }

    // ===== 方块位置操作 =====

    public void saveBlock(String world, int x, int y, int z, String blockId) {
        String sql = useMySQL
            ? "INSERT INTO custom_blocks (world, x, y, z, block_id) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE block_id=?"
            : "INSERT OR REPLACE INTO custom_blocks (world, x, y, z, block_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.setString(5, blockId);
            if (useMySQL) ps.setString(6, blockId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[Database] Save block failed: " + e.getMessage());
        }
    }

    public String loadBlock(String world, int x, int y, int z) {
        String sql = "SELECT block_id FROM custom_blocks WHERE world=? AND x=? AND y=? AND z=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("block_id");
        } catch (SQLException e) {
            plugin.getLogger().warning("[Database] Load block failed: " + e.getMessage());
        }
        return null;
    }

    public void deleteBlock(String world, int x, int y, int z) {
        String sql = "DELETE FROM custom_blocks WHERE world=? AND x=? AND y=? AND z=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[Database] Delete block failed: " + e.getMessage());
        }
    }

    // ===== 通用 SQL 操作（供 Addon 使用）=====

    public int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columns; i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }
        }
        return results;
    }

    @Override
    public void shutdown() {
        close();
    }

    private void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
