package com.noloverme.yopayment.database;

import com.noloverme.yopayment.config.MainConfig;
import java.io.File;
import java.util.logging.Logger;

/**
 * Фабрика для создания реализации DatabaseManager.
 */
public class DatabaseFactory {
    private DatabaseFactory() {
    }

    public static DatabaseManager create(MainConfig config, File dataFolder, Logger logger) {
        return switch (config.getDatabaseType().toLowerCase()) {
            case "h2" -> new H2Database(config, dataFolder, logger);
            case "mysql" -> new MySQLDatabase(config, logger);
            case "postgresql" -> new PostgreSQLDatabase(config, logger);
            default -> throw new IllegalArgumentException(
                "Unsupported database type: " + config.getDatabaseType()
            );
        };
    }
}
