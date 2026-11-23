package com.nhom04.library.exception;

public class InvalidTicketStatusException extends InvalidRequestException {

    public InvalidTicketStatusException(String message) {
        super(message);
    }
}