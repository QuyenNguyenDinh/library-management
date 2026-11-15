package com.nhom04.library.policy;

public class LecturerFinePolicy extends DailyRateFinePolicy {
    public static final double DAILY_RATE = 2000;

    @Override
    public double getDailyRate() {
        return DAILY_RATE;
    }

    @Override
    protected String getGroupName() {
        return "Giảng viên";
    }
}
