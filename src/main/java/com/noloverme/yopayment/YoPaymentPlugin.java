package com.noloverme.yopayment;

import com.noloverme.yopayment.api.YooKassaClient;
import com.noloverme.yopayment.cache.PaymentCacheManager;
import com.noloverme.yopayment.command.YoPaymentCommand;
import com.noloverme.yopayment.config.DonatesConfig;
import com.noloverme.yopayment.config.MainConfig;
import com.noloverme.yopayment.config.MessagesConfig;
import com.noloverme.yopayment.database.DatabaseFactory;
import com.noloverme.yopayment.database.DatabaseManager;
import com.noloverme.yopayment.listener.PlayerJoinListener;
import com.noloverme.yopayment.placeholder.PlaceholderAPIHook;
import com.noloverme.yopayment.task.PaymentCheckTask;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;

/**
 * Главный класс плагина YoPayment (совместим с 1.16+ и Java 17).
 */
public class YoPaymentPlugin extends JavaPlugin {
    private MainConfig mainConfig;
    private MessagesConfig messagesConfig;
    private DonatesConfig donatesConfig;
    private DatabaseManager databaseManager;
    private PaymentCacheManager cacheManager;
    private YooKassaClient yooKassaClient;
    private PaymentCheckTask paymentCheckTask;
    private PlaceholderAPIHook placeholderHook;

    @Override
    public void onEnable() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            saveDefaultConfig();
            saveResource("messages.yml", false);
            saveResource("donates.yml", false);

            this.cacheManager = new PaymentCacheManager();

            reloadPlugin(getDataFolder());

            // Регистрируем слушатель для отложенной выдачи и кэша
            getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(this, databaseManager, cacheManager, donatesConfig, messagesConfig, getLogger()),
                this
            );

            getLogger().info("YoPayment v1.0b enabled (compatible with 1.16+ & Java 17).");
        } catch (Exception e) {
            getLogger().severe("Failed to enable YoPayment: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (paymentCheckTask != null) {
                paymentCheckTask.cancel();
                paymentCheckTask = null;
            }

            if (placeholderHook != null) {
                placeholderHook.unregister();
                placeholderHook = null;
            }

            if (yooKassaClient != null) {
                yooKassaClient.shutdown();
                yooKassaClient = null;
            }

            if (databaseManager != null) {
                databaseManager.close();
                databaseManager = null;
            }

            if (cacheManager != null) {
                cacheManager.clear();
            }

            getLogger().info("YoPayment disabled.");
        } catch (Exception e) {
            getLogger().severe("Error disabling YoPayment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reloadPlugin(File dataFolder) throws SQLException {
        mainConfig = new MainConfig(getConfig());

        File messagesFile = new File(dataFolder, "messages.yml");
        messagesConfig = new MessagesConfig(
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(messagesFile)
        );

        File donatesFile = new File(dataFolder, "donates.yml");
        donatesConfig = new DonatesConfig(
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(donatesFile)
        );

        if (databaseManager != null) {
            databaseManager.close();
        }

        databaseManager = DatabaseFactory.create(mainConfig, dataFolder, getLogger());
        databaseManager.initialize();

        if (yooKassaClient != null) {
            yooKassaClient.shutdown();
        }
        yooKassaClient = new YooKassaClient(mainConfig, getLogger());

        if (paymentCheckTask != null) {
            paymentCheckTask.cancel();
        }

        paymentCheckTask = new PaymentCheckTask(
            this,
            databaseManager,
            yooKassaClient,
            cacheManager,
            messagesConfig,
            donatesConfig,
            mainConfig.getPaymentTimeout(),
            getLogger()
        );

        int intervalTicks = mainConfig.getCheckInterval() * 20;
        paymentCheckTask.runTaskTimerAsynchronously(this, 0, intervalTicks);

        YoPaymentCommand commandExecutor = new YoPaymentCommand(
            this,
            databaseManager,
            yooKassaClient,
            cacheManager,
            mainConfig,
            messagesConfig,
            donatesConfig,
            mainConfig.getPaymentTimeout(),
            getLogger()
        );

        PluginCommand cmd = getCommand("yopayment");
        if (cmd != null) {
            cmd.setExecutor(commandExecutor);
            cmd.setTabCompleter(commandExecutor);
        }

        registerPlaceholders();
    }

    private void registerPlaceholders() {
        try {
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                if (placeholderHook != null) {
                    placeholderHook.unregister();
                }
                placeholderHook = new PlaceholderAPIHook(databaseManager, donatesConfig, cacheManager, getLogger());
                placeholderHook.register();
            }
        } catch (Exception e) {
            getLogger().warning("Failed to register PlaceholderAPI: " + e.getMessage());
        }
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public DonatesConfig getDonatesConfig() {
        return donatesConfig;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PaymentCacheManager getCacheManager() {
        return cacheManager;
    }

    public YooKassaClient getYooKassaClient() {
        return yooKassaClient;
    }
}
