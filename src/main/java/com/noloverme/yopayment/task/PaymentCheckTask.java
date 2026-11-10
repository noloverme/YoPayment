package com.noloverme.yopayment.task;

import com.noloverme.yopayment.YoPaymentPlugin;
import com.noloverme.yopayment.api.YooKassaClient;
import com.noloverme.yopayment.api.model.PaymentResponse;
import com.noloverme.yopayment.cache.PaymentCacheManager;
import com.noloverme.yopayment.config.DonatesConfig;
import com.noloverme.yopayment.config.MessagesConfig;
import com.noloverme.yopayment.database.DatabaseManager;
import com.noloverme.yopayment.model.DonateItem;
import com.noloverme.yopayment.model.PaymentRecord;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Асинхронная задача для периодической проверки статуса платежей.
 * Обеспечивает защиту от состояния гонки (race conditions) и двойной выдачи доната.
 */
public class PaymentCheckTask extends BukkitRunnable {
    private final YoPaymentPlugin plugin;
    private final DatabaseManager database;
    private final YooKassaClient yooKassaClient;
    private final PaymentCacheManager cacheManager;
    private final MessagesConfig messages;
    private final DonatesConfig donates;
    private final int paymentTimeoutMinutes;
    private final Logger logger;

    // Защита от повторной/параллельной обработки одного и того же платежа
    private final Set<String> inFlightPayments = ConcurrentHashMap.newKeySet();

    public PaymentCheckTask(YoPaymentPlugin plugin, DatabaseManager database, YooKassaClient yooKassaClient,
                            PaymentCacheManager cacheManager, MessagesConfig messages, DonatesConfig donates,
                            int paymentTimeoutMinutes, Logger logger) {
        this.plugin = plugin;
        this.database = database;
        this.yooKassaClient = yooKassaClient;
        this.cacheManager = cacheManager;
        this.messages = messages;
        this.donates = donates;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
        this.logger = logger;
    }

    @Override
    public void run() {
        database.getActivePayments(payments -> {
            if (payments == null || payments.isEmpty()) {
                return;
            }
            for (PaymentRecord record : payments) {
                // Если платёж уже обрабатывается другой итерацией — пропускаем
                if (inFlightPayments.add(record.paymentId())) {
                    processPayment(record);
                }
            }
        });
    }

    /**
     * Обрабатывает один платёж: проверяет timeout и статус в API ЮKassa.
     */
    private void processPayment(PaymentRecord record) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutTime = record.createdAt().plus(paymentTimeoutMinutes, ChronoUnit.MINUTES);

        if (now.isAfter(timeoutTime)) {
            logger.info("Payment " + record.paymentId() + " timed out for player " + record.playerName());
            database.updatePaymentStatus(record.paymentId(), "canceled", now, () -> {
                updateCache(record, "canceled", now);
                notifyPlayer(record.playerName(), record.itemId(), "failed");
                inFlightPayments.remove(record.paymentId());
            });
            return;
        }

        yooKassaClient.getPaymentStatus(record.paymentId())
            .thenAccept(response -> handlePaymentStatus(record, response, now))
            .exceptionally(ex -> {
                logger.warning("Failed to check payment " + record.paymentId() + ": " + ex.getMessage());
                inFlightPayments.remove(record.paymentId());
                return null;
            });
    }

    /**
     * Обрабатывает результат проверки статуса платежа.
     */
    private void handlePaymentStatus(PaymentRecord record, PaymentResponse response, LocalDateTime now) {
        try {
            switch (response.status()) {
                case SUCCEEDED -> handleSucceeded(record, now);
                case CANCELED -> handleCanceled(record, now);
                case PENDING, WAITING_FOR_CAPTURE, UNKNOWN -> {
                    inFlightPayments.remove(record.paymentId());
                }
            }
        } catch (Exception e) {
            logger.severe("Error handling status for payment " + record.paymentId() + ": " + e.getMessage());
            inFlightPayments.remove(record.paymentId());
        }
    }

    /**
     * Обрабатывает успешно завершённый платёж с проверкой онлайна игрока.
     */
    private void handleSucceeded(PaymentRecord record, LocalDateTime now) {
        DonateItem item = donates.getItem(record.itemId()).orElse(null);
        if (item == null) {
            logger.warning("Item " + record.itemId() + " not found for payment " + record.paymentId());
            inFlightPayments.remove(record.paymentId());
            return;
        }

        // Проверяем статус игрока и выполняем выдачу в основном потоке сервера
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayerExact(record.playerName());

            if (player != null && player.isOnline()) {
                // Игрок онлайн — выдаём немедленно
                executeCommands(item, record.playerName());
                player.sendMessage(messages.paymentSuccess(record.itemId(), item.displayName()));

                database.updatePaymentStatus(record.paymentId(), "succeeded", now, () -> {
                    updateCache(record, "succeeded", now);
                    inFlightPayments.remove(record.paymentId());
                });
                logger.info("Payment " + record.paymentId() + " succeeded and delivered to " + record.playerName());
            } else {
                // Игрок офлайн — откладываем до его входа (pending_delivery)
                database.updatePaymentStatus(record.paymentId(), "pending_delivery", now, () -> {
                    updateCache(record, "pending_delivery", now);
                    inFlightPayments.remove(record.paymentId());
                });
                logger.info("Payment " + record.paymentId() + " succeeded, but player " + record.playerName() + " is offline. Marked as pending_delivery.");
            }
        });
    }

    /**
     * Выполняет команды от имени консоли с очисткой слэша и санитизацией имени.
     */
    private void executeCommands(DonateItem item, String playerName) {
        String sanitizedName = playerName.replaceAll("[^a-zA-Z0-9_]", "_");
        for (String cmd : item.commands()) {
            String finalCmd = cmd.replace("%player%", sanitizedName);
            if (finalCmd.startsWith("/")) {
                finalCmd = finalCmd.substring(1);
            }
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            } catch (Exception e) {
                logger.severe("Failed to execute command for player " + playerName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Обрабатывает отменённый платёж.
     */
    private void handleCanceled(PaymentRecord record, LocalDateTime now) {
        logger.info("Payment " + record.paymentId() + " canceled for player " + record.playerName());
        database.updatePaymentStatus(record.paymentId(), "canceled", now, () -> {
            updateCache(record, "canceled", now);
            notifyPlayer(record.playerName(), record.itemId(), "failed");
            inFlightPayments.remove(record.paymentId());
        });
    }

    private void updateCache(PaymentRecord record, String newStatus, LocalDateTime completedAt) {
        PaymentRecord updated = new PaymentRecord(
            record.paymentId(),
            record.playerName(),
            record.playerUuid(),
            record.itemId(),
            record.price(),
            newStatus,
            record.confirmationUrl(),
            record.createdAt(),
            completedAt,
            record.createdBy()
        );
        cacheManager.updateLatestPayment(updated);
    }

    /**
     * Отправляет уведомление игроку в основном потоке, если он в сети.
     */
    private void notifyPlayer(String playerName, String itemId, String type) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player != null && player.isOnline()) {
                String displayName = donates.getItem(itemId).map(DonateItem::displayName).orElse(itemId);
                if ("success".equals(type)) {
                    player.sendMessage(messages.paymentSuccess(itemId, displayName));
                } else {
                    player.sendMessage(messages.paymentFailed(itemId, displayName));
                }
            }
        });
    }
}
