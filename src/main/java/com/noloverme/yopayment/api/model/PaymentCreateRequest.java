package com.noloverme.yopayment.api.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * Модель запроса для создания платежа в ЮKassa.
 */
public class PaymentCreateRequest {
    private Amount amount;
    private boolean capture;
    private Confirmation confirmation;
    private String description;
    private Map<String, String> metadata;

    public PaymentCreateRequest(double price, String returnUrl, String description, String playerName, String itemId) {
        this.amount = new Amount(String.format(java.util.Locale.US, "%.2f", price), "RUB");
        this.capture = true;
        this.confirmation = new Confirmation("redirect", returnUrl);
        this.description = description != null ? description : "Payment";
        this.metadata = Map.of(
            "player_name", playerName,
            "item_id", itemId
        );
    }

    public static class Amount {
        private String value;
        private String currency;

        public Amount(String value, String currency) {
            this.value = value;
            this.currency = currency;
        }
    }

    public static class Confirmation {
        private String type;

        @SerializedName("return_url")
        private String returnUrl;

        public Confirmation(String type, String returnUrl) {
            this.type = type;
            this.returnUrl = returnUrl;
        }
    }
}
