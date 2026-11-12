package com.nhom04.library.repository;

import com.nhom04.library.model.Book;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void testSaveFindUpdateAndDelete() {
        BookRepository repo = new BookRepository(tempDir.toString());

        Book b1 = new Book("BK001", "Java OOP", "Tac Gia A", "CNTT", 5, 100000);
        repo.save(b1);

        assertEquals(1, repo.findAll().size());
        assertTrue(repo.findById("BK001").isPresent());

        b1.setAvailableQuantity(3);
        repo.save(b1);
        assertEquals(3, repo.findById("BK001").get().getAvailableQuantity());

        assertTrue(repo.deleteById("BK001"));
        assertFalse(repo.deleteById("BK001"));
        assertEquals(0, repo.findAll().size());
    }

    @Test
    void testSearchIgnoreCase() {
        BookRepository repo = new BookRepository(tempDir.toString());
        repo.save(new Book("BK001", "Lập trình Java", "Tác giả A", "CNTT", 5, 100000));
        repo.save(new Book("BK002", "Kinh tế vi mô", "Tác giả B", "Kinh tế", 4, 120000));

        List<Book> result = repo.search("java");
        assertEquals(1, result.size());
        assertEquals("BK001", result.get(0).getId());
    }
}