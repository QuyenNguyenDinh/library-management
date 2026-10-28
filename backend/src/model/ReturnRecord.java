package model;

import java.time.LocalDate;

/**
 * Lớp ReturnRecord lưu trữ thông tin lịch sử trả sách và tiền phạt đã nộp.
 */
public class ReturnRecord {

    private String recordId;
    private String ticketId;
    private String readerId;
    private String bookId;
    private int quantity;
    private LocalDate returnDate;
    private long overdueDays;
    private double fineAmount;
    private String note;

    public ReturnRecord() {
    }

    public ReturnRecord(String recordId, String ticketId, String readerId, String bookId, int quantity, LocalDate returnDate, long overdueDays, double fineAmount, String note) {
        this.recordId = recordId;
        this.ticketId = ticketId;
        this.readerId = readerId;
        this.bookId = bookId;
        this.quantity = quantity;
        this.returnDate = returnDate;
        this.overdueDays = overdueDays;
        this.fineAmount = fineAmount;
        this.note = note;
    }

    // Getters and Setters

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
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

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public long getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(long overdueDays) {
        this.overdueDays = overdueDays;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "ReturnRecord{" +
                "recordId='" + recordId + '\'' +
                ", ticketId='" + ticketId + '\'' +
                ", readerId='" + readerId + '\'' +
                ", bookId='" + bookId + '\'' +
                ", quantity=" + quantity +
                ", returnDate=" + returnDate +
                ", overdueDays=" + overdueDays +
                ", fineAmount=" + fineAmount +
                ", note='" + note + '\'' +
                '}';
    }
}
