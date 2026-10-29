package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp BorrowTicket đại diện cho Phiếu mượn sách trong thư viện.
 * Hỗ trợ cả mượn 1 cuốn hoặc 1 phiếu mượn nhiều sách (List<BorrowTicketDetail>).
 */
public class BorrowTicket {

    private String id;
    private String readerId;
    private String bookId; // Giữ để tương thích mượn 1 sách
    private int quantity;  // Tổng số lượng sách mượn trong phiếu
    private List<BorrowTicketDetail> details; // Danh sách chi tiết các sách được mượn
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private TicketStatus status;
    private double fineAmount;

    public BorrowTicket() {
        this.status = TicketStatus.BORROWING;
        this.fineAmount = 0.0;
        this.details = new ArrayList<>();
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
        this.details = new ArrayList<>();
        if (bookId != null && quantity > 0) {
            this.details.add(new BorrowTicketDetail(bookId, quantity));
        }
    }

    public BorrowTicket(String id, String readerId, List<BorrowTicketDetail> details, LocalDate borrowDate, LocalDate dueDate) {
        this.id = id;
        this.readerId = readerId;
        this.details = details != null ? new ArrayList<>(details) : new ArrayList<>();
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = null;
        this.status = TicketStatus.BORROWING;
        this.fineAmount = 0.0;
        
        // Tính tổng số lượng và đặt bookId đại diện
        int sum = 0;
        for (BorrowTicketDetail d : this.details) {
            sum += d.getQuantity();
        }
        this.quantity = sum;
        if (!this.details.isEmpty()) {
            this.bookId = this.details.get(0).getBookId();
        }
    }

    public void addDetail(BorrowTicketDetail detail) {
        if (detail != null) {
            this.details.add(detail);
            this.quantity += detail.getQuantity();
            if (this.bookId == null) {
                this.bookId = detail.getBookId();
            }
        }
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

    public List<BorrowTicketDetail> getDetails() {
        return details;
    }

    public void setDetails(List<BorrowTicketDetail> details) {
        this.details = details != null ? details : new ArrayList<>();
        int sum = 0;
        for (BorrowTicketDetail d : this.details) {
            sum += d.getQuantity();
        }
        this.quantity = sum;
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
                ", detailsCount=" + (details != null ? details.size() : 0) +
                ", borrowDate=" + borrowDate +
                ", dueDate=" + dueDate +
                ", returnDate=" + returnDate +
                ", status=" + status +
                ", fineAmount=" + fineAmount +
                '}';
    }
}
