package top.mrxiaom.sweetdata;
        
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.func.LanguageManager;
import top.mrxiaom.pluginbase.resolver.DefaultLibraryResolver;
import top.mrxiaom.pluginbase.utils.ClassLoaderWrapper;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.pluginbase.utils.scheduler.FoliaLibScheduler;
import top.mrxiaom.sweetdata.database.PlayerDatabase;

import java.io.File;
import java.net.URL;
import java.util.List;

public class SweetData extends BukkitPlugin {
    public static SweetData getInstance() {
        return (SweetData) BukkitPlugin.getInstance();
    }

    public SweetData() throws Exception {
        super(options()
                .bungee(true)
                .adventure(true)
                .database(true)
                .reconnectDatabaseWhenReloadConfig(false)
                .scanIgnore("top.mrxiaom.sweetdata.libs")
        );
        this.scheduler = new FoliaLibScheduler(this);

        info("正在检查依赖库状态");
        File librariesDir = ClassLoaderWrapper.isSupportLibraryLoader
                ? new File("libraries")
                : new File(this.getDataFolder(), "libraries");
        DefaultLibraryResolver resolver = new DefaultLibraryResolver(getLogger(), librariesDir);

        YamlConfiguration overrideLibraries = ConfigUtils.load(resolve("./.override-libraries.yml"));
        for (String key : overrideLibraries.getKeys(false)) {
            resolver.getStartsReplacer().put(key, overrideLibraries.getString(key));
        }
        resolver.addResolvedLibrary(BuildConstants.RESOLVED_LIBRARIES);

        List<URL> libraries = resolver.doResolve();
        info("正在添加 " + libraries.size() + " 个依赖库到类加载器");
        for (URL library : libraries) {
            this.classLoader.addURL(library);
        }
    }
    private boolean onlineMode = false;
    private PlayerDatabase playerDatabase;

    public PlayerDatabase getPlayerDatabase() {
        return playerDatabase;
    }

    @Override
    protected void beforeEnable() {
        options.registerDatabase(
                playerDatabase = new PlayerDatabase(this)
        );
        LanguageManager.inst()
                .setLangFile("messages.yml")
                .register(Messages.class, Messages::holder);
    }

    @Override
    protected void afterEnable() {
        if (PAPI.isEnabled()) {
            new Placeholders(this).register();
        }
        getLogger().info("SweetData 加载完毕");
    }

    @Override
    protected void beforeReloadConfig(FileConfiguration config) {
        String online = config.getString("online-mode", "auto").toLowerCase();
        switch (online) {
            case "true":
                onlineMode = true;
                break;
            case "false":
                onlineMode = false;
                break;
            case "auto":
            default:
                onlineMode = Bukkit.getOnlineMode();
                break;
        }
    }

    @NotNull
    public String databaseKey(@NotNull OfflinePlayer player) {
        if (onlineMode) {
            return player.getUniqueId().toString();
        } else {
            if (player instanceof Player) {
                // not null
                return ((Player) player).getName();
            }
            // nullable
            String name = player.getName();
            if (name == null) {
                throw new IllegalStateException("无法获取离线玩家 " + player.getUniqueId() + " 的名字");
            }
            return name;
        }
    }

    public static int limit(int value, Integer min, Integer max) {
        if (min != null && value < min) return min;
        if (max != null && value > max) return max;
        return value;
    }
}
