package com.nhom04.library.exception;

/**
 * Sách có mã được yêu cầu không tồn tại.
 */
public class BookNotFoundException extends NotFoundException {

    public BookNotFoundException(String message) {
        super(message);
    }
}
