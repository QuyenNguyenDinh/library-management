package com.nhom04.library.policy;

public class NormalStudentFinePolicy extends DailyRateFinePolicy {
    public static final double DAILY_RATE = 5000;

    @Override
    public double getDailyRate() {
        return DAILY_RATE;
    }

    @Override
    protected String getGroupName() {
        return "Sinh viên thường";
    }
}
