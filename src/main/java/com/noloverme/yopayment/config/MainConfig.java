package com.noloverme.yopayment.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * Обёртка над config.yml.
 */
public class MainConfig {
    private final FileConfiguration config;

    public MainConfig(FileConfiguration config) {
        this.config = config;
    }

    public String getShopId() {
        return config.getString("yookassa.shop-id", "000000");
    }

    public String getSecretKey() {
        return config.getString("yookassa.secret-key", "test_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    }

    public String getReturnUrl() {
        return config.getString("yookassa.return-url", "https://example.com/thanks");
    }

    public int getCheckInterval() {
        return config.getInt("check-interval", 5);
    }

    public int getPaymentTimeout() {
        return config.getInt("payment-timeout", 10);
    }

    public String getMessageFormat() {
        return config.getString("message-format", "{display_name} ({price} ₽): {url}");
    }

    public boolean isSilentMode() {
        return config.getBoolean("silent-mode", false);
    }

    public String getDatabaseType() {
        return config.getString("database.type", "h2").toLowerCase();
    }

    @Nullable
    public String getDatabaseH2File() {
        return config.getString("database.h2.file", "yopayment");
    }

    public String getDatabaseMySqlHost() {
        return config.getString("database.mysql.host", "localhost");
    }

    public int getDatabaseMySqlPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    public String getDatabaseMySqlName() {
        return config.getString("database.mysql.database", "yopayment");
    }

    public String getDatabaseMySqlUser() {
        return config.getString("database.mysql.username", "root");
    }

    public String getDatabaseMySqlPassword() {
        return config.getString("database.mysql.password", "password");
    }

    public int getDatabaseMySqlPoolSize() {
        return config.getInt("database.mysql.pool-size", 10);
    }

    public String getDatabasePostgresHost() {
        return config.getString("database.postgresql.host", "localhost");
    }

    public int getDatabasePostgresPort() {
        return config.getInt("database.postgresql.port", 5432);
    }

    public String getDatabasePostgresName() {
        return config.getString("database.postgresql.database", "yopayment");
    }

    public String getDatabasePostgresUser() {
        return config.getString("database.postgresql.username", "postgres");
    }

    public String getDatabasePostgresPassword() {
        return config.getString("database.postgresql.password", "password");
    }

    public int getDatabasePostgresPoolSize() {
        return config.getInt("database.postgresql.pool-size", 10);
    }
}
