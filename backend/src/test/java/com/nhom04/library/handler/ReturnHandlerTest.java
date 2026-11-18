package com.nhom04.library.handler;

import com.nhom04.library.model.Book;
import com.nhom04.library.model.BorrowTicket;
import com.nhom04.library.model.BorrowTicketDetail;
import com.nhom04.library.model.StudentReader;
import com.nhom04.library.repository.BookRepository;
import com.nhom04.library.repository.ReaderRepository;
import com.nhom04.library.repository.ReturnRecordRepository;
import com.nhom04.library.repository.TicketRepository;
import com.nhom04.library.service.ReturnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử API trả sách: đúng mã HTTP (200 / 400 / 404 / 409) và định dạng {@code ApiResponse} theo API contract.
 */
class ReturnHandlerTest {

    @TempDir
    Path dataDir;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        String dir = dataDir.toString();
        BookRepository bookRepository = new BookRepository(dir);
        ReaderRepository readerRepository = new ReaderRepository(dir);
        TicketRepository ticketRepository = new TicketRepository(dir);
        ReturnRecordRepository returnRecordRepository = new ReturnRecordRepository(dir);

        bookRepository.save(new Book("BK001", "Lập trình hướng đối tượng với Java", "Nguyễn Văn A", "CNTT", 4, 120000));
        readerRepository.save(new StudentReader("RD001", "Trần Thị B", "0901234567"));
        BorrowTicketDetail detail = new BorrowTicketDetail("BK001", "Lập trình hướng đối tượng với Java", 1);
        ticketRepository.save(new BorrowTicket("TK001", "RD001", List.of(detail),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15)));

        ReturnService returnService = new ReturnService(ticketRepository, readerRepository, bookRepository, returnRecordRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReturnHandler(returnService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ResultActions postReturn(String ticketId, String json) throws Exception {
        return mockMvc.perform(post("/api/return/" + ticketId).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void return_lateByFourDays_returns200WithFine() throws Exception {
        postReturn("TK001", "{ \"returnDate\": \"2026-11-19\" }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("RR001"))
                .andExpect(jsonPath("$.data.ticketId").value("TK001"))
                .andExpect(jsonPath("$.data.returnDate").value("2026-11-19"))
                .andExpect(jsonPath("$.data.overdueDays").value(4))
                .andExpect(jsonPath("$.data.fineAmount").value(20000))
                .andExpect(jsonPath("$.data.policyName").value("Sinh viên thường (5.000đ/ngày)"));
    }

    @Test
    void return_withoutBody_usesToday_returns200() throws Exception {
        mockMvc.perform(post("/api/return/TK001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ticketId").value("TK001"));
    }

    @Test
    void return_unknownTicket_returns404() throws Exception {
        postReturn("TK999", "{}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void return_alreadyReturned_returns409() throws Exception {
        postReturn("TK001", "{ \"returnDate\": \"2026-11-10\" }").andExpect(status().isOk());

        postReturn("TK001", "{ \"returnDate\": \"2026-11-11\" }")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void return_dateBeforeBorrowDate_returns400() throws Exception {
        postReturn("TK001", "{ \"returnDate\": \"2026-10-01\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void return_malformedDate_returns400() throws Exception {
        postReturn("TK001", "{ \"returnDate\": \"19/11/2026\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getReturnRecords_returns200WithHistory() throws Exception {
        postReturn("TK001", "{ \"returnDate\": \"2026-11-19\" }").andExpect(status().isOk());

        mockMvc.perform(get("/api/return-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("RR001"))
                .andExpect(jsonPath("$.data[0].fineAmount").value(20000));
    }
}
