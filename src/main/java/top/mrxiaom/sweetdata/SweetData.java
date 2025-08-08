package top.mrxiaom.sweetdata;
        
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.func.LanguageManager;
import top.mrxiaom.pluginbase.utils.PAPI;
import top.mrxiaom.pluginbase.utils.scheduler.FoliaLibScheduler;
import top.mrxiaom.sweetdata.database.PlayerDatabase;

public class SweetData extends BukkitPlugin {
    public static SweetData getInstance() {
        return (SweetData) BukkitPlugin.getInstance();
    }

    public SweetData() {
        super(options()
                .bungee(true)
                .adventure(true)
                .database(true)
                .reconnectDatabaseWhenReloadConfig(false)
                .scanIgnore("top.mrxiaom.sweetdata.libs")
        );
        this.scheduler = new FoliaLibScheduler(this);
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
