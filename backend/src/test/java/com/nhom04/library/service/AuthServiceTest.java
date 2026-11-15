package com.nhom04.library.service;

import com.nhom04.library.model.Admin;
import com.nhom04.library.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị cho {@link AuthService}.
 * Tuân thủ quy định: sử dụng @TempDir để không ghi vào thư mục data thật.
 */
class AuthServiceTest {

    @TempDir
    Path tempDir;

    private AdminRepository adminRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        String dataDir = tempDir.toString();
        adminRepository = new AdminRepository(dataDir);
        authService = new AuthService(adminRepository);

        // Khởi tạo tài khoản thủ thư mẫu: admin / admin123
        String hashedPass = authService.hashPassword("admin123");
        adminRepository.save(new Admin("AD001", "Thủ thư chính", "0900000000", "admin", hashedPass));
    }

    @Test
    @DisplayName("Băm SHA-256 chuỗi 'admin123' trả về đúng chuỗi hex chuẩn trong hợp đồng nhóm")
    void testHashPassword_MatchesContract() {
        String expectedHash = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
        String actualHash = authService.hashPassword("admin123");
        assertEquals(expectedHash, actualHash);
    }

    @Test
    @DisplayName("Băm mật khẩu null ném IllegalArgumentException")
    void testHashPassword_NullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> authService.hashPassword(null));
    }

    @Test
    @DisplayName("Đăng nhập thành công với tài khoản và mật khẩu chính xác")
    void testLogin_Success() {
        Optional<Admin> result = authService.login("admin", "admin123");
        assertTrue(result.isPresent());
        assertEquals("admin", result.get().getUsername());
        assertEquals("Thủ thư chính", result.get().getFullName());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu")
    void testLogin_WrongPassword() {
        Optional<Admin> result = authService.login("admin", "wrong_password");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tên đăng nhập không tồn tại")
    void testLogin_NonExistentUser() {
        Optional<Admin> result = authService.login("unknown_admin", "admin123");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tên đăng nhập hoặc mật khẩu rỗng/null")
    void testLogin_BlankOrNullInputs() {
        assertTrue(authService.login(null, "admin123").isEmpty());
        assertTrue(authService.login("admin", null).isEmpty());
        assertTrue(authService.login("", "admin123").isEmpty());
        assertTrue(authService.login("admin", "  ").isEmpty());
    }
}

