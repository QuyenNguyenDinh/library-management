package service;

import model.Admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service xử lý xác thực Đăng nhập Thủ thư với mật khẩu được mã hóa băm SHA-256.
 * Thuộc phạm vi chức năng [Thêm] của Trưởng nhóm.
 */
public class AuthService {

    private final Map<String, Admin> adminStore = new HashMap<>();

    public AuthService() {
        // Khởi tạo một tài khoản thủ thư mặc định: admin / admin123
        String hashedPass = hashPassword("admin123");
        adminStore.put("admin", new Admin("admin", hashedPass, "Thủ thư Quản trị"));
    }

    /**
     * Xác thực tài khoản đăng nhập thủ thư.
     *
     * @param username    Tên đăng nhập
     * @param rawPassword Mật khẩu chưa mã hóa
     * @return true nếu thông tin chính xác, false nếu sai
     */
    public boolean authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            return false;
        }

        Admin admin = adminStore.get(username.trim());
        if (admin == null) {
            return false;
        }

        String inputHash = hashPassword(rawPassword);
        return admin.getPasswordHash().equals(inputHash);
    }

    public Admin getAdmin(String username) {
        if (username == null) return null;
        return adminStore.get(username.trim());
    }

    /**
     * Mã hóa mật khẩu sử dụng thuật toán băm bảo mật SHA-256.
     *
     * @param password Mật khẩu gốc
     * @return Chuỗi Hex đại diện cho mã băm SHA-256
     */
    public static String hashPassword(String password) {
        if (password == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi hệ thống mã hóa: " + e.getMessage(), e);
        }
    }
}
