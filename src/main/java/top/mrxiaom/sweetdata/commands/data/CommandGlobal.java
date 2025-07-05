package top.mrxiaom.sweetdata.commands.data;

import com.google.common.collect.Iterables;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweetdata.Messages;
import top.mrxiaom.sweetdata.SweetData;
import top.mrxiaom.sweetdata.commands.CommandMain;
import top.mrxiaom.sweetdata.database.PlayerDatabase;
import top.mrxiaom.sweetdata.database.entry.GlobalCache;

import static top.mrxiaom.sweetdata.commands.CommandMain.*;

public class CommandGlobal {
    public final SweetData plugin;
    public final CommandMain parent;
    public final boolean bungeecord;
    public CommandGlobal(CommandMain parent) {
        this.parent = parent;
        this.plugin = parent.plugin;
        bungeecord = Bukkit.spigot().getConfig().getBoolean("settings.bungeecord", false);
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (check("get", "sweet.data.global.get", args[1], sender)) {
            String key = args[2];
            String value;

            PlayerDatabase db = plugin.getPlayerDatabase();
            GlobalCache global = db.getGlobalCache();
            value = global.get(key).orElse(null);

            if (value != null) {
                return Messages.command__global__get__success.tm(sender,
                        Pair.of("%key%", key),
                        Pair.of("%value%", value));
            } else {
                return Messages.command__global__get__not_found.tm(sender,
                        Pair.of("%key%", key));
            }
        }
        if (check("set", "sweet.data.global.set", args[1], sender)) {
            String key = args[2];
            String value = consume(args, 3);
            Player whoever;
            if (bungeecord) {
                whoever = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
                if (whoever == null && parent.isNotUnsafeMode()) {
                    return Messages.command__global__unsafe.tm(sender);
                }
            } else {
                whoever = null;
            }

            PlayerDatabase db = plugin.getPlayerDatabase();
            db.globalSet(key, value);
            db.sendRequireGlobalCacheUpdate(whoever, key, value);

            return Messages.command__global__set__success.tm(sender,
                    Pair.of("%key%", key),
                    Pair.of("%value%", value));
        }
        if (check(commandRemoveDel, "sweet.data.global.del", args[1], sender)) {
            String key = args[2];
            Player whoever;
            if (bungeecord) {
                whoever = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
                if (whoever == null && parent.isNotUnsafeMode()) {
                    return Messages.command__global__unsafe.tm(sender);
                }
            } else {
                whoever = null;
            }

            PlayerDatabase db = plugin.getPlayerDatabase();
            db.globalRemove(key);
            db.sendRequireGlobalCacheUpdate(whoever, key, null);

            return Messages.command__global__remove__success.tm(sender,
                    Pair.of("%key%", key));
        }
        if (check("plus", "sweet.data.global.plus", args[1], sender)) {
            String key = args[2];
            Integer toAdd = Util.parseInt(args[3]).orElse(null);
            if (toAdd == null) {
                return Messages.command__global__plus__not_integer.tm(sender,
                        Pair.of("%input%", args[3]));
            }
            Player whoever;
            if (bungeecord) {
                whoever = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
                if (whoever == null && parent.isNotUnsafeMode()) {
                    return Messages.command__global__unsafe.tm(sender);
                }
            } else {
                whoever = null;
            }
            PlayerDatabase db = plugin.getPlayerDatabase();
            Integer result = db.globalIntAdd(key, toAdd);
            if (result != null) {
                db.sendRequireGlobalCacheUpdate(whoever, key, String.valueOf(result));
            }

            if (parent.isConsoleSilentPlus() && sender instanceof ConsoleCommandSender) {
                return true;
            }
            if (result != null) {
                return Messages.command__global__plus__success.tm(sender,
                        Pair.of("%added%", toAdd),
                        Pair.of("%key%", key),
                        Pair.of("%value%", result));
            }
            return Messages.command__global__plus__fail.tm(sender,
                    Pair.of("%key%", key),
                    Pair.of("%added%", toAdd));
        }
        if (check("add", "sweet.data.global.add", args[1], sender)) {
            String key = args[2];
            Integer toAdd = Util.parseInt(args[3]).orElse(null);
            if (toAdd == null) {
                return Messages.command__global__add__not_integer.tm(sender,
                        Pair.of("%input%", args[3]));
            }
            Player whoever;
            if (bungeecord) {
                whoever = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
                if (whoever == null && parent.isNotUnsafeMode()) {
                    return Messages.command__global__unsafe.tm(sender);
                }
            } else {
                whoever = null;
            }
            PlayerDatabase db = plugin.getPlayerDatabase();
            Integer result = db.globalIntAdd(key, toAdd, true);
            db.sendRequireGlobalCacheUpdate(whoever, key, String.valueOf(result));

            if (parent.isConsoleSilentPlus() && sender instanceof ConsoleCommandSender) {
                return true;
            }
            return Messages.command__global__add__success.tm(sender,
                    Pair.of("%added%", toAdd),
                    Pair.of("%key%", key),
                    Pair.of("%value%", result));
        }
        return false;
    }
}
