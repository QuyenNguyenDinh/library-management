package com.nhom04.library.exception;

/**
 * Nhóm ngoại lệ: dữ liệu đầu vào không hợp lệ (HTTP 400).
 */
public abstract class InvalidRequestException extends LibraryException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
