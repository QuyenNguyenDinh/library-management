package com.nhom04.library.exception;

/**
 * Ngoại lệ ném ra khi tạo bạn đọc với mã đã tồn tại trong hệ thống.
 */
public class DuplicateReaderException extends BusinessRuleException {

    public DuplicateReaderException(String message) {
        super(message);
    }
}

