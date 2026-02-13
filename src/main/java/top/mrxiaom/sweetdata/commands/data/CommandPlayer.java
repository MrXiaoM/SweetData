package top.mrxiaom.sweetdata.commands.data;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweetdata.Messages;
import top.mrxiaom.sweetdata.SweetData;
import top.mrxiaom.sweetdata.commands.CommandMain;
import top.mrxiaom.sweetdata.database.PlayerDatabase;
import top.mrxiaom.sweetdata.database.entry.PlayerCache;

import static top.mrxiaom.sweetdata.SweetData.limit;
import static top.mrxiaom.sweetdata.commands.CommandMain.*;

public class CommandPlayer {
    public final SweetData plugin;
    public final CommandMain parent;
    public CommandPlayer(CommandMain parent) {
        this.parent = parent;
        this.plugin = parent.plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length >= 3 && check("get", "sweet.data.player.get", args[0], sender)) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.command__player_not_found.tm(sender,
                        Pair.of("%player%", args[1]));
            }
            String key = args[2];
            String value;

            PlayerDatabase db = plugin.getPlayerDatabase();
            PlayerCache cache = db.getCacheOrNull(player);
            if (cache != null) {
                value = cache.get(key).orElse(null);
            } else {
                value = db.playerGet(player, key).orElse(null);
            }
            if (value != null) {
                return Messages.command__get__success.tm(sender,
                        Pair.of("%player%", args[1]),
                        Pair.of("%key%", key),
                        Pair.of("%value%", value));
            } else {
                return Messages.command__get__not_found.tm(sender,
                        Pair.of("%player%", args[1]),
                        Pair.of("%key%", key));
            }
        }
        if (args.length >= 4 && check("set", "sweet.data.player.set", args[0], sender)) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.command__player_not_found.tm(sender,
                        Pair.of("%player%", args[1]));
            }
            String key = args[2];
            String value = consume(args, 3);

            PlayerDatabase db = plugin.getPlayerDatabase();
            PlayerCache cache = db.getCacheOrNull(player);
            if (cache != null) {
                cache.put(key, value);
                cache.setNextSubmitAfter(30 * 1000L, false);
            } else {
                db.playerSet(player, key, value);
            }
            return Messages.command__set__success.tm(sender,
                    Pair.of("%player%", args[1]),
                    Pair.of("%key%", key),
                    Pair.of("%value%", value));
        }
        if (args.length >= 3 && check(commandRemoveDel, "sweet.data.player.del", args[0], sender)) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.command__player_not_found.tm(sender,
                        Pair.of("%player%", args[1]));
            }
            String key = args[2];

            PlayerDatabase db = plugin.getPlayerDatabase();
            PlayerCache cache = db.getCacheOrNull(player);
            if (cache != null) {
                cache.remove(key);
            }
            db.playerRemove(player, key);
            return Messages.command__remove__success.tm(sender,
                    Pair.of("%player%", args[1]),
                    Pair.of("%key%", key));
        }
        if (args.length >= 2 && check("clear", "sweet.data.player.clear", args[0], sender)) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.command__player_not_found.tm(sender,
                        Pair.of("%player%", args[1]));
            }
            Map<String, String> params = collectArgs(args, 2);
            boolean confirm = getBoolean(params, "confirm", false);
            if (!confirm) {
                return Messages.command__clear__confirm.tm(sender,
                        Pair.of("%player%", args[1]));
            }

            PlayerDatabase db = plugin.getPlayerDatabase();
            db.playerClear(player);

            return Messages.command__clear__success.tm(sender,
                    Pair.of("%player%", args[1]));
        }
        if (args.length >= 4 && check("plus", "sweet.data.player.plus", args[0], sender)) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.command__player_not_found.tm(sender,
                        Pair.of("%player%", args[1]));
            }
            String key = args[2];
            Integer toAdd = Util.parseInt(args[3]).orElse(null);
            if (toAdd == null) {
                return Messages.command__plus__not_integer.tm(sender,
                        Pair.of("%input%", args[3]));
            }
            Integer min = args.length > 4 ? Util.parseInt(args[4]).orElse(null) : null;
            Integer max = args.length > 5 ? Util.parseInt(args[5]).orElse(null) : null;
            PlayerDatabase db = plugin.getPlayerDatabase();
            PlayerCache cache = db.getCacheOrNull(player);
            Integer result = null;
            if (cache != null) {
                Integer value = cache.getInt(key).orElse(null);
                if (value != null) {
                    result = limit(value + toAdd, min, max);
                    cache.put(key, result);
                    cache.setNextSubmitAfter(30 * 1000L, false);
                }
            } else {
                result = db.playerIntAdd(player, key, toAdd, min, max);
            }
            if (parent.isConsoleSilentPlus() && sender instanceof ConsoleCommandSender) {
                return true;
            }
            if (result != null) {
                return Messages.command__plus__success.tm(sender,
                        Pair.of("%player%", args[1]),
                        Pair.of("%key%", key),
                        Pair.of("%value%", result));
            }
            return Messages.command__plus__fail.tm(sender,
                    Pair.of("%player%", args[1]),
                    Pair.of("%key%", key),
                    Pair.of("%added%", toAdd));
        }
        if (args.length >= 4 && check("add", "sweet.data.player.add", args[0], sender)) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.command__player_not_found.tm(sender,
                        Pair.of("%player%", args[1]));
            }
            String key = args[2];
            Integer toAdd = Util.parseInt(args[3]).orElse(null);
            if (toAdd == null) {
                return Messages.command__add__not_integer.tm(sender,
                        Pair.of("%input%", args[3]));
            }
            Integer min = args.length > 4 ? Util.parseInt(args[4]).orElse(null) : null;
            Integer max = args.length > 5 ? Util.parseInt(args[5]).orElse(null) : null;
            PlayerDatabase db = plugin.getPlayerDatabase();
            PlayerCache cache = db.getCacheOrNull(player);
            int result;
            if (cache != null) {
                Integer value = cache.getInt(key).orElse(0);
                result = limit(value + toAdd, min, max);
                cache.put(key, result);
                cache.setNextSubmitAfter(30 * 1000L, false);
            } else {
                result = db.playerIntAdd(player, key, toAdd, true, min, max);
            }
            if (parent.isConsoleSilentAdd() && sender instanceof ConsoleCommandSender) {
                return true;
            }
            return Messages.command__add__success.tm(sender,
                    Pair.of("%player%", args[1]),
                    Pair.of("%key%", key),
                    Pair.of("%value%", result));
        }
        return false;
    }
}
