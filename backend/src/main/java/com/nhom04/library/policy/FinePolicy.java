package com.nhom04.library.policy;

/** Chính sách tính tiền phạt trả trễ (TV4 - Đạt). */
public interface FinePolicy {
    /** overdueDays <= 0 -> 0. */
    double calculateFine(long overdueDays);

    double getDailyRate();

    String getPolicyName();
}
