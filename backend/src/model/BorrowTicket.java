package model;

import java.time.LocalDate;

/**
 * Lớp BorrowTicket đại diện cho Phiếu mượn sách trong thư viện.
 * Áp dụng tính chất Đóng gói (Encapsulation) với tất cả thuộc tính private.
 */
public class BorrowTicket {

    private String id;
    private String readerId;
    private String bookId;
    private int quantity;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private TicketStatus status;
    private double fineAmount;

    public BorrowTicket() {
        this.status = TicketStatus.BORROWING;
        this.fineAmount = 0.0;
    }

    public BorrowTicket(String id, String readerId, String bookId, int quantity, LocalDate borrowDate, LocalDate dueDate) {
        this.id = id;
        this.readerId = readerId;
        this.bookId = bookId;
        this.quantity = quantity;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = null;
        this.status = TicketStatus.BORROWING;
        this.fineAmount = 0.0;
    }

    public BorrowTicket(String id, String readerId, String bookId, int quantity, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate, TicketStatus status, double fineAmount) {
        this.id = id;
        this.readerId = readerId;
        this.bookId = bookId;
        this.quantity = quantity;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
        this.fineAmount = fineAmount;
    }

    public boolean isBorrowing() {
        return this.status == TicketStatus.BORROWING;
    }

    public boolean isReturned() {
        return this.status == TicketStatus.RETURNED;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReaderId() {
        return readerId;
    }

    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDate borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    @Override
    public String toString() {
        return "BorrowTicket{" +
                "id='" + id + '\'' +
                ", readerId='" + readerId + '\'' +
                ", bookId='" + bookId + '\'' +
                ", quantity=" + quantity +
                ", borrowDate=" + borrowDate +
                ", dueDate=" + dueDate +
                ", returnDate=" + returnDate +
                ", status=" + status +
                ", fineAmount=" + fineAmount +
                '}';
    }
}
