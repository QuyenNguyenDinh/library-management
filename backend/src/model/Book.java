package model;

import policy.Borrowable;

/**
 * Lớp Book đại diện cho Sách trong thư viện.
 * Triển khai interface Borrowable mô tả hành vi mượn/trả sách.
 * Áp dụng tính chất Đóng gói (Encapsulation) với các thuộc tính private.
 */
public class Book implements Borrowable {

    private String id;
    private String title;
    private String author;
    private String category;
    private int quantity;
    private double price;

    public Book() {
    }

    public Book(String id, String title, String author, String category, int quantity, double price) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
    }

    @Override
    public boolean isAvailable() {
        return this.quantity > 0;
    }

    @Override
    public int getAvailableQuantity() {
        return this.quantity;
    }

    @Override
    public void borrowItem(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng mượn phải lớn hơn 0!");
        }
        if (this.quantity < quantity) {
            throw new IllegalStateException("Số lượng sách trong kho không đủ để mượn!");
        }
        this.quantity -= quantity;
    }

    @Override
    public void returnItem(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng trả phải lớn hơn 0!");
        }
        this.quantity += quantity;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", category='" + category + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}

