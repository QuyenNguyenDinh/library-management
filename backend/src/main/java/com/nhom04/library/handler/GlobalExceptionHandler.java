package com.nhom04.library.handler;

import com.nhom04.library.exception.BusinessRuleException;
import com.nhom04.library.exception.InvalidRequestException;
import com.nhom04.library.exception.NotFoundException;
import com.nhom04.library.utils.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.format.DateTimeParseException;

/**
 * Bắt mọi ngoại lệ ném ra từ các handler và chuyển thành HTTP response dạng {@link ApiResponse}.
 * <p>
 * Nhờ lớp này, các handler khác không cần viết try-catch: chỉ cần gọi service,
 * nếu service ném ngoại lệ thì lớp này sẽ trả về mã lỗi và thông báo phù hợp.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Không tìm thấy sách / bạn đọc / phiếu mượn → 404. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Vi phạm business rule (hết sách, vượt hạn mức, phiếu đã trả, ...) → 409. */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRule(BusinessRuleException e) {
        return build(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Dữ liệu đầu vào sai (nghiệp vụ hoặc setter trong model báo lỗi) → 400. */
    @ExceptionHandler({InvalidRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(RuntimeException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Body JSON sai cú pháp, sai kiểu tham số hoặc sai định dạng ngày (yyyy-MM-dd) → 400. */
    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            DateTimeParseException.class})
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception e) {
        return build(HttpStatus.BAD_REQUEST,
                "Dữ liệu gửi lên không đúng định dạng (ngày phải có dạng yyyy-MM-dd, số lượng phải là số).");
    }

    /** Frontend gọi sai đường dẫn API → 404. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoEndpoint(NoResourceFoundException e) {
        return build(HttpStatus.NOT_FOUND, "Endpoint không tồn tại: /" + e.getResourcePath());
    }

    /** Frontend gọi đúng đường dẫn nhưng sai method (ví dụ GET thay vì POST) → 405. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleWrongMethod(HttpRequestMethodNotSupportedException e) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "Phương thức " + e.getMethod() + " không được hỗ trợ cho endpoint này.");
    }

    /** Lỗi không lường trước (lỗi đọc/ghi file, bug, ...) → 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Lỗi không mong muốn", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi máy chủ: " + e.getMessage());
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }
}
