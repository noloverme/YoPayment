package com.noloverme.yopayment.security;

import java.util.regex.Pattern;

/**
 * Валидатор для проверки безопасности команд перед исполнением.
 * Предотвращает инъекции и выполнение опасных команд.
 */
public final class CommandValidator {
    private static final Pattern DANGEROUS_CHARS = Pattern.compile("[;|&`$()\\n\\r]");
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");

    private CommandValidator() {
        throw new AssertionError("Utility class");
    }

    /**
     * Проверяет, содержит ли строка опасные символы для выполнения в консоли.
     *
     * @param input входная строка
     * @return true если строка безопасна
     */
    public static boolean isSafeForCommand(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return !DANGEROUS_CHARS.matcher(input).find();
    }

    /**
     * Проверяет, является ли строка корректным именем игрока.
     *
     * @param playerName имя игрока
     * @return true если имя соответствует требованиям Minecraft
     */
    public static boolean isValidPlayerName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        return PLAYER_NAME_PATTERN.matcher(playerName).matches();
    }

    /**
     * Санитизирует строку для использования в командах.
     * Удаляет все опасные символы.
     *
     * @param input входная строка
     * @return санитизированная строка
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return DANGEROUS_CHARS.matcher(input).replaceAll("_");
    }
}
