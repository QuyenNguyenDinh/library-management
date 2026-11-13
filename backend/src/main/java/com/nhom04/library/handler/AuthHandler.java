package com.nhom04.library.handler;

import com.nhom04.library.model.Admin;
import com.nhom04.library.service.AuthService;
import com.nhom04.library.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * REST Controller tiếp nhận và xử lý yêu cầu Đăng nhập của Thủ thư.
 * Endpoint: POST /api/auth/login
 */
@RestController
@RequestMapping("/api/auth")
public class AuthHandler {

    private final AuthService authService;

    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminResponse>> login(@RequestBody LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Tên đăng nhập hoặc mật khẩu không chính xác."));
        }

        Optional<Admin> adminOpt = authService.login(request.getUsername(), request.getPassword());
        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Tên đăng nhập hoặc mật khẩu không chính xác."));
        }

        Admin admin = adminOpt.get();
        AdminResponse responseData = new AdminResponse(
                admin.getId(),
                admin.getUsername(),
                admin.getFullName(),
                admin.getPhone()
        );

        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công.", responseData));
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public LoginRequest() {
        }

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class AdminResponse {
        private final String id;
        private final String username;
        private final String fullName;
        private final String phone;

        public AdminResponse(String id, String username, String fullName, String phone) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.phone = phone;
        }

        public String getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getFullName() {
            return fullName;
        }

        public String getPhone() {
            return phone;
        }
    }
}

