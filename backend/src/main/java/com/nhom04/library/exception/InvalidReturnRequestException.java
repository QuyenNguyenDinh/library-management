package com.nhom04.library.exception;

/**
 * Yêu cầu trả sách không hợp lệ (thiếu mã phiếu, ngày trả trước ngày mượn, ...).
 */
public class InvalidReturnRequestException extends InvalidRequestException {

    public InvalidReturnRequestException(String message) {
        super(message);
    }
}
