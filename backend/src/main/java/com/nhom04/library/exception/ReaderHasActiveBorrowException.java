package com.nhom04.library.exception;

/**
 * Ngoại lệ ném ra khi cố gắng xóa bạn đọc đang có sách mượn chưa trả.
 */
public class ReaderHasActiveBorrowException extends BusinessRuleException {

    public ReaderHasActiveBorrowException(String message) {
        super(message);
    }
}

