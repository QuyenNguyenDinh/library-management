package exception;

/**
 * Ngoại lệ ném ra khi bạn đọc yêu cầu mượn vượt quá hạn mức tối đa cho phép.
 */
public class BorrowLimitExceededException extends LibraryException {

    public BorrowLimitExceededException(String message) {
        super(message);
    }
}
