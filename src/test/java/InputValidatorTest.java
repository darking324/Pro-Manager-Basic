import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InputValidatorTest {

    @Test
    void requireNonBlankReturnsTrimmedValue() {
        String value = InputValidator.requireNonBlank("title", "  Demo Project  ");

        assertEquals("Demo Project", value);
    }

    @Test
    void requireNonBlankThrowsForNullValue() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> InputValidator.requireNonBlank("name", null)
        );

        assertEquals("Name cannot be empty.", ex.getMessage());
    }

    @Test
    void requireNonBlankUsesDefaultLabelForBlankFieldName() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> InputValidator.requireNonBlank("   ", " ")
        );

        assertEquals("Value cannot be empty.", ex.getMessage());
    }

    @Test
    void requirePositiveReturnsInputForPositiveNumber() {
        int value = InputValidator.requirePositive("revenue", 100);

        assertEquals(100, value);
    }

    @Test
    void requirePositiveThrowsForNonPositiveNumber() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> InputValidator.requirePositive("deadline", 0)
        );

        assertEquals("Deadline must be greater than zero.", ex.getMessage());
    }
}
