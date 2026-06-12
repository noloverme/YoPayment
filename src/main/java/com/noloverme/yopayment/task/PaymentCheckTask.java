package com.noloverme.yopayment.task;

import com.noloverme.yopayment.api.YooKassaClient;
import com.noloverme.yopayment.api.model.PaymentStatus;
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
import java.util.List;
import java.util.logging.Logger;

/**
 * Периодическая задача для проверки статуса платежей.
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

    private void processPayment(PaymentRecord record) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutTime = record.createdAt().plus(paymentTimeoutMinutes, ChronoUnit.MINUTES);

        if (now.isAfter(timeoutTime)) {
            logger.info("[YoPayment] Payment " + record.paymentId() + " timed out for player " + record.playerName());
            database.updatePaymentStatus(record.paymentId(), "canceled", now, () -> {
                notifyPlayerIfOnline(record.playerName(), record.itemId(), "failed");
            });
            return;
        }

        try {
            PaymentResponse response = yooKassaClient.getPaymentStatus(record.paymentId());
            handlePaymentStatus(record, response, now);
        } catch (YooKassaClient.YooKassaException e) {
            logger.warning("[YoPayment] Failed to check payment " + record.paymentId() + ": " + e.getMessage());
        }
    }

    private void handlePaymentStatus(PaymentRecord record, PaymentResponse response, LocalDateTime now) {
        switch (response.status()) {
            case SUCCEEDED -> handleSucceeded(record, response, now);
            case CANCELED -> handleCanceled(record, now);
            case PENDING, WAITING_FOR_CAPTURE, UNKNOWN -> {} // Do nothing
        }
    }

    private void handleSucceeded(PaymentRecord record, PaymentResponse response, LocalDateTime now) {
        logger.info("[YoPayment] Payment " + record.paymentId() + " succeeded! Executing commands for " + record.playerName() + "...");
        database.updatePaymentStatus(record.paymentId(), "succeeded", now, () -> {
            DonateItem item = donates.getItem(record.itemId()).orElse(null);
            if (item == null) {
                logger.warning("[YoPayment] Item " + record.itemId() + " not found for payment " + record.paymentId());
                return;
            }

            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("YoPayment"), () -> {
                for (String cmd : item.commands()) {
                    String finalCmd = cmd.replace("%player%", record.playerName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                }

                notifyPlayerIfOnline(record.playerName(), record.itemId(), "success");
            });
        });
    }

    private void handleCanceled(PaymentRecord record, LocalDateTime now) {
        logger.info("[YoPayment] Payment " + record.paymentId() + " canceled for player " + record.playerName());
        database.updatePaymentStatus(record.paymentId(), "canceled", now, () -> {
            notifyPlayerIfOnline(record.playerName(), record.itemId(), "failed");
        });
    }

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
