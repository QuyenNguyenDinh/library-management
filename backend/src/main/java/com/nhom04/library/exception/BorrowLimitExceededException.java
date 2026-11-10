package com.nhom04.library.exception;

/**
 * Bạn đọc mượn vượt quá số lượng sách tối đa theo loại bạn đọc.
 */
public class BorrowLimitExceededException extends BusinessRuleException {

    public BorrowLimitExceededException(String message) {
        super(message);
    }
}
