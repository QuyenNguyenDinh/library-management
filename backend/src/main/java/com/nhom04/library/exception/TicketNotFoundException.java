package com.nhom04.library.exception;

/**
 * Phiếu mượn có mã được yêu cầu không tồn tại.
 */
public class TicketNotFoundException extends NotFoundException {

    public TicketNotFoundException(String message) {
        super(message);
    }
}
