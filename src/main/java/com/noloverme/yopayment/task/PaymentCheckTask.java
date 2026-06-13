package com.noloverme.yopayment.task;

import com.noloverme.yopayment.api.YooKassaClient;
import com.noloverme.yopayment.api.model.PaymentResponse;
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
import java.util.logging.Logger;

/**
 * Асинхронная задача для проверки статуса платежей.
 * Выполняется периодически и обновляет статусы платежей через API ЮKassa.
 */
public class PaymentCheckTask extends BukkitRunnable {
    private final DatabaseManager database;
    private final YooKassaClient yooKassaClient;
    private final MessagesConfig messages;
    private final DonatesConfig donates;
    private final int paymentTimeoutMinutes;
    private final Logger logger;

    public PaymentCheckTask(DatabaseManager database, YooKassaClient yooKassaClient,
                            MessagesConfig messages, DonatesConfig donates,
                            int paymentTimeoutMinutes, Logger logger) {
        this.database = database;
        this.yooKassaClient = yooKassaClient;
        this.messages = messages;
        this.donates = donates;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
        this.logger = logger;
    }

    @Override
    public void run() {
        database.getActivePayments(payments -> {
            for (PaymentRecord record : payments) {
                processPayment(record);
            }
        });
    }

    /**
     * Обрабатывает один платёж: проверяет timeout и статус в API.
     */
    private void processPayment(PaymentRecord record) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutTime = record.createdAt().plus(paymentTimeoutMinutes, ChronoUnit.MINUTES);

        if (now.isAfter(timeoutTime)) {
            logger.info("Payment " + record.paymentId() + " timed out for player " + record.playerName());
            database.updatePaymentStatus(record.paymentId(), "canceled", now, () -> {
                notifyPlayerIfOnline(record.playerName(), record.itemId(), "failed");
            });
            return;
        }

        yooKassaClient.getPaymentStatus(record.paymentId())
            .thenAccept(response -> handlePaymentStatus(record, response, now))
            .exceptionally(ex -> {
                logger.warning("Failed to check payment " + record.paymentId() + ": " + ex.getMessage());
                return null;
            });
    }

    /**
     * Обрабатывает результат проверки статуса платежа.
     */
    private void handlePaymentStatus(PaymentRecord record, PaymentResponse response, LocalDateTime now) {
        switch (response.status()) {
            case SUCCEEDED -> handleSucceeded(record, response, now);
            case CANCELED -> handleCanceled(record, now);
            case PENDING, WAITING_FOR_CAPTURE, UNKNOWN -> {} // Do nothing
        }
    }

    /**
     * Обрабатывает успешно завершённый платёж.
     * Выполняет команды и уведомляет игрока.
     */
    private void handleSucceeded(PaymentRecord record, PaymentResponse response, LocalDateTime now) {
        logger.info("Payment " + record.paymentId() + " succeeded! Executing commands for " + record.playerName() + "...");

        database.updatePaymentStatus(record.paymentId(), "succeeded", now, () -> {
            DonateItem item = donates.getItem(record.itemId()).orElse(null);
            if (item == null) {
                logger.warning("Item " + record.itemId() + " not found for payment " + record.paymentId());
                return;
            }

            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("YoPayment"), () -> {
                executeCommands(item, record.playerName());
                notifyPlayerIfOnline(record.playerName(), record.itemId(), "success");
            });
        });
    }

    /**
     * Безопасно выполняет команды с валидацией имени игрока.
     */
    private void executeCommands(DonateItem item, String playerName) {
        for (String cmd : item.commands()) {
            String finalCmd = replacePlayerPlaceholder(cmd, playerName);
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            } catch (Exception e) {
                logger.severe("Failed to execute command for player " + playerName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Заменяет плейсхолдер %player% на имя игрока с валидацией.
     */
    private String replacePlayerPlaceholder(String cmd, String playerName) {
        if (!cmd.contains("%player%")) {
            return cmd;
        }

        // Санитизируем имя перед подстановкой
        String sanitizedName = playerName.replaceAll("[^a-zA-Z0-9_]", "_");
        return cmd.replace("%player%", sanitizedName);
    }

    /**
     * Обрабатывает отменённый платёж.
     */
    private void handleCanceled(PaymentRecord record, LocalDateTime now) {
        logger.info("Payment " + record.paymentId() + " canceled for player " + record.playerName());
        database.updatePaymentStatus(record.paymentId(), "canceled", now, () -> {
            notifyPlayerIfOnline(record.playerName(), record.itemId(), "failed");
        });
    }

    /**
     * Отправляет уведомление игроку если он онлайн.
     */
    private void notifyPlayerIfOnline(String playerName, String itemId, String type) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null && player.isOnline()) {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("YoPayment"), () -> {
                String displayName = donates.getItem(itemId).map(DonateItem::displayName).orElse(itemId);
                if ("success".equals(type)) {
                    player.sendMessage(messages.paymentSuccess(itemId, displayName));
                } else {
                    player.sendMessage(messages.paymentFailed(itemId, displayName));
                }
            });
        }
    }
}
