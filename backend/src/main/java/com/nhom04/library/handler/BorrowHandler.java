package com.nhom04.library.handler;

import com.nhom04.library.model.BorrowTicket;
import com.nhom04.library.model.BorrowTicketDetail;
import com.nhom04.library.service.BorrowService;
import com.nhom04.library.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * API mượn sách.
 * <pre>
 * POST /api/borrow
 * { "readerId": "RD001",
 *   "items": [ { "bookId": "BK001", "quantity": 1 } ],
 *   "borrowDate": "2026-11-13",   // không bắt buộc, mặc định hôm nay
 *   "dueDate": "2026-11-27" }     // không bắt buộc, mặc định ngày mượn + 14 ngày
 * </pre>
 * Thành công trả 201 kèm phiếu mượn. Lỗi do {@link GlobalExceptionHandler} chuyển thành 400 / 404 / 409.
 */
@RestController
@RequestMapping("/api/borrow")
public class BorrowHandler {

    private final BorrowService borrowService;

    public BorrowHandler(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BorrowTicket>> borrow(@RequestBody BorrowRequest request) {
        BorrowTicket ticket = borrowService.borrow(
                request.readerId(), request.toDetails(), request.borrowDate(), request.dueDate());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Lập phiếu mượn " + ticket.getId() + " thành công.", ticket));
    }

    /** Body của yêu cầu mượn sách. */
    public record BorrowRequest(String readerId, List<BorrowItem> items,
                                LocalDate borrowDate, LocalDate dueDate) {

        /** Chuyển danh sách dòng yêu cầu sang chi tiết phiếu; việc kiểm tra hợp lệ do service đảm nhận. */
        List<BorrowTicketDetail> toDetails() {
            if (items == null) {
                return null;
            }
            List<BorrowTicketDetail> details = new ArrayList<>();
            for (BorrowItem item : items) {
                if (item == null) {
                    details.add(null);
                    continue;
                }
                BorrowTicketDetail detail = new BorrowTicketDetail();
                if (item.bookId() != null && !item.bookId().isBlank()) {
                    detail.setBookId(item.bookId());
                }
                detail.setQuantity(item.quantity());
                details.add(detail);
            }
            return details;
        }
    }

    /** Một dòng trong yêu cầu mượn: mã sách và số lượng. */
    public record BorrowItem(String bookId, int quantity) {
    }
}
