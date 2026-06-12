package com.noloverme.yopayment.config;

import org.bukkit.configuration.file.FileConfiguration;
import com.noloverme.yopayment.util.TextUtil;
import java.util.Map;

/**
 * Обёртка над messages.yml. Использует HEX-цвета в формате &#RRGGBB.
 */
public class MessagesConfig {
    private final FileConfiguration config;
    private String prefix;

    public MessagesConfig(FileConfiguration config) {
        this.config = config;
        loadPrefix();
    }

    private void loadPrefix() {
        this.prefix = config.getString("prefix", "&#27ae60[&#00bfff YoPayment&#27ae60] ");
    }

    /**
     * Возвращает строку с подставленными плейсхолдерами и префиксом.
     */
    public String get(String key, Map<String, String> placeholders) {
        String raw = config.getString(key, "");
        if (raw.isEmpty()) {
            return "";
        }
        String replaced = TextUtil.replacePlaceholders(raw, placeholders);
        return TextUtil.hexToColor(prefix + replaced);
    }

    /**
     * Возвращает строку без плейсхолдеров.
     */
    public String get(String key) {
        String raw = config.getString(key, "");
        if (raw.isEmpty()) {
            return "";
        }
        return TextUtil.hexToColor(prefix + raw);
    }

    public void reload(FileConfiguration config) {
        loadPrefix();
    }

    // Методы для удобства

    public String paymentCreated(String itemId, String displayName, String price, String timeout) {
        return get("payment-created", Map.of(
            "item", itemId,
            "display_name", displayName,
            "price", price,
            "timeout", timeout
        ));
    }

    public String paymentCreatedForOther(String player, String itemId, String displayName, String price) {
        return get("payment-created-for-other", Map.of(
            "player", player,
            "item", itemId,
            "display_name", displayName,
            "price", price
        ));
    }

    public String paymentSuccess(String itemId, String displayName) {
        return get("payment-success", Map.of("item", itemId, "display_name", displayName));
    }

    public String paymentFailed(String itemId, String displayName) {
        return get("payment-failed", Map.of("item", itemId, "display_name", displayName));
    }

    public String itemNotFound(String item, String items) {
        return get("item-not-found", Map.of("item", item, "items", items));
    }

    public String playerNotFound(String player) {
        return get("player-not-found", Map.of("player", player));
    }

    public String noPermission() {
        return get("no-permission");
    }

    public String paymentError() {
        return get("payment-error");
    }

    public String onlyPlayer() {
        return get("only-player");
    }

    public String unknownSubcommand() {
        return get("unknown-subcommand");
    }

    public String configReloaded() {
        return get("config-reloaded");
    }

    public String listActiveHeader() {
        return get("list-active-header");
    }

    public String listActiveEntry(String paymentId, String itemId, String displayName, String player, String price) {
        return get("list-active-entry", Map.of(
            "payment_id", paymentId,
            "item", itemId,
            "display_name", displayName,
            "player", player,
            "price", price
        ));
    }

    public String listActiveEmpty() {
        return get("list-active-empty");
    }

    public String listPlayerHeader(String player) {
        return get("list-player-header", Map.of("player", player));
    }

    public String listPlayerEntry(String paymentId, String itemId, String displayName, String price, String status, String date) {
        return get("list-player-entry", Map.of(
            "payment_id", paymentId,
            "item", itemId,
            "display_name", displayName,
            "price", price,
            "status", status,
            "date", date
        ));
    }

    public String listPlayerEmpty(String player) {
        return get("list-player-empty", Map.of("player", player));
    }

    public String statusPending() {
        return get("status-pending");
    }

    public String statusSucceeded() {
        return get("status-succeeded");
    }

    public String statusCanceled() {
        return get("status-canceled");
    }

    public String statusWaitingForCapture() {
        return get("status-waiting_for_capture");
    }

    public String help() {
        return get("help");
    }

    public String unknownCommand(String command) {
        return get("unknown-command", Map.of("command", command));
    }

    public String createUsage() {
        return get("create-usage");
    }

    public String createItems(String items) {
        return get("create-items", Map.of("items", items));
    }

    public String listUsage() {
        return get("list-usage");
    }

    public String invalidPlayerName(String player) {
        return get("invalid-player-name", Map.of("player", player));
    }

    public String getLocalizedStatus(String status) {
        return switch (status) {
            case "pending" -> statusPending();
            case "succeeded" -> statusSucceeded();
            case "canceled" -> statusCanceled();
            case "waiting_for_capture" -> statusWaitingForCapture();
            default -> status;
        };
    }
}
