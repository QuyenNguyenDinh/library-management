package com.nhom04.library.model;

import com.nhom04.library.policy.FinePolicy;
import com.nhom04.library.policy.PriorityStudentFinePolicy;

/**
 * Đại diện cho Bạn đọc là Sinh viên ưu tiên.
 * Hạn mức mượn tối đa: 5 cuốn sách.
 * Chính sách phạt trễ hạn: {@link PriorityStudentFinePolicy} (3.000đ/ngày).
 */
public class PriorityStudentReader extends Reader {

    public static final int MAX_BORROW_LIMIT = 5;
    private static final FinePolicy FINE_POLICY = new PriorityStudentFinePolicy();

    public PriorityStudentReader() {
        super();
    }

    public PriorityStudentReader(String fullName, String phone) {
        super(fullName, phone);
    }

    public PriorityStudentReader(String id, String fullName, String phone) {
        super(id, fullName, phone);
    }

    public PriorityStudentReader(String id, String fullName, String phone, int currentBorrowCount) {
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
        return ReaderType.PRIORITY_STUDENT;
    }
}
