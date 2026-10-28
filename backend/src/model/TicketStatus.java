package model;

/**
 * Định nghĩa trạng thái của Phiếu mượn.
 */
public enum TicketStatus {
    BORROWING("Đang mượn"),
    RETURNED("Đã trả");

    private final String description;

    TicketStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
