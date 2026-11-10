package com.nhom04.library.policy;

/**
 * Hành vi của một tài nguyên có thể được mượn trong thư viện (Abstraction).
 * <p>
 * {@code BorrowService} chỉ làm việc qua interface này, không phụ thuộc vào lớp cụ thể.
 * Lớp {@code model.Book} (TV3) cài đặt interface này; sau này nếu thư viện cho mượn thêm
 * tài nguyên khác (tạp chí, luận văn, ...) chỉ cần thêm lớp mới implement {@code Borrowable}.
 */
public interface Borrowable {

    /** @return true nếu số lượng hiện có lớn hơn 0 */
    boolean isAvailable();

    /** @return số lượng hiện có trong kho, sẵn sàng cho mượn */
    int getAvailableQuantity();

    /**
     * Giảm số lượng hiện có khi cho mượn.
     *
     * @param quantity số lượng mượn, phải lớn hơn 0 và không vượt quá số lượng hiện có
     */
    void borrowItem(int quantity);

    /**
     * Tăng lại số lượng hiện có khi bạn đọc trả sách.
     *
     * @param quantity số lượng trả, phải lớn hơn 0
     */
    void returnItem(int quantity);

    /**
     * Kiểm tra còn đủ số lượng để cho mượn hay không.
     *
     * @param quantity số lượng muốn mượn
     * @return true nếu quantity lớn hơn 0 và không vượt quá số lượng hiện có
     */
    default boolean hasEnough(int quantity) {
        return quantity > 0 && getAvailableQuantity() >= quantity;
    }
}
