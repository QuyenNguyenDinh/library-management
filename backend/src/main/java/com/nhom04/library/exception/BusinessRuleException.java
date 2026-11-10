package com.nhom04.library.exception;

/**
 * Nhóm ngoại lệ: yêu cầu hợp lệ về dữ liệu nhưng vi phạm quy tắc nghiệp vụ (HTTP 409).
 */
public abstract class BusinessRuleException extends LibraryException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
