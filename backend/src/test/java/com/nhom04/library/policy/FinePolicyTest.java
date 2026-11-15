package com.nhom04.library.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FinePolicyTest {

    @Test
    void normalStudent_late4Days_is20000() {
        FinePolicy policy = new NormalStudentFinePolicy();
        assertEquals(20000, policy.calculateFine(4));
        assertEquals("Sinh viên thường (5.000đ/ngày)", policy.getPolicyName());
    }

    @Test
    void lecturer_late4Days_is8000() {
        assertEquals(8000, new LecturerFinePolicy().calculateFine(4));
    }

    @Test
    void priorityStudent_usesOwnRate() {
        FinePolicy policy = new PriorityStudentFinePolicy();
        assertEquals(3000, policy.getDailyRate());
        assertEquals(9000, policy.calculateFine(3));
    }

    @Test
    void polymorphism_sameDaysDifferentFine() {
        FinePolicy[] policies = {new NormalStudentFinePolicy(), new PriorityStudentFinePolicy(), new LecturerFinePolicy()};
        double[] expected = {50000, 30000, 20000};
        for (int i = 0; i < policies.length; i++) {
            assertEquals(expected[i], policies[i].calculateFine(10));
        }
    }

    @Test
    void notOverdue_isZero() {
        FinePolicy policy = new NormalStudentFinePolicy();
        assertEquals(0, policy.calculateFine(0));
        assertEquals(0, policy.calculateFine(-3));
    }
}
