package exception;

/**
 * Exception cha cho tất cả các ngoại lệ nghiệp vụ trong hệ thống thư viện.
 */
public class LibraryException extends RuntimeException {

    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
