package com.noloverme.yopayment.database;

import com.noloverme.yopayment.model.PaymentRecord;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Базовая реализация DatabaseManager для SQL-баз данных.
 */
public abstract class AbstractSQLDatabase implements DatabaseManager {
    protected HikariDataSource dataSource;
    protected final Logger logger;
    private final Thread executor;

    public AbstractSQLDatabase(Logger logger) {
        this.logger = logger;
        this.executor = Thread.currentThread();
        this.dataSource = null;
    }

    private synchronized HikariDataSource getDataSource() {
        if (dataSource == null || dataSource.isClosed()) {
            dataSource = createDataSource();
        }
        return dataSource;
    }

    /**
     * Возвращает JDBC URL для подключения.
     */
    protected abstract String getJdbcUrl();

    /**
     * Возвращает имя пользователя БД.
     */
    protected abstract String getUsername();

    /**
     * Возвращает пароль БД.
     */
    protected abstract String getPassword();

    /**
     * Возвращает размер пула соединений.
     */
    protected abstract int getPoolSize();

    private HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getJdbcUrl());
        config.setUsername(getUsername());
        config.setPassword(getPassword());
        config.setMaximumPoolSize(getPoolSize());
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        return new HikariDataSource(config);
    }

    @Override
    public void initialize() throws SQLException {
        try (Connection conn = getDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS yop_payments (
                    payment_id    VARCHAR(255) PRIMARY KEY,
                    player_name   VARCHAR(64)  NOT NULL,
                    player_uuid   VARCHAR(36),
                    item_id       VARCHAR(64)  NOT NULL,
                    price         DECIMAL(10,2) NOT NULL,
                    status        VARCHAR(32)  NOT NULL DEFAULT 'pending',
                    confirm_url   TEXT,
                    created_at    TIMESTAMP    NOT NULL,
                    completed_at  TIMESTAMP,
                    created_by    VARCHAR(64)  NOT NULL
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_yop_status ON yop_payments(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_yop_player ON yop_payments(player_name)");
            logger.info("[YoPayment] Database initialized successfully");
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                dataSource = null;
                logger.info("[YoPayment] Database connection pool closed");
            }
        }
    }

    @Override
    public void savePayment(PaymentRecord record, Runnable onComplete) {
        new Thread(() -> {
            String sql = """
                INSERT INTO yop_payments (
                    payment_id, player_name, player_uuid, item_id, price, status,
                    confirm_url, created_at, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            try (Connection conn = getDataSource().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, record.paymentId());
                pstmt.setString(2, record.playerName());
                pstmt.setString(3, record.playerUuid());
                pstmt.setString(4, record.itemId());
                pstmt.setDouble(5, record.price());
                pstmt.setString(6, record.status());
                pstmt.setString(7, record.confirmationUrl());
                pstmt.setTimestamp(8, Timestamp.valueOf(record.createdAt()));
                pstmt.setString(9, record.createdBy());
                pstmt.executeUpdate();
                if (onComplete != null) onComplete.run();
            } catch (SQLException e) {
                logger.severe("[YoPayment] Failed to save payment: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void updatePaymentStatus(String paymentId, String status, LocalDateTime completedAt, Runnable onComplete) {
        new Thread(() -> {
            String sql = "UPDATE yop_payments SET status = ?, completed_at = ? WHERE payment_id = ?";

            try (Connection conn = getDataSource().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, status);
                pstmt.setTimestamp(2, completedAt != null ? Timestamp.valueOf(completedAt) : null);
                pstmt.setString(3, paymentId);
                pstmt.executeUpdate();
                if (onComplete != null) onComplete.run();
            } catch (SQLException e) {
                logger.severe("[YoPayment] Failed to update payment status: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void getActivePayments(Consumer<List<PaymentRecord>> callback) {
        new Thread(() -> {
            String sql = "SELECT * FROM yop_payments WHERE status = 'pending'";
            List<PaymentRecord> records = new ArrayList<>();

            try (Connection conn = getDataSource().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    records.add(mapResultSet(rs));
                }
            } catch (SQLException e) {
                logger.severe("[YoPayment] Failed to get active payments: " + e.getMessage());
            }

            if (callback != null) callback.accept(records);
        }).start();
    }

    @Override
    public void getPaymentsByPlayer(String playerName, Consumer<List<PaymentRecord>> callback) {
        new Thread(() -> {
            String sql = "SELECT * FROM yop_payments WHERE player_name = ? ORDER BY created_at DESC";
            List<PaymentRecord> records = new ArrayList<>();

            try (Connection conn = getDataSource().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        records.add(mapResultSet(rs));
                    }
                }
            } catch (SQLException e) {
                logger.severe("[YoPayment] Failed to get payments by player: " + e.getMessage());
            }

            if (callback != null) callback.accept(records);
        }).start();
    }

    @Override
    public void getPayment(String paymentId, Consumer<Optional<PaymentRecord>> callback) {
        new Thread(() -> {
            String sql = "SELECT * FROM yop_payments WHERE payment_id = ?";
            Optional<PaymentRecord> result = Optional.empty();

            try (Connection conn = getDataSource().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, paymentId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        result = Optional.of(mapResultSet(rs));
                    }
                }
            } catch (SQLException e) {
                logger.severe("[YoPayment] Failed to get payment: " + e.getMessage());
            }

            if (callback != null) callback.accept(result);
        }).start();
    }

    protected PaymentRecord mapResultSet(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = rs.getTimestamp("created_at") != null
            ? rs.getTimestamp("created_at").toLocalDateTime()
            : LocalDateTime.now();

        Timestamp completedAtTs = rs.getTimestamp("completed_at");
        LocalDateTime completedAt = completedAtTs != null ? completedAtTs.toLocalDateTime() : null;

        return new PaymentRecord(
            rs.getString("payment_id"),
            rs.getString("player_name"),
            rs.getString("player_uuid"),
            rs.getString("item_id"),
            rs.getDouble("price"),
            rs.getString("status"),
            rs.getString("confirm_url"),
            createdAt,
            completedAt,
            rs.getString("created_by")
        );
    }
}
