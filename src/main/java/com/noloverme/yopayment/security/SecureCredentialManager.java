package com.noloverme.yopayment.security;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Менеджер для безопасного хранения и управления учётными данными в памяти.
 * Поддерживает очистку чувствительных данных.
 */
public final class SecureCredentialManager {
    private final byte[] encryptionKey;
    private volatile String shopId;
    private volatile byte[] encryptedSecretKey;
    private volatile String returnUrl;
    private final SecureRandom random;

    public SecureCredentialManager(String shopId, String secretKey, String returnUrl) {
        this.shopId = shopId;
        this.returnUrl = returnUrl;
        this.random = new SecureRandom();
        this.encryptionKey = generateEncryptionKey();
        this.encryptedSecretKey = encryptSensitiveData(secretKey);
    }

    /**
     * Генерирует криптографически стойкий ключ шифрования.
     */
    private byte[] generateEncryptionKey() {
        byte[] key = new byte[32];
        random.nextBytes(key);
        return key;
    }

    /**
     * Шифрует чувствительные данные (базовое XOR для демонстрации).
     * В продакшене используйте AES-256 с правильным IV и authenticated encryption.
     */
    private byte[] encryptSensitiveData(String data) {
        if (data == null) {
            return new byte[0];
        }
        byte[] dataBytes = data.getBytes();
        byte[] encrypted = new byte[dataBytes.length];

        for (int i = 0; i < dataBytes.length; i++) {
            encrypted[i] = (byte) (dataBytes[i] ^ encryptionKey[i % encryptionKey.length]);
        }

        return encrypted;
    }

    /**
     * Расшифровывает и возвращает secret key.
     * ВНИМАНИЕ: Данные остаются в памяти! Используйте сразу после получения.
     */
    public String getSecretKey() {
        byte[] decrypted = new byte[encryptedSecretKey.length];

        for (int i = 0; i < encryptedSecretKey.length; i++) {
            decrypted[i] = (byte) (encryptedSecretKey[i] ^ encryptionKey[i % encryptionKey.length]);
        }

        String result = new String(decrypted);
        java.util.Arrays.fill(decrypted, (byte) 0);

        return result;
    }

    /**
     * Генерирует криптографически стойкий ключ идемпотентности.
     */
    public String generateIdempotencyKey(String playerName, String itemId) {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return playerName + "-" + itemId + "-" + randomPart;
    }

    public String getShopId() {
        return shopId;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    /**
     * Очищает все чувствительные данные из памяти.
     */
    public void clear() {
        shopId = null;
        java.util.Arrays.fill(encryptedSecretKey, (byte) 0);
        java.util.Arrays.fill(encryptionKey, (byte) 0);
    }
}
