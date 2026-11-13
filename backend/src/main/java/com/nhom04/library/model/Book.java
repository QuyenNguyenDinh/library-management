package com.nhom04.library.model;

import com.nhom04.library.exception.BookOutOfStockException;
import com.nhom04.library.policy.Borrowable;
import com.nhom04.library.utils.Validator;

public class Book implements Borrowable {

    private String id;
    private String title;
    private String author;
    private String category;
    private int availableQuantity;
    private double price;

    public Book() {
    }

    public Book(String id, String title, String author, String category,
                int availableQuantity, double price) {
        setId(id);
        setTitle(title);
        setAuthor(author);
        setCategory(category);
        setAvailableQuantity(availableQuantity);
        setPrice(price);
    }

    @Override
    public boolean isAvailable() {
        return availableQuantity > 0;
    }

    @Override
    public int getAvailableQuantity() {
        return availableQuantity;
    }

    @Override
    public void borrowItem(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng mượn phải lớn hơn 0.");
        }
        if (!hasEnough(quantity)) {
            throw new BookOutOfStockException(
                    "Sách '" + id + "' chỉ còn " + availableQuantity + " cuốn, không đủ để mượn " + quantity + " cuốn.");
        }
        this.availableQuantity -= quantity;
    }

    @Override
    public void returnItem(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng trả phải lớn hơn 0.");
        }
        this.availableQuantity += quantity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = Validator.requireNonBlank(id, "Mã sách");
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Validator.requireNonBlank(title, "Tên sách");
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = Validator.requireNonBlank(author, "Tác giả");
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = Validator.requireNonBlank(category, "Thể loại");
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = Validator.requireNonNegative(availableQuantity, "Số lượng hiện có");
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = Validator.requireNonNegative(price, "Giá sách");
    }
}