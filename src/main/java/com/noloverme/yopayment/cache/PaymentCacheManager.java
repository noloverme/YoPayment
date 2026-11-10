package com.noloverme.yopayment.cache;

import com.noloverme.yopayment.database.DatabaseManager;
import com.noloverme.yopayment.model.PaymentRecord;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Потокобезопасный кэш последних платежей для моментальной отдачи в PlaceholderAPI
 * и быстрого доступа без блокировки основного потока сервера.
 */
public class PaymentCacheManager {
    private final ConcurrentHashMap<String, PaymentRecord> latestPayments = new ConcurrentHashMap<>();

    /**
     * Обновляет или сохраняет последнюю запись о платеже для игрока.
     */
    public void updateLatestPayment(PaymentRecord record) {
        if (record == null || record.playerName() == null) {
            return;
        }
        latestPayments.put(record.playerName().toLowerCase(Locale.ROOT), record);
    }

    /**
     * Возвращает последнюю запись о платеже для игрока из памяти (0 мс, без блокировок).
     */
    @Nullable
    public PaymentRecord getLatestPayment(String playerName) {
        if (playerName == null) {
            return null;
        }
        return latestPayments.get(playerName.toLowerCase(Locale.ROOT));
    }

    /**
     * Загружает последний платёж игрока из базы данных асинхронно и сохраняет в кэш.
     */
    public void loadPlayerLatest(DatabaseManager database, String playerName) {
        if (database == null || playerName == null) {
            return;
        }
        database.getPaymentsByPlayer(playerName, payments -> {
            if (payments != null && !payments.isEmpty()) {
                updateLatestPayment(payments.get(0));
            }
        });
    }

    /**
     * Очищает кэш.
     */
    public void clear() {
        latestPayments.clear();
    }
}
