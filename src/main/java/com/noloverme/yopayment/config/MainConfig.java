package com.noloverme.yopayment.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * Обёртка над config.yml с поддержкой переменных окружения для чувствительных данных.
 * Учётные данные могут быть установлены через ENV переменные для максимальной безопасности.
 */
public class MainConfig {
    private final FileConfiguration config;

    public MainConfig(FileConfiguration config) {
        this.config = config;
    }

    /**
     * Получает значение с fallback на переменную окружения.
     * Переменная окружения имеет приоритет над config.yml.
     */
    private String getWithEnvFallback(String configKey, String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue;
        }
        return config.getString(configKey, defaultValue);
    }

    public String getShopId() {
        return getWithEnvFallback("yookassa.shop-id", "YOOKASSA_SHOP_ID", "");
    }

    public String getSecretKey() {
        return getWithEnvFallback("yookassa.secret-key", "YOOKASSA_SECRET_KEY", "");
    }

    public String getReturnUrl() {
        return config.getString("yookassa.return-url", "https://example.com/thanks");
    }

    public int getCheckInterval() {
        return Math.max(1, config.getInt("check-interval", 5));
    }

    public int getPaymentTimeout() {
        return Math.max(1, config.getInt("payment-timeout", 10));
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
        return getWithEnvFallback("database.mysql.host", "MYSQL_HOST", "localhost");
    }

    public int getDatabaseMySqlPort() {
        String envPort = System.getenv("MYSQL_PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                return Integer.parseInt(envPort);
            } catch (NumberFormatException ignored) {
            }
        }
        return config.getInt("database.mysql.port", 3306);
    }

    public String getDatabaseMySqlName() {
        return getWithEnvFallback("database.mysql.database", "MYSQL_DATABASE", "yopayment");
    }

    public String getDatabaseMySqlUser() {
        return getWithEnvFallback("database.mysql.username", "MYSQL_USER", "root");
    }

    public String getDatabaseMySqlPassword() {
        return getWithEnvFallback("database.mysql.password", "MYSQL_PASSWORD", "");
    }

    public boolean isDatabaseMySqlSsl() {
        String envSsl = System.getenv("MYSQL_SSL");
        if (envSsl != null && !envSsl.trim().isEmpty()) {
            return Boolean.parseBoolean(envSsl);
        }
        return config.getBoolean("database.mysql.ssl", false);
    }

    public int getDatabaseMySqlPoolSize() {
        return config.getInt("database.mysql.pool-size", 10);
    }

    public String getDatabasePostgresHost() {
        return getWithEnvFallback("database.postgresql.host", "POSTGRES_HOST", "localhost");
    }

    public int getDatabasePostgresPort() {
        String envPort = System.getenv("POSTGRES_PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                return Integer.parseInt(envPort);
            } catch (NumberFormatException ignored) {
            }
        }
        return config.getInt("database.postgresql.port", 5432);
    }

    public String getDatabasePostgresName() {
        return getWithEnvFallback("database.postgresql.database", "POSTGRES_DATABASE", "yopayment");
    }

    public String getDatabasePostgresUser() {
        return getWithEnvFallback("database.postgresql.username", "POSTGRES_USER", "postgres");
    }

    public String getDatabasePostgresPassword() {
        return getWithEnvFallback("database.postgresql.password", "POSTGRES_PASSWORD", "");
    }

    public int getDatabasePostgresPoolSize() {
        return config.getInt("database.postgresql.pool-size", 10);
    }

    // 54-ФЗ Чеки
    public boolean isReceiptEnabled() {
        return config.getBoolean("receipt.enabled", false);
    }

    public String getReceiptDefaultEmail() {
        return config.getString("receipt.default-email", "donate@example.com");
    }

    public int getReceiptTaxSystemCode() {
        return config.getInt("receipt.tax-system-code", 0);
    }

    public int getReceiptVatCode() {
        return config.getInt("receipt.vat-code", 1);
    }
}
