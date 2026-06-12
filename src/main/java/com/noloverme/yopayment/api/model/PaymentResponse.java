package com.noloverme.yopayment.api.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

/**
 * Модель ответа от ЮKassa при создании или получении статуса платежа.
 */
public record PaymentResponse(
    String id,
    PaymentStatus status,
    boolean paid,
    @Nullable String confirmationUrl,
    String createdAt
) {
    private static final Gson gson = new Gson();

    /**
     * Парсит JSON-ответ ЮKassa в PaymentResponse.
     */
    public static PaymentResponse fromJson(String json) {
        JsonObject obj = gson.fromJson(json, JsonObject.class);

        String id = obj.get("id").getAsString();
        String statusStr = obj.get("status").getAsString();
        boolean paid = obj.get("paid").getAsBoolean();
        String createdAt = obj.get("created_at").getAsString();

        String confirmationUrl = null;
        if (obj.has("confirmation") && obj.get("confirmation").isJsonObject()) {
            JsonObject confirmation = obj.getAsJsonObject("confirmation");
            if (confirmation.has("confirmation_url")) {
                confirmationUrl = confirmation.get("confirmation_url").getAsString();
            }
        }

        return new PaymentResponse(
            id,
            PaymentStatus.fromString(statusStr),
            paid,
            confirmationUrl,
            createdAt
        );
    }

    /**
     * Сериализует ответ обратно в JSON (для отладки).
     */
    public String toJson() {
        return gson.toJson(new Object() {
            public final String id = PaymentResponse.this.id;
            public final String status = PaymentResponse.this.status.getValue();
            public final boolean paid = PaymentResponse.this.paid;
            public final String confirmation_url = PaymentResponse.this.confirmationUrl;
            public final String created_at = PaymentResponse.this.createdAt;
        });
    }
}
