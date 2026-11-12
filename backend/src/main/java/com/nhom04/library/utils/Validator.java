package com.nhom04.library.utils;

public final class Validator {

    private Validator() {
    }

    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
        return value.trim();
    }

    public static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " không được âm.");
        }
        return value;
    }

    public static double requireNonNegative(double value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " không được âm.");
        }
        return value;
    }
}