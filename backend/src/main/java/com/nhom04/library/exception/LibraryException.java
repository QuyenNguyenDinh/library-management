package com.nhom04.library.exception;

/**
 * Lớp cha của mọi ngoại lệ nghiệp vụ trong hệ thống thư viện.
 * <p>
 * Tầng service chỉ ném các ngoại lệ thuộc cây này và không biết gì về HTTP.
 * Tầng handler ({@code GlobalExceptionHandler}) dựa vào nhóm ngoại lệ để chọn mã HTTP:
 * <ul>
 *   <li>{@link NotFoundException} → 404</li>
 *   <li>{@link BusinessRuleException} → 409</li>
 *   <li>{@link InvalidRequestException} → 400</li>
 * </ul>
 * Thành viên nào cần ngoại lệ mới chỉ việc kế thừa đúng nhóm, không phải sửa handler.
 */
public abstract class LibraryException extends RuntimeException {

    protected LibraryException(String message) {
        super(message);
    }
}
