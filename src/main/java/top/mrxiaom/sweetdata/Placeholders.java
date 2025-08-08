package top.mrxiaom.sweetdata;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.PlaceholdersExpansion;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweetdata.database.PlayerDatabase;
import top.mrxiaom.sweetdata.database.entry.GlobalCache;
import top.mrxiaom.sweetdata.database.entry.PlayerCache;

import java.util.Optional;
import java.util.function.Function;

public class Placeholders extends PlaceholdersExpansion<SweetData> {
    public Placeholders(SweetData plugin) {
        super(plugin);
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("global_")) { // 全局数值
            PlayerDatabase db = plugin.getPlayerDatabase();
            GlobalCache global = db.getGlobalCache();
            String params1 = params.substring(7);

            if (params1.startsWith("$")) {
                String[] split = params1.substring(1).split("\\$", 2);
                if (split.length == 1) return bool(false);
                String[] conditionArray = split[0].split(",");
                String[] split1 = split[1].split(";", 2);
                return $(conditionArray, split1, global::get);
            }

            String[] split = params1.split(";", 2);
            String key = split[0];
            String def = split.length == 2 ? split[1] : "";

            return global.get(key).orElse(def);
        }
        if (params.startsWith("@")) {
            String[] split = params.substring(1).split("@_", 2);
            if (split.length == 2) {
                OfflinePlayer p = Util.getOfflinePlayer(split[0]).orElse(null);
                return playerRequest(p, split[1]);
            }
        }
        String request = super.onRequest(player, params);
        if (request != null) {
            return request;
        }
        if (player != null) { // 玩家数值
            return playerRequest(player, params);
        }
        return null;
    }

    private String playerRequest(@Nullable OfflinePlayer player, String params) {
        if (params.startsWith("$")) {
            String[] split = params.substring(1).split("\\$", 2);
            if (split.length == 1) return bool(false);
            String[] conditionArray = split[0].split(",");
            String[] split1 = split[1].split(";", 2);
            if (player == null) {
                return split1[1]; // def
            }
            return $(conditionArray, split1, key -> get(player, key));
        }

        String[] split = params.split(";", 2);
        String key = split[0];
        String def = split.length == 2 ? split[1] : "";
        if (player == null) {
            return def;
        }

        return get(player, key).orElse(def);
    }

    private String $(String[] conditionArray, String[] split, Function<String, Optional<String>> get) {
        String key = split[0];
        String def = split.length == 2 ? split[1] : "";

        if ("range".equals(conditionArray[0]) && conditionArray.length == 3) {
            Integer rangeMin = Util.parseInt(conditionArray[1]).orElse(null);
            Integer rangeMax = Util.parseInt(conditionArray[2]).orElse(null);
            if (rangeMin == null || rangeMax == null) return bool(false);
            String str = get.apply(key).orElse(def);
            Integer value = Util.parseInt(str).orElse(null);
            if (value == null) return bool(false);
            return bool(value >= rangeMin && value <= rangeMax);
        }
        return bool(false);
    }

    private Optional<String> get(@NotNull OfflinePlayer player, String key) {
        PlayerDatabase db = plugin.getPlayerDatabase();
        PlayerCache cache = db.getCacheOrNull(player);
        if (cache != null) {
            return cache.get(key);
        } else {
            return db.playerGet(player, key);
        }
    }

    public static String bool(boolean value) {
        return value ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
    }
}
