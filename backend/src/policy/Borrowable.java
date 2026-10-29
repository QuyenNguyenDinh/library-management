package policy;

/**
 * Interface Borrowable mô tả hành vi của các thực thể có thể được mượn trong hệ thống thư viện.
 * Áp dụng nguyên lý Trừu tượng (Abstraction).
 */
public interface Borrowable {

    /**
     * Kiểm tra xem thực thể có sẵn sàng để mượn không (còn số lượng trong kho).
     *
     * @return true nếu còn sách trong kho (> 0), ngược lại false
     */
    boolean isAvailable();

    /**
     * Lấy số lượng hiện có trong kho sẵn sàng phục vụ mượn.
     *
     * @return Số lượng khả dụng
     */
    int getAvailableQuantity();

    /**
     * Thực hiện thao tác mượn tài nguyên (giảm số lượng tồn kho).
     *
     * @param quantity Số lượng yêu cầu mượn (> 0)
     */
    void borrowItem(int quantity);

    /**
     * Thực hiện thao tác trả lại tài nguyên (tăng số lượng tồn kho).
     *
     * @param quantity Số lượng trả lại kho (> 0)
     */
    void returnItem(int quantity);
}

