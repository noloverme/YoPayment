package com.noloverme.yopayment.placeholder;

import com.noloverme.yopayment.config.DonatesConfig;
import com.noloverme.yopayment.database.DatabaseManager;
import com.noloverme.yopayment.model.DonateItem;
import com.noloverme.yopayment.model.PaymentRecord;
import com.noloverme.yopayment.util.TextUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class YoPaymentPlaceholder extends PlaceholderExpansion {
    private final DatabaseManager database;
    private final DonatesConfig donates;

    public YoPaymentPlaceholder(DatabaseManager database, DonatesConfig donates) {
        this.database = database;
        this.donates = donates;
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
        return "1.0a";
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
        AtomicReference<String> link = new AtomicReference<>("N/A");

        database.getPaymentsByPlayer(playerName, payments -> {
            if (!payments.isEmpty()) {
                for (PaymentRecord p : payments) {
                    if (p.confirmationUrl() != null && !p.confirmationUrl().isEmpty()) {
                        link.set(p.confirmationUrl());
                        break;
                    }
                }
            }
        });

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return link.get();
    }

    private String getCustomFormat(String playerName, String format) {
        AtomicReference<String> result = new AtomicReference<>("");

        database.getPaymentsByPlayer(playerName, payments -> {
            if (!payments.isEmpty()) {
                PaymentRecord latest = payments.get(0);
                Optional<DonateItem> item = donates.getItem(latest.itemId());

                String output = format;
                output = output.replace("{display_name}", item.map(DonateItem::displayName).orElse("Unknown"));
                output = output.replace("{item}", latest.itemId());
                output = output.replace("{price}", TextUtil.formatPrice(latest.price()));
                output = output.replace("{description}", item.map(DonateItem::description).orElse(""));
                output = output.replace("{link}", latest.confirmationUrl() != null ? latest.confirmationUrl() : "N/A");
                output = output.replace("{status}", latest.status());
                output = output.replace("{created_at}", TextUtil.formatDate(latest.createdAt()));

                output = TextUtil.hexToColor(output);

                result.set(output);
            }
        });

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return result.get();
    }
}
