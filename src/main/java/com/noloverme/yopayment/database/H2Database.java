package com.noloverme.yopayment.database;

import com.noloverme.yopayment.config.MainConfig;
import java.io.File;
import java.util.logging.Logger;

/**
 * H2 Database реализация.
 */
public class H2Database extends AbstractSQLDatabase {
    private final File dataFolder;
    private final String fileName;

    public H2Database(MainConfig config, File dataFolder, Logger logger) {
        super(logger);
        this.dataFolder = dataFolder;
        this.fileName = config.getDatabaseH2File() != null ? config.getDatabaseH2File() : "yopayment";
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 Driver not found", e);
        }
    }

    @Override
    protected String getJdbcUrl() {
        if (dataFolder == null) {
            throw new RuntimeException("Data folder is null");
        }
        return "jdbc:h2:file:" + new File(dataFolder, fileName).getAbsolutePath();
    }

    @Override
    protected String getUsername() {
        return "sa";
    }

    @Override
    protected String getPassword() {
        return "";
    }

    @Override
    protected int getPoolSize() {
        return 5;
    }
}
