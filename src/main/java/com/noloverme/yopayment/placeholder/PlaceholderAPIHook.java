package com.noloverme.yopayment.placeholder;

import com.noloverme.yopayment.config.DonatesConfig;
import com.noloverme.yopayment.database.DatabaseManager;
import java.util.logging.Logger;

public class PlaceholderAPIHook {
    private final DatabaseManager database;
    private final DonatesConfig donates;
    private final Logger logger;

    public PlaceholderAPIHook(DatabaseManager database, DonatesConfig donates, Logger logger) {
        this.database = database;
        this.donates = donates;
        this.logger = logger;
    }

    public void register() {
        try {
            Class<?> placeholderClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            YoPaymentPlaceholder placeholder = new YoPaymentPlaceholder(database, donates);
            placeholderClass.getMethod("register").invoke(placeholder);
            logger.info("PlaceholderAPI hook enabled");
        } catch (ClassNotFoundException e) {
            logger.fine("PlaceholderAPI not available");
        } catch (Exception e) {
            logger.warning("Failed to register PlaceholderAPI: " + e.getMessage());
        }
    }
}
