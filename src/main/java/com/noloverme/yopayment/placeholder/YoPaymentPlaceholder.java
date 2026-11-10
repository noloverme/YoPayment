package com.noloverme.yopayment.placeholder;

import com.noloverme.yopayment.cache.PaymentCacheManager;
import com.noloverme.yopayment.config.DonatesConfig;
import com.noloverme.yopayment.database.DatabaseManager;
import com.noloverme.yopayment.model.DonateItem;
import com.noloverme.yopayment.model.PaymentRecord;
import com.noloverme.yopayment.util.TextUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Расширение PlaceholderAPI для YoPayment.
 * Работает полностью без блокировок и без задержек основного потока сервера,
 * получая данные из памяти через PaymentCacheManager.
 */
public class YoPaymentPlaceholder extends PlaceholderExpansion {
    private final DatabaseManager database;
    private final DonatesConfig donates;
    private final PaymentCacheManager cacheManager;

    public YoPaymentPlaceholder(DatabaseManager database, DonatesConfig donates, PaymentCacheManager cacheManager) {
        this.database = database;
        this.donates = donates;
        this.cacheManager = cacheManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "YoPayment";
    }

    @Override
    public @NotNull String getAuthor() {
        return "noloverme";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0b";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        if ("Link".equalsIgnoreCase(params)) {
            return getLatestPaymentLink(player.getName());
        }

        if (params.startsWith("Custom:")) {
            String format = params.substring(7);
            return getCustomFormat(player.getName(), format);
        }

        return "";
    }

    private String getLatestPaymentLink(String playerName) {
        PaymentRecord latest = cacheManager.getLatestPayment(playerName);
        if (latest == null) {
            // Загружаем асинхронно для последующих вызовов, не блокируя тик
            cacheManager.loadPlayerLatest(database, playerName);
            return "N/A";
        }

        if (latest.confirmationUrl() != null && !latest.confirmationUrl().isEmpty()) {
            return latest.confirmationUrl();
        }
        return "N/A";
    }

    private String getCustomFormat(String playerName, String format) {
        PaymentRecord latest = cacheManager.getLatestPayment(playerName);
        if (latest == null) {
            // Загружаем асинхронно для последующих вызовов, не блокируя тик
            cacheManager.loadPlayerLatest(database, playerName);
            return "";
        }

        Optional<DonateItem> item = donates.getItem(latest.itemId());

        String output = format;
        output = output.replace("{display_name}", item.map(DonateItem::displayName).orElse("Unknown"));
        output = output.replace("{item}", latest.itemId());
        output = output.replace("{price}", TextUtil.formatPrice(latest.price()));
        output = output.replace("{description}", item.map(DonateItem::description).orElse(""));
        output = output.replace("{link}", latest.confirmationUrl() != null ? latest.confirmationUrl() : "N/A");
        output = output.replace("{status}", latest.status());
        output = output.replace("{created_at}", TextUtil.formatDate(latest.createdAt()));

        return TextUtil.hexToColor(output);
    }
}
