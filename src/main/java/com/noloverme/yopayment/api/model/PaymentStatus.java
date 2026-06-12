package com.noloverme.yopayment.api.model;

/**
 * Статусы платежа в ЮKassa.
 */
public enum PaymentStatus {
    PENDING("pending"),
    WAITING_FOR_CAPTURE("waiting_for_capture"),
    SUCCEEDED("succeeded"),
    CANCELED("canceled"),
    UNKNOWN("unknown");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PaymentStatus fromString(String s) {
        if (s == null) return UNKNOWN;
        for (PaymentStatus ps : values()) {
            if (ps.value.equalsIgnoreCase(s)) return ps;
        }
        return UNKNOWN;
    }
}
