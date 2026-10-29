package model;

/**
 * Lớp BorrowTicketDetail đại diện cho chi tiết một đầu sách trong phiếu mượn.
 * Hỗ trợ nghiệp vụ "Một phiếu mượn nhiều sách" (Mục Nên).
 */
public class BorrowTicketDetail {

    private String bookId;
    private String bookTitle;
    private int quantity;
    private double price;

    public BorrowTicketDetail() {
    }

    public BorrowTicketDetail(String bookId, int quantity) {
        this.bookId = bookId;
        this.quantity = quantity;
    }

    public BorrowTicketDetail(String bookId, String bookTitle, int quantity, double price) {
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.quantity = quantity;
        this.price = price;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "BorrowTicketDetail{" +
                "bookId='" + bookId + '\'' +
                ", bookTitle='" + bookTitle + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}

