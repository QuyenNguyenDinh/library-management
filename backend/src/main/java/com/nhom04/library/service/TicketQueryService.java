
package com.nhom04.library.service;

import com.nhom04.library.exception.InvalidTicketStatusException;
import com.nhom04.library.exception.TicketNotFoundException;
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

    public TicketQueryService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    // Lấy danh sách phiếu theo trạng thái và mã bạn đọc
    public List<Map<String, Object>> getTickets(
            String status, String readerId) {

        LocalDate today = LocalDate.now();

        // Kiểm tra trạng thái trước khi lọc
        if (status != null && !status.isBlank()) {
            String value = status.trim().toUpperCase(Locale.ROOT);

            if (!List.of("BORROWING", "OVERDUE", "RETURNED")
                    .contains(value)) {
                throw new InvalidTicketStatusException(
                        "Trạng thái '" + status
                        + "' không hợp lệ. Chỉ nhận BORROWING, OVERDUE hoặc RETURNED.");
            }
        }

        return ticketRepository.findAll().stream()
                .filter(t -> status == null
                        || status.isBlank()
                        || matchesStatus(t, status, today))
                .filter(t -> readerId == null
                        || readerId.isBlank()
                        || readerId.equalsIgnoreCase(t.getReaderId()))
                .map(this::toView)
                .toList();
    }

    // Lấy thông tin chi tiết phiếu mượn
    public Map<String, Object> getTicketById(String id) {

        BorrowTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(
                        "Không tìm thấy phiếu mượn có mã: " + id
                ));

        return toView(ticket);
    }

    // Lọc phiếu theo trạng thái
    private boolean matchesStatus(
            BorrowTicket t,
            String status,
            LocalDate today) {

        String value = status.trim().toUpperCase(Locale.ROOT);

        return switch (value) {
            case "BORROWING" ->
                    t.getStatus() == TicketStatus.BORROWING;

            case "OVERDUE" ->
                    t.isOverdue(today);

            case "RETURNED" ->
                    t.getStatus() == TicketStatus.RETURNED;

            default -> throw new InvalidTicketStatusException(
                    "Trạng thái '" + status
                    + "' không hợp lệ. Chỉ nhận BORROWING, OVERDUE hoặc RETURNED.");
        };
    }

    // Chuyển phiếu mượn thành dữ liệu trả về API
    private Map<String, Object> toView(BorrowTicket t) {

        LocalDate today = LocalDate.now();

        LocalDate end = t.getReturnDate() != null
                ? t.getReturnDate()
                : today;

        long days = Math.max(
                0,
                ChronoUnit.DAYS.between(t.getBorrowDate(), end)
        );

        long overdueDays = t.isOverdue(today)
                ? ChronoUnit.DAYS.between(t.getDueDate(), today)
                : 0;

        return Map.of(
                "id", t.getId(),
                "readerId", t.getReaderId(),
                "details", t.getDetails(),
                "borrowDate", t.getBorrowDate(),
                "dueDate", t.getDueDate(),
                "returnDate", t.getReturnDate() == null
                        ? "" : t.getReturnDate(),
                "status", t.getStatus(),
                "fineAmount", t.getFineAmount(),
                "daysBorrowed", days,
                "overdue", overdueDays > 0
        );
    }
}
