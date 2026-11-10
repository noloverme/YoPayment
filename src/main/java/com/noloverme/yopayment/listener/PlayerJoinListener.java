package com.noloverme.yopayment.listener;

import com.noloverme.yopayment.YoPaymentPlugin;
import com.noloverme.yopayment.cache.PaymentCacheManager;
import com.noloverme.yopayment.config.DonatesConfig;
import com.noloverme.yopayment.config.MessagesConfig;
import com.noloverme.yopayment.database.DatabaseManager;
import com.noloverme.yopayment.model.DonateItem;
import com.noloverme.yopayment.model.PaymentRecord;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

/**
 * Слушатель входа игроков:
 * 1. Загружает последний платёж в кэш для PlaceholderAPI.
 * 2. Выдаёт отложенные покупки (pending_delivery), которые игрок оплатил, находясь офлайн.
 */
public class PlayerJoinListener implements Listener {
    private final YoPaymentPlugin plugin;
    private final DatabaseManager database;
    private final PaymentCacheManager cacheManager;
    private final DonatesConfig donates;
    private final MessagesConfig messages;
    private final Logger logger;

    public PlayerJoinListener(YoPaymentPlugin plugin, DatabaseManager database, PaymentCacheManager cacheManager,
                              DonatesConfig donates, MessagesConfig messages, Logger logger) {
        this.plugin = plugin;
        this.database = database;
        this.cacheManager = cacheManager;
        this.donates = donates;
        this.messages = messages;
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        // 1. Прогрев кэша для PlaceholderAPI
        cacheManager.loadPlayerLatest(database, playerName);

        // 2. Проверка отложенных покупок
        database.getPendingDeliveriesByPlayer(playerName, pendingDeliveries -> {
            if (pendingDeliveries == null || pendingDeliveries.isEmpty()) {
                return;
            }

            // Выполняем выдачу на основном потоке сервера
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }

                for (PaymentRecord record : pendingDeliveries) {
                    DonateItem item = donates.getItem(record.itemId()).orElse(null);
                    if (item == null) {
                        logger.warning("Item " + record.itemId() + " not found for pending payment " + record.paymentId());
                        continue;
                    }

                    logger.info("Delivering pending payment " + record.paymentId() + " to player " + playerName);

                    // Исполнение команд
                    for (String cmd : item.commands()) {
                        String finalCmd = cmd.replace("%player%", playerName.replaceAll("[^a-zA-Z0-9_]", "_"));
                        if (finalCmd.startsWith("/")) {
                            finalCmd = finalCmd.substring(1);
                        }
                        try {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                        } catch (Exception e) {
                            logger.severe("Failed to execute command for player " + playerName + ": " + e.getMessage());
                        }
                    }

                    // Уведомление
                    player.sendMessage(messages.paymentDelivered(item.id(), item.displayName()));

                    // Обновление статуса в БД
                    LocalDateTime now = LocalDateTime.now();
                    database.updatePaymentStatus(record.paymentId(), "succeeded", now, null);

                    PaymentRecord updated = new PaymentRecord(
                        record.paymentId(),
                        record.playerName(),
                        record.playerUuid(),
                        record.itemId(),
                        record.price(),
                        "succeeded",
                        record.confirmationUrl(),
                        record.createdAt(),
                        now,
                        record.createdBy()
                    );
                    cacheManager.updateLatestPayment(updated);
                }
            });
        });
    }
}
