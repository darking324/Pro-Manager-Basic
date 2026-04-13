import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectTest {

    @Test
    void constructorInitializesAllFields() {
        Project project = new Project(1, "P100", "Alpha", 5, 2000, "PENDING");

        assertEquals(1, project.getId());
        assertEquals("P100", project.getProjectCode());
        assertEquals("Alpha", project.getTitle());
        assertEquals(5, project.getDeadline());
        assertEquals(2000, project.getRevenue());
        assertEquals("PENDING", project.getStatus());
        assertEquals(0.0, project.getScore());
        assertEquals(0, project.getWeeksPending());
    }

    @Test
    void settersUpdateMutableFields() {
        Project project = new Project(2, "P101", "Beta", 7, 1000, "PENDING");

        project.setDeadline(3);
        project.setScore(42.5);
        project.setStatus("SCHEDULED");
        project.setWeeksPending(2);

        assertEquals(3, project.getDeadline());
        assertEquals(42.5, project.getScore());
        assertEquals("SCHEDULED", project.getStatus());
        assertEquals(2, project.getWeeksPending());
    }

    @Test
    void toStringIncludesCoreFields() {
        Project project = new Project(9, "P500", "Gamma", 4, 1500, "PENDING");

        String result = project.toString();

        assertTrue(result.contains("ID=9"));
        assertTrue(result.contains("Code='P500'"));
        assertTrue(result.contains("Title='Gamma'"));
        assertTrue(result.contains("Deadline=4"));
        assertTrue(result.contains("Revenue=1500"));
        assertTrue(result.contains("Status='PENDING'"));
    }
}
