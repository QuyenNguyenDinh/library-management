package com.nhom04.library.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.nhom04.library.policy.FinePolicy;

/**
 * Lớp trừu tượng Reader đại diện cho Bạn đọc thư viện.
 * Kế thừa từ {@link User} và áp dụng tính Đa hình (Polymorphism) qua {@link FinePolicy}.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = StudentReader.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StudentReader.class, name = "STUDENT"),
        @JsonSubTypes.Type(value = PriorityStudentReader.class, name = "PRIORITY_STUDENT"),
        @JsonSubTypes.Type(value = LecturerReader.class, name = "LECTURER")
})
public abstract class Reader extends User {

    private int currentBorrowCount = 0;

    /** Constructor rỗng cho Gson / Jackson. */
    public Reader() {
        super();
    }

    public Reader(String fullName, String phone) {
        super(fullName, phone);
    }

    public Reader(String id, String fullName, String phone) {
        super(id, fullName, phone);
    }

    public Reader(String id, String fullName, String phone, int currentBorrowCount) {
        super(id, fullName, phone);
        setCurrentBorrowCount(currentBorrowCount);
    }

    /**
     * Hạn mức số lượng sách được mượn tối đa: 3 (SV thường) / 5 (SV ưu tiên) / 10 (Giảng viên).
     */
    public abstract int getMaxBorrowLimit();

    /**
     * Chính sách phạt tương ứng khi trả sách trễ hạn.
     */
    public abstract FinePolicy getFinePolicy();

    /**
     * Phân loại bạn đọc (STUDENT / PRIORITY_STUDENT / LECTURER).
     */
    public abstract ReaderType getType();

    /**
     * Kiểm tra bạn đọc có đủ hạn mức để mượn thêm số lượng sách chỉ định hay không.
     */
    public boolean canBorrow(int requestedQuantity) {
        if (requestedQuantity <= 0) {
            return false;
        }
        return (this.currentBorrowCount + requestedQuantity) <= getMaxBorrowLimit();
    }

    /**
     * Tính tiền phạt theo số ngày trễ hạn bằng cách ủy quyền cho chính sách phạt tương ứng.
     */
    public double calculateFine(long overdueDays) {
        FinePolicy policy = getFinePolicy();
        return policy != null ? policy.calculateFine(overdueDays) : 0.0;
    }

    public int getCurrentBorrowCount() {
        return currentBorrowCount;
    }

    public void setCurrentBorrowCount(int currentBorrowCount) {
        if (currentBorrowCount < 0) {
            throw new IllegalArgumentException("Số lượng sách đang mượn không được âm.");
        }
        this.currentBorrowCount = currentBorrowCount;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id='" + getId() + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", phone='" + getPhone() + '\'' +
                ", type=" + getType() +
                ", maxBorrowLimit=" + getMaxBorrowLimit() +
                ", currentBorrowCount=" + currentBorrowCount +
                ", finePolicy=" + (getFinePolicy() != null ? getFinePolicy().getPolicyName() : "None") +
                '}';
    }
}
