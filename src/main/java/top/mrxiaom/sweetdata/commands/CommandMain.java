package top.mrxiaom.sweetdata.commands;
        
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweetdata.Messages;
import top.mrxiaom.sweetdata.SweetData;
import top.mrxiaom.sweetdata.commands.data.CommandGlobal;
import top.mrxiaom.sweetdata.commands.data.CommandPlayer;
import top.mrxiaom.sweetdata.database.PlayerDatabase;
import top.mrxiaom.sweetdata.func.AbstractModule;

import java.util.*;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    public final CommandGlobal commandGlobal;
    public final CommandPlayer commandPlayer;
    public CommandMain(SweetData plugin) {
        super(plugin);
        this.commandGlobal = new CommandGlobal(this);
        this.commandPlayer = new CommandPlayer(this);
        registerCommand("sweetdata", this);
    }

    boolean consoleSilentPlus, consoleSilentAdd;
    boolean unsafeMode;

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        consoleSilentPlus = config.getBoolean("console-silent.plus", config.getBoolean("console-silent-plus", true));
        consoleSilentAdd = config.getBoolean("console-silent.add", true);
        unsafeMode = config.getBoolean("unsafe-mode", false);
    }

    public boolean isConsoleSilentPlus() {
        return consoleSilentPlus;
    }

    public boolean isConsoleSilentAdd() {
        return consoleSilentAdd;
    }

    public boolean isNotUnsafeMode() {
        return !unsafeMode;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 1 && ("global".equalsIgnoreCase(args[0]) || "g".equalsIgnoreCase(args[0]))) {
            if (commandGlobal.execute(sender, args)) {
                return true;
            }
        }
        if (commandPlayer.execute(sender, args)) {
            return true;
        }
        if (args.length > 0 && check("reload", "sweet.data.reload", args[0], sender)) {
            if (args.length > 1 && "database".equalsIgnoreCase(args[1])) {
                plugin.options.database().reloadConfig();
                plugin.options.database().reconnect();
                PlayerDatabase db = plugin.getPlayerDatabase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    db.refreshCache(player);
                }
                return Messages.command__reload__database.tm(sender);
            }
            plugin.reloadConfig();
            return Messages.command__reload__normal.tm(sender);
        }
        if (sender.hasPermission("sweet.data.help")) {
            return Messages.command__help.tm(sender);
        }
        return true;
    }

    private void tab(CommandSender sender, List<String> list, String permission, String... args) {
        if (sender.hasPermission(permission)) {
            list.addAll(Arrays.asList(args));
        }
    }

    public static final String[] commandRemoveDel = {"remove", "del"};
    private static final List<String> emptyList = Collections.emptyList();
    private static final List<String> listArg1Player = Lists.newArrayList(
            "get", "set", "plus", "remove", "del");
    private static final List<String> listArg1Reload = Lists.newArrayList(
            "database");
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            tab(sender, list, "sweet.data.global", "global", "g");
            tab(sender, list, "sweet.data.player.get", "get");
            tab(sender, list, "sweet.data.player.set", "set");
            tab(sender, list, "sweet.data.player.del", "remove", "del");
            tab(sender, list, "sweet.data.player.plus", "plus");
            tab(sender, list, "sweet.data.reload", "reload");
            return startsWith(list, args[0]);
        }
        if (args.length == 2) {
            if (listArg1Player.contains(args[0].toLowerCase())) {
                String permKey = (args[0].equalsIgnoreCase("remove") ? "del" : args[0]).toLowerCase();
                if (sender.hasPermission(permKey)) {
                    return null;
                }
            }
            if ("global".equalsIgnoreCase(args[0]) || "g".equalsIgnoreCase(args[0])) {
                List<String> list = new ArrayList<>();
                tab(sender, list, "sweet.data.global.get", "get");
                tab(sender, list, "sweet.data.global.set", "set");
                tab(sender, list, "sweet.data.global.del", "remove", "del");
                tab(sender, list, "sweet.data.global.plus", "plus");
                return startsWith(list, args[1]);
            }
            if (check("reload", "sweet.data.reload", args[0], sender)) {
                return startsWith(listArg1Reload, args[1]);
            }
        }
        return emptyList;
    }

    public static List<String> startsWith(Collection<String> list, String s) {
        return startsWith(null, list, s);
    }
    public static List<String> startsWith(String[] addition, Collection<String> list, String s) {
        String s1 = s.toLowerCase();
        List<String> stringList = new ArrayList<>(list);
        if (addition != null) stringList.addAll(0, Lists.newArrayList(addition));
        stringList.removeIf(it -> !it.toLowerCase().startsWith(s1));
        return stringList;
    }

    @SuppressWarnings("SameParameterValue")
    public static boolean check(String[] command, String permission, String arg, CommandSender sender) {
        for (String s : command) {
            if (arg.equalsIgnoreCase(s)) {
                return sender.hasPermission(permission);
            }
        }
        return false;
    }
    public static boolean check(String command, String permission, String arg, CommandSender sender) {
        return arg.equalsIgnoreCase(command) && sender.hasPermission(permission);
    }

    public static Map<String, String> collectArgs(String[] args, int startIndex) {
        Map<String, String> map = new HashMap<>();
        for (int i = startIndex; i < args.length; i++) {
            String text = args[i];
            if (text.startsWith("--")) {
                int j = text.indexOf('=');
                if (j != -1) {
                    map.put(text.substring(2, j), text.substring(j + 1));
                } else {
                    map.put(text.substring(2), "true");
                }
            }
        }
        return map;
    }

    public static boolean getBoolean(Map<String, String> params, String key, boolean def) {
        String text = params.getOrDefault(key, String.valueOf(def));
        if (text.equals("true") || text.equals("yes")) return true;
        if (text.equals("false") || text.equals("no")) return false;
        return def;
    }

    public static String consume(String[] args, int startIndex) {
        StringJoiner joiner = new StringJoiner(" ");
        for (int i = startIndex; i < args.length; i++) {
            joiner.add(args[i]);
        }
        return joiner.toString();
    }
}
