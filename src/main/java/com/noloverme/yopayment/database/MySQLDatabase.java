package com.noloverme.yopayment.database;

import com.noloverme.yopayment.config.MainConfig;
import java.util.logging.Logger;

/**
 * MySQL Database реализация с настраиваемым SSL-шифрованием.
 */
public class MySQLDatabase extends AbstractSQLDatabase {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int poolSize;
    private final boolean ssl;

    public MySQLDatabase(MainConfig config, Logger logger) {
        super(logger);
        this.host = config.getDatabaseMySqlHost();
        this.port = config.getDatabaseMySqlPort();
        this.database = config.getDatabaseMySqlName();
        this.username = config.getDatabaseMySqlUser();
        this.password = config.getDatabaseMySqlPassword();
        this.poolSize = config.getDatabaseMySqlPoolSize();
        this.ssl = config.isDatabaseMySqlSsl();
    }

    @Override
    protected String getJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database
            + "?useSSL=" + ssl
            + (ssl ? "&requireSSL=true" : "&allowPublicKeyRetrieval=true")
            + "&autoReconnect=true"
            + "&characterEncoding=UTF-8"
            + "&serverTimezone=UTC"
            + "&allowMultiQueries=false"
            + "&allowUrlInLocalInfile=false"
            + "&allowLoadLocalInfileInPath=/dev/null";
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
