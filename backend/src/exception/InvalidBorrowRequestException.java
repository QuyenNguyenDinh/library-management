package exception;

/**
 * Ngoại lệ ném ra khi yêu cầu mượn không hợp lệ (ví dụ: số lượng mượn <= 0, ngày mượn không hợp lệ...).
 */
public class InvalidBorrowRequestException extends LibraryException {

    public InvalidBorrowRequestException(String message) {
        super(message);
    }
}
