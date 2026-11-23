
package com.nhom04.library.service;

import com.nhom04.library.model.Book;
import com.nhom04.library.model.BorrowTicket;
import com.nhom04.library.model.BorrowTicketDetail;
import com.nhom04.library.model.TicketStatus;
import com.nhom04.library.repository.BookRepository;
import com.nhom04.library.repository.TicketRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatsServiceTest {

    @TempDir
    Path tempDir;

    private TicketRepository ticketRepository;
    private BookRepository bookRepository;
    private StatsService service;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        ticketRepository = new TicketRepository(tempDir.toString());
        bookRepository = new BookRepository(tempDir.toString());

        service = new StatsService(ticketRepository, bookRepository);
    }

    private void createTicket(
            String id,
            String bookId,
            int quantity,
            LocalDate dueDate,
            TicketStatus status,
            double fineAmount) {

        BorrowTicketDetail detail = new BorrowTicketDetail();
        detail.setBookId(bookId);
        detail.setQuantity(quantity);

        BorrowTicket ticket = new BorrowTicket(
                id,
                "RD001",
                List.of(detail),
                today.minusDays(20),
                dueDate
        );

        ticket.setStatus(status);

        if (status == TicketStatus.RETURNED) {
            ticket.setReturnDate(today.minusDays(1));
        }

        ticket.setFineAmount(fineAmount);
        ticketRepository.save(ticket);
    }

    // TEST 1: Khong co du lieu
    @Test
    void getStats_emptyData_returnsZero() {
        Map<String, Object> result = service.getStats();

        assertEquals(0L, result.get("overdueCount"));
        assertEquals(0.0, (Double) result.get("totalFine"), 0.001);

        assertTrue(
                ((List<?>) result.get("topBooks")).isEmpty()
        );

        assertTrue(
                ((List<?>) result.get("lowStockBooks")).isEmpty()
        );
    }

    // TEST 2: Dem so phieu dang muon qua han
    @Test
    void getStats_countsOnlyOverdueBorrowingTickets() {
        createTicket(
                "TK001", "BK001", 1,
                today.minusDays(2),
                TicketStatus.BORROWING,
                0
        );

        createTicket(
                "TK002", "BK002", 1,
                today.plusDays(5),
                TicketStatus.BORROWING,
                0
        );

        createTicket(
                "TK003", "BK003", 1,
                today.minusDays(5),
                TicketStatus.RETURNED,
                20000
        );

        Map<String, Object> result = service.getStats();

        assertEquals(1L, result.get("overdueCount"));
    }

    // TEST 3: Tinh tong tien phat
    @Test
    void getStats_calculatesTotalFine() {
        createTicket(
                "TK001", "BK001", 1,
                today.minusDays(5),
                TicketStatus.RETURNED,
                20000
        );

        createTicket(
                "TK002", "BK002", 1,
                today.minusDays(3),
                TicketStatus.RETURNED,
                8000
        );

        createTicket(
                "TK003", "BK003", 1,
                today.plusDays(5),
                TicketStatus.BORROWING,
                0
        );

        Map<String, Object> result = service.getStats();

        assertEquals(
                28000.0,
                (Double) result.get("totalFine"),
                0.001
        );
    }

    // TEST 4: Sap xep sach muon nhieu nhat
    @Test
    @SuppressWarnings("unchecked")
    void getStats_topBooks_sortedByBorrowCount() {
        createTicket(
                "TK001", "BK001", 1,
                today.plusDays(5),
                TicketStatus.BORROWING,
                0
        );

        createTicket(
                "TK002", "BK002", 3,
                today.plusDays(5),
                TicketStatus.BORROWING,
                0
        );

        createTicket(
                "TK003", "BK003", 2,
                today.plusDays(5),
                TicketStatus.BORROWING,
                0
        );

        Map<String, Object> result = service.getStats();

        List<Map<String, Object>> topBooks =
                (List<Map<String, Object>>) result.get("topBooks");

        assertEquals(3, topBooks.size());

        assertEquals("BK002", topBooks.get(0).get("bookId"));
        assertEquals(3, topBooks.get(0).get("borrowCount"));

        assertEquals("BK003", topBooks.get(1).get("bookId"));
        assertEquals(2, topBooks.get(1).get("borrowCount"));

        assertEquals("BK001", topBooks.get(2).get("bookId"));
        assertEquals(1, topBooks.get(2).get("borrowCount"));
    }

    // TEST 5: Chi lay toi da 5 sach
    @Test
    @SuppressWarnings("unchecked")
    void getStats_topBooks_limitsToFive() {
        for (int i = 1; i <= 6; i++) {
            createTicket(
                    "TK00" + i,
                    "BK00" + i,
                    i,
                    today.plusDays(5),
                    TicketStatus.BORROWING,
                    0
            );
        }

        Map<String, Object> result = service.getStats();

        List<Map<String, Object>> topBooks =
                (List<Map<String, Object>>) result.get("topBooks");

        assertEquals(5, topBooks.size());
        assertEquals("BK006", topBooks.get(0).get("bookId"));
    }

    // TEST 6: Sach con 0, 1, 2 cuon duoc xem la sap het
    @Test
    @SuppressWarnings("unchecked")
    void getStats_lowStockBooks_onlyIncludesQuantityAtMostTwo() {
        bookRepository.save(new Book(
                "BK001", "Sách Java", "Tác giả A",
                "Lập trình", 0, 100000
        ));

        bookRepository.save(new Book(
                "BK002", "Sách Python", "Tác giả B",
                "Lập trình", 1, 120000
        ));

        bookRepository.save(new Book(
                "BK003", "Sách SQL", "Tác giả C",
                "Cơ sở dữ liệu", 2, 150000
        ));

        bookRepository.save(new Book(
                "BK004", "Sách Toán", "Tác giả D",
                "Giáo dục", 3, 90000
        ));

        Map<String, Object> result = service.getStats();

        List<Map<String, Object>> lowStockBooks =
                (List<Map<String, Object>>) result.get("lowStockBooks");

        assertEquals(3, lowStockBooks.size());

        assertTrue(lowStockBooks.stream()
                .anyMatch(b -> b.get("id").equals("BK001")));

        assertTrue(lowStockBooks.stream()
                .anyMatch(b -> b.get("id").equals("BK002")));

        assertTrue(lowStockBooks.stream()
                .anyMatch(b -> b.get("id").equals("BK003")));

        assertFalse(lowStockBooks.stream()
                .anyMatch(b -> b.get("id").equals("BK004")));
    }

    // TEST 7: Khong co sach sap het
    @Test
    @SuppressWarnings("unchecked")
    void getStats_lowStockBooks_returnsEmptyWhenStockIsEnough() {
        bookRepository.save(new Book(
                "BK001", "Sách Java", "Tác giả A",
                "Lập trình", 5, 100000
        ));

        bookRepository.save(new Book(
                "BK002", "Sách Python", "Tác giả B",
                "Lập trình", 10, 120000
        ));

        Map<String, Object> result = service.getStats();

        List<Map<String, Object>> lowStockBooks =
                (List<Map<String, Object>>) result.get("lowStockBooks");

        assertTrue(lowStockBooks.isEmpty());
    }
}
