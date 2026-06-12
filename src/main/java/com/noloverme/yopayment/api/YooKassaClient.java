package com.noloverme.yopayment.api;

import com.noloverme.yopayment.api.model.PaymentCreateRequest;
import com.noloverme.yopayment.api.model.PaymentResponse;
import com.noloverme.yopayment.model.DonateItem;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * HTTP-клиент для работы с ЮKassa API.
 */
public class YooKassaClient {
    private static final String API_URL = "https://api.yookassa.ru/v3/payments";
    private static final String USER_AGENT = "YoPayment/1.0a";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Gson gson = new Gson();

    private final String shopId;
    private final String secretKey;
    private final String returnUrl;
    private final HttpClient httpClient;
    private final Logger logger;
    private final boolean silentMode;

    public YooKassaClient(String shopId, String secretKey, String returnUrl, Logger logger) {
        this(shopId, secretKey, returnUrl, logger, false);
    }

    public YooKassaClient(String shopId, String secretKey, String returnUrl, Logger logger, boolean silentMode) {
        this.shopId = shopId;
        this.secretKey = secretKey;
        this.returnUrl = returnUrl;
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
     * Должен вызваться из не-главного потока.
     */
    public PaymentResponse createPayment(DonateItem item, String playerName) throws YooKassaException {
        log("[YoPayment] Creating payment for player " + playerName + ", item: " + item.id() + ", price: " + item.price() + " RUB");

        PaymentCreateRequest request = new PaymentCreateRequest(
            item.price(),
            returnUrl,
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
                .header("Idempotence-Key", generateIdempotencyKey(playerName, item.id()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                logger.severe("[YoPayment] ERROR: Failed to create payment (HTTP " + response.statusCode() + "): " + response.body());
                throw new YooKassaException(response.statusCode(), "Failed to create payment: " + response.body());
            }

            PaymentResponse paymentResponse = PaymentResponse.fromJson(response.body());
            log("[YoPayment] Payment created: id=" + paymentResponse.id() + ", status=" + paymentResponse.status().getValue());

            return paymentResponse;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.severe("[YoPayment] ERROR: Payment creation interrupted");
            throw new YooKassaException(0, "Interrupted: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("[YoPayment] ERROR: " + e.getMessage());
            throw new YooKassaException(0, e.getMessage());
        }
    }

    /**
     * Запрашивает текущий статус платежа.
     */
    public PaymentResponse getPaymentStatus(String paymentId) throws YooKassaException {
        log("[YoPayment] Checking payment status: id=" + paymentId);

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
                logger.severe("[YoPayment] ERROR: Failed to get payment status (HTTP " + response.statusCode() + "): " + response.body());
                throw new YooKassaException(response.statusCode(), "Failed to get payment status: " + response.body());
            }

            PaymentResponse paymentResponse = PaymentResponse.fromJson(response.body());
            log("[YoPayment] Payment " + paymentId + " status: " + paymentResponse.status().getValue());

            return paymentResponse;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.severe("[YoPayment] ERROR: Status check interrupted");
            throw new YooKassaException(0, "Interrupted: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("[YoPayment] ERROR: " + e.getMessage());
            throw new YooKassaException(0, e.getMessage());
        }
    }

    private String getBasicAuth() {
        String credentials = shopId + ":" + secretKey;
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String generateIdempotencyKey(String playerName, String itemId) {
        return playerName + "-" + itemId + "-" + System.currentTimeMillis();
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
