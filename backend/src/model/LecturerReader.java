package model;

import policy.LecturerFinePolicy;

/**
 * Lớp đại diện cho Bạn đọc là Giảng viên.
 * Hạn mức mượn tối đa: 10 cuốn sách.
 * Chính sách phạt trễ hạn: LecturerFinePolicy (2.000đ/ngày).
 */
public class LecturerReader extends Reader {

    public static final int DEFAULT_MAX_BORROW_LIMIT = 10;

    public LecturerReader() {
        super();
        setReaderType(ReaderType.LECTURER);
        setMaxBorrowLimit(DEFAULT_MAX_BORROW_LIMIT);
        setFinePolicy(new LecturerFinePolicy());
    }

    public LecturerReader(String id, String name, String phone) {
        super(id, name, phone, ReaderType.LECTURER, DEFAULT_MAX_BORROW_LIMIT, new LecturerFinePolicy());
    }

    public LecturerReader(String id, String name, String phone, int currentBorrowCount) {
        super(id, name, phone, ReaderType.LECTURER, DEFAULT_MAX_BORROW_LIMIT, currentBorrowCount, new LecturerFinePolicy());
    }
}

