package com.nhom04.library.service;

import com.nhom04.library.model.Admin;
import com.nhom04.library.repository.AdminRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ xác thực đăng nhập của Thủ thư với mật khẩu băm SHA-256.
 */
@Service
public class AuthService {

    private final AdminRepository adminRepository;

    public AuthService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    /**
     * Xác thực thông tin đăng nhập của Thủ thư.
     *
     * @param username    Tên đăng nhập
     * @param rawPassword Mật khẩu chưa mã hóa
     * @return {@link Optional} chứa thông tin {@link Admin} nếu đúng, hoặc empty nếu sai
     */
    public Optional<Admin> login(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return Optional.empty();
        }

        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isEmpty()) {
            return Optional.empty();
        }

        Admin admin = adminOpt.get();
        String hashedInput = hashPassword(rawPassword);
        if (hashedInput.equalsIgnoreCase(admin.getPasswordHash())) {
            return Optional.of(admin);
        }

        return Optional.empty();
    }

    /**
     * Băm chuỗi mật khẩu sang định dạng Hex SHA-256.
     *
     * @param password Mật khẩu gốc dạng plain text
     * @return Chuỗi Hex đại diện cho mã băm SHA-256
     */
    public String hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Mật khẩu không được để null.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Thuật toán mã hóa SHA-256 không khả dụng: " + e.getMessage(), e);
        }
    }
}

