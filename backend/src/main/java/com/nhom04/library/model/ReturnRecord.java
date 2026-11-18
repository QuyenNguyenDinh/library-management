package com.nhom04.library.model;

import java.time.LocalDate;

/**
 * Bản ghi lịch sử một lần trả sách (mã RR001, RR002, ...).
 * Lưu kèm số ngày trễ, tiền phạt và tên chính sách đã áp dụng để xem lại sau này.
 */
public class ReturnRecord {

    private String id;
    private String ticketId;
    private String readerId;
    private LocalDate returnDate;
    private long overdueDays;
    private double fineAmount;
    private String policyName;

    /** Constructor rỗng cho Gson. */
    public ReturnRecord() {
    }

    public ReturnRecord(String id, String ticketId, String readerId, LocalDate returnDate,
                        long overdueDays, double fineAmount, String policyName) {
        setId(id);
        setTicketId(ticketId);
        setReaderId(readerId);
        setReturnDate(returnDate);
        setOverdueDays(overdueDays);
        setFineAmount(fineAmount);
        setPolicyName(policyName);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Mã bản ghi trả sách không được để trống.");
        }
        this.id = id.trim();
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("Mã phiếu mượn không được để trống.");
        }
        this.ticketId = ticketId.trim();
    }

    public String getReaderId() {
        return readerId;
    }

    public void setReaderId(String readerId) {
        if (readerId == null || readerId.isBlank()) {
            throw new IllegalArgumentException("Mã bạn đọc không được để trống.");
        }
        this.readerId = readerId.trim();
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        if (returnDate == null) {
            throw new IllegalArgumentException("Ngày trả không được để trống.");
        }
        this.returnDate = returnDate;
    }

    public long getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(long overdueDays) {
        if (overdueDays < 0) {
            throw new IllegalArgumentException("Số ngày trễ không được âm.");
        }
        this.overdueDays = overdueDays;
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

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        if (policyName == null || policyName.isBlank()) {
            throw new IllegalArgumentException("Tên chính sách phạt không được để trống.");
        }
        this.policyName = policyName;
    }

    @Override
    public String toString() {
        return "ReturnRecord{" + id + ", ticket=" + ticketId + ", reader=" + readerId
                + ", " + returnDate + ", trễ " + overdueDays + " ngày, phạt " + fineAmount + "}";
    }
}
