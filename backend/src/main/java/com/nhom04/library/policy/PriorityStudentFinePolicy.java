package com.nhom04.library.policy;

public class PriorityStudentFinePolicy extends DailyRateFinePolicy {
    // Quy ước chưa ghi đơn giá sinh viên ưu tiên -> tạm chọn 3.000đ/ngày (giữa 5.000 và 2.000). Hỏi nhóm nếu cần đổi.
    public static final double DAILY_RATE = 3000;

    @Override
    public double getDailyRate() {
        return DAILY_RATE;
    }

    @Override
    protected String getGroupName() {
        return "Sinh viên ưu tiên";
    }
}
