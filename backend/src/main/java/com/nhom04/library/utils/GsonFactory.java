package com.nhom04.library.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDate;

public final class GsonFactory {

    private GsonFactory() {
    }

    public static GsonBuilder builder() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .serializeNulls();
    }

    public static Gson create() {
        return builder().create();
    }
}