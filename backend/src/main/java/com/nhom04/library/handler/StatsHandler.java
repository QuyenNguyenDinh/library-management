package com.nhom04.library.handler;
import com.nhom04.library.service.StatsService;
import com.nhom04.library.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/stats")
public class StatsHandler {
    private final StatsService service;
    public StatsHandler(StatsService service) { this.service=service; }
    @GetMapping public ResponseEntity<ApiResponse<Map<String,Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok("Lấy thống kê thành công.", service.getStats()));
    }
}
