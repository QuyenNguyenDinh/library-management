
package com.nhom04.library.service;

import com.nhom04.library.exception.InvalidTicketStatusException;
import com.nhom04.library.exception.TicketNotFoundException;
import com.nhom04.library.model.BorrowTicket;
import com.nhom04.library.model.BorrowTicketDetail;
import com.nhom04.library.model.TicketStatus;
import com.nhom04.library.repository.TicketRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TicketQueryServiceTest {

    @TempDir
    Path tempDir;

    private TicketRepository repository;
    private TicketQueryService service;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        repository = new TicketRepository(tempDir.toString());
        service = new TicketQueryService(repository);
    }

    private BorrowTicket createTicket(
            String id,
            String readerId,
            LocalDate borrowDate,
            LocalDate dueDate,
            TicketStatus status) {

        BorrowTicketDetail detail = new BorrowTicketDetail();
        detail.setBookId("BK001");
        detail.setQuantity(1);

        BorrowTicket ticket = new BorrowTicket(
                id,
                readerId,
                List.of(detail),
                borrowDate,
                dueDate
        );

        ticket.setStatus(status);

        if (status == TicketStatus.RETURNED) {
            ticket.setReturnDate(today.minusDays(1));
        }

        repository.save(ticket);
        return ticket;
    }

    @Test
    void getTickets_withoutFilter_returnsAll() {
        createTicket(
                "TK001", "RD001",
                today.minusDays(10),
                today.plusDays(5),
                TicketStatus.BORROWING
        );

        createTicket(
                "TK002", "RD002",
                today.minusDays(10),
                today.minusDays(2),
                TicketStatus.BORROWING
        );

        List<Map<String, Object>> result =
                service.getTickets(null, null);

        assertEquals(2, result.size());
    }

    @Test
    void getTickets_borrowing_includesOverdueTickets() {
        createTicket(
                "TK001", "RD001",
                today.minusDays(10),
                today.plusDays(5),
                TicketStatus.BORROWING
        );

        createTicket(
                "TK002", "RD002",
                today.minusDays(10),
                today.minusDays(2),
                TicketStatus.BORROWING
        );

        List<Map<String, Object>> result =
                service.getTickets("BORROWING", null);

        assertEquals(2, result.size());
    }

    @Test
    void getTickets_overdue_returnsOnlyOverdueBorrowingTickets() {
        createTicket(
                "TK001", "RD001",
                today.minusDays(10),
                today.minusDays(2),
                TicketStatus.BORROWING
        );

        createTicket(
                "TK002", "RD002",
                today.minusDays(10),
                today.plusDays(5),
                TicketStatus.BORROWING
        );

        List<Map<String, Object>> result =
                service.getTickets("OVERDUE", null);

        assertEquals(1, result.size());
        assertEquals("TK001", result.get(0).get("id"));
        assertEquals(true, result.get(0).get("overdue"));
    }

    @Test
    void getTickets_returned_returnsOnlyReturnedTickets() {
        createTicket(
                "TK001", "RD001",
                today.minusDays(10),
                today.plusDays(5),
                TicketStatus.BORROWING
        );

        createTicket(
                "TK002", "RD002",
                today.minusDays(10),
                today.minusDays(5),
                TicketStatus.RETURNED
        );

        List<Map<String, Object>> result =
                service.getTickets("RETURNED", null);

        assertEquals(1, result.size());
        assertEquals("TK002", result.get(0).get("id"));
    }

    @Test
    void getTickets_invalidStatus_throwsException() {
        createTicket(
                "TK001", "RD001",
                today.minusDays(5),
                today.plusDays(5),
                TicketStatus.BORROWING
        );

        assertThrows(
                InvalidTicketStatusException.class,
                () -> service.getTickets("ABC", null)
        );
    }

    @Test
    void getTickets_invalidStatus_emptyRepository_stillThrows() {
        assertThrows(
                InvalidTicketStatusException.class,
                () -> service.getTickets("ABC", null)
        );
    }

    @Test
    void getTickets_filterByReaderId() {
        createTicket(
                "TK001", "RD001",
                today.minusDays(5),
                today.plusDays(5),
                TicketStatus.BORROWING
        );

        createTicket(
                "TK002", "RD002",
                today.minusDays(5),
                today.plusDays(5),
                TicketStatus.BORROWING
        );

        List<Map<String, Object>> result =
                service.getTickets(null, "RD001");

        assertEquals(1, result.size());
        assertEquals("TK001", result.get(0).get("id"));
    }

    @Test
    void getTicketById_existingTicket_returnsTicket() {
        createTicket(
                "TK001", "RD001",
                today.minusDays(5),
                today.plusDays(5),
                TicketStatus.BORROWING
        );

        Map<String, Object> result =
                service.getTicketById("TK001");

        assertNotNull(result);
        assertEquals("TK001", result.get("id"));
        assertEquals("RD001", result.get("readerId"));
        assertEquals(5L, result.get("daysBorrowed"));
    }

    @Test
    void getTicketById_unknownTicket_throwsException() {
        assertThrows(
                TicketNotFoundException.class,
                () -> service.getTicketById("UNKNOWN")
        );
    }
}
