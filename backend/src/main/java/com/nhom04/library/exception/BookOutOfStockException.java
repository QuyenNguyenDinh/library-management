package com.nhom04.library.exception;

/**
 * Sách không còn đủ số lượng hiện có để cho mượn.
 */
public class BookOutOfStockException extends BusinessRuleException {

    public BookOutOfStockException(String message) {
        super(message);
    }
}
