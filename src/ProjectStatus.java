public enum ProjectStatus {
    PENDING,
    SCHEDULED,
    COMPLETED,
    EXPIRED;

    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        for (ProjectStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return true;
            }
        }

        return false;
    }
}
