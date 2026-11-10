package com.nhom04.library.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BorrowTicketTest {

    private static final LocalDate BORROW = LocalDate.of(2026, 11, 1);
    private static final LocalDate DUE = LocalDate.of(2026, 11, 15);

    private BorrowTicket newTicket() {
        return new BorrowTicket("TK001", "RD001", List.of(
                new BorrowTicketDetail("BK001", "Lập trình Java", 2),
                new BorrowTicketDetail("BK002", "Cấu trúc dữ liệu", 1)), BORROW, DUE);
    }

    @Test
    void newTicketIsBorrowingWithTotalQuantity() {
        BorrowTicket ticket = newTicket();
        assertEquals(TicketStatus.BORROWING, ticket.getStatus());
        assertEquals(3, ticket.getTotalQuantity());
        assertEquals(0, ticket.getFineAmount());
    }

    @Test
    void overdueOnlyAfterDueDateWhileBorrowing() {
        BorrowTicket ticket = newTicket();
        assertFalse(ticket.isOverdue(DUE));
        assertTrue(ticket.isOverdue(DUE.plusDays(1)));

        ticket.setStatus(TicketStatus.RETURNED);
        assertFalse(ticket.isOverdue(DUE.plusDays(1)));
    }

    @Test
    void rejectsInvalidData() {
        assertThrows(IllegalArgumentException.class,
                () -> new BorrowTicket("TK001", "RD001", List.of(), BORROW, DUE));
        assertThrows(IllegalArgumentException.class,
                () -> new BorrowTicket("TK001", "RD001",
                        List.of(new BorrowTicketDetail("BK001", "A", 1)), DUE, BORROW));
        assertThrows(IllegalArgumentException.class, () -> newTicket().setFineAmount(-1));
    }

    @Test
    void detailsCannotBeModifiedFromOutside() {
        BorrowTicket ticket = newTicket();
        assertThrows(UnsupportedOperationException.class,
                () -> ticket.getDetails().add(new BorrowTicketDetail("BK003", "C", 1)));
    }
}
