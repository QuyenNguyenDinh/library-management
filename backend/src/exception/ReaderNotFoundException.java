package exception;

/**
 * Ngoại lệ ném ra khi không tìm thấy bạn đọc theo mã yêu cầu.
 */
public class ReaderNotFoundException extends LibraryException {

    public ReaderNotFoundException(String message) {
        super(message);
    }
}
