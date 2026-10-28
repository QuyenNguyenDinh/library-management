package exception;

/**
 * Ngoại lệ ném ra khi không tìm thấy phiếu mượn theo mã yêu cầu.
 */
public class TicketNotFoundException extends LibraryException {

    public TicketNotFoundException(String message) {
        super(message);
    }
}
