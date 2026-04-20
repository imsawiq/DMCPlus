package org.sawiq.dmcplus.client.feature.trade;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PriceParser {

    private static final Pattern SLOT_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(?:сл\\.?|слот(?:а|ов)?)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*шт\\.?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern AR_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*ар\\.?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final double ITEMS_PER_SLOT = 64.0D;

    private PriceParser() {
    }

    public static PriceQuote parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        String normalized = rawText.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replace("=", " ")
                .replace("-", " ")
                .replace("—", " ")
                .replace("–", " ")
                .replaceAll("\\s+", " ")
                .trim();

        Double slots = extractNumber(SLOT_PATTERN, normalized);
        if (slots == null) {
            Double items = extractNumber(ITEM_PATTERN, normalized);
            if (items != null) {
                slots = items / ITEMS_PER_SLOT;
            }
        }

        Double ars = extractNumber(AR_PATTERN, normalized);

        if (slots == null && (normalized.contains("слот") || normalized.contains("сл"))) {
            slots = 1.0D;
        }
        if (ars == null && normalized.contains("ар")) {
            ars = 1.0D;
        }

        if (slots == null || ars == null || slots <= 0.0D || ars <= 0.0D) {
            return null;
        }

        return new PriceQuote(slots, ars, rawText);
    }

    private static Double extractNumber(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        try {
            return Double.parseDouble(matcher.group(1).replace(',', '.'));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
