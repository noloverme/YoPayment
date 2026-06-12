package com.noloverme.yopayment.database;

import com.noloverme.yopayment.config.MainConfig;
import java.util.logging.Logger;

/**
 * PostgreSQL Database реализация.
 */
public class PostgreSQLDatabase extends AbstractSQLDatabase {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int poolSize;

    public PostgreSQLDatabase(MainConfig config, Logger logger) {
        super(logger);
        this.host = config.getDatabasePostgresHost();
        this.port = config.getDatabasePostgresPort();
        this.database = config.getDatabasePostgresName();
        this.username = config.getDatabasePostgresUser();
        this.password = config.getDatabasePostgresPassword();
        this.poolSize = config.getDatabasePostgresPoolSize();
    }

    @Override
    protected String getJdbcUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    @Override
    protected String getUsername() {
        return username;
    }

    @Override
    protected String getPassword() {
        return password;
    }

    @Override
    protected int getPoolSize() {
        return poolSize;
    }
}
