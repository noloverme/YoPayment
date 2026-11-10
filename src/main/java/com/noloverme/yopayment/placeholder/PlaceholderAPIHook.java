package com.noloverme.yopayment.placeholder;

import com.noloverme.yopayment.cache.PaymentCacheManager;
import com.noloverme.yopayment.config.DonatesConfig;
import com.noloverme.yopayment.database.DatabaseManager;
import java.util.logging.Logger;

public class PlaceholderAPIHook {
    private final DatabaseManager database;
    private final DonatesConfig donates;
    private final PaymentCacheManager cacheManager;
    private final Logger logger;
    private YoPaymentPlaceholder expansion;

    public PlaceholderAPIHook(DatabaseManager database, DonatesConfig donates, PaymentCacheManager cacheManager, Logger logger) {
        this.database = database;
        this.donates = donates;
        this.cacheManager = cacheManager;
        this.logger = logger;
    }

    public void register() {
        try {
            if (expansion != null) {
                expansion.unregister();
            }
            expansion = new YoPaymentPlaceholder(database, donates, cacheManager);
            expansion.register();
            logger.info("PlaceholderAPI hook enabled");
        } catch (NoClassDefFoundError | Exception e) {
            logger.fine("PlaceholderAPI not available or failed: " + e.getMessage());
        }
    }

    public void unregister() {
        if (expansion != null) {
            try {
                expansion.unregister();
            } catch (Exception ignored) {
            }
            expansion = null;
        }
    }
}
