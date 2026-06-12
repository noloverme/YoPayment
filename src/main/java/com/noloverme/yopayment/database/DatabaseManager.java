package com.noloverme.yopayment.database;

import com.noloverme.yopayment.model.PaymentRecord;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Интерфейс для управления базой данных платежей.
 */
public interface DatabaseManager {
    /**
     * Создаёт таблицы, применяет миграции.
     */
    void initialize() throws SQLException;

    /**
     * Закрывает пул соединений.
     */
    void close();

    /**
     * Сохраняет новый платёж в БД асинхронно.
     */
    void savePayment(PaymentRecord record, Runnable onComplete);

    /**
     * Обновляет статус платежа асинхронно.
     */
    void updatePaymentStatus(String paymentId, String status, LocalDateTime completedAt, Runnable onComplete);

    /**
     * Получает все активные платежи асинхронно.
     */
    void getActivePayments(Consumer<List<PaymentRecord>> callback);

    /**
     * Получает все платежи конкретного игрока асинхронно.
     */
    void getPaymentsByPlayer(String playerName, Consumer<List<PaymentRecord>> callback);

    /**
     * Получает платёж по ID асинхронно.
     */
    void getPayment(String paymentId, Consumer<Optional<PaymentRecord>> callback);
}
