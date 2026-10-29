package exception;

/**
 * Ngoại lệ ném ra khi không tìm thấy sách theo mã yêu cầu.
 */
public class BookNotFoundException extends LibraryException {

    public BookNotFoundException(String message) {
        super(message);
    }
}

