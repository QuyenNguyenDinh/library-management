package com.nhom04.library.model;

/**
 * Chi tiết một đầu sách trong phiếu mượn (một phiếu có thể mượn nhiều đầu sách).
 * <p>
 * Quy tắc "số lượng mượn phải lớn hơn 0" là business rule nên được kiểm tra ở
 * {@code BorrowService} để trả về thông báo lỗi cụ thể, không kiểm tra ở đây.
 */
public class BorrowTicketDetail {

    private String bookId;
    private String bookTitle;   // Lưu kèm tên sách để hiển thị phiếu mà không cần tra lại
    private int quantity;

    /** Constructor rỗng cho Gson. */
    public BorrowTicketDetail() {
    }

    public BorrowTicketDetail(String bookId, String bookTitle, int quantity) {
        setBookId(bookId);
        this.bookTitle = bookTitle;
        this.quantity = quantity;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        if (bookId == null || bookId.isBlank()) {
            throw new IllegalArgumentException("Mã sách trong phiếu mượn không được để trống.");
        }
        this.bookId = bookId.trim();
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return bookId + " x" + quantity;
    }
}
