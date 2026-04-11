package org.example.bill.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public final class TradeTimeUtil {

    private TradeTimeUtil() {}

    public static Optional<LocalDateTime> parse(String tradeTime) {
        if (tradeTime == null || tradeTime.isBlank()) {
            return Optional.empty();
        }
        String s = tradeTime.trim();
        try {
            return Optional.of(
                    LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Optional.of(
                    LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Optional.of(LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public static Optional<LocalDate> parseDate(String tradeTime) {
        return parse(tradeTime).map(LocalDateTime::toLocalDate);
    }
}
