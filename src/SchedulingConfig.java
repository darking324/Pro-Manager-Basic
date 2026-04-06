public final class SchedulingConfig {

    public static final int DAYS_PER_WEEK = 5;
    public static final int DAYS_DECREASE_PER_WEEK = 7;

    public static final double REVENUE_WEIGHT = 0.5;
    public static final double URGENCY_WEIGHT = 0.3;
    public static final double PENALTY_WEIGHT = 0.2;

    public static final int URGENCY_MULTIPLIER = 5000;
    public static final int LONG_DEADLINE_THRESHOLD_DAYS = 5;
    public static final double LOW_VALUE_REVENUE_MULTIPLIER = 0.5;

    public static final double MOST_RECENT_REVENUE_WEIGHT = 0.5;
    public static final double SECOND_RECENT_REVENUE_WEIGHT = 0.3;
    public static final double THIRD_RECENT_REVENUE_WEIGHT = 0.2;

    private SchedulingConfig() {
    }
}
