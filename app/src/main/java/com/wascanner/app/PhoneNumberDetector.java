package com.wascanner.app;

import java.util.regex.Pattern;

public class PhoneNumberDetector {

    // Matches international and local phone number formats
    private static final Pattern WA_NUMBER_PATTERN = Pattern.compile(
            "^[+]?[\\d][\\d\\s\\-().]{5,17}[\\d]$"
    );

    /**
     * Returns true if the text string looks like a phone number
     * (i.e. it IS a phone number, not a real person's name).
     */
    public static boolean isPhoneNumberText(String text) {
        if (text == null) return false;
        String t = text.trim();
        if (t.length() < 6 || t.length() > 22) return false;

        // Count digits
        int digits = 0;
        for (char c : t.toCharArray()) {
            if (Character.isDigit(c)) digits++;
        }

        // If majority of chars are digits, treat as number
        double ratio = (double) digits / t.length();
        if (ratio >= 0.55 && digits >= 6) return true;

        return WA_NUMBER_PATTERN.matcher(t).matches();
    }

    /**
     * Normalize a phone number — strip spaces, dashes, parens. Keep leading +.
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        boolean hasPlus = t.startsWith("+");
        String digits = t.replaceAll("[^0-9]", "");
        if (digits.length() < 6) return null;
        return hasPlus ? "+" + digits : digits;
    }
}
