package com.nhom04.library.repository;

import com.nhom04.library.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tầng lưu trữ BookRepository trên thư mục tạm @TempDir.
 */
class BookRepositoryTest {

    @TempDir
    Path tempDir;

    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository = new BookRepository(tempDir.toString());
    }

    @Test
    @DisplayName("Khởi tạo khi chưa có file JSON trả về danh sách rỗng")
    void findAll_whenFileDoesNotExist_returnsEmptyList() {
        List<Book> books = bookRepository.findAll();
        assertNotNull(books);
        assertTrue(books.isEmpty());
    }

    @Test
    @DisplayName("Lưu sách mới và tìm lại theo mã sách")
    void saveAndFindById_persistsBookCorrectly() {
        Book book = new Book("BK001", "Lập trình hướng đối tượng với Java", "Phạm Văn Ất", "CNTT", 5, 120000);
        bookRepository.save(book);

        Optional<Book> found = bookRepository.findById("BK001");
        assertTrue(found.isPresent());
        assertEquals("Lập trình hướng đối tượng với Java", found.get().getTitle());
        assertEquals("Phạm Văn Ất", found.get().getAuthor());
        assertEquals(5, found.get().getAvailableQuantity());
        assertEquals(120000, found.get().getPrice());
    }

    @Test
    @DisplayName("Lưu sách trùng mã sẽ cập nhật thông tin thay vì tạo bản sao")
    void save_existingId_updatesBookInPlace() {
        bookRepository.save(new Book("BK001", "Cơ sở dữ liệu", "Lê Tiến Vương", "CNTT", 4, 95000));
        bookRepository.save(new Book("BK001", "Cơ sở dữ liệu nâng cao", "Lê Tiến Vương", "CNTT", 9, 115000));

        List<Book> all = bookRepository.findAll();
        assertEquals(1, all.size());
        assertEquals("Cơ sở dữ liệu nâng cao", all.get(0).getTitle());
        assertEquals(9, all.get(0).getAvailableQuantity());
        assertEquals(115000, all.get(0).getPrice());
    }

    @Test
    @DisplayName("Xóa sách theo mã trả về true khi tồn tại và false khi không tồn tại")
    void deleteById_removesBookIfPresent() {
        bookRepository.save(new Book("BK001", "Nhà giả kim", "Paulo Coelho", "Văn học", 10, 79000));
        bookRepository.save(new Book("BK002", "Đắc nhân tâm", "Dale Carnegie", "Kỹ năng sống", 8, 86000));

        assertTrue(bookRepository.deleteById("BK001"));
        assertFalse(bookRepository.deleteById("BK001"));
        assertFalse(bookRepository.deleteById("BK999"));

        List<Book> remaining = bookRepository.findAll();
        assertEquals(1, remaining.size());
        assertEquals("BK002", remaining.get(0).getId());
    }
}