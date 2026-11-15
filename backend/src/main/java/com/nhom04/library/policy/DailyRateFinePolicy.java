package com.nhom04.library.policy;

import java.text.NumberFormat;
import java.util.Locale;

/** Phạt theo công thức: số ngày trễ x đơn giá mỗi ngày. Lớp con chỉ cần cho đơn giá và tên nhóm. */
public abstract class DailyRateFinePolicy implements FinePolicy {

    @Override
    public double calculateFine(long overdueDays) {
        if (overdueDays <= 0) {
            return 0;
        }
        return overdueDays * getDailyRate();
    }

    /** Tên nhóm bạn đọc, ví dụ "Sinh viên thường". */
    protected abstract String getGroupName();

    @Override
    public String getPolicyName() {
        String rate = NumberFormat.getIntegerInstance(Locale.GERMANY).format((long) getDailyRate());
        return getGroupName() + " (" + rate + "đ/ngày)";
    }
}
