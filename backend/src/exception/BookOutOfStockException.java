package exception;

/**
 * Ngoại lệ ném ra khi sách trong kho đã hết hoặc không đủ số lượng để mượn.
 */
public class BookOutOfStockException extends LibraryException {

    public BookOutOfStockException(String message) {
        super(message);
    }
}
