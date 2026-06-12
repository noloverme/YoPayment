package com.noloverme.yopayment.command;

import com.noloverme.yopayment.YoPaymentPlugin;
import com.noloverme.yopayment.api.YooKassaClient;
import com.noloverme.yopayment.config.DonatesConfig;
import com.noloverme.yopayment.config.MainConfig;
import com.noloverme.yopayment.config.MessagesConfig;
import com.noloverme.yopayment.database.DatabaseManager;
import com.noloverme.yopayment.model.DonateItem;
import com.noloverme.yopayment.model.PaymentRecord;
import com.noloverme.yopayment.api.model.PaymentResponse;
import com.noloverme.yopayment.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Обработчик команды /yopayment.
 */
public class YoPaymentCommand implements CommandExecutor, TabCompleter {
    private final DatabaseManager database;
    private final YooKassaClient yooKassaClient;
    private final MainConfig mainConfig;
    private final MessagesConfig messages;
    private final DonatesConfig donates;
    private final int paymentTimeout;
    private final Logger logger;

    public YoPaymentCommand(DatabaseManager database, YooKassaClient yooKassaClient,
                            MainConfig mainConfig, MessagesConfig messages, DonatesConfig donates,
                            int paymentTimeout, Logger logger) {
        this.database = database;
        this.yooKassaClient = yooKassaClient;
        this.mainConfig = mainConfig;
        this.messages = messages;
        this.donates = donates;
        this.paymentTimeout = paymentTimeout;
        this.logger = logger;
    }

    private void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty() || "none".equalsIgnoreCase(message)) {
            return;
        }
        sender.sendMessage(TextUtil.hexToColor(message));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendMessage(sender, messages.help());
            return true;
        }

        String subcommand = args[0].toLowerCase();

        return switch (subcommand) {
            case "create" -> handleCreate(sender, args);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender, args);
            default -> {
                sendMessage(sender, messages.unknownCommand(subcommand));
                sendMessage(sender, messages.help());
                yield false;
            }
        };
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("yopayment.create")) {
            sendMessage(sender, messages.noPermission());
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, messages.createUsage());
            String items = String.join(", ", donates.getItemIds());
            sendMessage(sender, messages.createItems(items));
            return true;
        }

        String itemId = args[1];
        Optional<DonateItem> item = donates.getItem(itemId);

        if (item.isEmpty()) {
            String items = String.join(", ", donates.getItemIds());
            sendMessage(sender, messages.itemNotFound(itemId, items));
            return true;
        }

        String targetPlayer;
        if (args.length >= 3) {
            if (!sender.hasPermission("yopayment.create.others")) {
                sendMessage(sender, messages.noPermission());
                return true;
            }
            targetPlayer = args[2];
        } else {
            if (!(sender instanceof Player)) {
                sendMessage(sender, messages.onlyPlayer());
                return true;
            }
            targetPlayer = ((Player) sender).getName();
        }

        String createdBy = sender instanceof Player ? ((Player) sender).getName() : "Console";
        DonateItem donateItem = item.get();

        new Thread(() -> {
            try {
                PaymentResponse response = yooKassaClient.createPayment(donateItem, targetPlayer);

                PaymentRecord record = new PaymentRecord(
                    response.id(),
                    targetPlayer,
                    null,
                    donateItem.id(),
                    donateItem.price(),
                    response.status().getValue(),
                    response.confirmationUrl(),
                    LocalDateTime.now(),
                    null,
                    createdBy
                );

                database.savePayment(record, () -> {
                    Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("YoPayment"), () -> {
                        if (args.length >= 3) {
                            sendMessage(sender, messages.paymentCreatedForOther(
                                targetPlayer,
                                donateItem.id(),
                                donateItem.displayName(),
                                TextUtil.formatPrice(donateItem.price())
                            ));
                        } else {
                            sendMessage(sender, messages.paymentCreated(
                                donateItem.id(),
                                donateItem.displayName(),
                                TextUtil.formatPrice(donateItem.price()),
                                String.valueOf(paymentTimeout)
                            ));
                        }
                        sendMessage(sender, "&#a0a0a0Ссылка: " + (response.confirmationUrl() != null ? response.confirmationUrl() : "error"));
                    });
                });
            } catch (YooKassaClient.YooKassaException e) {
                logger.severe("[YoPayment] Failed to create payment: " + e.getMessage());
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("YoPayment"), () -> {
                    sendMessage(sender, messages.paymentError());
                });
            }
        }).start();

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("yopayment.reload")) {
            sendMessage(sender, messages.noPermission());
            return true;
        }

        try {
            YoPaymentPlugin plugin = (YoPaymentPlugin) Bukkit.getPluginManager().getPlugin("YoPayment");
            if (plugin != null) {
                plugin.reloadConfig();
                plugin.reloadPlugin(plugin.getDataFolder());
                sendMessage(sender, messages.configReloaded());
            } else {
                sendMessage(sender, "&#ff0000Плагин YoPayment не найден");
            }
        } catch (Exception e) {
            sendMessage(sender, "&#ff0000Ошибка при перезагрузке конфигурации: " + e.getMessage());
        }
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("yopayment.list")) {
            sendMessage(sender, messages.noPermission());
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, messages.listUsage());
            return true;
        }

        String listType = args[1].toLowerCase();

        if ("active".equals(listType)) {
            database.getActivePayments(payments -> {
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("YoPayment"), () -> {
                    sendMessage(sender, messages.listActiveHeader());
                    if (payments.isEmpty()) {
                        sendMessage(sender, messages.listActiveEmpty());
                    } else {
                        for (PaymentRecord payment : payments) {
                            String displayName = donates.getItem(payment.itemId()).map(DonateItem::displayName).orElse(payment.itemId());
                            sendMessage(sender, messages.listActiveEntry(
                                payment.paymentId(),
                                payment.itemId(),
                                displayName,
                                payment.playerName(),
                                TextUtil.formatPrice(payment.price())
                            ));
                        }
                    }
                });
            });
        } else {
            String playerName = listType;

            if (!sender.hasPermission("yopayment.list.others") && !playerName.equals(sender.getName())) {
                sendMessage(sender, messages.noPermission());
                return true;
            }

            database.getPaymentsByPlayer(playerName, payments -> {
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("YoPayment"), () -> {
                    sendMessage(sender, messages.listPlayerHeader(playerName));
                    if (payments.isEmpty()) {
                        sendMessage(sender, messages.listPlayerEmpty(playerName));
                    } else {
                        for (PaymentRecord payment : payments) {
                            String displayName = donates.getItem(payment.itemId()).map(DonateItem::displayName).orElse(payment.itemId());
                            sendMessage(sender, messages.listPlayerEntry(
                                payment.paymentId(),
                                payment.itemId(),
                                displayName,
                                TextUtil.formatPrice(payment.price()),
                                messages.getLocalizedStatus(payment.status()),
                                TextUtil.formatDate(payment.createdAt())
                            ));
                        }
                    }
                });
            });
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "reload", "list");
        }

        if (args.length == 2) {
            if ("create".equals(args[0].toLowerCase())) {
                return new ArrayList<>(donates.getItemIds());
            }
            if ("list".equals(args[0].toLowerCase())) {
                List<String> completions = new ArrayList<>(List.of("active"));
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
                return completions;
            }
        }

        if (args.length == 3 && "create".equals(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
        }

        return List.of();
    }
}
