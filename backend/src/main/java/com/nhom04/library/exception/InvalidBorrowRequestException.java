package com.nhom04.library.exception;

/**
 * Yêu cầu mượn sách không hợp lệ (danh sách rỗng, số lượng mượn nhỏ hơn hoặc bằng 0, ...).
 */
public class InvalidBorrowRequestException extends InvalidRequestException {

    public InvalidBorrowRequestException(String message) {
        super(message);
    }
}
