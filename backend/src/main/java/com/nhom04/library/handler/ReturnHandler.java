package com.nhom04.library.handler;

import com.nhom04.library.model.ReturnRecord;
import com.nhom04.library.service.ReturnService;
import com.nhom04.library.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * API trả sách.
 * <pre>
 * POST /api/return/{ticketId}
 * { "returnDate": "2026-11-19" }     // không bắt buộc (có thể bỏ cả body), mặc định hôm nay
 *
 * GET  /api/return-records           // lịch sử trả sách
 * </pre>
 * Thành công trả 200. Lỗi do {@link GlobalExceptionHandler} chuyển thành 400 / 404 / 409.
 */
@RestController
@RequestMapping("/api")
public class ReturnHandler {

    private final ReturnService returnService;

    public ReturnHandler(ReturnService returnService) {
        this.returnService = returnService;
    }

    @PostMapping("/return/{ticketId}")
    public ResponseEntity<ApiResponse<ReturnRecord>> returnBook(
            @PathVariable String ticketId,
            @RequestBody(required = false) ReturnRequest request) {
        LocalDate returnDate = request != null ? request.returnDate() : null;
        ReturnRecord record = returnService.returnBook(ticketId, returnDate);
        return ResponseEntity.ok(ApiResponse.ok("Trả phiếu " + record.getTicketId() + " thành công.", record));
    }

    @GetMapping("/return-records")
    public ResponseEntity<ApiResponse<List<ReturnRecord>>> getReturnRecords() {
        return ResponseEntity.ok(ApiResponse.ok("Lấy lịch sử trả sách thành công", returnService.getAllReturnRecords()));
    }

    /** Body của yêu cầu trả sách. */
    public record ReturnRequest(LocalDate returnDate) {
    }
}
