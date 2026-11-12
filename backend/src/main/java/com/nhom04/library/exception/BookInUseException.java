package com.nhom04.library.exception;

public class BookInUseException extends BusinessRuleException {
    public BookInUseException(String message) {
        super(message);
    }
}
