package com.nhom04.library.utils;

/**
 * Định dạng response thống nhất cho mọi API:
 * <pre>
 * { "success": true,  "message": "...", "data": { ... } }
 * { "success": false, "message": "Lý do lỗi cụ thể", "data": null }
 * </pre>
 *
 * @param <T> kiểu dữ liệu trả về trong trường {@code data}
 */
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /** Response thành công kèm dữ liệu. */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /** Response thành công chỉ có dữ liệu. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Thành công", data);
    }

    /** Response lỗi kèm thông báo nguyên nhân. */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
