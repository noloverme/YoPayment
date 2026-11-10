package com.noloverme.yopayment.api.model;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Модель запроса для создания платежа в ЮKassa (с поддержкой 54-ФЗ).
 */
public class PaymentCreateRequest {
    private Amount amount;
    private boolean capture;
    private Confirmation confirmation;
    private String description;
    private Map<String, String> metadata;
    private Receipt receipt;

    public PaymentCreateRequest(double price, String returnUrl, String description, String playerName, String itemId, Receipt receipt) {
        this.amount = new Amount(String.format(java.util.Locale.US, "%.2f", price), "RUB");
        this.capture = true;
        this.confirmation = new Confirmation("redirect", returnUrl);
        this.description = description != null && !description.isEmpty() ? description : "Покупка товара " + itemId;
        this.metadata = Map.of(
            "player_name", playerName,
            "item_id", itemId
        );
        this.receipt = receipt;
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

    public static class Receipt {
        private Customer customer;
        private List<ReceiptItem> items;
        @SerializedName("tax_system_code")
        private Integer taxSystemCode;

        public Receipt(String customerEmail, String itemDescription, double price, int vatCode, Integer taxSystemCode) {
            this.customer = new Customer(customerEmail);
            this.items = Collections.singletonList(new ReceiptItem(itemDescription, price, vatCode));
            if (taxSystemCode != null && taxSystemCode > 0) {
                this.taxSystemCode = taxSystemCode;
            }
        }
    }

    public static class Customer {
        private String email;

        public Customer(String email) {
            this.email = email;
        }
    }

    public static class ReceiptItem {
        private String description;
        private String quantity;
        private Amount amount;
        @SerializedName("vat_code")
        private int vatCode;
        @SerializedName("payment_mode")
        private String paymentMode;
        @SerializedName("payment_subject")
        private String paymentSubject;

        public ReceiptItem(String description, double price, int vatCode) {
            this.description = description != null && description.length() > 128
                ? description.substring(0, 128)
                : (description != null ? description : "Товар");
            this.quantity = "1.00";
            this.amount = new Amount(String.format(java.util.Locale.US, "%.2f", price), "RUB");
            this.vatCode = vatCode;
            this.paymentMode = "full_payment";
            this.paymentSubject = "service";
        }
    }
}
