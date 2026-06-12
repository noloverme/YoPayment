package com.noloverme.yopayment.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

/**
 * Утилиты для работы с текстом.
 */
public final class TextUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    private TextUtil() {
    }

    /**
     * Преобразует HEX коды &#RRGGBB и обычные цвета &c в BungeeCord ChatColor.
     */
    public static String hexToColor(String text) {
        if (text == null) return null;

        String result = text;

        // Сначала преобразуем HEX коды &#RRGGBB в BungeeCord ChatColor
        result = HEX_PATTERN.matcher(result).replaceAll(match -> {
            String hex = match.group(1);
            return ChatColor.of("#" + hex).toString();
        });

        // Потом преобразуем обычные цвета Bukkit (&c, &a, &f, etc.) в §
        result = result.replace("&", "§");

        return result;
    }

    /**
     * Заменяет плейсхолдеры вида {key} в строке.
     */
    public static String replacePlaceholders(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return hexToColor(result);
    }

    /**
     * Форматирует цену: 30.0 → "30.00"
     */
    public static String formatPrice(double price) {
        return String.format(java.util.Locale.US, "%.2f", price);
    }

    /**
     * Форматирует LocalDateTime в читаемую строку dd.MM.yyyy HH:mm
     */
    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "—";
        return dateTime.format(DATE_FORMATTER);
    }
}
