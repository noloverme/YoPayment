package com.noloverme.yopayment.model;

import org.jetbrains.annotations.Nullable;
import java.time.LocalDateTime;

/**
 * Запись о платеже в базе данных.
 */
public record PaymentRecord(
    String paymentId,
    String playerName,
    @Nullable String playerUuid,
    String itemId,
    double price,
    String status,
    String confirmationUrl,
    LocalDateTime createdAt,
    @Nullable LocalDateTime completedAt,
    String createdBy
) {}
