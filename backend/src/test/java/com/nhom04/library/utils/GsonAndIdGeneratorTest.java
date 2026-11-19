package com.nhom04.library.utils;

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử hạ tầng GsonFactory, LocalDateAdapter và IdGenerator dùng chung cho toàn nhóm.
 */
class GsonAndIdGeneratorTest {

    static class SampleDateHolder {
        String code;
        LocalDate date;

        SampleDateHolder(String code, LocalDate date) {
            this.code = code;
            this.date = date;
        }
    }

    @Test
    @DisplayName("GsonFactory serialize và deserialize LocalDate đúng định dạng yyyy-MM-dd")
    void gsonFactory_handlesLocalDateFormat() {
        Gson gson = GsonFactory.builder().create();
        SampleDateHolder original = new SampleDateHolder("TK001", LocalDate.of(2026, 11, 19));

        String json = gson.toJson(original);
        assertTrue(json.contains("\"2026-11-19\""));

        SampleDateHolder restored = gson.fromJson(json, SampleDateHolder.class);
        assertEquals("TK001", restored.code);
        assertEquals(LocalDate.of(2026, 11, 19), restored.date);
    }

    @Test
    @DisplayName("GsonFactory xử lý giá trị LocalDate null an toàn")
    void gsonFactory_handlesNullLocalDate() {
        Gson gson = GsonFactory.builder().create();
        SampleDateHolder original = new SampleDateHolder("TK002", null);

        String json = gson.toJson(original);
        SampleDateHolder restored = gson.fromJson(json, SampleDateHolder.class);

        assertEquals("TK002", restored.code);
        assertNull(restored.date);
    }

    @Test
    @DisplayName("IdGenerator sinh mã kế tiếp từ danh sách rỗng hoặc không theo thứ tự")
    void idGenerator_generatesNextSequentialCode() {
        assertEquals("BK001", IdGenerator.next("BK", List.of()));
        assertEquals("BK008", IdGenerator.next("BK", List.of("BK001", "BK007")));
        assertEquals("BK012", IdGenerator.next("BK", List.of("BK011", "BK003", "BK009")));
        assertEquals("TK001", IdGenerator.next("TK", List.of("BK001", "RD005")));
    }
}