package com.sms.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC-4180 style CSV helpers.
 *
 * <p>The store keeps exactly one record per physical line, so embedded newlines
 * are encoded with {@link #encodeNewlines(String)} before quoting and restored
 * on read. Everything else (commas, quotes) follows the usual quote-and-double
 * convention.</p>
 */
public final class Csv {

    private static final char SEPARATOR = ',';
    private static final char QUOTE = '"';

    private Csv() {
    }

    /** Quotes {@code value} only when it actually needs quoting. */
    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean needsQuotes = value.indexOf(SEPARATOR) >= 0
                || value.indexOf(QUOTE) >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0
                || value.startsWith(" ")
                || value.endsWith(" ");
        if (!needsQuotes) {
            return value;
        }
        return QUOTE + value.replace("\"", "\"\"") + QUOTE;
    }

    /** Joins fields into a single CSV line, escaping each one. */
    public static String join(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    /**
     * Splits one CSV line into its fields.
     *
     * @param line a single physical line (without the trailing newline)
     * @return the parsed fields; never {@code null}, never empty
     */
    public static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == QUOTE) {
                    boolean escapedQuote = i + 1 < line.length() && line.charAt(i + 1) == QUOTE;
                    if (escapedQuote) {
                        current.append(QUOTE);
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(ch);
                }
            } else if (ch == QUOTE) {
                inQuotes = true;
            } else if (ch == SEPARATOR) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    /** Replaces real line breaks with the two-character sequence {@code \n}. */
    public static String encodeNewlines(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\r\n", "\\n")
                .replace('\r', '\n')
                .replace("\n", "\\n");
    }

    /** Inverse of {@link #encodeNewlines(String)}. */
    public static String decodeNewlines(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                if (next == 'n') {
                    sb.append('\n');
                } else if (next == '\\') {
                    sb.append('\\');
                } else {
                    sb.append(ch).append(next);
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
