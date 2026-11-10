package com.nhom04.library.model;

/**
 * Trạng thái của phiếu mượn.
 */
public enum TicketStatus {
    BORROWING("Đang mượn"),
    RETURNED("Đã trả");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    /** @return tên tiếng Việt để hiển thị trên giao diện */
    public String getLabel() {
        return label;
    }
}
