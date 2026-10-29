package model;

import policy.PriorityStudentFinePolicy;

/**
 * Lớp đại diện cho Bạn đọc là Sinh viên ưu tiên.
 * Hạn mức mượn tối đa: 5 cuốn sách.
 * Chính sách phạt trễ hạn: PriorityStudentFinePolicy (3.000đ/ngày).
 */
public class PriorityStudentReader extends Reader {

    public static final int DEFAULT_MAX_BORROW_LIMIT = 5;

    public PriorityStudentReader() {
        super();
        setReaderType(ReaderType.PRIORITY_STUDENT);
        setMaxBorrowLimit(DEFAULT_MAX_BORROW_LIMIT);
        setFinePolicy(new PriorityStudentFinePolicy());
    }

    public PriorityStudentReader(String id, String name, String phone) {
        super(id, name, phone, ReaderType.PRIORITY_STUDENT, DEFAULT_MAX_BORROW_LIMIT, new PriorityStudentFinePolicy());
    }

    public PriorityStudentReader(String id, String name, String phone, int currentBorrowCount) {
        super(id, name, phone, ReaderType.PRIORITY_STUDENT, DEFAULT_MAX_BORROW_LIMIT, currentBorrowCount, new PriorityStudentFinePolicy());
    }
}

