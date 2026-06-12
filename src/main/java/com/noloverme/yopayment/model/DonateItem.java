package com.noloverme.yopayment.model;

import java.util.Collections;
import java.util.List;

/**
 * Запись о доступном товаре из donates.yml.
 */
public record DonateItem(
    String id,
    String displayName,
    double price,
    String description,
    List<String> commands
) {
    public DonateItem {
        commands = Collections.unmodifiableList(commands);
    }
}
