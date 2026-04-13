import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectStatusTest {

    @Test
    void isValidReturnsTrueForExactEnumValue() {
        assertTrue(ProjectStatus.isValid("PENDING"));
    }

    @Test
    void isValidReturnsTrueForTrimmedCaseInsensitiveValue() {
        assertTrue(ProjectStatus.isValid("  completed  "));
    }

    @Test
    void isValidReturnsFalseForNullOrBlank() {
        assertFalse(ProjectStatus.isValid(null));
        assertFalse(ProjectStatus.isValid("   "));
    }

    @Test
    void isValidReturnsFalseForUnknownValue() {
        assertFalse(ProjectStatus.isValid("ARCHIVED"));
    }
}
