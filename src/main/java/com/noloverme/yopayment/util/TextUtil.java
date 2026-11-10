package com.noloverme.yopayment.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Утилиты для форматирования текста и отправки сообщений (1.16+ HEX и кликабельные компоненты).
 */
public final class TextUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    private TextUtil() {
    }

    /**
     * Преобразует HEX коды &#RRGGBB и обычные цвета &c в форматированный цвет.
     */
    public static String hexToColor(String text) {
        if (text == null) return null;

        String result = text;

        // Преобразуем HEX коды &#RRGGBB в BungeeCord ChatColor (работает с 1.16+)
        result = HEX_PATTERN.matcher(result).replaceAll(match -> {
            String hex = match.group(1);
            return ChatColor.of("#" + hex).toString();
        });

        // Преобразуем стандартные коды цветов (&a, &c и т.д.)
        result = ChatColor.translateAlternateColorCodes('&', result);

        return result;
    }

    /**
     * Заменяет плейсхолдеры вида {key} в строке.
     */
    public static String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null) return "";
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return hexToColor(result);
    }

    /**
     * Отправляет интерактивную ссылку с кнопкой перехода в браузер для игроков (1.16+),
     * или обычный текст со ссылкой для консоли.
     */
    public static void sendClickableLink(CommandSender sender, String message, String buttonText, String url, String hoverText) {
        if (sender instanceof Player player) {
            TextComponent root = new TextComponent(hexToColor(message));
            TextComponent button = new TextComponent(hexToColor(buttonText));
            button.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
            button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hexToColor(hoverText))));
            root.addExtra(button);
            player.spigot().sendMessage(root);
        } else {
            sender.sendMessage(hexToColor(message + " " + url));
        }
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
