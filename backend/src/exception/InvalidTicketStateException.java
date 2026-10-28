package exception;

/**
 * Ngoại lệ ném ra khi cố gắng thao tác (ví dụ trả sách) trên phiếu mượn có trạng thái không hợp lệ
 * (ví dụ: phiếu đã ở trạng thái "Đã trả").
 */
public class InvalidTicketStateException extends LibraryException {

    public InvalidTicketStateException(String message) {
        super(message);
    }
}
