package com.nhom04.library.handler;

import com.nhom04.library.model.Book;
import com.nhom04.library.model.StudentReader;
import com.nhom04.library.repository.BookRepository;
import com.nhom04.library.repository.ReaderRepository;
import com.nhom04.library.repository.TicketRepository;
import com.nhom04.library.service.BorrowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử API {@code POST /api/borrow}: đúng mã HTTP và định dạng {@code ApiResponse} theo API contract.
 */
class BorrowHandlerTest {

    @TempDir
    Path dataDir;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        String dir = dataDir.toString();
        BookRepository bookRepository = new BookRepository(dir);
        ReaderRepository readerRepository = new ReaderRepository(dir);
        TicketRepository ticketRepository = new TicketRepository(dir);

        bookRepository.save(new Book("BK001", "Lập trình hướng đối tượng với Java", "Nguyễn Văn A", "CNTT", 5, 120000));
        bookRepository.save(new Book("BK002", "Cấu trúc dữ liệu và giải thuật", "Trần Văn B", "CNTT", 0, 95000));
        readerRepository.save(new StudentReader("RD001", "Trần Thị B", "0901234567"));

        BorrowService borrowService = new BorrowService(bookRepository, readerRepository, ticketRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(new BorrowHandler(borrowService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions postBorrow(String json) throws Exception {
        return mockMvc.perform(post("/api/borrow").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void borrow_success_returns201WithTicket() throws Exception {
        postBorrow("""
                { "readerId": "RD001", "items": [ { "bookId": "BK001", "quantity": 1 } ],
                  "borrowDate": "2026-11-13" }
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("TK001"))
                .andExpect(jsonPath("$.data.status").value("BORROWING"))
                .andExpect(jsonPath("$.data.borrowDate").value("2026-11-13"))
                .andExpect(jsonPath("$.data.dueDate").value("2026-11-27"))
                .andExpect(jsonPath("$.data.details[0].bookId").value("BK001"));
    }

    @Test
    void borrow_zeroQuantity_returns400() throws Exception {
        postBorrow("""
                { "readerId": "RD001", "items": [ { "bookId": "BK001", "quantity": 0 } ] }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void borrow_unknownReader_returns404() throws Exception {
        postBorrow("""
                { "readerId": "RD999", "items": [ { "bookId": "BK001", "quantity": 1 } ] }
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void borrow_outOfStock_returns409() throws Exception {
        postBorrow("""
                { "readerId": "RD001", "items": [ { "bookId": "BK002", "quantity": 1 } ] }
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }
}
