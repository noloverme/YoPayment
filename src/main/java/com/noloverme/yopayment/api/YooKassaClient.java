package com.noloverme.yopayment.api;

import com.noloverme.yopayment.api.model.PaymentCreateRequest;
import com.noloverme.yopayment.api.model.PaymentResponse;
import com.noloverme.yopayment.model.DonateItem;
import com.noloverme.yopayment.security.SecureCredentialManager;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * HTTP-клиент для работы с ЮKassa API.
 * Обеспечивает безопасное хранение учётных данных и защиту от основных уязвимостей.
 */
public class YooKassaClient {
    private static final String API_URL = "https://api.yookassa.ru/v3/payments";
    private static final String USER_AGENT = "YoPayment/1.0a";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Gson gson = new Gson();

    private final SecureCredentialManager credentialManager;
    private final HttpClient httpClient;
    private final Logger logger;
    private final boolean silentMode;

    public YooKassaClient(String shopId, String secretKey, String returnUrl, Logger logger) {
        this(shopId, secretKey, returnUrl, logger, false);
    }

    public YooKassaClient(String shopId, String secretKey, String returnUrl, Logger logger, boolean silentMode) {
        this.credentialManager = new SecureCredentialManager(shopId, secretKey, returnUrl);
        this.logger = logger;
        this.silentMode = silentMode;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    }

    private void log(String message) {
        if (!silentMode) {
            logger.info(message);
        }
    }

    /**
     * Создаёт платёж в ЮKassa.
     *
     * @return CompletableFuture с результатом создания платежа
     */
    public CompletableFuture<PaymentResponse> createPayment(DonateItem item, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            log("Creating payment for player " + playerName + ", item: " + item.id() +
                ", price: " + item.price() + " RUB");

            PaymentCreateRequest request = new PaymentCreateRequest(
                item.price(),
                credentialManager.getReturnUrl(),
                item.description(),
                playerName,
                item.id()
            );

            String jsonBody = gson.toJson(request);

            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Basic " + getBasicAuth())
                    .header("Content-Type", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .header("Idempotence-Key", credentialManager.generateIdempotencyKey(playerName, item.id()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(TIMEOUT)
                    .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    logger.severe("ERROR: Failed to create payment (HTTP " + response.statusCode() + ")");
                    throw new YooKassaException(response.statusCode(), "Failed to create payment");
                }

                PaymentResponse paymentResponse = PaymentResponse.fromJson(response.body());
                log("Payment created: id=" + paymentResponse.id() + ", status=" + paymentResponse.status().getValue());

                return paymentResponse;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.severe("ERROR: Payment creation interrupted");
                throw new YooKassaException(0, "Interrupted: " + e.getMessage());
            } catch (Exception e) {
                logger.severe("ERROR: " + e.getMessage());
                throw new YooKassaException(0, e.getMessage());
            }
        });
    }

    /**
     * Запрашивает текущий статус платежа.
     *
     * @return CompletableFuture с результатом проверки статуса
     */
    public CompletableFuture<PaymentResponse> getPaymentStatus(String paymentId) {
        return CompletableFuture.supplyAsync(() -> {
            log("Checking payment status: id=" + paymentId);

            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/" + paymentId))
                    .header("Authorization", "Basic " + getBasicAuth())
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .timeout(TIMEOUT)
                    .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    logger.severe("ERROR: Failed to get payment status (HTTP " + response.statusCode() + ")");
                    throw new YooKassaException(response.statusCode(), "Failed to get payment status");
                }

                PaymentResponse paymentResponse = PaymentResponse.fromJson(response.body());
                log("Payment " + paymentId + " status: " + paymentResponse.status().getValue());

                return paymentResponse;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.severe("ERROR: Status check interrupted");
                throw new YooKassaException(0, "Interrupted: " + e.getMessage());
            } catch (Exception e) {
                logger.severe("ERROR: " + e.getMessage());
                throw new YooKassaException(0, e.getMessage());
            }
        });
    }

    /**
     * Генерирует Base64-кодированную пару учётных данных для Basic Auth.
     * Secret key расшифровывается только в момент использования.
     */
    private String getBasicAuth() {
        String secretKey = credentialManager.getSecretKey();
        try {
            String credentials = credentialManager.getShopId() + ":" + secretKey;
            return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        } finally {
            // Очищаем secret key из строки (Java GC это сделает)
            java.util.Arrays.fill(secretKey.toCharArray(), '\0');
        }
    }

    /**
     * Очищает чувствительные данные при выключении.
     */
    public void shutdown() {
        credentialManager.clear();
    }

    /**
     * Кастомное исключение для ошибок ЮKassa.
     */
    public static class YooKassaException extends RuntimeException {
        private final int httpCode;

        public YooKassaException(int httpCode, String message) {
            super(message);
            this.httpCode = httpCode;
        }

        public int getHttpCode() {
            return httpCode;
        }
    }
}
