package com.nhom04.library.exception;

/**
 * Phiếu mượn không ở trạng thái phù hợp với thao tác (ví dụ: trả lại phiếu đã trả).
 */
public class InvalidTicketStateException extends BusinessRuleException {

    public InvalidTicketStateException(String message) {
        super(message);
    }
}
