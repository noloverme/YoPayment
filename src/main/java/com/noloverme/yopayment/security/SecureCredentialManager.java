package com.noloverme.yopayment.security;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Менеджер для хранения учётных данных и генерации криптографически стойких ключей идемпотентности.
 */
public final class SecureCredentialManager {
    private final String shopId;
    private final String secretKey;
    private final String returnUrl;
    private final SecureRandom random;

    public SecureCredentialManager(String shopId, String secretKey, String returnUrl) {
        this.shopId = shopId != null ? shopId.trim() : "";
        this.secretKey = secretKey != null ? secretKey.trim() : "";
        this.returnUrl = returnUrl != null ? returnUrl.trim() : "";
        this.random = new SecureRandom();
    }

    /**
     * Возвращает секретный ключ для Basic Auth.
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * Генерирует криптографически стойкий ключ идемпотентности до 64 символов (рекомендация ЮKassa).
     */
    public String generateIdempotencyKey(String playerName, String itemId) {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String prefix = (playerName + "_" + itemId + "_").replaceAll("[^a-zA-Z0-9_-]", "");
        if (prefix.length() > 32) {
            prefix = prefix.substring(0, 32);
        }
        return prefix + randomPart;
    }

    public String getShopId() {
        return shopId;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    /**
     * Очистка данных при завершении работы плагина.
     */
    public void clear() {
        // Garbage collection handles cleared references
    }
}
