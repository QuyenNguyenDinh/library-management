package com.nhom04.library.service;

import com.nhom04.library.model.BorrowTicket;
import com.nhom04.library.model.TicketStatus;
import com.nhom04.library.repository.TicketRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TicketQueryService {
    private final TicketRepository ticketRepository;
    public TicketQueryService(TicketRepository ticketRepository) { this.ticketRepository = ticketRepository; }

    public List<Map<String, Object>> getTickets(String status, String readerId) {
        return ticketRepository.findAll().stream()
            .filter(t -> status == null || status.isBlank() || matchesStatus(t, status))
            .filter(t -> readerId == null || readerId.isBlank() || readerId.equalsIgnoreCase(t.getReaderId()))
            .map(this::toView).toList();
    }

    public Map<String, Object> getTicketById(String id) {
        BorrowTicket ticket = ticketRepository.findById(id).orElse(null);
        return ticket == null ? null : toView(ticket);
    }

    private boolean matchesStatus(BorrowTicket t, String status) {
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (value.equals("OVERDUE")) return t.isOverdue(LocalDate.now());
        if (value.equals("BORROWING")) return t.getStatus() == TicketStatus.BORROWING && !t.isOverdue(LocalDate.now());
        if (value.equals("RETURNED")) return t.getStatus() == TicketStatus.RETURNED;
        return t.getStatus().name().equals(value);
    }

    private Map<String, Object> toView(BorrowTicket t) {
        LocalDate today = LocalDate.now();
        LocalDate end = t.getReturnDate() != null ? t.getReturnDate() : today;
        long days = Math.max(0, ChronoUnit.DAYS.between(t.getBorrowDate(), end));
        long overdue = t.isOverdue(today) ? ChronoUnit.DAYS.between(t.getDueDate(), today) : 0;
        return Map.of("id", t.getId(), "readerId", t.getReaderId(), "details", t.getDetails(),
            "borrowDate", t.getBorrowDate(), "dueDate", t.getDueDate(),
            "returnDate", t.getReturnDate() == null ? "" : t.getReturnDate(),
            "status", t.getStatus(), "fineAmount", t.getFineAmount(),
            "daysBorrowed", days, "overdue", overdue > 0);
    }
}
