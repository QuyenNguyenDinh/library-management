package com.nhom04.library.service;

import com.nhom04.library.exception.BookInUseException;
import com.nhom04.library.exception.BookNotFoundException;
import com.nhom04.library.model.Book;
import com.nhom04.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookServiceTest {

    @TempDir
    Path tempDir;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        BookRepository repo = new BookRepository(tempDir.toString());
        bookService = new BookService(repo, tempDir.toString());
    }

    @Test
    void testCreateSearchAndDeleteBook() {
        Book created = bookService.createBook(
                new Book("TMP", "Clean Code", "Robert Martin", "CNTT", 5, 200000));
        assertEquals("BK001", created.getId());

        List<Book> found = bookService.searchBooks("clean");
        assertEquals(1, found.size());

        bookService.deleteBook("BK001");
        assertThrows(BookNotFoundException.class, () -> bookService.getBookById("BK001"));
    }

    @Test
    void testDeleteBookWhileBorrowingThrowsBookInUseException() throws Exception {
        bookService.createBook(
                new Book("TMP", "Java Cơ Bản", "Tác giả A", "CNTT", 5, 100000));

        String ticketJson = """
                [
                  {
                    "id": "TK001",
                    "readerId": "RD001",
                    "borrowDate": "2026-11-01",
                    "dueDate": "2026-11-15",
                    "status": "BORROWING",
                    "details": [
                      { "bookId": "BK001", "bookTitle": "Java Cơ Bản", "quantity": 1 }
                    ]
                  }
                ]
                """;
        Files.writeString(tempDir.resolve("tickets.json"), ticketJson);

        assertThrows(BookInUseException.class, () -> bookService.deleteBook("BK001"));
    }
}