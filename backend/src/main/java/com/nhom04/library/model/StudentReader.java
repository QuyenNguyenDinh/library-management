package com.nhom04.library.model;

import com.nhom04.library.policy.FinePolicy;
import com.nhom04.library.policy.NormalStudentFinePolicy;

/**
 * Đại diện cho Bạn đọc là Sinh viên thông thường.
 * Hạn mức mượn tối đa: 3 cuốn sách.
 * Chính sách phạt trễ hạn: {@link NormalStudentFinePolicy} (5.000đ/ngày).
 */
public class StudentReader extends Reader {

    public static final int MAX_BORROW_LIMIT = 3;
    private static final FinePolicy FINE_POLICY = new NormalStudentFinePolicy();

    public StudentReader() {
        super();
    }

    public StudentReader(String fullName, String phone) {
        super(fullName, phone);
    }

    public StudentReader(String id, String fullName, String phone) {
        super(id, fullName, phone);
    }

    public StudentReader(String id, String fullName, String phone, int currentBorrowCount) {
        super(id, fullName, phone, currentBorrowCount);
    }

    @Override
    public int getMaxBorrowLimit() {
        return MAX_BORROW_LIMIT;
    }

    @Override
    public FinePolicy getFinePolicy() {
        return FINE_POLICY;
    }

    @Override
    public ReaderType getType() {
        return ReaderType.STUDENT;
    }
}
