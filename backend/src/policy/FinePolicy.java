package policy;

/**
 * Interface FinePolicy định nghĩa hợp đồng tính phí phạt khi trả sách quá hạn.
 * Áp dụng nguyên lý Trừu tượng (Abstraction) và Đa hình (Polymorphism).
 */
public interface FinePolicy {

    /**
     * Tính toán số tiền phạt dựa trên số ngày trễ hạn.
     *
     * @param overdueDays Số ngày trả trễ (>= 0)
     * @return Số tiền phạt tương ứng tính bằng VNĐ
     */
    double calculateFine(long overdueDays);

    /**
     * Lấy đơn giá phạt theo ngày.
     *
     * @return Đơn giá phạt (VNĐ/ngày)
     */
    double getDailyRate();

    /**
     * Lấy tên mô tả chính sách phạt.
     *
     * @return Tên chính sách
     */
    String getPolicyName();
}

