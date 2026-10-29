package model;

import policy.FinePolicy;

/**
 * Lớp trừu tượng Reader đại diện cho Bạn đọc thư viện.
 * Kế thừa từ User (Tính Kế thừa - Inheritance) và áp dụng Đóng gói (Encapsulation).
 * Chứa chính sách phạt tương ứng thông qua FinePolicy (Tính Đa hình - Polymorphism).
 */
public abstract class Reader extends User {

    private ReaderType readerType;
    private int maxBorrowLimit;
    private int currentBorrowCount;
    private FinePolicy finePolicy;

    public Reader() {
        super();
        this.currentBorrowCount = 0;
    }

    public Reader(String id, String name, String phone, ReaderType readerType, int maxBorrowLimit, FinePolicy finePolicy) {
        super(id, name, phone);
        this.readerType = readerType;
        this.maxBorrowLimit = maxBorrowLimit;
        this.currentBorrowCount = 0;
        this.finePolicy = finePolicy;
    }

    public Reader(String id, String name, String phone, ReaderType readerType, int maxBorrowLimit, int currentBorrowCount, FinePolicy finePolicy) {
        super(id, name, phone);
        this.readerType = readerType;
        this.maxBorrowLimit = maxBorrowLimit;
        this.currentBorrowCount = currentBorrowCount;
        this.finePolicy = finePolicy;
    }

    /**
     * Kiểm tra bạn đọc có đủ điều kiện mượn thêm số lượng sách chỉ định hay không.
     *
     * @param requestedQuantity Số lượng sách muốn mượn
     * @return true nếu số lượng hiện tại + số lượng muốn mượn <= hạn mức cho phép
     */
    public boolean canBorrow(int requestedQuantity) {
        return (this.currentBorrowCount + requestedQuantity) <= this.maxBorrowLimit;
    }

    /**
     * Tăng số lượng sách đang mượn của bạn đọc.
     *
     * @param quantity Số lượng mượn thêm
     */
    public void increaseBorrowCount(int quantity) {
        this.currentBorrowCount += quantity;
    }

    /**
     * Giảm số lượng sách đang mượn khi trả sách.
     *
     * @param quantity Số lượng trả lại
     */
    public void decreaseBorrowCount(int quantity) {
        this.currentBorrowCount = Math.max(0, this.currentBorrowCount - quantity);
    }

    /**
     * Tính tiền phạt bằng cách ủy quyền cho FinePolicy được gán cho loại bạn đọc này.
     *
     * @param overdueDays Số ngày quá hạn
     * @return Tiền phạt (VNĐ)
     */
    public double calculateFine(long overdueDays) {
        if (this.finePolicy == null) {
            return 0.0;
        }
        return this.finePolicy.calculateFine(overdueDays);
    }

    // Getters and Setters

    public ReaderType getReaderType() {
        return readerType;
    }

    public void setReaderType(ReaderType readerType) {
        this.readerType = readerType;
    }

    public int getMaxBorrowLimit() {
        return maxBorrowLimit;
    }

    public void setMaxBorrowLimit(int maxBorrowLimit) {
        this.maxBorrowLimit = maxBorrowLimit;
    }

    public int getCurrentBorrowCount() {
        return currentBorrowCount;
    }

    public void setCurrentBorrowCount(int currentBorrowCount) {
        this.currentBorrowCount = currentBorrowCount;
    }

    public FinePolicy getFinePolicy() {
        return finePolicy;
    }

    public void setFinePolicy(FinePolicy finePolicy) {
        this.finePolicy = finePolicy;
    }

    @Override
    public String toString() {
        return "Reader{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", phone='" + getPhone() + '\'' +
                ", readerType=" + readerType +
                ", maxBorrowLimit=" + maxBorrowLimit +
                ", currentBorrowCount=" + currentBorrowCount +
                ", finePolicy=" + (finePolicy != null ? finePolicy.getPolicyName() : "None") +
                '}';
    }
}

