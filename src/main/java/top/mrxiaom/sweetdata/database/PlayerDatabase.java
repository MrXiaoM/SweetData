package top.mrxiaom.sweetdata.database;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.pluginbase.utils.Bytes;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweetdata.SweetData;
import top.mrxiaom.sweetdata.database.entry.GlobalCache;
import top.mrxiaom.sweetdata.database.entry.PlayerCache;
import top.mrxiaom.sweetdata.func.AbstractPluginHolder;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static top.mrxiaom.sweetdata.SweetData.limit;

public class PlayerDatabase extends AbstractPluginHolder implements IDatabase, Listener {
    private String TABLE_PLAYERS, TABLE_GLOBAL;
    private final Map<Player, PlayerCache> cacheMap = new HashMap<>();
    private final GlobalCache globalCache = new GlobalCache();
    public PlayerDatabase(SweetData plugin) {
        super(plugin);
        registerEvents();
        registerBungee();
        register();
        long interval = 30 * 20L; // 每隔 30 秒检查一次，缓存是否需要提交，以及刷新全局数值缓存
        plugin.getScheduler().runTaskTimerAsync(() -> {
            for (PlayerCache cache : cacheMap.values()) {
                if (cache.shouldSubmitCache()) {
                    Player player = cache.getPlayer();
                    List<Pair<String, String>> pairs = cache.getToSubmitPairs();
                    submitCache(player, pairs);
                }
            }
            globalRefresh();
        }, interval, interval);
    }

    @Override
    public void receiveBungee(String subChannel, DataInputStream in) throws IOException {
        if (subChannel.equals("RefreshGlobalCache")) {
            // 因为有可能出现无人的子服一进玩家就一股脑地接收大量 BungeeCord 消息的情况
            // 请不要在这里访问数据库
            String key = in.readUTF();
            if (in.readBoolean()) {
                String value = in.readUTF();
                globalCache.put(key, value);
            } else {
                globalCache.remove(key);
            }
        }
    }

    public void sendRequireGlobalCacheUpdate(@Nullable Player player, String key, @Nullable String value) {
        if (player == null) return;
        player.sendPluginMessage(plugin, "BungeeCord", Bytes.build(out -> {
            out.writeUTF(key);
            boolean setCache = value != null;
            out.writeBoolean(setCache);
            if (setCache) {
                out.writeUTF(value);
            }
        }, /*subChannel:*/"Forward", /*arguments:*/"ALL", "RefreshGlobalCache"));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        cacheMap.remove(player);
        refreshCache(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        PlayerCache cache = cacheMap.remove(player);
        if (cache != null) {
            List<Pair<String, String>> pairs = cache.getToSubmitPairs();
            if (!pairs.isEmpty()) {
                submitCache(player, pairs);
            }
        }
    }

    @Override
    public void reload(Connection conn, String prefix) throws SQLException {
        TABLE_PLAYERS = prefix + "players";
        TABLE_GLOBAL = prefix + "global";
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_PLAYERS + "`(" +
                        "`player` VARCHAR(48)," + // 玩家名称 或 UUID
                        "`key` VARCHAR(64)," +    // 键
                        "`value` LONGTEXT," +     // 值
                        "PRIMARY KEY(`player`, `key`)" +
                ");"
        )) {
            ps.execute();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_GLOBAL + "`(" +
                        "`key` VARCHAR(64) PRIMARY KEY," + // 键
                        "`value` LONGTEXT" +               // 值
                ");"
        )) {
            ps.execute();
        }
    }

    public GlobalCache getGlobalCache() {
        return globalCache;
    }

    public void submitCache(OfflinePlayer player, List<Pair<String, String>> pairs) {
        try (Connection conn = plugin.getConnection()) {
            String p = plugin.databaseKey(player);
            set(conn, p, pairs);
        } catch (SQLException e) {
            warn(e);
        }
    }

    @Nullable
    public PlayerCache getCacheOrNull(@Nullable OfflinePlayer p) {
        Player player = (p != null && p.isOnline()) ? p.getPlayer() : null;
        if (player != null) {
            return getCache(player);
        } else {
            return null;
        }
    }

    @NotNull
    public PlayerCache getCache(Player player) {
        PlayerCache cache = cacheMap.get(player);
        if (cache != null) {
            return cache;
        } else {
            return refreshCache(player);
        }
    }

    @NotNull
    public PlayerCache refreshCache(Player player) {
        PlayerCache cache = cacheMap.computeIfAbsent(player, id -> new PlayerCache(player));
        try (Connection conn = plugin.getConnection()) {
            String p = plugin.databaseKey(player);
            List<Pair<String, String>> pairs = get(conn, p);
            cache.putAll(pairs);
        } catch (SQLException e) {
            warn(e);
        }
        return cache;
    }

    @Nullable
    @Deprecated
    public Integer intAdd(@NotNull OfflinePlayer player, @NotNull String key, int toAdd) {
        return playerIntAdd(player, key, toAdd, false, null, null);
    }

    @Nullable
    public Integer playerIntAdd(@NotNull OfflinePlayer player, @NotNull String key, int toAdd, @Nullable Integer min, @Nullable Integer max) {
        return playerIntAdd(player, key, toAdd, false, min, max);
    }

    @Nullable
    @Deprecated
    @Contract("_,_,_,true->!null")
    public Integer intAdd(@NotNull OfflinePlayer player, @NotNull String key, int toAdd, boolean ignoreNonInteger) {
        return playerIntAdd(player, key, toAdd, ignoreNonInteger, null, null);
    }

    @Nullable
    @Contract("_,_,_,true,_,_->!null")
    public Integer playerIntAdd(@NotNull OfflinePlayer player, @NotNull String key, int toAdd, boolean ignoreNonInteger, @Nullable Integer min, @Nullable Integer max) {
        try (Connection conn = plugin.getConnection()) {
            String p = plugin.databaseKey(player);
            Integer value = get(conn, p, key).flatMap(Util::parseInt).orElse(null);
            if (value != null) {
                int finalValue = limit(value + toAdd, min, max);
                set(conn, p, key, String.valueOf(finalValue));
                return finalValue;
            } else if (ignoreNonInteger) {
                int finalValue = limit(toAdd, min, max);
                set(conn, p, key, String.valueOf(finalValue));
                return finalValue;
            }
        } catch (SQLException e) {
            warn(e);
        }
        if (ignoreNonInteger) {
            return 0;
        } else {
            return null;
        }
    }

    public Optional<String> get(@NotNull OfflinePlayer player, @NotNull String key) {
        try (Connection conn = plugin.getConnection()) {
            String p = plugin.databaseKey(player);
            return get(conn, p, key);
        } catch (SQLException e) {
            warn(e);
        }
        return Optional.empty();
    }

    public void set(@NotNull OfflinePlayer player, @NotNull String key, @Nullable String value) {
        if (value == null) {
            remove(player, key);
            return;
        }
        try (Connection conn = plugin.getConnection()) {
            String p = plugin.databaseKey(player);
            set(conn, p, key, value);
        } catch (SQLException e) {
            warn(e);
        }
    }

    public void remove(@NotNull OfflinePlayer player, @NotNull String key) {
        try (Connection conn = plugin.getConnection()) {
            PlayerCache cache = getCacheOrNull(player);
            if (cache != null) {
                cache.remove(key);
            }
            String p = plugin.databaseKey(player);
            remove(conn, p, key);
        } catch (SQLException e) {
            warn(e);
        }
    }

    @Nullable
    public Integer globalIntAdd(@NotNull String key, int toAdd) {
        return globalIntAdd(key, toAdd, false, null, null);
    }

    @Nullable
    public Integer globalIntAdd(@NotNull String key, int toAdd, @Nullable Integer min, @Nullable Integer max) {
        return globalIntAdd(key, toAdd, false, min, max);
    }

    @Nullable
    @Contract("_,_,true->!null")
    public Integer globalIntAdd(@NotNull String key, int toAdd, boolean ignoreNonInteger) {
        return globalIntAdd(key, toAdd, ignoreNonInteger, null, null);
    }

    @Nullable
    @Contract("_,_,true,_,_->!null")
    public Integer globalIntAdd(@NotNull String key, int toAdd, boolean ignoreNonInteger, @Nullable Integer min, @Nullable Integer max) {
        try (Connection conn = plugin.getConnection()) {
            Integer value = globalGet(conn, key).flatMap(Util::parseInt).orElse(null);
            if (value != null) {
                int finalValue = limit(value + toAdd, min, max);
                String str = String.valueOf(finalValue);
                globalCache.put(key, str);
                globalSet(conn, key, str);
                return finalValue;
            } else if (ignoreNonInteger) {
                int finalValue = limit(toAdd, min, max);
                String str = String.valueOf(finalValue);
                globalCache.put(key, str);
                globalSet(conn, key, str);
                return toAdd;
            }
        } catch (SQLException e) {
            warn(e);
        }
        if (ignoreNonInteger) {
            return 0;
        } else {
            return null;
        }
    }

    public Optional<String> globalGet(@NotNull String key) {
        try (Connection conn = plugin.getConnection()) {
            return globalGet(conn, key);
        } catch (SQLException e) {
            warn(e);
        }
        return Optional.empty();
    }

    public void globalSet(@NotNull String key, @Nullable String value) {
        if (value == null) {
            globalRemove(key);
            return;
        }
        try (Connection conn = plugin.getConnection()) {
            globalCache.put(key, value);
            globalSet(conn, key, value);
        } catch (SQLException e) {
            warn(e);
        }
    }

    public void globalRemove(@NotNull String key) {
        try (Connection conn = plugin.getConnection()) {
            globalCache.remove(key);
            globalRemove(conn, key);
        } catch (SQLException e) {
            warn(e);
        }
    }

    @NotNull
    private Optional<String> get(@NotNull Connection conn, @NotNull String player, @NotNull String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT `value` FROM `" + TABLE_PLAYERS + "` WHERE `player=`? AND `key`=?;"
        )) {
            ps.setString(1, player);
            ps.setString(2, key);
            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    String value = result.getString("value");
                    return Optional.of(value);
                }
            }
        }
        return Optional.empty();
    }

    @NotNull
    private List<Pair<String, String>> get(@NotNull Connection conn, @NotNull String player) throws SQLException {
        List<Pair<String, String>> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM `" + TABLE_PLAYERS + "` WHERE `player`=?;"
        )) {
            ps.setString(1, player);
            try (ResultSet result = ps.executeQuery()) {
                while (result.next()) {
                    String key = result.getString("key");
                    String value = result.getString("value");
                    list.add(Pair.of(key, value));
                }
            }
        }
        return list;
    }

    private void set(@NotNull Connection conn, @NotNull String player, @NotNull String key, @NotNull String value) throws SQLException {
        String statement;
        boolean mySQL = plugin.options.database().isMySQL();
        if (mySQL) {
            statement = "INSERT INTO `" + TABLE_PLAYERS + "`(`player`,`key`,`value`) VALUES(?, ?, ?) on duplicate key update `value`=?;";
        } else {
            statement = "INSERT OR REPLACE INTO `" + TABLE_PLAYERS + "`(`player`,`key`,`value`) VALUES(?, ?, ?);";
        }
        try (PreparedStatement ps = conn.prepareStatement(statement)) {
            ps.setString(1, player);
            ps.setString(2, key);
            ps.setString(3, value);
            if (mySQL) {
                ps.setString(4, value);
            }
            ps.execute();
        }
    }

    private void set(@NotNull Connection conn, @NotNull String player, @NotNull List<Pair<String, String>> pairs) throws SQLException {
        String statement;
        boolean mySQL = plugin.options.database().isMySQL();
        if (mySQL) {
            statement = "INSERT INTO `" + TABLE_PLAYERS + "`(`player`,`key`,`value`) VALUES(?, ?, ?) on duplicate key update `value`=?;";
        } else {
            statement = "INSERT OR REPLACE INTO `" + TABLE_PLAYERS + "`(`player`,`key`,`value`) VALUES(?, ?, ?);";
        }
        try (PreparedStatement ps = conn.prepareStatement(statement)) {
            for (Pair<String, String> pair : pairs) {
                ps.setString(1, player);
                ps.setString(2, pair.key());
                ps.setString(3, pair.value());
                if (mySQL) {
                    ps.setString(4, pair.value());
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void remove(@NotNull Connection conn, @NotNull String player, @NotNull String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM `" + TABLE_PLAYERS + "` WHERE `player`=? AND `key`=?;"
        )) {
            ps.setString(1, player);
            ps.setString(2, key);
            ps.execute();
        }
    }

    private void globalRefresh() {
        try (Connection conn = plugin.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM `" + TABLE_GLOBAL + "`;"
             )) {
            try (ResultSet result = ps.executeQuery()) {
                while (result.next()) {
                    String key = result.getString("key");
                    String value = result.getString("value");
                    globalCache.put(key, value);
                }
            }
        } catch (SQLException e) {
            warn(e);
        }
    }

    @NotNull
    private Optional<String> globalGet(@NotNull Connection conn, @NotNull String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT `value` FROM `" + TABLE_GLOBAL + "` WHERE `key`=?;"
        )) {
            ps.setString(1, key);
            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    String value = result.getString("value");
                    return Optional.of(value);
                }
            }
        }
        return Optional.empty();
    }

    private void globalSet(@NotNull Connection conn, @NotNull String key, @NotNull String value) throws SQLException {
        String statement;
        boolean mySQL = plugin.options.database().isMySQL();
        if (mySQL) {
            statement = "INSERT INTO `" + TABLE_GLOBAL + "`(`key`,`value`) VALUES(?, ?) on duplicate key update `value`=?;";
        } else {
            statement = "INSERT OR REPLACE INTO `" + TABLE_GLOBAL + "`(`key`,`value`) VALUES(?, ?);";
        }
        try (PreparedStatement ps = conn.prepareStatement(statement)) {
            ps.setString(1, key);
            ps.setString(2, value);
            if (mySQL) {
                ps.setString(3, value);
            }
            ps.execute();
        }
    }

    private void globalRemove(@NotNull Connection conn, @NotNull String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM `" + TABLE_GLOBAL + "` WHERE `key`=?;"
        )) {
            ps.setString(1, key);
            ps.execute();
        }
    }

}
