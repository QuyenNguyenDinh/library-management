package com.nhom04.library.handler;

import com.nhom04.library.model.Book;
import com.nhom04.library.model.BorrowTicket;
import com.nhom04.library.model.BorrowTicketDetail;
import com.nhom04.library.repository.BookRepository;
import com.nhom04.library.repository.TicketRepository;
import com.nhom04.library.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử API Quản lý Sách qua MockMvc.
 */
class BookHandlerTest {

    @TempDir
    Path dataDir;

    private MockMvc mockMvc;
    private BookRepository bookRepository;
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        String dir = dataDir.toString();
        bookRepository = new BookRepository(dir);
        ticketRepository = new TicketRepository(dir);

        bookRepository.save(new Book("BK001", "Lập trình hướng đối tượng với Java", "Nguyễn Văn A", "CNTT", 5, 120000));
        bookRepository.save(new Book("BK002", "Cấu trúc dữ liệu và giải thuật", "Đỗ Xuân Lôi", "CNTT", 8, 145000));

        BookService bookService = new BookService(bookRepository, ticketRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(new BookHandler(bookService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/books hợp lệ trả về 201 và có mã dạng BKxxx")
    void createBook_valid_returns201() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Clean Code",
                                  "author": "Robert C. Martin",
                                  "category": "Kỹ thuật phần mềm",
                                  "availableQuantity": 4,
                                  "price": 250000
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(matchesPattern("^BK\\d{3}$")))
                .andExpect(jsonPath("$.data.title").value("Clean Code"));
    }

    @Test
    @DisplayName("POST /api/books với tên sách rỗng trả về 400")
    void createBook_emptyTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "author": "Nguyễn Văn A",
                                  "category": "CNTT",
                                  "availableQuantity": 5,
                                  "price": 100000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/books/{id} sửa thông tin sách trả về 200")
    void updateBook_valid_returns200() throws Exception {
        mockMvc.perform(put("/api/books/BK001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Lập trình Java nâng cao",
                                  "author": "Nguyễn Văn A",
                                  "category": "CNTT",
                                  "availableQuantity": 10,
                                  "price": 135500
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Lập trình Java nâng cao"))
                .andExpect(jsonPath("$.data.price").value(135500));
    }

    @Test
    @DisplayName("DELETE /api/books/{id} khi sách đang được mượn trả về 409")
    void deleteBook_currentlyBorrowed_returns409() throws Exception {
        ticketRepository.save(new BorrowTicket(
                "TK001",
                "RD001",
                List.of(new BorrowTicketDetail("BK001", "Lập trình hướng đối tượng với Java", 1)),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(13)
        ));

        mockMvc.perform(delete("/api/books/BK001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/books?keyword=... tìm theo tên trả về đúng sách (Tình huống test 10)")
    void searchBooks_byTitle_returnsMatchingBooks() throws Exception {
        mockMvc.perform(get("/api/books").param("keyword", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value("BK001"));
    }
}