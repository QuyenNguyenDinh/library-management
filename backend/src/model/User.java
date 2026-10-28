package model;

/**
 * Lớp User đại diện cho người dùng chung trong hệ thống.
 * Áp dụng tính chất Đóng gói (Encapsulation) với các thuộc tính private.
 */
public class User {

    private String id;
    private String name;
    private String phone;

    public User() {
    }

    public User(String id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
