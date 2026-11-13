package com.nhom04.library.model;

import com.nhom04.library.policy.FinePolicy;
import com.nhom04.library.policy.LecturerFinePolicy;

/**
 * Đại diện cho Bạn đọc là Giảng viên.
 * Hạn mức mượn tối đa: 10 cuốn sách.
 * Chính sách phạt trễ hạn: {@link LecturerFinePolicy} (2.000đ/ngày).
 */
public class LecturerReader extends Reader {

    public static final int MAX_BORROW_LIMIT = 10;
    private static final FinePolicy FINE_POLICY = new LecturerFinePolicy();

    public LecturerReader() {
        super();
    }

    public LecturerReader(String fullName, String phone) {
        super(fullName, phone);
    }

    public LecturerReader(String id, String fullName, String phone) {
        super(id, fullName, phone);
    }

    public LecturerReader(String id, String fullName, String phone, int currentBorrowCount) {
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
        return ReaderType.LECTURER;
    }
}
