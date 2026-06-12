package com.noloverme.yopayment;

import com.noloverme.yopayment.api.YooKassaClient;
import com.noloverme.yopayment.command.YoPaymentCommand;
import com.noloverme.yopayment.config.DonatesConfig;
import com.noloverme.yopayment.config.MainConfig;
import com.noloverme.yopayment.config.MessagesConfig;
import com.noloverme.yopayment.database.DatabaseFactory;
import com.noloverme.yopayment.database.DatabaseManager;
import com.noloverme.yopayment.placeholder.PlaceholderAPIHook;
import com.noloverme.yopayment.task.PaymentCheckTask;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.SQLException;

/**
 * Главный класс плагина YoPayment.
 */
public class YoPaymentPlugin extends JavaPlugin {
    private MainConfig mainConfig;
    private MessagesConfig messagesConfig;
    private DonatesConfig donatesConfig;
    private DatabaseManager databaseManager;
    private YooKassaClient yooKassaClient;
    private PaymentCheckTask paymentCheckTask;

    @Override
    public void onEnable() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            saveDefaultConfig();
            saveResource("messages.yml", false);
            saveResource("donates.yml", false);

            reloadPlugin(getDataFolder());

            getLogger().info("YoPayment v1.0a by noloverme enabled.");
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
            }

            if (databaseManager != null) {
                databaseManager.close();
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
        MessagesConfig tempMessages = new MessagesConfig(
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(messagesFile)
        );
        messagesConfig = tempMessages;

        File donatesFile = new File(dataFolder, "donates.yml");
        DonatesConfig tempDonates = new DonatesConfig(
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(donatesFile)
        );
        donatesConfig = tempDonates;

        if (databaseManager != null) {
            databaseManager.close();
        }

        databaseManager = DatabaseFactory.create(mainConfig, dataFolder, getLogger());
        databaseManager.initialize();

        yooKassaClient = new YooKassaClient(
            mainConfig.getShopId(),
            mainConfig.getSecretKey(),
            mainConfig.getReturnUrl(),
            getLogger(),
            mainConfig.isSilentMode()
        );

        if (paymentCheckTask != null) {
            paymentCheckTask.cancel();
        }

        paymentCheckTask = new PaymentCheckTask(
            databaseManager,
            yooKassaClient,
            messagesConfig,
            donatesConfig,
            mainConfig.getPaymentTimeout(),
            getLogger()
        );

        int intervalTicks = mainConfig.getCheckInterval() * 20;
        paymentCheckTask.runTaskTimerAsynchronously(this, 0, intervalTicks);

        YoPaymentCommand commandExecutor = new YoPaymentCommand(
            databaseManager,
            yooKassaClient,
            mainConfig,
            messagesConfig,
            donatesConfig,
            mainConfig.getPaymentTimeout(),
            getLogger()
        );

        getCommand("yopayment").setExecutor(commandExecutor);
        getCommand("yopayment").setTabCompleter(commandExecutor);

        registerPlaceholders(databaseManager, donatesConfig);
    }

    private void registerPlaceholders(DatabaseManager databaseManager, DonatesConfig donatesConfig) {
        try {
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new PlaceholderAPIHook(databaseManager, donatesConfig, getLogger()).register();
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

    public YooKassaClient getYooKassaClient() {
        return yooKassaClient;
    }
}
