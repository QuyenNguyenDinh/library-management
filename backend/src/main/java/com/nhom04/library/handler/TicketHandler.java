
package com.nhom04.library.handler;

import com.nhom04.library.service.TicketQueryService;
import com.nhom04.library.utils.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketHandler {

    private final TicketQueryService service;

    public TicketHandler(TicketQueryService service) {
        this.service = service;
    }

    // GET /api/tickets?status=BORROWING&readerId=RD001
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String readerId) {

        List<Map<String, Object>> tickets =
                service.getTickets(status, readerId);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Lấy danh sách phiếu mượn thành công.",
                        tickets
                )
        );
    }

    // GET /api/tickets/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTicket(
            @PathVariable String id) {

        Map<String, Object> ticket = service.getTicketById(id);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Lấy chi tiết phiếu mượn thành công.",
                        ticket
                )
        );
    }
}
