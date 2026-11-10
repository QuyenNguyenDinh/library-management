package com.nhom04.library.exception;

/**
 * Nhóm ngoại lệ: đối tượng được yêu cầu không tồn tại (HTTP 404).
 */
public abstract class NotFoundException extends LibraryException {

    public NotFoundException(String message) {
        super(message);
    }
}
