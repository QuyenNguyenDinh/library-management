package com.nhom04.library.model;

/**
 * Đại diện cho Tài khoản Thủ thư / Quản trị viên hệ thống.
 * Kế thừa từ {@link User}, lưu trữ mật khẩu dưới dạng mã băm SHA-256.
 */
public class Admin extends User {

    private String username;
    private String passwordHash;

    /** Constructor rỗng cho Gson / Jackson. */
    public Admin() {
        super();
    }

    public Admin(String id, String fullName, String phone, String username, String passwordHash) {
        super(id, fullName, phone);
        setUsername(username);
        setPasswordHash(passwordHash);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
        }
        this.username = username.trim();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu băm không được để trống.");
        }
        this.passwordHash = passwordHash.trim();
    }

    @Override
    public String toString() {
        return "Admin{" +
                "id='" + getId() + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", phone='" + getPhone() + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}

