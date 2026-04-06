public final class InputValidator {

    private InputValidator() {
    }

    public static String requireNonBlank(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(capitalize(fieldName) + " cannot be empty.");
        }

        return value.trim();
    }

    public static int requirePositive(String fieldName, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(capitalize(fieldName) + " must be greater than zero.");
        }

        return value;
    }

    private static String capitalize(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return "Value";
        }

        String trimmed = fieldName.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }
}
