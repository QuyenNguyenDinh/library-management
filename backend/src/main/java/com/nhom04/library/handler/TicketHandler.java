package com.nhom04.library.handler;

import com.nhom04.library.service.TicketQueryService;
import com.nhom04.library.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketHandler {
    private final TicketQueryService service;
    public TicketHandler(TicketQueryService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String readerId) {
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách phiếu mượn thành công.", service.getTickets(status, readerId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTicket(@PathVariable String id) {
        Map<String, Object> ticket = service.getTicketById(id);
        if (ticket == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy phiếu mượn: " + id));
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết phiếu mượn thành công.", ticket));
    }
}
