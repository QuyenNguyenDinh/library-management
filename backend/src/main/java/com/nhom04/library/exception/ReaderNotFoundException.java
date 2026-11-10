package com.nhom04.library.exception;

/**
 * Bạn đọc có mã được yêu cầu không tồn tại.
 */
public class ReaderNotFoundException extends NotFoundException {

    public ReaderNotFoundException(String message) {
        super(message);
    }
}
