package policy;

/**
 * Chính sách phạt dành cho Sinh viên thông thường: 5.000 VNĐ / ngày trễ.
 */
public class NormalStudentFinePolicy implements FinePolicy {

    private static final double DAILY_RATE = 5000.0;
    private static final String POLICY_NAME = "Chính sách phạt Sinh viên thường (5.000đ/ngày)";

    @Override
    public double calculateFine(long overdueDays) {
        if (overdueDays <= 0) {
            return 0.0;
        }
        return overdueDays * DAILY_RATE;
    }

    @Override
    public double getDailyRate() {
        return DAILY_RATE;
    }

    @Override
    public String getPolicyName() {
        return POLICY_NAME;
    }

    @Override
    public String toString() {
        return POLICY_NAME;
    }
}
