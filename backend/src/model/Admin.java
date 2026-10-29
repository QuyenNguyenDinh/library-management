package model;

/**
 * Lớp đại diện cho Tài khoản Thủ thư / Quản trị viên hệ thống.
 * Mật khẩu được lưu trữ dưới dạng băm mã hóa (SHA-256).
 */
public class Admin {

    private String username;
    private String passwordHash;
    private String fullName;

    public Admin() {
    }

    public Admin(String username, String passwordHash, String fullName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                '}';
    }
}

