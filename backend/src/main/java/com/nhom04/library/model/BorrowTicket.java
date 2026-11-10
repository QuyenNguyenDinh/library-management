package com.nhom04.library.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Phiếu mượn sách: một bạn đọc mượn một hoặc nhiều đầu sách trong cùng một lần.
 * <p>
 * Lớp chỉ chứa dữ liệu và các phép tính đơn giản trên chính dữ liệu của nó
 * (tổng số lượng, quá hạn hay chưa). Logic mượn/trả nằm ở tầng service.
 */
public class BorrowTicket {

    private String id;
    private String readerId;
    private List<BorrowTicketDetail> details = new ArrayList<>();
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;          // null khi chưa trả
    private TicketStatus status = TicketStatus.BORROWING;
    private double fineAmount;             // 0 khi chưa trả hoặc trả đúng hạn

    /** Constructor rỗng cho Gson. */
    public BorrowTicket() {
    }

    /**
     * Tạo phiếu mượn mới ở trạng thái "Đang mượn".
     */
    public BorrowTicket(String id, String readerId, List<BorrowTicketDetail> details,
                        LocalDate borrowDate, LocalDate dueDate) {
        setId(id);
        setReaderId(readerId);
        setDetails(details);
        setBorrowDate(borrowDate);
        setDueDate(dueDate);
    }

    // ===================== Phép tính trên dữ liệu phiếu =====================

    /** @return tổng số cuốn sách trong phiếu (cộng số lượng tất cả các dòng chi tiết) */
    public int getTotalQuantity() {
        int total = 0;
        for (BorrowTicketDetail detail : details) {
            total += detail.getQuantity();
        }
        return total;
    }

    public boolean isBorrowing() {
        return status == TicketStatus.BORROWING;
    }

    /**
     * Phiếu được coi là quá hạn nếu đang mượn và ngày {@code today} đã sau ngày hẹn trả.
     */
    public boolean isOverdue(LocalDate today) {
        return isBorrowing() && today.isAfter(dueDate);
    }

    // ===================== Getter / Setter =====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Mã phiếu mượn không được để trống.");
        }
        this.id = id.trim();
    }

    public String getReaderId() {
        return readerId;
    }

    public void setReaderId(String readerId) {
        if (readerId == null || readerId.isBlank()) {
            throw new IllegalArgumentException("Mã bạn đọc trong phiếu mượn không được để trống.");
        }
        this.readerId = readerId.trim();
    }

    /** @return danh sách chi tiết chỉ đọc; muốn thay đổi phải dùng {@link #setDetails}. */
    public List<BorrowTicketDetail> getDetails() {
        return Collections.unmodifiableList(details);
    }

    public void setDetails(List<BorrowTicketDetail> details) {
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("Phiếu mượn phải có ít nhất một đầu sách.");
        }
        this.details = new ArrayList<>(details);
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDate borrowDate) {
        if (borrowDate == null) {
            throw new IllegalArgumentException("Ngày mượn không được để trống.");
        }
        this.borrowDate = borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            throw new IllegalArgumentException("Ngày hẹn trả không được để trống.");
        }
        if (borrowDate != null && dueDate.isBefore(borrowDate)) {
            throw new IllegalArgumentException("Ngày hẹn trả không được trước ngày mượn.");
        }
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        if (returnDate != null && borrowDate != null && returnDate.isBefore(borrowDate)) {
            throw new IllegalArgumentException("Ngày trả không được trước ngày mượn.");
        }
        this.returnDate = returnDate;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Trạng thái phiếu mượn không được để trống.");
        }
        this.status = status;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        if (fineAmount < 0) {
            throw new IllegalArgumentException("Tiền phạt không được âm.");
        }
        this.fineAmount = fineAmount;
    }

    @Override
    public String toString() {
        return "BorrowTicket{" + id + ", reader=" + readerId + ", " + details
                + ", " + borrowDate + " -> " + dueDate + ", " + status.getLabel() + "}";
    }
}
