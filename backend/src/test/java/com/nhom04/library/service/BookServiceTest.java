package com.nhom04.library.service;

import com.nhom04.library.exception.BookInUseException;
import com.nhom04.library.exception.BookNotFoundException;
import com.nhom04.library.model.Book;
import com.nhom04.library.model.BorrowTicket;
import com.nhom04.library.model.BorrowTicketDetail;
import com.nhom04.library.repository.BookRepository;
import com.nhom04.library.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookServiceTest {

    @TempDir
    Path tempDir;

    private BookService bookService;
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        String dir = tempDir.toString();
        BookRepository repo = new BookRepository(dir);
        ticketRepository = new TicketRepository(dir);
        bookService = new BookService(repo, ticketRepository);
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
    void testDeleteBookWhileBorrowingThrowsBookInUseException() {
        bookService.createBook(
                new Book("TMP", "Java Cơ Bản", "Tác giả A", "CNTT", 5, 100000));

        ticketRepository.save(new BorrowTicket(
                "TK001",
                "RD001",
                List.of(new BorrowTicketDetail("BK001", "Java Cơ Bản", 1)),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 15)
        ));

        assertThrows(BookInUseException.class, () -> bookService.deleteBook("BK001"));
    }
}