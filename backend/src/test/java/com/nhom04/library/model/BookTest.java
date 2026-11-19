package com.nhom04.library.model;

import com.nhom04.library.exception.BookOutOfStockException;
import com.nhom04.library.utils.IdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử nghiệp vụ lớp Book (Borrowable) và IdGenerator.
 */
class BookTest {

    @Test
    @DisplayName("Mượn và trả sách cập nhật đúng số lượng tồn kho")
    void borrowAndReturnItem_updatesAvailableQuantity() {
        Book book = new Book("BK001", "Lập trình Java", "Phạm Văn Ất", "CNTT", 5, 120000);

        assertTrue(book.hasEnough(5));
        assertFalse(book.hasEnough(6));

        book.borrowItem(2);
        assertEquals(3, book.getAvailableQuantity());

        book.returnItem(2);
        assertEquals(5, book.getAvailableQuantity());
    }

    @Test
    @DisplayName("Mượn vượt quá số lượng tồn ném BookOutOfStockException")
    void borrowMoreThanAvailable_throwsBookOutOfStockException() {
        Book book = new Book("BK002", "Nguyên lý Marketing", "Philip Kotler", "Kinh tế", 1, 195000);

        assertThrows(BookOutOfStockException.class, () -> book.borrowItem(2));
        assertThrows(IllegalArgumentException.class, () -> book.borrowItem(0));
        assertThrows(IllegalArgumentException.class, () -> book.returnItem(-1));
    }

    @Test
    @DisplayName("Setter kiểm tra dữ liệu đầu vào không hợp lệ")
    void setters_rejectInvalidValues() {
        Book book = new Book("BK003", "Nhà giả kim", "Paulo Coelho", "Văn học", 4, 79000);

        assertThrows(IllegalArgumentException.class, () -> book.setTitle("   "));
        assertThrows(IllegalArgumentException.class, () -> book.setAuthor(""));
        assertThrows(IllegalArgumentException.class, () -> book.setCategory(null));
        assertThrows(IllegalArgumentException.class, () -> book.setAvailableQuantity(-1));
        assertThrows(IllegalArgumentException.class, () -> book.setPrice(-5000));
    }

    @Test
    @DisplayName("IdGenerator sinh mã kế tiếp chuẩn xác")
    void idGenerator_generatesNextSequentialId() {
        assertEquals("BK001", IdGenerator.next("BK", List.of()));
        assertEquals("BK008", IdGenerator.next("BK", List.of("BK001", "BK007")));
    }
}