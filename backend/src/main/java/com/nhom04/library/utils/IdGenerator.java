package com.nhom04.library.utils;

import java.util.Collection;

public final class IdGenerator {

    private IdGenerator() {
    }

    /**
     * Sinh mã tăng dần theo tiền tố, ví dụ: ("TK", ["TK001", "TK007"]) -> "TK008"
     */
    public static String next(String prefix, Collection<String> existingIds) {
        int max = 0;
        if (existingIds != null) {
            for (String id : existingIds) {
                if (id != null && id.startsWith(prefix)) {
                    try {
                        int num = Integer.parseInt(id.substring(prefix.length()));
                        if (num > max) {
                            max = num;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return String.format("%s%03d", prefix, max + 1);
    }
}