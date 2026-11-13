package com.nhom04.library.model;

/**
 * Lớp trừu tượng User đại diện cho người dùng chung trong hệ thống thư viện.
 * Áp dụng tính chất Đóng gói (Encapsulation) và Kế thừa (Inheritance).
 */
public abstract class User {

    private String id;
    private String fullName;
    private String phone;

    /** Constructor rỗng cho Gson/Jackson. */
    public User() {
    }

    public User(String fullName, String phone) {
        setFullName(fullName);
        setPhone(phone);
    }

    public User(String id, String fullName, String phone) {
        if (id != null && !id.isBlank()) {
            setId(id);
        }
        setFullName(fullName);
        setPhone(phone);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Mã người dùng không được để trống.");
        }
        this.id = id.trim();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ và tên không được để trống.");
        }
        this.fullName = fullName.trim();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }
        this.phone = phone.trim();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
