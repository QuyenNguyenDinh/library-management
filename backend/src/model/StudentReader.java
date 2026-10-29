package model;

import policy.NormalStudentFinePolicy;

/**
 * Lớp đại diện cho Bạn đọc là Sinh viên thông thường.
 * Hạn mức mượn tối đa: 3 cuốn sách.
 * Chính sách phạt trễ hạn: NormalStudentFinePolicy (5.000đ/ngày).
 */
public class StudentReader extends Reader {

    public static final int DEFAULT_MAX_BORROW_LIMIT = 3;

    public StudentReader() {
        super();
        setReaderType(ReaderType.NORMAL_STUDENT);
        setMaxBorrowLimit(DEFAULT_MAX_BORROW_LIMIT);
        setFinePolicy(new NormalStudentFinePolicy());
    }

    public StudentReader(String id, String name, String phone) {
        super(id, name, phone, ReaderType.NORMAL_STUDENT, DEFAULT_MAX_BORROW_LIMIT, new NormalStudentFinePolicy());
    }

    public StudentReader(String id, String name, String phone, int currentBorrowCount) {
        super(id, name, phone, ReaderType.NORMAL_STUDENT, DEFAULT_MAX_BORROW_LIMIT, currentBorrowCount, new NormalStudentFinePolicy());
    }
}

